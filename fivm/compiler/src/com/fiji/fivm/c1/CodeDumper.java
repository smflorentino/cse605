/*
 * CodeDumper.java
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

import java.io.*;
import java.util.*;

public class CodeDumper {
    
    private CodeDumper() {}
    
    public static enum Mode {
	NO_ANALYSES { boolean displayAnalyses() { return false; } },
	ANALYSES_IF_PRESENT,
	COMPUTE_ANALYSES { boolean computeAnalyses() { return true; } };
	
	boolean computeAnalyses() { return false; }
	boolean displayAnalyses() { return true; }
    }
    
    public static void dump(Code c,PrintWriter w) {
	dump(c,w,Mode.ANALYSES_IF_PRESENT);
    }

    static void dumpOpSet(PrintWriter w,
			  String label,
			  Set< ? extends Operation > set) {
	if (!set.isEmpty()) {
	    w.print(" ("+label+": ");
	    boolean first=true;
	    for (Operation o : set) {
		if (first) {
		    first=false;
		} else {
		    w.print(", ");
		}
		w.print(o.label());
	    }
	    w.print(")");
	}
    }
    
    public static void dump(Code c,PrintWriter w,Mode mode) {
	c.recomputeOrder();
	
        try {
            Throwable rethrow=null;
	
            if (mode.computeAnalyses()) {
                try {
                    c.killPreds();
                    c.getPreds();
                    c.killSimpleLiveness();
                    c.getSimpleLiveness();
                    c.killLoops();
                    c.getLoops();
                    if (c.hasPhi()) {
                        c.killLocalMustAlias();
                        c.getLocalMustAlias();
                    }
                } catch (Throwable e) {
                    w.println("Caught "+e+" while doing analysis on "+c+"; dumping as-is.");
                    rethrow=e;
                }
            }
	
            w.print("Code for ");
            if (c.method()==null) {
                w.print("<anonymous>");
            } else {
                w.print(c.method());
            }
            w.println(" owned by "+c.getOwner());
            w.println("Result: "+c.result());
            w.println("Params: "+Util.dump(c.params()));
            w.print("Vars: ");
            
            boolean first=true;
            for (Var v : c.vars()) {
                if (first) {
                    first=false;
                } else {
                    w.print(", ");
                }
                w.print(""+v);
                if (v.inst()!=null) {
                    w.print(" -> "+v.inst());
                } else if (v.isMultiAssigned()) {
                    w.print(" -> MULTI");
                }
            }
            w.println();
            
            for (Header h : c.headersRootFirst()) {
                w.println(h+": \t"+h.di());
                if (mode.displayAnalyses() && c.predsCalc!=null) {
                    w.println("Preds: "+Util.dump(c.predsCalc.normalPredecessors(h)));
                    if (!c.predsCalc.exceptionalPredecessors(h).isEmpty()) {
                        w.println("Exc Preds: "+
                                  Util.dump(c.predsCalc.exceptionalPredecessors(h)));
                    }
                }
                if (mode.displayAnalyses() && c.simpleLivenessCalc!=null) {
                    w.println("Live: "+Util.dump(c.simpleLivenessCalc.liveAtHead(h)));
                }
                if (mode.displayAnalyses() && c.localMustAliasCalc!=null) {
                    w.print("Must-alias: ");
                    TwoWayMap< Var, Instruction > map=c.localMustAliasCalc.aliasAtHead(h);
                    if (map==null) {
                        w.println("<dead>");
                    } else {
                        first=true;
                        for (Var v : map.keySet()) {
                            Set< Instruction > srcs=map.valuesForKey(v);
                            assert srcs.size()<=1;
                            if (srcs.size()==1) {
                                Instruction src=srcs.iterator().next();
                                if (first) {
                                    first=false;
                                } else {
                                    w.print(", ");
                                }
                                w.print(v+" -> "+src.label());
                            }
                        }
                        w.println();
                    }
                }
                if (mode.displayAnalyses() && c.loopCalc!=null) {
                    w.print("Loops: ");
                    first=true;
                    for (Header h2 : c.loopCalc.loopsFor(h)) {
                        if (first) {
                            first=false;
                        } else {
                            w.print(", ");
                        }
                        w.print(h2);
                    }
                    w.println();
                }
                for (Operation o : h.operations()) {
                    try {
                        w.print(o.label());
                    } catch (Throwable e) {
                        e.printStackTrace(w);
                        w.print("<NO LABEL>");
                    }
                    w.print(": "+o.toString());
                    if (mode.displayAnalyses() && c.monitorUseCalc!=null) {
                        if (c.monitorUseCalc.isMultiEnter(o)) {
                            w.print(" (multi-enter)");
                        }
                        if (c.monitorUseCalc.isEscapingEnter(o)) {
                            w.print(" (escaping-enter)");
                        }
                        dumpOpSet(w,"enclosing-enters",c.monitorUseCalc.enclosingEnters(o));
                        dumpOpSet(w,"nested-enters",c.monitorUseCalc.nestedEnters(o));
                        dumpOpSet(w,"matching-exits",c.monitorUseCalc.exitsForEnter(o));
                        dumpOpSet(w,"matching-enters",c.monitorUseCalc.entersForExit(o));
                    }
                    w.println();
                }
                LinkedList< Header > nds=h.getFooter().conditionalSuccessors();
                if (!nds.isEmpty()) {
                    w.print("Cnd Succ: ");
                    first=true;
                    for (Header h2 : nds) {
                        if (first) {
                            first=false;
                        } else {
                            w.print(", ");
                        }
                        w.print(h2);
                    }
                    w.println();
                }
                if (h.getFooter().next()!=null) {
                    w.println("Def Succ: "+h.getFooter().next());
                }
                if (h.handlers().iterator().hasNext()) {
                    w.print("Handler Stack: ");
                    first=true;
                    for (ExceptionHandler eh : h.handlers()) {
                        if (first) {
                            first=false;
                        } else {
                            w.print(" -> ");
                        }
                        w.print(eh+"("+eh.next()+")");
                    }
                    w.println();
                }
            }
	
            for (ExceptionHandler eh : c.handlers()) {
                w.print(eh+": Class = "+eh.handles());
                if (eh.dropsTo()!=null) {
                    w.print(", Drops to = "+eh.dropsTo());
                }
                w.println(", Succ: "+eh.next());
            }
	
            Util.rethrow(rethrow);
        } catch (Throwable e) {
            if (mode!=Mode.NO_ANALYSES) {
                w.println("Caught "+e+" while dumping with analyses:");
                e.printStackTrace(w);
                w.println("Redumping without analyses.");
                dump(c,w,Mode.NO_ANALYSES);
            }
        }
    }
    
}

