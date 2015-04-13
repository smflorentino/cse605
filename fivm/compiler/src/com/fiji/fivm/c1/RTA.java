/*
 * RTA.java
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

public /* FIXME */ abstract class RTA extends TypeBasedAnalysis {
    // results
    
    LinkedHashSet< Type > instantiatedTypes=new LinkedHashSet< Type >();

    LinkedHashSet< VisibleClass > instantiatedClasses=new LinkedHashSet< VisibleClass >();
    
    // either this type or one of its subtypes have been instantiated
    LinkedHashSet< VisibleClass > subInstantiated=new LinkedHashSet< VisibleClass >();
    
    HashSet< VisibleMethod > knownDynables=new HashSet< VisibleMethod >();
    
    LinkedHashSet< VisibleMethod > directCalled=new LinkedHashSet< VisibleMethod >();

    LinkedHashSet< VisibleMethod > dynCalled=new LinkedHashSet< VisibleMethod >();
    
    LinkedHashSet< VisibleMethod > executed=new LinkedHashSet< VisibleMethod >();
    
    // FIXME: what about used types?
    
    // constraints
    
    HashSet< VisibleClass > loaded=new HashSet< VisibleClass >();
    
    TwoWayMap< VisibleMethod, VisibleMethod > directCalls=
        new TwoWayMap< VisibleMethod, VisibleMethod >();
    
    TwoWayMap< VisibleMethod, VisibleMethod > dynCalls=
        new TwoWayMap< VisibleMethod, VisibleMethod >();
    
    TwoWayMap< VisibleMethod, Type > instantiations=
        new TwoWayMap< VisibleMethod, Type >();
    
    TwoWayMap< VisibleMethod, VisibleClass > initializations=
        new TwoWayMap< VisibleMethod, VisibleClass >();
    
    // worklist
    
    LinkedHashSet< VisibleMethod > toExecute=new LinkedHashSet< VisibleMethod >();
    
    // finalized results
    
    LinkedHashSet< VisibleField > fields=new LinkedHashSet< VisibleField >();
    
    // stuffs
    
    CodeRepo repo;
    RootsRepo rootsRepo;
    
    public RTA(CodeRepo repo,
               RootsRepo rootsRepo) {
        this.repo=repo;
        this.rootsRepo=rootsRepo;
        
        if (Global.verbosity>=1) {
            Global.log.println("Running RTA.");
        }
        
        long before=System.currentTimeMillis();
        
        reflect(rootsRepo.rootRoots());
        
        if (Global.verbosity>=1) {
            Global.log.println(
                "Roots set up, performing fixpoint.");
        }
        
        boolean changed=true;
        for (int cnt=1;changed;++cnt) {
            long innerBefore=System.currentTimeMillis();

            changed=false;
            
            if (!toExecute.isEmpty()) {
                for (VisibleMethod vm : toExecute) {
                    execute(vm);
                }
                toExecute.clear();
                changed=true;
            }
            
            long innerAfter=System.currentTimeMillis();
            if (Global.verbosity>=1) {
                Global.log.println("Fixpoint iteration "+cnt+" completed in "+(innerAfter-innerBefore)+" ms");
            }
        }
        
        if (Global.verbosity>=1) {
            Global.log.println("Fixpoint complete, post-processing results.");
        }
        
        // FIXME
        
        if (Global.verbosity>=1) {
            Global.log.println("RTA took "+(System.currentTimeMillis()-before)+" ms");
        }
    }
    
    boolean directCall(VisibleMethod vm) {
        if (directCalled.add(vm)) {
            repo.willWant(vm.getClazz());
            if ((vm.isStatic() || vm.isInitializer() || !vm.hasObjectReceiver() ||
                 subInstantiated.contains(vm.getClazz())) &&
                !executed.contains(vm)) {
                toExecute.add(vm);
            }
            return true;
        } else {
            return false;
        }
    }
    
    boolean dynCall(VisibleMethod vm) {
        if (dynCalled.add(vm)) {
            repo.willWant(vm.getClazz());
            if ((vm.isStatic() || vm.isInitializer() || !vm.hasObjectReceiver() ||
                 knownDynables.contains(vm)) &&
                !executed.contains(vm)) {
                toExecute.add(vm);
            }
            return true;
        } else {
            return false;
        }
    }
    
    boolean instantiate(Type t) {
        if (instantiatedTypes.add(t)) {
            VisibleClass vc=t.effectiveClass();
            if (instantiatedClasses.add(vc)) {
                for (VisibleClass cur=vc;
                     cur!=null && !subInstantiated.contains(cur);
                     cur=cur.getSuperclass()) {
                    subInstantiated.add(cur);
                    for (VisibleMethod vm : cur.methods()) {
                        if (directCalled.contains(vm) && !executed.contains(vm)) {
                            toExecute.add(vm);
                        }
                    }
                }
                knownDynables.addAll(vc.allExactDynableMethods());
                for (VisibleMethod vm : vc.allExactDynableMethods()) {
                    if (dynCalled.contains(vm) && !executed.contains(vm)) {
                        toExecute.add(vm);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    boolean execute(VisibleMethod vm) {
        boolean result=false;
        load(vm.getClazz());
        for (VisibleMethod trg : directCalls.valuesForKey(vm)) {
            result|=directCall(trg);
        }
        for (VisibleMethod trg : dynCalls.valuesForKey(vm)) {
            result|=dynCall(trg);
        }
        for (Type t : instantiations.valuesForKey(vm)) {
            result|=instantiate(t);
        }
        for (VisibleClass vc : initializations.valuesForKey(vm)) {
            for (VisibleMethod trg : vc.relevantStaticInits()) {
                result|=directCall(trg);
            }
        }
        result|=reflect(rootsRepo.rootsFor(vm));
        return result;
    }
    
    void load(VisibleClass vc) {
        if (loaded.contains(vc)) {
            return;
        }
        
        // FIXME deal with static initializers
        
        for (Code c : repo.codeForClass(vc)) {
            VisibleMethod vm=c.method();
            if (vm!=null) {
                for (Header h : c.headers()) {
                    for (Instruction i : h.instructions()) {
                        switch (i.opcode()) {
                        case InvokeDynamic: 
                        case InvokeStatic:
                        case Invoke: {
                            MethodInst mi=(MethodInst)i;
                            if (mi.dynamicCall() &&
                                mi.method().hasObjectReceiver()) {
                                dynCalls.put(vm,mi.method());
                            } else {
                                directCalls.put(vm,mi.staticTarget());
                            }
                            break;
                        }
                        case New:
                        case NewArray:
                        case CastExact: {
                            TypeInst ti=(TypeInst)i;
                            instantiations.put(vm,ti.getType());
                            break;
                        }
                        case CheckInit: {
                            ClassInst ci=(ClassInst)i;
                            repo.willWant(ci.value());
                            initializations.put(vm,ci.value());
                            break;
                        }
                        default: break;
                        }
                    }
                }
            }
        }
        
        loaded.add(vc);
    }
    
    boolean reflect(Roots roots) {
        boolean changed=false;
        if (roots!=Roots.EMPTY) {
            for (VisibleMethod vm : roots.called()) {
                directCall(vm);
            }
            for (VisibleMethod vm : roots.dynCalled()) {
                dynCall(vm);
            }
            for (Type t : roots.alloced()) {
                instantiate(t);
            }
        }
        return changed;
    }
}

