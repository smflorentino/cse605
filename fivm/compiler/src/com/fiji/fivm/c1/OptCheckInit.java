/*
 * OptCheckInit.java
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

import com.fiji.fivm.*;
import java.util.*;

public class OptCheckInit extends CodePhase {
    public OptCheckInit(Code c) { super(c); }
    
    public void visitCode() {
	// FIXME: we should have a global version of this pass.  or have some way of
	// identifying static initializers that are totally worthless.  java.lang.Math
	// is particularly retarded.  of course, we have to be careful, since some
	// of these initializers are actually required.  this isn't going to be fun.
	
	// there's always the self-modifying code idea...  generate CheckInit as an
	// assembly sequence that we magically blow away once the class is initialized.
	
	// it would be really great to have a profiling mechanism for the compiler.
	// some global repository of counters.  and we could squirt a piece of counter
	// increment code anywhere by just naming a counter.
	
	// NOTE: we now have such a profiling mechanism and it tells me beyond any
	// doubt that CheckInit checks have no effect on performance.
	
	// note we're going to be conservatively stupid for exception handlers.
	// that's fine I think - we're already killing off enough CheckInits as it
	// is.
	
	// 1) figure out which classes are initialized as a result of a
	//    successful execution of each basic block.

        HashMap< Header, HashSet< VisibleClass > > initedInside=
            new HashMap< Header, HashSet< VisibleClass > >();
	
	for (Header h : code.headers()) {
	    HashSet< VisibleClass > curInited=new HashSet< VisibleClass >();
	    for (Instruction i : h.instructions()) {
		if (i.opcode()==OpCode.CheckInit) {
		    for (VisibleClass c : ((ClassInst)i).value().allSupertypes()) {
			curInited.add(c);
		    }
		}
	    }
	    initedInside.put(h,curInited);
            if (Global.verbosity>=5) {
                Global.log.println("After "+h+" have inited "+curInited);
            }
	}
	
	// 2) figure out which classes are initialized at the heads of
	//    basic blocks by doing some flow analysis.
	
	HashMap< Header, HashSet< VisibleClass > > initedAtHead=
	    new HashMap< Header, HashSet< VisibleClass > >();
	HashMap< Header, HashSet< VisibleClass > > initedAtTail=
	    new HashMap< Header, HashSet< VisibleClass > >();
	
	Worklist worklist=new Worklist();
	
	worklist.push(code.root());
	
	HashSet< VisibleClass > initedAtRoot=new HashSet< VisibleClass >();
        if (!Settings.FULL_CHECK_INIT_SEMANTICS) {
            if (code.method()!=null) {
                for (VisibleClass c : code.method().getClazz().allSupertypes()) {
                    initedAtRoot.add(c);
                }
            }
        }
	initedAtHead.put(code.root(),initedAtRoot);
	
	while (!worklist.empty()) {
	    Header h=worklist.pop();

	    HashSet< VisibleClass > curAtHead=initedAtHead.get(h);
	    
	    for (Header h2 : h.exceptionalSuccessors()) {
		HashSet< VisibleClass > theirAtHead=initedAtHead.get(h2);
		if (theirAtHead==null) {
		    initedAtHead.put(h2,Util.copy(curAtHead));
                    if (Global.verbosity>=5) {
                        Global.log.println("At "+h+"->"+h2+" set inited (throw) to "+initedAtHead.get(h2));
                    }
		    worklist.push(h2);
		} else if (theirAtHead.retainAll(curAtHead)) {
                    if (Global.verbosity>=5) {
                        Global.log.println("At "+h+"->"+h2+" reduced inited (throw) to "+initedAtHead.get(h2));
                    }
		    worklist.push(h2);
		}
	    }

            HashSet< VisibleClass > cur=new HashSet< VisibleClass >();
            cur.addAll(curAtHead);
            cur.addAll(initedInside.get(h));
            
	    HashSet< VisibleClass > curAtTail=initedAtTail.get(h);
            if (curAtTail==null || !curAtTail.equals(cur)) {
                initedAtTail.put(h,cur);
                
                for (Header h2 : h.normalSuccessors()) {
                    HashSet< VisibleClass > theirAtHead=initedAtHead.get(h2);
                    if (theirAtHead==null) {
                        initedAtHead.put(h2,Util.copy(cur));
                        if (Global.verbosity>=5) {
                            Global.log.println("At "+h+"->"+h2+" set inited (normal) to "+initedAtHead.get(h2));
                        }
                        worklist.push(h2);
                    } else if (theirAtHead.retainAll(cur)) {
                        if (Global.verbosity>=5) {
                            Global.log.println("At "+h+"->"+h2+" reduced inited (normal) to "+initedAtHead.get(h2));
                        }
                        worklist.push(h2);
                    }
                }
            }
	}
	
	// 3) figure out which CheckInit calls are redundant, and remove
	//    them.
	
	for (Header h : code.headers()) {
	    HashSet< VisibleClass > curInited=initedAtHead.get(h);
            
            if (Global.verbosity>=5) {
                Global.log.println("At "+h+" have inited "+curInited);
            }
	    
            if (curInited!=null) {
                for (Instruction i : h.instructions()) {
                    if (i.opcode()==OpCode.CheckInit) {
                        if (!curInited.add(((ClassInst)i).value())) {
                            i.remove();
                            setChangedCode("Removed "+i.label()+" "+i);
                        }
                    }
                }
            } // else dead code
	}
	
	if (changedCode()) {
	    code.killAllAnalyses();
	}
    }
}


