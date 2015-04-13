/*
 * CallGraph.java
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

import java.util.Set;
import java.io.IOException;

import com.fiji.fivm.om.OMClass;

public class CallGraph extends Code.Visitor {
    TwoWayMap< VisibleMethod, VisibleMethod > callEdges=
	new TwoWayMap< VisibleMethod, VisibleMethod >();
    TwoWayMap< VisibleMethod, VisibleMethod > execEdges=
	new TwoWayMap< VisibleMethod, VisibleMethod >();
    TwoWayMap< VisibleMethod, VisibleClass > classesUsed=
        new TwoWayMap< VisibleMethod, VisibleClass >();

    VisibleMethod curMethod;
    
    Visitor< Void > visitor=new Visitor< Void >(){
        public Void visit(MethodInst mi) {
            callEdges.put(curMethod,mi.method());
            execEdges.putMulti(curMethod,mi.possibleTargets());
            classesUsed.put(curMethod,mi.method().getClazz());
            return null;
        }
        public Void visit(ClassInst ci) {
            classesUsed.put(curMethod,ci.value());
            return null;
        }
        public Void visit(TypeInst ti) {
            if (ti.getType().hasEffectiveClass()) {
                classesUsed.put(curMethod,ti.getType().effectiveClass());
            }
            return null;
        }
        public Void visit(MemoryAccessInst mai) {
            if (mai.getType().hasEffectiveClass()) {
                classesUsed.put(curMethod,mai.getType().effectiveClass());
            }
            return null;
        }
        public Void visit(FieldInst fi) {
            classesUsed.put(curMethod,fi.field().getClazz());
            return null;
        }
        public Void visit(HeapAccessInst hai) {
            if (!hai.isArrayAccess()) {
                classesUsed.put(curMethod,hai.fieldField().getClazz());
            }
            return null;
        }
        public Void visit(TypeCheckInst tci) {
            if (tci.typeToCheck().hasEffectiveClass()) {
                classesUsed.put(curMethod,tci.typeToCheck().effectiveClass());
            }
            if (tci.typeToThrow().hasEffectiveClass()) {
                classesUsed.put(curMethod,tci.typeToThrow().effectiveClass());
            }
            return null;
        }
        public Void visit(MultiNewArrayInst mnai) {
            if (mnai.type().hasEffectiveClass()) {
                classesUsed.put(curMethod,mnai.type().effectiveClass());
            }
            return null;
        }
    };
    
    public void visit(Code code) {
	if (code.method()!=null) {
            curMethod=code.method();
        } else {
            curMethod=VisibleMethod.ANONYMOUS;
        }
        for (Header h : code.headers()) {
            for (Operation o : h.operations()) {
                o.accept(visitor);
            }
        }
    }
    
    public Set< VisibleMethod > execees(VisibleMethod m) {
	return execEdges.valuesForKey(m);
    }
    
    public Set< VisibleMethod > execers(VisibleMethod m) {
	return execEdges.keysForValue(m);
    }
    
    public Set< VisibleMethod > callees(VisibleMethod m) {
	return callEdges.valuesForKey(m);
    }
    
    public Set< VisibleMethod > callers(VisibleMethod m) {
	return callEdges.keysForValue(m);
    }
    
    public Set< VisibleClass > classesUsed(VisibleMethod m) {
        return classesUsed.valuesForKey(m);
    }
    
    public Set< VisibleMethod > methodsUsing(OMClass c) {
        return classesUsed.keysForValue(c);
    }

    public TwoWayMap< VisibleMethod, VisibleMethod > callEdges() {
        return callEdges;
    }

    public TwoWayMap< VisibleMethod, VisibleMethod > execEdges() {
        return execEdges;
    }
    
    public TwoWayMap< VisibleMethod, VisibleClass > classesUsed() {
        return classesUsed;
    }
    
    public void dump(String filenamePrefix) throws IOException {
        Util.dumpMapJNI(filenamePrefix+"_callEdges.txt",callEdges);
        Util.dumpMapJNI(filenamePrefix+"_execEdges.txt",execEdges);
        Util.dumpMapJNI(filenamePrefix+"_classesUsed.txt",classesUsed);
    }
}


