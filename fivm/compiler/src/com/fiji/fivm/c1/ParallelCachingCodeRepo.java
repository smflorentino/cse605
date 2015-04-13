/*
 * ParallelCachingCodeRepo.java
 * Copyright 2008, 2009, 2010, 2011, 2012, 2013 Fiji Systems Inc.
 * This file is part of the FIJI VM Software licensed under the FIJI PUBLIC
 * LICENSE Version 3 or any later version.  A copy of the FIJI PUBLIC LICENSE is
 * available at fivm/LEGAL and can also be found at
 * http://www.fiji-systems.com/FPL3.txt
 * 
 * By installing, reproducing, distributing, and/or using the FIJI VM Software
 * you agree to the terms of the FIJI PUBLIC LICENSE.  You may exercise the
 * rights granted under the FIJI PUBLIC LICENSE subject to the conditions and
 * restrictions stated therein.  Among other conditions and restrictions, the
 * FIJI PUBLIC LICENSE states that:
 * 
 * a. You may only make non-commercial use of the FIJI VM Software.
 * 
 * b. Any adaptation you make must be licensed under the same terms 
 * of the FIJI PUBLIC LICENSE.
 * 
 * c. You must include a copy of the FIJI PUBLIC LICENSE in every copy of any
 * file, adaptation or output code that you distribute and cause the output code
 * to provide a notice of the FIJI PUBLIC LICENSE. 
 * 
 * d. You must not impose any additional conditions.
 * 
 * e. You must not assert or imply any connection, sponsorship or endorsement by
 * the author of the FIJI VM Software
 * 
 * f. You must take no derogatory action in relation to the FIJI VM Software
 * which would be prejudicial to the FIJI VM Software author's honor or
 * reputation.
 * 
 * 
 * The FIJI VM Software is provided as-is.  FIJI SYSTEMS INC does not make any
 * representation and provides no warranty of any kind concerning the software.
 * 
 * The FIJI PUBLIC LICENSE and any rights granted therein terminate
 * automatically upon any breach by you of the terms of the FIJI PUBLIC LICENSE.
 */

package com.fiji.fivm.c1;

import java.util.*;
import java.util.concurrent.*;
import com.fiji.fivm.config.Configuration;

public class ParallelCachingCodeRepo extends CodeRepo {
    ConcurrentHashMap< VisibleClass, ProcessedCode > cache=
        new ConcurrentHashMap< VisibleClass, ProcessedCode >();
    LinkedBlockingQueue< ProcessedCode > workQueue=
        new LinkedBlockingQueue< ProcessedCode >();
    boolean threadsShouldStop;
    ArrayList< HappyThread > threads;
    
    public ParallelCachingCodeRepo(Configuration config) {
        threads=new ArrayList< HappyThread >();
        for (int i=0;i<config.getJobs()-1;++i) {
            HappyThread t=new HappyThread(){
                    public void doStuff() throws Throwable {
                        if (Global.verbosity>=1) {
                            Global.log.println("PCCR thread running.");
                        }
                        while (!threadsShouldStop) {
                            ProcessedCode code=workQueue.take();
                            if (code==STOP) {
                                break;
                            }
                            code.processCode();
                        }
                    }
                };
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        
        List< String > classNames=config.getPreload();
        if (Global.verbosity>=1) {
            Global.log.println("preloading "+classNames.size()+" classes.");
        }
        for (String className : classNames) {
            VisibleClass klass=Global.root().tryGetClass(className);
            if (klass!=null) {
                willWant(klass);
            }
        }
    }
    
    public synchronized void stopAsynchrony() {
        try {
            if (threads!=null) {
                threadsShouldStop=true;
                for (int i=0;i<threads.size();++i) {
                    workQueue.offer(STOP);
                }
                for (Thread t : threads) {
                    t.join();
                }
                threads=null;
                workQueue=null;
            }
        } catch (Throwable e) {
            Util.rethrow(e);
        }
    }
    
    private ProcessedCode getCanonicalCodeForClass(VisibleClass klass) {
        ProcessedCode code=cache.get(klass);
        if (code==null) {
            code=new ProcessedCode(klass);
            ProcessedCode oldCode=cache.putIfAbsent(klass,code);
            if (oldCode!=null) {
                code=oldCode;
            }
        }
        return code;
    }

    public ArrayList< Code > codeForClass(VisibleClass klass,
                                          Set< MethodSignature > methods) {
        ProcessedCode code=getCanonicalCodeForClass(klass);
        synchronized (code) {
            return CachingCodeRepo.prepare(code.getCode(),
                                           methods,
                                           true /* make copies */,
                                           Global.analysisEpoch>code.analysisEpoch);
        }
    }
    
    private boolean tryEnqueue(VisibleClass klass) {
        ProcessedCode code=getCanonicalCodeForClass(klass);
        if (code.shouldEnqueue()) {
            workQueue.offer(code);
            return true;
        } else {
            return false;
        }
    }
    
    public void willWant(VisibleClass klass) {
        if (threads!=null &&
            klass.resolved() &&
            tryEnqueue(klass)) {
            for (VisibleClass k2 : klass.allSupertypes()) {
                tryEnqueue(k2);
            }
        }
    }
    
    static class ProcessedCode {
        VisibleClass klass;
        ArrayList< Code > code;
        int analysisEpoch;
        boolean enqueued;
        boolean done;
        
        ProcessedCode() {} // internal don't use
        
        ProcessedCode(VisibleClass klass) {
            this.klass=klass;
        }
        
        boolean shouldEnqueue() {
            // this is deliberately unsychronized even though it leads to a "data race",
            // or something.  if you fix it you will get Pizlenated.
            boolean result=!enqueued;
            enqueued=true;
            return result;
        }
        
        synchronized boolean processCode() {
            if (code==null) {
                code=Util.codeForClass(klass,null);
                analysisEpoch=Global.analysisEpoch();
                done=true;
                return true;
            } else {
                return false;
            }
        }
        
        ArrayList< Code > getCode() {
            if (!done && Global.verbosity>=1) {
                Global.log.println("requesting "+klass.getName()+" before it was ready.");
            }
            processCode();
            return code;
        }
    }
    
    private static final ProcessedCode STOP=new ProcessedCode();
}

