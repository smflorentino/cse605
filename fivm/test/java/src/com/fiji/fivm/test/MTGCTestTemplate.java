/*
 * MTGCTestTemplate.java
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

package com.fiji.fivm.test;

import com.fiji.util.*;
import java.util.*;

public class MTGCTestTemplate {
    int numThreads;
    int numMaps;
    int restartProbability;
    int topLevelResetProbability;
    int boxResetProbability;
    int maxSize;
    int deleteProbability;
    
    static class MapBox {
        Map< Object, Object > map;
        
        MapBox(Map< Object, Object > map) {
            this.map=map;
        }
    }
    
    MapBox[] maps;
    int[] ops;
    int threadsStarted;
    
    Map< Object, Object > makeMap(MarsenneTwister mt) {
        if ((mt.nextInt()%2)==0) {
            return new TreeMap< Object, Object >();
        } else {
            return new HashMap< Object, Object >();
        }
    }

    class Mutator implements Runnable {
        MarsenneTwister mt=new MarsenneTwister();
        Map< Object, Object > makeMap() {
            return MTGCTest1.makeMap(mt);
        }
        Object mutateMap(Map< Object, Object > map,
                         Object stuff) {
            String key=""+mt.nextInt();
            Util.ensure(map.size()<=maxSize);
            if (map.size()==maxSize ||
                (mt.nextInt()%deleteProbability)==0) {
                return map.remove(key);
            } else {
                map.put(key,stuff);
                return null;
            }
        }
        public void run() {
            try {
                Map< Object, Object > subMap=makeMap();
                for (;;) {
                    if (restartProbability!=0 && (mt.nextInt()%restartProbability)==0) {
                        synchronized (MTGCTest1.class) {
                            threadsStarted++;
                        }
                        new Thread(new Mutator()).start();
                        return;
                    } else {
                        int i=Math.abs(mt.nextInt()%maps.length);
                        ops[i]++; // unsynchronized but thats ok
                        if ((mt.nextInt()%topLevelResetProbability)==0) {
                            maps[i]=new MapBox(makeMap());
                        } else {
                            MapBox mb=maps[i];
                            synchronized (mb) {
                                if ((mt.nextInt()%boxResetProbability)==0) {
                                    mb.map=makeMap();
                                } else {
                                    subMap=(Map< Object, Object >)mutateMap(mb.map,subMap);
                                }
                            }
                        }
                    
                        if (subMap!=null) {
                            mutateMap(subMap,""+mt.nextInt());
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
    
    public void doit(long time) throws Exception {
        MarsenneTwister mt=new MarsenneTwister();
        maps=new MapBox[numMaps];
        ops=new int[numMaps];
        for (int i=0;i<numMaps;++i) {
            maps[i]=new MapBox(makeMap(mt));
        }
        new Thread() {
            public void run() {
                try {
                    for (;;) {
                        SimpleStats mapSize=new SimpleStats();
                        SimpleStats numOps=new SimpleStats();
                        for (int i=0;i<maps.length;++i) {
                            MapBox mb=maps[i];
                            synchronized (mb) {
                                mapSize.add(mb.map.size());
                            }
                            numOps.add(ops[i]);
                        }
                        System.out.println("Stats:");
                        System.out.println("   Map Sizes: "+mapSize);
                        System.out.println("   Num Ops:   "+numOps);
                        System.out.println("   Threads:   "+threadsStarted);
                        Thread.sleep(5000);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }.start();
        for (int i=0;i<numThreads;++i) {
            new Thread(new Mutator()).start();
        }
        Thread.sleep(time);
        System.out.println("We didn't crash.  Success.");
    }
}


