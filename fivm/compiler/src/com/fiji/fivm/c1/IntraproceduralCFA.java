/*
 * IntraproceduralCFA.java
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

// NOTE: it seems as if this should not be flow-sensitive since we have SSA.
// but, that is not the case.  control flow teaches us more about the values
// of variables than SSA can, for example:
//
// x = ... stuff ...
// if (x!=0) {
//     ... code that uses x ...
// }
//
// In the code that uses x, we know that x is non-zero.  But we don't know
// this information anywhere outside of that block.  So - x's value is
// control-flow-sensitive.

// FIXME: there are some null checks that this should trivially kill, but
// it's failing to.  see Hashtable.rehash.  this may be because of our
// little Ipsilon problem.

// FIXME: split this into an analysis and a transformation ... so that the analysis
// can be used for other transformations as well, and to build more complicated
// analyses.

// this is 19% of my execution time.  IntraproceduralCFA.update is 8%
public class IntraproceduralCFA extends CodePhase {
    public IntraproceduralCFA(Code c,
                              CFAMode mode) {
        super(c);
        this.mode=mode;
    }
    
    CFAMode mode;
    
    SimpleLivenessCalc slc;
    
    HashMap< Header, HashMap< Var, AbsValue > > valAtHead;
    HashMap< Header, HashMap< Var, AbsValue > > valAtTail;
    HashMap< Header, HashMap< Var, AbsValue > > valForHandler;
    
    Worklist worklist;

    Header curHeader;
    HashMap< Var, AbsValue > curValues;
    HashMap< Var, AbsValue > curForHandler;

    SimpleLivenessCalc.ForwardLocalCalc lc;
    
    public void visitCode() {
	//assert code.isSSA(); // this used to be important but it isn't anymore.
	
	if (Global.verbosity>=4) {
	    Global.log.println("Beginning OptConst on "+code);
	}
	
	valAtHead=
	    new HashMap< Header, HashMap< Var, AbsValue > >();
	valAtTail=
	    new HashMap< Header, HashMap< Var, AbsValue > >();
	valForHandler=
	    new HashMap< Header, HashMap< Var, AbsValue > >();
	
	worklist=new Worklist();

	slc=code.getSimpleLiveness();
	
	// do the analysis
	worklist.push(code.root());
	valAtHead.put(code.root(),new HashMap< Var, AbsValue >());
	while (!worklist.empty()) {
	    Header h=curHeader=worklist.pop();
	    
	    try {
		curValues=Util.copy(valAtHead.get(h));
		curForHandler=Util.copy(curValues);
		lc=slc.new ForwardLocalCalc(h);
		
		if (Global.verbosity>=5) {
		    Global.log.println("Handling "+h+" with "+curValues);
		}
		
		boolean endReached=true;
		boolean threw=false;
		
		for (Instruction i : h.instructions()) {
		    threw|=code.getThrows().get(i);
		    if (!update(i)) {
			endReached=false;
			break;
		    }
		}
		
		if (endReached) {
		    HashMap< Var, AbsValue > oldValues=valAtTail.get(h);
		    if (oldValues==null || !oldValues.equals(curValues)) {
			valAtTail.put(h,curValues);
			Footer f=h.getFooter();
                        try {
                            switch (f.opcode()) {
                            case BranchNonZero:
                            case BranchZero: {
                                Header zeroCase;
                                Header nonzeroCase;
                                switch (f.opcode()) {
                                case BranchNonZero:
                                    zeroCase=f.defaultSuccessor();
                                    nonzeroCase=((Branch)f).target();
                                    break;
                                case BranchZero:
                                    zeroCase=((Branch)f).target();
                                    nonzeroCase=f.defaultSuccessor();
                                    break;
                                default: throw new Error();
                                }
                                if (makeAbsValue(f.rhs(0)).isZero()) {
                                    handleEdge(h,zeroCase,curValues);
                                } else if (makeAbsValue(f.rhs(0)).isNonZero()) {
                                    handleEdge(h,nonzeroCase,curValues);
                                } else if (f.rhs(0) instanceof Var && code.isSSA()) {
                                    Var predVar=(Var)f.rhs(0);
                                    
                                    Instruction predSrc=predVar.inst();
				
                                    if (Global.verbosity>=6) {
                                        Global.log.println("At footer of "+h+" predSrc = "+
                                                            predSrc);
                                    }
				
                                    // handle non-zero case
                                    HashMap< Var, AbsValue > myValues=Util.copy(curValues);
                                    myValues.put(
                                        predVar,
                                        myValues.get(predVar).asNonZero());
                                    if (predSrc.opcode()==OpCode.Instanceof) {
                                        TypeInst ti=(TypeInst)predSrc;
                                        Arg a=predSrc.rhs(0);
                                        if (lc.currentlyLive(a)) {
                                            Var v=(Var)a;
                                            myValues.put(
                                                v,myValues.get(v).cast(
                                                    ti.getType()).asNonZero());
                                        }
                                    } else if ((predSrc.opcode()==OpCode.Eq ||
                                                predSrc.opcode()==OpCode.Neq) &&
                                               (predSrc.rhs(0).equals(0) ||
                                                predSrc.rhs(1).equals(0))) {
                                        Arg testSrc;
                                        if (predSrc.rhs(0).equals(0)) {
                                            testSrc=predSrc.rhs(1);
                                        } else {
                                            testSrc=predSrc.rhs(0);
                                        }
                                        if (lc.currentlyLive(testSrc)) {
                                            Var v=(Var)testSrc;
                                            // guard against weirdness
                                            AbsValue vval=myValues.get(v);
                                            if (!vval.isNonZero()) {
                                                if (predSrc.opcode()==OpCode.Eq) {
                                                    myValues.put(
                                                        v,vval.asZero(v.type()));
                                                } else {
                                                    myValues.put(
                                                        v,vval.asNonZero());
                                                }
                                            }
                                        }
                                    }
                                    handleEdge(h,nonzeroCase,myValues);
				
                                    // handle zero case
                                    myValues=Util.copy(curValues);
                                    myValues.put(
                                        predVar,
                                        myValues.get(predVar).asZero(predVar.type()));
                                    if ((predSrc.opcode()==OpCode.Eq ||
                                         predSrc.opcode()==OpCode.Neq) &&
                                        (predSrc.rhs(0).equals(0) ||
                                         predSrc.rhs(1).equals(0))) {
                                        Arg testSrc;
                                        if (predSrc.rhs(0).equals(0)) {
                                            testSrc=predSrc.rhs(1);
                                        } else {
                                            testSrc=predSrc.rhs(0);
                                        }
                                        if (lc.currentlyLive(testSrc)) {
                                            Var v=(Var)testSrc;
                                            AbsValue vval=myValues.get(v);
                                            if (!vval.isNonZero()) {
                                                if (predSrc.opcode()==OpCode.Eq) {
                                                    myValues.put(
                                                        v,vval.asNonZero());
                                                } else {
                                                    myValues.put(
                                                        v,vval.asZero(v.type()));
                                                }
                                            }
                                        }
                                    }				
                                    handleEdge(h,zeroCase,myValues);
                                } else {
                                    handleNormalEdges(h);
                                }
                                break;
                            }
                            case Switch: {
                                AbsValue predVal=makeAbsValue(f.rhs(0));
                                if (predVal instanceof IntAbsValue) {
                                    handleEdge(h,
                                               ((Switch)f).targetFor(
                                                   ((IntAbsValue)predVal).value),
                                               curValues);
                                } else {
                                    handleNormalEdges(h);
                                }
                                break;
                            }
                            case Throw:
                            case Rethrow:
                            case PatchPoint:
                            case PatchPointFooter: {
                                threw=true;
                                break;
                            }
                            default: {
                                handleNormalEdges(h);
                                break;
                            }}
                        } catch (Throwable e) {
                            throw new CompilerException("For "+f,e);
                        }
		    }
		}
		
		if (Global.verbosity>=5) {
		    Global.log.println("For "+h+" we have threw = "+threw);
		}
		if (threw) {
		    HashMap< Var, AbsValue > oldForHandler=valForHandler.get(h);
		    if (oldForHandler==null || !oldForHandler.equals(curForHandler)) {
			if (Global.verbosity>=5) {
			    Global.log.println("Updating handlers for "+h);
			}
			valForHandler.put(h,curForHandler);
			for (Header h2 : h.exceptionalSuccessors()) {
			    handleEdge(h,h2,curForHandler);
			}
		    }
		}
	    } catch (Throwable e) {
		throw new CompilerException("For "+h+" in "+code,e);
	    }
	}

        switch (mode) {
        case CFA_FOR_TRANSFORMATION:
            // do the transformation
            for (Header h : code.headers()) {
                curHeader=h;
                try {
                    curValues=valAtHead.get(h);
                    if (curValues==null) {
                        // dead code!
                        continue;
                    }
                    lc=slc.new ForwardLocalCalc(h);
                opFor:
                    for (Operation o : h.operations()) {
                        try {
                            if (o.opcode()!=OpCode.Phi) {
                                for (int i=0;i<o.rhs.length;++i) {
                                    Arg newArg=makeAbsValue(o.rhs[i]).replaceArg(o.rhs[i]);
                                    if (o.rhs[i]!=newArg) {
                                        setChangedCode("refined an argument");
                                        o.rhs[i]=newArg;
                                    }
                                }
                            } /* else NEVER change Phi's */
                            boolean replaced=false;
                        
                            if (o instanceof HeapAccessInst) {
                                HeapAccessInst hai=(HeapAccessInst)o;
                                if (hai.mode().hasScopeCheck() &&
                                    makeAbsValue(StoreSourceCalc.get(hai)).location()
                                    == ObjectLocation.IN_HEAP &&
                                    !Global.gcScopedMemory) {
                                    if (hai.mode().hasScopeCheck()) {
                                        hai.setMode(hai.mode().withoutScopeCheck());
                                        setChangedCode("removed scope check");
                                    }
                                }
                            }
                        
                            switch (o.opcode()) {
                            case InvokeDynamic: {
                                MethodInst mi=(MethodInst)o;
                                AbsValue recVal=makeAbsValue(o.rhs(0));
                                if (mi.method().isInstance() &&
                                    recVal instanceof ObjAbsValue) {
                                    ObjAbsValue objRecVal=(ObjAbsValue)recVal;
                                    if (mi.refine(
                                            objRecVal.ps.asClassBound())) {
                                        setChangedCode("refined InvokeDynamic bound");
                                    }
                                }
                                break;
                            }
                            case InvokeStatic: {
                                MethodInst mi=(MethodInst)o;
                                if (mi.method()==Runtime.arraycopy ||
                                    mi.method()==Runtime.fcArraycopy ||
                                    mi.method()==Runtime.vmArraycopy) {
                                    AbsValue srcVal=makeAbsValue(o.rhs(0));
                                    AbsValue trgVal=makeAbsValue(o.rhs(2));
                                    if (srcVal instanceof ObjAbsValue &&
                                        trgVal instanceof ObjAbsValue) {
                                        ObjAbsValue objSrcVal=(ObjAbsValue)srcVal;
                                        ObjAbsValue objTrgVal=(ObjAbsValue)trgVal;
                                        if (objTrgVal.ps.leastType().isArray() &&
                                            objSrcVal.ps.leastType().isSubtypeOf(
                                                objTrgVal.ps.leastType()) &&
                                            objSrcVal.ps.isTypeExact() &&
                                            !Global.gc.forceSafeArrayCopyFor(
                                                objTrgVal.ps.leastType(),
                                                objSrcVal.ps.leastType())) {
                                            mi.method=Runtime.fastUnsafeArrayCopy;
                                            mi.rhs=new Arg[]{
                                                mi.rhs(0),
                                                mi.rhs(1),
                                                mi.rhs(2),
                                                mi.rhs(3),
                                                mi.rhs(4),
                                                new PointerConst(
                                                    objTrgVal.ps.leastType().arrayElement()
                                                    .effectiveBasetype().bytes)
                                            };
                                            mi.rerefine();
                                            setChangedCode("turned System.arraycopy into a fast "+
                                                           "unsafe array copy");
                                            replaced=true;
                                        }
                                    }
                                }
                                break;
                            }
                            case NullCheck:
                            case ThrowRTEOnZero:
                            case CheckDivisor: {
                                AbsValue val=makeAbsValue(o.rhs[0]);
                                if (Global.verbosity>=5) {
                                    Global.log.println("At "+o+" in "+h+" we have val = "+val);
                                }
                                if (val.isNonZero()) {
                                    ((Instruction)o).remove();
                                    setChangedCode("removed non-zero check");
                                    replaced=true;
                                }
                                break;
                            }
                            case TypeCheck: {
                                // FIXME: if we know this cast will fail, then
                                // turn this into a ThrowRTEOnZero.
                                // huh?
                                TypeCheckInst tci=(TypeCheckInst)o;
                                AbsValue val=makeAbsValue(o.rhs[0]);
                                if (val instanceof ObjAbsValue) {
                                    ObjAbsValue objVal=(ObjAbsValue)val;
                                    if (objVal.ps.leastType().isSubtypeOf(
                                            tci.typeToCheck())) {
                                        tci.prepend(
                                            new TypeInst(
                                                tci.di(),OpCode.Cast,
                                                tci.lhs(),tci.rhs(),
                                                tci.typeToCheck()));
                                        tci.remove();
                                        setChangedCode("removed TypeCheck");
                                        replaced=true;
                                    }
                                }
                                break;
                            }
                                // FIXME: may be wise to special-case Instanceof.  we can
                                // turn it into a not-equal-zero test if we know that the
                                // types match but the value is possibly zero.
                            case ArrayCheckStore: {
                                SimpleInst si=(SimpleInst)o;
                                AbsValue trgVal=makeAbsValue(si.rhs[0]);
                                AbsValue srcVal=makeAbsValue(si.rhs[1]);
                                if (srcVal.isZero()) {
                                    si.remove();
                                    setChangedCode("removed ArrayCheckStore due to zero");
                                    replaced=true;
                                } else if (trgVal instanceof ObjAbsValue &&
                                           srcVal instanceof ObjAbsValue) {
                                    ObjAbsValue trgObjVal=(ObjAbsValue)trgVal;
                                    ObjAbsValue srcObjVal=(ObjAbsValue)srcVal;
                                    if (trgObjVal.ps.isTypeExact() &&
                                        srcObjVal.ps.leastType().isSubtypeOf(
                                            trgObjVal.ps.leastType().arrayElement())) {
                                        si.remove();
                                        setChangedCode("removed ArrayCheckStore due to exact "+
                                                       "type info");
                                        replaced=true;
                                    }
                                    // FIXME: if we know that the target type is exact
                                    // but we don't know that the source is a subtype
                                    // of it, then we can still make this more efficient
                                    // by turning it into a TypeCheck.
                                }
                                break;
                            }
                            case ArrayBoundsCheck: {
                                SimpleInst si=(SimpleInst)o;
                                AbsValue arrVal=makeAbsValue(si.rhs[0]);
                                AbsValue idxVal=makeAbsValue(si.rhs[1]);
                                if (arrVal instanceof ObjAbsValue) {
                                    ObjAbsValue arrObjVal=(ObjAbsValue)arrVal;
                                    if (idxVal.uLessThan(
                                            IntAbsValue.make(
                                                arrObjVal.lengthLB)).isNonZero()) {
                                        si.remove();
                                        setChangedCode("removed ArrayBoundsCheck");
                                        replaced=true;
                                    }
                                }
                                break;
                            }
                            case InHeapCheck:
                            case ScopeReturnCheck: {
                                if (makeAbsValue(o.rhs(0)).location()==ObjectLocation.IN_HEAP) {
                                    o.remove();
                                    setChangedCode("removed in-heap or scope-return check");
                                    replaced=true;
                                }
                                break;
                            }
                            default: break;
                            }
                            if (o instanceof Instruction) {
                                Instruction i=(Instruction)o;
                                if (update(i)) {
                                    if (!replaced &&
                                        !code.getSideEffects().get(i) &&
                                        !ConstInstCalc.get(i) &&
                                        i.opcode()!=OpCode.Phi /* let SimpleProp handle
                                                                  Phi functions; otherwise
                                                                  we end up breaking SSA
                                                                  conventions. */) {
                                        if (makeAbsValue(i.lhs()).replaceInst(i)) {
                                            setChangedCode("replaced instruction with constant");
                                        }
                                    }
                                } else {
                                    if (h.notReachedAfter(i)) {
                                        setChangedCode("found unreachable code");
                                    }
                                    break opFor;
                                }
                            }
                        } catch (Throwable e) {
                            throw new CompilerException("For "+o,e);
                        }
                    }
                    switch (h.getFooter().opcode()) {
                    case BranchNonZero: {
                        Branch b=(Branch)h.getFooter();
                        if (makeAbsValue(b.rhs[0]).isNonZero()) {
                            setChangedCode("replaced Branch with jump to true");
                            h.setFooter(new Jump(b.di(),b.target()));
                        } else if (makeAbsValue(b.rhs[0]).isZero()) {
                            setChangedCode("replaced Branch with jump to false");
                            h.setFooter(new Jump(b.di(),b.defaultSuccessor()));
                        }
                        break;
                    }
                    case BranchZero: {
                        Branch b=(Branch)h.getFooter();
                        if (makeAbsValue(b.rhs[0]).isZero()) {
                            setChangedCode("replaced Branch with jump to true");
                            h.setFooter(new Jump(b.di(),b.target()));
                        } else if (makeAbsValue(b.rhs[0]).isNonZero()) {
                            setChangedCode("replaced Branch with jump to false");
                            h.setFooter(new Jump(b.di(),b.defaultSuccessor()));
                        }
                        break;
                    }
                    case Switch: {
                        Switch s=(Switch)h.getFooter();
                        AbsValue predVal=makeAbsValue(s.rhs[0]);
                        if (predVal instanceof IntAbsValue) {
                            setChangedCode("replaced Switch with jump");
                            h.setFooter(
                                new Jump(s.di(),
                                         s.targetFor(((IntAbsValue)predVal).value)));
                        }
                        break;
                    }
                    default: break;
                    }
                } catch (Throwable e) {
                    throw new CompilerException("while transforming "+h+" in "+code,e);
                }
            }
	
            if (changedCode()) code.killAllAnalyses();
            break;
        case CFA_FOR_VERIFICATION:
            if (code.typeLowered) {
                code.changeVerifiability(VerifiabilityMode.NOT_VERIFIABLE,
                                         "types already lowered");
            }
            for (Header h : code.headers()) {
                curHeader=h;
                try {
                    curValues=valAtHead.get(h);
                    if (curValues==null) {
                        // dead code!
                        continue;
                    }
                    lc=slc.new ForwardLocalCalc(h);
                    
                    HashSet< ArrayStoreChecked > checkedArrayStores=
                        new HashSet< ArrayStoreChecked >();
                    
                opFor:
                    for (Operation o : h.operations()) {
                        try {
                            switch (o.opcode()) {
                            case GetField:
                            case PutField:
                            case WeakCASField:
                            case ArrayLoad:
                            case ArrayStore:
                            case WeakCASElement:
                            case Invoke:
                            case InvokeDynamic:
                                if (o.rhs(0).type().isObject() &&
                                    !((MemberInstable)o).member().noNullCheckOnAccess()) {
                                    AbsValue value=makeAbsValue(o.rhs(0));
                                    if (!value.isNonZero()) {
                                        code.changeVerifiability(
                                            VerifiabilityMode.CONTAINS_UNSAFE,
                                            "receiver is not provably non-null at line "+
                                            o.di().lineNumber());
                                    }
                                }
                                break;
                            case Div:
                                if (o.rhs(1).type().effectiveBasetype().isInteger) {
                                    AbsValue value=makeAbsValue(o.rhs(1));
                                    if (!value.isNonZero()) {
                                        code.changeVerifiability(
                                            VerifiabilityMode.CONTAINS_UNSAFE,
                                            "Divisor is not provably non-zero "+
                                            "at line "+o.di().lineNumber());
                                    }
                                }
                                break;
                            default:
                                break;
                            }
                            
                            switch (o.opcode()) {
                            case InvokeDynamic: {
                                MethodInst mi=(MethodInst)o;
                                if (mi.method().getClazz().isInterface()) {
                                    AbsValue value=makeAbsValue(mi.rhs(0));
                                    if (!(value instanceof ObjAbsValue)) {
                                        code.changeVerifiability(
                                            VerifiabilityMode.CONTAINS_UNSAFE,
                                            "Receiver of interface method call is not provably "+
                                            "an object at line "+o.di().lineNumber());
                                    } else {
                                        ObjAbsValue objVal=(ObjAbsValue)value;
                                        if (!objVal.ps.leastType().isSubtypeOf(
                                                mi.method().getClazz().asType())) {
                                            code.changeVerifiability(
                                                VerifiabilityMode.CONTAINS_UNSAFE,
                                                "Receiver of interface method call to "+
                                                mi.method().jniName()+" is not "+
                                                "provably a subtype of "+
                                                mi.method().getClazz().jniName()+" at "+
                                                "line "+o.di().lineNumber()+" ("+
                                                "can only prove it to be a subtype of "+
                                                objVal.ps.leastType().jniName()+")");
                                        }
                                    }
                                }
                                break;
                            }
                            default:
                                break;
                            }
                            
                            switch (o.opcode()) {
                            case ArrayCheckStore:
                                checkedArrayStores.add(
                                    new ArrayStoreChecked(o.rhs(0),o.rhs(1)));
                                break;
                            case ArrayStore: {
                                if (o.rhs(2).type().isObject()) {
                                    if (checkedArrayStores.contains(
                                            new ArrayStoreChecked(o.rhs(0),o.rhs(2)))) {
                                        // ok!
                                    } else {
                                        AbsValue trgVal=makeAbsValue(o.rhs(0));
                                        AbsValue srcVal=makeAbsValue(o.rhs(2));
                                        if (srcVal.isZero()) {
                                            // ok!
                                        } else if (!(trgVal instanceof ObjAbsValue)) {
                                            code.changeVerifiability(
                                                VerifiabilityMode.CONTAINS_UNSAFE,
                                                "Target of array store is not provably "+
                                                "an object at line "+o.di().lineNumber());
                                        } else if (!(srcVal instanceof ObjAbsValue)) {
                                            code.changeVerifiability(
                                                VerifiabilityMode.CONTAINS_UNSAFE,
                                                "Source of array store is not provable "+
                                                "an object at line "+o.di().lineNumber());
                                        } else {
                                            ObjAbsValue trgObjVal=(ObjAbsValue)trgVal;
                                            ObjAbsValue srcObjVal=(ObjAbsValue)srcVal;
                                            if (trgObjVal.ps.isTypeExact() &&
                                                srcObjVal.ps.leastType().isSubtypeOf(
                                                    trgObjVal.ps.leastType().arrayElement())) {
                                                // ok!
                                            } else {
                                                code.changeVerifiability(
                                                    VerifiabilityMode.CONTAINS_UNSAFE,
                                                    "Array store is not checked, and the "+
                                                    "source object is not provably a subtype "+
                                                    "of the element type of the target object "+
                                                    "at line "+o.di().lineNumber());
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            default: break;
                            }
                            
                            switch (o.opcode()) {
                            case Cast: {
                                TypeInst ti=(TypeInst)o;
                                if (ti.getType().isObject()) {
                                    AbsValue value=makeAbsValue(o.rhs(0));
                                    if (!(value instanceof ObjAbsValue)) {
                                        code.changeVerifiability(
                                            VerifiabilityMode.CONTAINS_UNSAFE,
                                            "Argument to unchecked object cast is not provably "+
                                            "an object at line "+o.di().lineNumber());
                                    } else {
                                        ObjAbsValue objVal=(ObjAbsValue)value;
                                        if (!objVal.ps.leastType().asExectype().isSubtypeOf(
                                                ti.getType().asExectype())) {
                                            code.changeVerifiability(
                                                VerifiabilityMode.CONTAINS_UNSAFE,
                                                "Argument to unchecked object cast is not "+
                                                "provably a subtype of "+ti.getType().jniName()+
                                                " at line "+o.di().lineNumber());
                                        }
                                    }
                                }
                                break;
                            }
                            default:
                                break;
                            }
                            
                            if (o instanceof Instruction) {
                                if (!update((Instruction)o)) {
                                    break opFor;
                                }
                            }
                        } catch (Throwable e) {
                            throw new CompilerException("For "+o,e);
                        }
                    }
                } catch (Throwable e) {
                    throw new CompilerException("while verifying "+h+" in "+code,e);
                }
            }
            break;
        default: throw new Error("Invalid CFA mode: "+mode);
        }
	
	// help GC and ensure fixpoint correctness (remove this and die)
	slc=null;
	valAtHead=null;
	valAtTail=null;
	valForHandler=null;
	worklist=null;
	curHeader=null;
	curForHandler=null;
	lc=null;
    }
    
    ObjectLocation allocLocation() {
	return Global.ola.allocLocation(code);
    }
    
    // returns the abstract value produced by this instruction.  note that this
    // is never called for non-Instructions.
    class UpdateVisitor extends Visitor< AbsValue > {
	public AbsValue visit(Node n) {
	    throw new Error();
	}
	public AbsValue visit(Instruction i) {
	    return TOP;
	}
        public AbsValue visit(CFieldInst cfi) {
            if (cfi.opcode()==OpCode.GetCVarAddress) {
                return CVARADDRESS;
            } else {
                return TOP;
            }
        }
	public AbsValue visit(ArgInst ai) {
            switch (ai.opcode()) {
            case GetArg:
                if (ai.getIdx()==0 && code.method()!=null && code.method().isInstance() &&
                    code.param(0).isObject() && !code.method().noNullCheckOnAccess()) {
                    // FIXME how do we get PT sets for params and results of random Code
                    // objects that don't have corresponding methods?
                    // for now it shouldn't matter...
                    // FIXME: something funny is happening here.
                    return ObjAbsValue.makeNonZero(code.paramSet(0),
                                                   code.paramLoc(0));
                } else if (code.param(ai.getIdx()).isObject()) {
                    return ObjAbsValue.make(code.paramSet(ai.getIdx()),
                                            code.paramLoc(ai.getIdx()));
                } else {
                    return TOP;
                }
            case GetCArg:
            case SaveRef:
                return TOP;
            case GetCArgAddress:
                return CVARADDRESS;
            default:
                assert false:ai.opcode();
                return null;
            }
	}
	public AbsValue visit(GetStringInst gsi) {
	    return ObjAbsValue.makeExactNonZero(Global.root().stringType,ObjectLocation.IN_HEAP);
	}
	private AbsValue handleNewArray(Instruction i,Type t) {
	    AbsValue length=makeAbsValue(i.rhs(0));
	    if (length instanceof IntAbsValue) {
		return ObjAbsValue.makeExactNonZeroArray(
		    t,((IntAbsValue)length).value,allocLocation());
	    } else {
		return ObjAbsValue.makeExactNonZero(t,allocLocation());
	    }
	}
	public AbsValue visit(MultiNewArrayInst mnai) {
	    // NB we should never see this case, under the current system.
	    return handleNewArray(mnai,mnai.type);
	}
        public AbsValue visit(HeapAccessInst fi) {
	    if (!code.typeLowered &&
		(fi.opcode()==OpCode.GetField ||
		 fi.opcode()==OpCode.PutField)) {
		AbsValue recv=makeAbsValue(fi.rhs(0));
		assert recv.isNonZero() || fi.di().origin().noSafetyChecks()
		    : "fi = "+fi+", recv = "+recv;
	    }
            
            if (StoreSourceCalc.isReference(fi)) {
                if (fi.isStatic() ||
                    makeAbsValue(fi.rhs(0)).location()==ObjectLocation.IN_HEAP) {
                    makeInHeap(StoreSourceCalc.get(fi));
                }
            }
            
	    switch (fi.opcode()) {
	    case GetStatic:
	    case GetField: {
		AbsValue result;
		VisibleField f=(VisibleField)fi.field();
		if (f.getType().isObject()) {
		    AbsValue recv;
		    if (fi.opcode()==OpCode.GetField) {
			recv=makeAbsValue(fi.rhs(0));
		    } else {
			recv=null;
		    }
		    if (recv instanceof ObjAbsValue) {
			result=ObjAbsValue.make(
			    Global.analysis().setFor(
				((ObjAbsValue)recv).ps,f),
			    Global.ola.setFor(
				((ObjAbsValue)recv).ps,recv.location(),f));
		    } else {
			result=ObjAbsValue.make(
			    Global.analysis().setFor(f),
			    Global.ola.setFor(f));
		    }
		} else {
		    result=TOP;
		}
		if (f.isNonZero()) {
		    result=result.asNonZero();
		}
		return result;
	    }
	    case ArrayLoad: {
		if (fi.rhs(0).type().arrayElement().isObject()) {
		    AbsValue arrVal_=makeAbsValue(fi.rhs(0));
		    if (arrVal_ instanceof ObjAbsValue) {
			ObjAbsValue arrVal=(ObjAbsValue)arrVal_;
			assert arrVal.ps.leastType().isSubtypeOf(
			    Type.make(fi.rhs(0).type()))
			    : "In "+fi+", with arrVal = "+arrVal;
			// FIXME: we should have some intelligence for array
			// elements!
			return ObjAbsValue.make(arrVal.ps.leastType().arrayElement(),
						arrVal.location());
		    } else {
			return ObjAbsValue.make(fi.rhs(0).type().arrayElement(),
						arrVal_.location());
		    }
		} else {
		    return TOP;
		}
	    }
	    default: return TOP;
	    }
        }
	public AbsValue visit(FieldInst fi) {
            // currently we don't have anything intelligent to say here.
	    switch (fi.opcode()) {
	    default: return TOP;
	    }
	}
        public AbsValue visit(GetMethodInst mi) {
            assert mi.opcode()==OpCode.GetMethodRec;
            return NONZERO;
        }
	public AbsValue visit(MethodInst mi) {
	    if (!code.typeLowered &&
		mi.opcode()!=OpCode.InvokeStatic &&
		mi.rhs(0).type().isObject() &&
		!mi.method().noNullCheckOnAccess()) {
		AbsValue recv=makeAbsValue(mi.rhs(0));
		assert recv.isNonZero() || mi.di().origin().noSafetyChecks()
		    : "mi = "+mi+", recv = "+recv;
	    }
	    AbsValue result;
	    if (mi.method().getType().isObject()) {
		AbsValue recv;
		if (mi.opcode()!=OpCode.InvokeStatic) {
		    recv=makeAbsValue(mi.rhs(0));
		} else {
		    recv=null;
		}
		if (recv instanceof ObjAbsValue) {
		    result=ObjAbsValue.make(
			Global.analysis().returnSetForCall(
			    ((ObjAbsValue)recv).ps,mi.method()),
			Global.ola.returnSetForCall(
			    ((ObjAbsValue)recv).ps,mi.method()));
		} else {
		    result=ObjAbsValue.make(
			Global.analysis().returnSetForCall(mi.method()),
			Global.ola.returnSetForCall(mi.method()));
		}
	    } else {
		result=TOP;
	    }
	    if (mi.method().isNonZero()) {
		result=result.asNonZero();
	    }
	    return result;
	}
	public AbsValue visit(SimpleInst si) {
	    // FIXME: could have IntToPointerZeroFill in here...  but who cares.
	    switch (si.opcode()) {
	    case Mov:
	    case Ipsilon:
	    case LikelyZero:
	    case LikelyNonZero:
	    case SemanticallyLikelyZero:
	    case SemanticallyLikelyNonZero:
	    case Phi: return makeAbsValue(si.rhs(0));
	    case Add: return makeAbsValue(si.rhs(0)).add(makeAbsValue(si.rhs(1)));
	    case Sub: return makeAbsValue(si.rhs(0)).sub(makeAbsValue(si.rhs(1)));
	    case Mul: return makeAbsValue(si.rhs(0)).mul(makeAbsValue(si.rhs(1)));
	    case Div: return makeAbsValue(si.rhs(0)).div(makeAbsValue(si.rhs(1)));
	    case Mod: return makeAbsValue(si.rhs(0)).mod(makeAbsValue(si.rhs(1)));
	    case Neg: return makeAbsValue(si.rhs(0)).neg();
	    case Not: return makeAbsValue(si.rhs(0)).not();
	    case Boolify: return makeAbsValue(si.rhs(0)).not().not();
	    case BitNot: return makeAbsValue(si.rhs(0)).bitNot();
	    case Shl: return makeAbsValue(si.rhs(0)).shl(makeAbsValue(si.rhs(1)));
	    case Shr: return makeAbsValue(si.rhs(0)).shr(makeAbsValue(si.rhs(1)));
	    case Ushr: return makeAbsValue(si.rhs(0)).ushr(makeAbsValue(si.rhs(1)));
	    case And: return makeAbsValue(si.rhs(0)).and(makeAbsValue(si.rhs(1)));
	    case Or: return makeAbsValue(si.rhs(0)).or(makeAbsValue(si.rhs(1)));
	    case Xor: return makeAbsValue(si.rhs(0)).xor(makeAbsValue(si.rhs(1)));
            case Sqrt: return makeAbsValue(si.rhs(0)).sqrt();
	    case CompareG: return makeAbsValue(si.rhs(0)).compareG(makeAbsValue(si.rhs(1)));
	    case CompareL: return makeAbsValue(si.rhs(0)).compareL(makeAbsValue(si.rhs(1)));
	    case LessThan: return makeAbsValue(si.rhs(0)).lessThan(makeAbsValue(si.rhs(1)));
            case LessThanEq: return makeAbsValue(si.rhs(0)).lessThanEq(makeAbsValue(si.rhs(1)));
	    case ULessThan: return makeAbsValue(si.rhs(0)).uLessThan(makeAbsValue(si.rhs(1)));
	    case ULessThanEq: return makeAbsValue(si.rhs(0)).uLessThanEq(makeAbsValue(si.rhs(1)));
	    case Eq:
		if (si.rhs(0)==si.rhs(1) && !si.rhs(0).effectiveBasetype().isFloat) return ONE;
		return makeAbsValue(si.rhs(0)).eq(makeAbsValue(si.rhs(1)));
            case Neq:
                if (si.rhs(0)==si.rhs(1) && !si.rhs(0).effectiveBasetype().isFloat) return ZERO;
                return makeAbsValue(si.rhs(0)).eq(makeAbsValue(si.rhs(1))).not();
	    case GetTypeDataForObject: return NONZERO;
	    case NullCheck:
	    case CheckDivisor: {
		if (makeAbsValue(si.rhs(0)).isZero()) {
		    return BOTTOM;
		} else {
		    makeNonZero(si.rhs(0));
		    return TOP;
		}
	    }
	    case InHeapCheck: {
		makeInHeap(si.rhs(0));
		return TOP;
	    }
            case CastNonZero: return makeAbsValue(si.rhs(0)).asNonZero();
	    default: return TOP;
	    }
	}
	public AbsValue visit(TypeCheckInst tci) {
	    assert tci.opcode()==OpCode.TypeCheck;
	    // NOTE: this returns bottom if the cast is known to fail.
	    // and I'm not sure if that's entirely correct... but it seems
	    // to work.
            AbsValue input=makeAbsValue(tci.rhs(0));
            AbsValue result=input.cast(tci.typeToCheck());
            putNewValue(tci.rhs(0),result);
            return result;
	}
        public AbsValue visit(MemoryAccessInst mai) {
            switch (mai.opcode()) {
                // NOTE: we currently don't have anything we can do for these...
            default: return TOP;
            }
        }
	public AbsValue visit(TypeInst ti) {
	    switch (ti.opcode()) {
	    case ThrowRTEOnZero: {
		if (makeAbsValue(ti.rhs(0)).isZero()) {
		    return BOTTOM;
		} else {
		    makeNonZero(ti.rhs(0));
		    return TOP;
		}
	    }
	    case GetType: return ObjAbsValue.makeExactNonZero(Global.root().classType,
							      ObjectLocation.IN_HEAP);
	    case GetTypeData: return NONZERO;
	    case NewArray: return handleNewArray(ti,ti.getType());
	    case New: return ObjAbsValue.makeExactNonZero(ti.getType(),
							  allocLocation());
	    case GetException:
                if (Settings.EXCEPTIONS_MAY_BE_NULL) {
                    return ObjAbsValue.make(ti.getType(),
                                            ObjectLocation.IN_HEAP);
                } else {
                    return ObjAbsValue.makeNonZero(ti.getType(),
                                                   ObjectLocation.IN_HEAP);
                }
	    case Cast: return makeAbsValue(ti.rhs(0)).cast(ti.getType());
            case CastExact: return makeAbsValue(ti.rhs(0)).castExact(ti.getType());
            case Fiat: return makeAbsValue(ti.rhs(0)).fiat(ti.getType());
	    case Instanceof: {
		AbsValue inp=makeAbsValue(ti.rhs(0));
		// FIXME: use cast here?  to simplify the code?
                // NO!  this is more precise than cast().
		if (inp instanceof ObjAbsValue) {
		    ObjAbsValue obj=(ObjAbsValue)inp;
		    if (obj.nonzero &&
			obj.ps.leastType().isSubtypeOf(ti.getType())) {
			return ONE;
		    } else if ((obj.ps.isTypeExact() &&
				!obj.ps.leastType().isSubtypeOf(ti.getType())) ||
			       !ti.getType().hasInstances() ||
                               !obj.ps.intersects(new TypeBound(ti.getType()))) {
			return ZERO;
		    }
		} else if (inp.isZero()) {
		    return ZERO;
		}
		return TOP;
	    }
	    default: return TOP;
	    }
	}
    };
    UpdateVisitor updateVisitor=new UpdateVisitor();
    
    void makeNonZero(Arg a) {
	AbsValue val=makeAbsValue(a);
	if (!val.isNonZero()) {
	    putNewValue(a,val.asNonZero());
	}
    }
    
    void makeInHeap(Arg a) {
	AbsValue val=makeAbsValue(a);
	if (val.location()!=ObjectLocation.IN_HEAP) {
	    putNewValue(a,val.asHeap());
	}
    }
    
    static void putNewValue(HashMap< Var, AbsValue > values,
			    Arg a,AbsValue newValue) {
	if (a instanceof Var) {
	    values.put((Var)a,newValue);
	}
    }
    
    void putNewValue(Arg a,AbsValue newValue) {
	putNewValue(curValues,a,newValue);
    }
    
    boolean update(Instruction i) {
	try {
	    AbsValue curValue=i.accept(updateVisitor);
	    if (Global.verbosity>=7) {
		Global.log.println("At "+i+": we have result: "+curValue);
	    }
	    if (curValue==BOTTOM) {
		return false;
	    }
	    curValues.put(i.lhs(),curValue);
	    AbsValue handlerValue=curForHandler.get(i.lhs());
	    if (handlerValue!=null) {
		curForHandler.put(i.lhs(),handlerValue.merge(curValue));
	    }
	    for (Var v : lc.deaths(i)) {
		if (!lc.births(i,v)) {
		    if (Global.verbosity>=5) {
			Global.log.println("Removing "+v+" at "+i);
		    }
		    curValues.remove(v);
		}
	    }
	    lc.update(i);
	    return true;
	} catch (Throwable e) {
	    throw new CompilerException("For "+i+" in "+curHeader,e);
	}
    }
    
    boolean merge(Header h,Header h2,HashMap< Var, AbsValue > myValues) {
	HashMap< Var, AbsValue > theirValues=valAtHead.get(h2);
	if (theirValues==null) {
	    valAtHead.put(h2,theirValues=new HashMap< Var, AbsValue >());
	    for (Var v : slc.liveAtHead(h2)) {
		AbsValue result=myValues.get(v);
		if (result==null) result=TOP;
		if (Global.verbosity>=6) {
		    Global.log.println("From "+h+" to "+h2+" setting "+v+" to "+result);
		}
		theirValues.put(v,result);
	    }
	    return true;
	} else {
	    boolean changed=false;
	    for (Var v : slc.liveAtHead(h2)) {
		AbsValue theirOldValue=theirValues.get(v);
		assert theirOldValue!=null : "For variable "+v+" in merge from "+h+" to "+h2;
		AbsValue myValue=myValues.get(v);
		if (myValue==null) {
		    myValue=TOP;
		    if (Global.verbosity>=7) {
			Global.log.println("From "+h+" to "+h2+" setting "+v+" to "+TOP+
					    " because I don't have it");
		    }
		}
		AbsValue theirNewValue=theirOldValue.merge(myValue);
		if (theirOldValue!=theirNewValue) {
		    if (Global.verbosity>=6) {
			Global.log.println("From "+h+" to "+h2+" setting "+v+" to "+theirNewValue+
					    " as a result of merge between "+theirOldValue+
					    " and "+myValue);
		    }
		    theirValues.put(v,theirNewValue);
		    changed=true;
		}
	    }
	    return changed;
	}
    }
    
    void handleEdge(Header h,Header h2,HashMap< Var, AbsValue > myValues) {
	if (Global.verbosity>=6) {
	    Global.log.println("At edge from "+h+" to "+h2+" we have "+myValues);
	}
	if (merge(h,h2,myValues)) {
	    worklist.push(h2);
	}
    }
    
    void handleEdges(Header h,
		     Iterable< Header > successors,
		     HashMap< Var, AbsValue > myValues) {
	for (Header h2 : successors) {
	    handleEdge(h,h2,myValues);
	}
    }
    
    void handleNormalEdges(Header h) {
	handleEdges(h,h.normalSuccessors(),curValues);
    }
    
    AbsValue makeAbsValue(Arg a) {
	if (a==Arg.NULL) {
	    return NULL;
	} else if (a instanceof IntConst) {
	    return IntAbsValue.make(((IntConst)a).value());
	} else if (a instanceof Var) {
	    AbsValue result=curValues.get(a);
	    assert result!=null : "For a = "+a+" with curValues = "+curValues;
	    return result;
	} else if (a instanceof PointerConst) {
	    return PointerAbsValue.make(((PointerConst)a).value());
        } else if (a instanceof LongConst) {
            return new LongAbsValue(((LongConst)a).value());
        } else if (a instanceof FloatConst) {
            return new FloatAbsValue(((FloatConst)a).value());
        } else if (a instanceof DoubleConst) {
            return new DoubleAbsValue(((DoubleConst)a).value());
	} else {
	    return TOP;
	}
    }
    
    static abstract class AbsValue {
	boolean isZero() { return false; }
	boolean isNonZero() { return false; }
	ObjectLocation location() { return ObjectLocation.top(); }
	abstract AbsValue asZero(Exectype t);
	AbsValue asHeap() { return this; } // ?? could do better
	AbsValue asNonZero() { return NONZERO; }
	AbsValue add(AbsValue other) { return TOP; }
	AbsValue sub(AbsValue other) { return TOP; }
	AbsValue mul(AbsValue other) { return TOP; }
	AbsValue div(AbsValue other) { return TOP; }
	AbsValue mod(AbsValue other) { return TOP; }
	AbsValue neg() { return TOP; }
	AbsValue not() { return TOP; }
	AbsValue bitNot() { return TOP; }
	AbsValue shl(AbsValue other) { return TOP; }
	AbsValue shr(AbsValue other) { return TOP; }
	AbsValue ushr(AbsValue other) { return TOP; }
	AbsValue and(AbsValue other) { return TOP; }
	AbsValue or(AbsValue other) { return TOP; }
	AbsValue xor(AbsValue other) { return TOP; }
        AbsValue sqrt() { return TOP; }
	AbsValue compareG(AbsValue other) { return TOP; }
	AbsValue compareL(AbsValue other) { return TOP; }
	AbsValue eq(AbsValue other) { return TOP; }
	AbsValue neq(AbsValue other) { return TOP; }
	AbsValue lessThan(AbsValue other) { return TOP; }
	AbsValue lessThanEq(AbsValue other) { return TOP; }
	AbsValue uLessThan(AbsValue other) {
	    if (isZero() && other.isNonZero()) {
		return ONE;
	    } else if (other.isZero()) {
		return ZERO;
	    } else {
		return TOP;
	    }
	}
	AbsValue uLessThanEq(AbsValue other) {
	    if (isZero()) {
		return ONE;
	    } else {
		return TOP;
	    }
	}
	AbsValue cast(Type t) {
            if (t.isObject()) {
                if (isZero()) {
                    return NULL;
                } else if (isNonZero()) {
                    return ObjAbsValue.makeNonZero(t,location());
                } else {
                    return ObjAbsValue.make(t,location());
                }
            } else {
                return TOP;
            }
        }
        AbsValue castExact(Type t) {
            if (t.isObject()) {
                if (isZero()) {
                    return NULL;
                } else if (isNonZero()) {
                    return ObjAbsValue.makeExactNonZero(t,location());
                } else {
                    return ObjAbsValue.makeExact(t,location());
                }
            } else {
                return cast(t);
            }
        }
        AbsValue fiat(Type t) {
            return TOP;
        }
        AbsValue cast(TypeBound tb) {
            if (tb.isExact()) {
                return castExact(tb.type());
            } else {
                return cast(tb.type());
            }
        }
	boolean replaceInst(Instruction i) {
	    return false;
	}
	Arg replaceArg(Arg a) {
	    return a;
	}
	AbsValue merge(AbsValue other) {
	    if (this==other || other==BOTTOM || this.eq(other).isNonZero()) {
		return this;
            } else if (isNonZero() && other.isNonZero()) {
                return NONZERO;
	    } else {
		return TOP;
	    }
	}
    }
    
    static abstract class ConstAbsValue extends AbsValue {
	AbsValue asNonZero() {
	    return this;
	}
	AbsValue asZero(Exectype t) {
	    return this;
	}
    }
    
    static class IntAbsValue extends ConstAbsValue {
	int value;
	IntAbsValue(int value) {
	    this.value=value;
	}
	static IntAbsValue make(int value) {
	    switch (value) {
	    case 0: return ZERO;
	    case 1: return ONE;
	    case -1: return MINUSONE;
	    default: return new IntAbsValue(value);
	    }
	}
	static IntAbsValue make(boolean value) {
	    if (value) return ONE;
	    else return ZERO;
	}
	boolean isZero() { return value==0; }
	boolean isNonZero() { return value!=0; }
	AbsValue add(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value+((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue sub(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value-((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mul(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value*((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue div(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value/((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mod(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value%((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue neg() { return IntAbsValue.make(-value); }
	AbsValue not() { return IntAbsValue.make(value==0?1:0); }
	AbsValue bitNot() { return IntAbsValue.make(~value); }
	AbsValue shl(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value<<((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue shr(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value>>((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue ushr(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value>>>((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue and(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value&((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue or(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value|((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue xor(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value^((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue compareG(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		int otherValue=((IntAbsValue)other).value;
		if (value<otherValue) {
		    return IntAbsValue.make(-1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue compareL(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		int otherValue=((IntAbsValue)other).value;
		if (value>otherValue) {
		    return IntAbsValue.make(1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(-1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue eq(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value==((IntAbsValue)other).value);
	    } else if (value==0) {
		if (other.isZero()) {
		    return ONE;
		} else if (other.isNonZero()) {
		    return ZERO;
		}
	    }
	    return TOP;
	}
	AbsValue neq(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value!=((IntAbsValue)other).value);
	    } else if (value==0) {
		if (other.isNonZero()) {
		    return ONE;
		} else if (other.isZero()) {
		    return ZERO;
		}
	    }
	    return TOP;
	}
	AbsValue lessThan(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value<((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThanEq(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return IntAbsValue.make(value<=((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue uLessThan(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		int otherValue=((IntAbsValue)other).value;
                if (IntUtil.uLessThan(value,otherValue)) {
                    return IntAbsValue.make(1);
                } else {
                    return IntAbsValue.make(0);
                }
	    } else {
		return super.uLessThan(other);
	    }
	}
	AbsValue uLessThanEq(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		int otherValue=((IntAbsValue)other).value;
                if (value==otherValue || IntUtil.uLessThan(value,otherValue)) {
                    return IntAbsValue.make(1);
                } else {
                    return IntAbsValue.make(0);
                }
	    } else {
		return super.uLessThan(other);
	    }
	}
	AbsValue cast(Type t) {
	    switch (t.effectiveBasetype()) {
	    case BOOLEAN: if (value!=0) return ONE; else return ZERO;
	    case BYTE: return IntAbsValue.make((byte)value);
	    case CHAR: return IntAbsValue.make((char)value);
	    case SHORT: return IntAbsValue.make((short)value);
	    case INT: return this;
	    case LONG: return new LongAbsValue(value);
	    case FLOAT: return new FloatAbsValue((float)value);
	    case DOUBLE: return new DoubleAbsValue((double)value);
	    case POINTER: return PointerAbsValue.make(value);
	    default: return TOP;
	    }
	}
        AbsValue fiat(Type t) {
            assert t==Type.FLOAT;
            return new FloatAbsValue(Float.intBitsToFloat(value));
        }
	boolean replaceInst(Instruction i) {
	    // NB we handle Ipsilon specially.  we NEVER want to replace an
	    // Ipsilon instruction with a Mov instruction.
	    i.prepend(
		new SimpleInst(
		    i.di(),i.opcode()==OpCode.Ipsilon?OpCode.Ipsilon:OpCode.Mov,
		    i.lhs(),new Arg[]{IntConst.make(value)}));
	    i.remove();
	    return true;
	}
	Arg replaceArg(Arg a) {
            if (a instanceof IntConst) {
                assert ((IntConst)a).value()==value;
                return a;
            } else {
                return IntConst.make(value);
            }
	}
	public String toString() {
	    return "IntAbsValue["+value+"]";
	}
    }
    
    static class LongAbsValue extends ConstAbsValue {
	long value;
	LongAbsValue(long value) {
	    this.value=value;
	}
	boolean isZero() { return value==0; }
	boolean isNonZero() { return value!=0; }
	AbsValue add(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value+((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue sub(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value-((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mul(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value*((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue div(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value/((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mod(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value%((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue neg() { return new LongAbsValue(-value); }
	AbsValue not() { return new LongAbsValue(value==0?1:0); }
	AbsValue bitNot() { return new LongAbsValue(~value); }
	AbsValue shl(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return new LongAbsValue(value<<((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue shr(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return new LongAbsValue(value>>((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue ushr(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		return new LongAbsValue(value>>>((IntAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue and(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value&((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue or(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value|((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue xor(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return new LongAbsValue(value^((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue compareG(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		long otherValue=((LongAbsValue)other).value;
		if (value<otherValue) {
		    return IntAbsValue.make(-1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue compareL(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		long otherValue=((LongAbsValue)other).value;
		if (value>otherValue) {
		    return IntAbsValue.make(1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(-1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue eq(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return IntAbsValue.make(value==((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue neq(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return IntAbsValue.make(value!=((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThan(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return IntAbsValue.make(value<((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThanEq(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		return IntAbsValue.make(value<=((LongAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue uLessThan(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		long otherValue=((LongAbsValue)other).value;
                if (IntUtil.uLessThan(value,otherValue)) {
                    return IntAbsValue.make(1);
                } else {
                    return IntAbsValue.make(0);
                }
	    } else {
		return super.uLessThan(other);
	    }
	}
	AbsValue uLessThanEq(AbsValue other) {
	    if (other instanceof LongAbsValue) {
		long otherValue=((LongAbsValue)other).value;
                if (value==otherValue || IntUtil.uLessThan(value,otherValue)) {
                    return IntAbsValue.make(1);
                } else {
                    return IntAbsValue.make(0);
                }
	    } else {
		return super.uLessThan(other);
	    }
	}
	AbsValue cast(Type t) {
	    switch (t.effectiveBasetype()) {
	    case BOOLEAN: if (value!=0) return ONE; else return ZERO;
	    case BYTE: return IntAbsValue.make((byte)value);
	    case CHAR: return IntAbsValue.make((char)value);
	    case SHORT: return IntAbsValue.make((short)value);
	    case INT: return IntAbsValue.make((int)value);
	    case LONG: return this;
	    case FLOAT: return new FloatAbsValue((float)value);
	    case DOUBLE: return new DoubleAbsValue((double)value);
	    case POINTER: return PointerAbsValue.make(value);
	    default: return TOP;
	    }
	}
        AbsValue fiat(Type t) {
            switch (t.effectiveBasetype()) {
            case BOOLEAN:
            case BYTE:
                if (Settings.IS_LITTLE_ENDIAN) {
                    return IntAbsValue.make((byte)value);
                } else {
                    return IntAbsValue.make((byte)(value>>56));
                }
            case CHAR:
                if (Settings.IS_LITTLE_ENDIAN) {
                    return IntAbsValue.make((char)value);
                } else {
                    return IntAbsValue.make((char)(value>>48));
                }
            case SHORT:
                if (Settings.IS_LITTLE_ENDIAN) {
                    return IntAbsValue.make((short)value);
                } else {
                    return IntAbsValue.make((short)(value>>48));
                }
            case INT:
                if (Settings.IS_LITTLE_ENDIAN) {
                    return IntAbsValue.make((int)value);
                } else {
                    return IntAbsValue.make((int)(value>>32));
                }
            case LONG:
                return this;
            case POINTER:
                if (Global.pointerSize==8) {
                    return PointerAbsValue.make(value);
                } else {
                    if (Settings.IS_LITTLE_ENDIAN) {
                        return PointerAbsValue.make((int)value);
                    } else {
                        return PointerAbsValue.make((int)(value>>32));
                    }
                }
            case FLOAT:
                if (Settings.IS_LITTLE_ENDIAN) {
                    return new FloatAbsValue(Float.intBitsToFloat((int)value));
                } else {
                    return new FloatAbsValue(Float.intBitsToFloat((int)(value>>32)));
                }
            case DOUBLE:
                return new DoubleAbsValue(Double.longBitsToDouble(value));
            default:
                throw new Error();
            }
        }
	boolean replaceInst(Instruction i) {
	    i.prepend(
		new SimpleInst(
		    i.di(),i.opcode()==OpCode.Ipsilon?OpCode.Ipsilon:OpCode.Mov,
		    i.lhs(),new Arg[]{LongConst.make(value)}));
	    i.remove();
	    return true;
	}
	Arg replaceArg(Arg a) {
            if (a instanceof LongConst) {
                assert ((LongConst)a).value()==value;
                return a;
            } else {
                return LongConst.make(value);
            }
	}
	public String toString() {
	    return "LongAbsValue["+value+"]";
	}
    }
    
    static class PointerAbsValue extends ConstAbsValue {
	long value;
	PointerAbsValue(long value) {
	    this.value=value;
	    if (Global.pointerSize==4) {
		value=(int)value;
	    }
	}
	static PointerAbsValue make(long value) {
	    if (value==0) return NULL;
	    else return new PointerAbsValue(value);
	}
	boolean isZero() { return value==0; }
	boolean isNonZero() { return value!=0; }
	ObjectLocation location() { return ObjectLocation.IN_HEAP; }
	AbsValue add(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make((int)value+(int)((PointerAbsValue)other).value);
		} else {
		    return PointerAbsValue.make(value+((PointerAbsValue)other).value);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue sub(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make((int)value-(int)((PointerAbsValue)other).value);
		} else {
		    return PointerAbsValue.make(value-((PointerAbsValue)other).value);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue mul(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make((int)value*(int)((PointerAbsValue)other).value);
		} else {
		    return PointerAbsValue.make(value*((PointerAbsValue)other).value);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue div(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make(
                        IntUtil.udiv((int)value,(int)((PointerAbsValue)other).value));
		} else {
		    return PointerAbsValue.make(
                        IntUtil.udiv(value,((PointerAbsValue)other).value));
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue mod(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make(
                        IntUtil.umod((int)value,(int)((PointerAbsValue)other).value));
		} else {
		    return PointerAbsValue.make(
                        IntUtil.umod(value,((PointerAbsValue)other).value));
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue neg() { return PointerAbsValue.make(-value); }
	AbsValue not() { return PointerAbsValue.make(value==0?1:0); }
	AbsValue bitNot() {
	    if (Global.pointerSize==4) {
		return PointerAbsValue.make(~(int)value);
	    } else {
		return PointerAbsValue.make(~value);
	    }
	}
	AbsValue shl(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make((int)value<<((IntAbsValue)other).value);
		} else {
		    return PointerAbsValue.make(value<<((IntAbsValue)other).value);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue shr(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make((int)value>>((IntAbsValue)other).value);
		} else {
		    return PointerAbsValue.make(value>>((IntAbsValue)other).value);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue ushr(AbsValue other) {
	    if (other instanceof IntAbsValue) {
		if (Global.pointerSize==4) {
		    return PointerAbsValue.make((int)value>>>((IntAbsValue)other).value);
		} else {
		    return PointerAbsValue.make(value>>>((IntAbsValue)other).value);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue and(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		return PointerAbsValue.make(value&((PointerAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue or(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		return PointerAbsValue.make(value|((PointerAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue xor(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		return PointerAbsValue.make(value^((PointerAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue compareG(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		long otherValue=((PointerAbsValue)other).value;
		if (value<otherValue) {
		    return IntAbsValue.make(-1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue compareL(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		long otherValue=((PointerAbsValue)other).value;
		if (value>otherValue) {
		    return IntAbsValue.make(1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(-1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue eq(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		return IntAbsValue.make(value==((PointerAbsValue)other).value);
	    } else if (value==0) {
		if (other.isZero()) {
		    return ONE;
		} else if (other.isNonZero()) {
		    return ZERO;
		}
	    }
	    return TOP;
	}
	AbsValue neq(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		return IntAbsValue.make(value!=((PointerAbsValue)other).value);
	    } else if (value==0) {
		if (other.isNonZero()) {
		    return ONE;
		} else if (other.isZero()) {
		    return ZERO;
		}
	    }
	    return TOP;
	}
	AbsValue lessThan(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		return IntAbsValue.make(value<((PointerAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThanEq(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		return IntAbsValue.make(value<=((PointerAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue uLessThan(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		long otherValue=((PointerAbsValue)other).value;
                if (IntUtil.uLessThan(value,otherValue)) {
                    return IntAbsValue.make(1);
                } else {
                    return IntAbsValue.make(0);
                }
	    } else {
		return super.uLessThan(other);
	    }
	}
	AbsValue uLessThanEq(AbsValue other) {
	    if (other instanceof PointerAbsValue) {
		long otherValue=((PointerAbsValue)other).value;
                if (value==otherValue || IntUtil.uLessThan(value,otherValue)) {
                    return IntAbsValue.make(1);
                } else {
                    return IntAbsValue.make(0);
                }
	    } else {
		return super.uLessThan(other);
	    }
	}
	AbsValue cast(Type t) {
	    switch (t.effectiveBasetype()) {
	    case BOOLEAN: if (value!=0) return ONE; else return ZERO;
	    case BYTE: return IntAbsValue.make((byte)value);
	    case CHAR: return IntAbsValue.make((char)value);
	    case SHORT: return IntAbsValue.make((short)value);
	    case INT: return IntAbsValue.make((int)value);
	    case LONG: return new LongAbsValue(value);
	    case FLOAT: return new FloatAbsValue((float)value);
	    case DOUBLE: return new DoubleAbsValue((double)value);
	    case POINTER: return this;
	    case OBJECT: return super.cast(t);
	    default: return TOP;
	    }
	}
	boolean replaceInst(Instruction i) {
	    Arg a;
	    if (i.lhs().type().isObject()) {
		assert value==0;
		a=Arg.NULL;
	    } else {
		a=new PointerConst(value);
	    }
	    i.prepend(
		new SimpleInst(
		    i.di(),i.opcode()==OpCode.Ipsilon?OpCode.Ipsilon:OpCode.Mov,
		    i.lhs(),new Arg[]{a}));
	    i.remove();
	    return true;
	}
	Arg replaceArg(Arg a) {
	    if (a.type().isObject()) {
		assert value==0;
		return Arg.NULL;
	    } else if (a instanceof PointerConst) {
                assert ((PointerConst)a).value==value;
                return a;
            } else {
		return new PointerConst(value);
	    }
	}
	AbsValue merge(AbsValue other) {
	    if (value==0 && other instanceof ObjAbsValue) {
		ObjAbsValue objOther=(ObjAbsValue)other;
		return new ObjAbsValue(
		    objOther.ps,false,objOther.lengthLB,
		    other.location());
	    } else {
		return super.merge(other);
	    }
	}
	public String toString() {
	    return "PointerAbsValue["+value+"]";
	}
    }
    
    static class FloatAbsValue extends ConstAbsValue {
	float value;
	FloatAbsValue(float value) {
	    this.value=value;
	}
	boolean isZero() { return value==0.0; }
	boolean isNonZero() { return value!=0.0; }
	AbsValue add(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return new FloatAbsValue(value+((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue sub(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return new FloatAbsValue(value-((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mul(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return new FloatAbsValue(value*((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue div(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return new FloatAbsValue(value/((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mod(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return new FloatAbsValue(value%((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue neg() { return new FloatAbsValue(-value); }
        AbsValue sqrt() { return new FloatAbsValue((float)Math.sqrt(value)); }
	AbsValue compareG(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		float otherValue=((FloatAbsValue)other).value;
		if (value<otherValue) {
		    return IntAbsValue.make(-1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue compareL(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		float otherValue=((FloatAbsValue)other).value;
		if (value>otherValue) {
		    return IntAbsValue.make(1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(-1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue eq(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return IntAbsValue.make(value==((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue neq(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return IntAbsValue.make(value!=((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThan(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return IntAbsValue.make(value<((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThanEq(AbsValue other) {
	    if (other instanceof FloatAbsValue) {
		return IntAbsValue.make(value<=((FloatAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue cast(Type t) {
	    switch (t.effectiveBasetype()) {
	    case BOOLEAN: if (value!=0) return ONE; else return ZERO;
	    case BYTE: return IntAbsValue.make((byte)value);
	    case CHAR: return IntAbsValue.make((char)value);
	    case SHORT: return IntAbsValue.make((short)value);
	    case INT: return IntAbsValue.make((int)value);
	    case LONG: return new LongAbsValue((long)value);
	    case FLOAT: return this;
	    case DOUBLE: return new DoubleAbsValue(value);
	    case POINTER: return PointerAbsValue.make((long)value);
	    default: return TOP;
	    }
	}
        AbsValue fiat(Type t) {
            assert t==Type.INT;
            return IntAbsValue.make(Float.floatToRawIntBits(value));
        }
	boolean replaceInst(Instruction i) {
	    i.prepend(
		new SimpleInst(
		    i.di(),i.opcode()==OpCode.Ipsilon?OpCode.Ipsilon:OpCode.Mov,
		    i.lhs(),new Arg[]{FloatConst.make(value)}));
	    i.remove();
	    return true;
	}
	Arg replaceArg(Arg a) {
            if (a instanceof FloatConst) {
                assert Float.floatToRawIntBits(((FloatConst)a).value())
                    == Float.floatToRawIntBits(value);
                return a;
            } else {
                return FloatConst.make(value);
            }
	}
	public String toString() {
	    return "FloatAbsValue["+value+"]";
	}
    }
    
    static class DoubleAbsValue extends ConstAbsValue {
	double value;
	DoubleAbsValue(double value) {
	    this.value=value;
	}
	boolean isZero() { return value==0.0; }
	boolean isNonZero() { return value!=0.0; }
	AbsValue add(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return new DoubleAbsValue(value+((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue sub(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return new DoubleAbsValue(value-((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mul(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return new DoubleAbsValue(value*((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue div(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return new DoubleAbsValue(value/((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue mod(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return new DoubleAbsValue(value%((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue neg() { return new DoubleAbsValue(-value); }
        AbsValue sqrt() { return new DoubleAbsValue(Math.sqrt(value)); }
	AbsValue compareG(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		double otherValue=((DoubleAbsValue)other).value;
		if (value<otherValue) {
		    return IntAbsValue.make(-1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue compareL(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		double otherValue=((DoubleAbsValue)other).value;
		if (value>otherValue) {
		    return IntAbsValue.make(1);
		} else if (value==otherValue) {
		    return IntAbsValue.make(0);
		} else {
		    return IntAbsValue.make(-1);
		}
	    } else {
		return TOP;
	    }
	}
	AbsValue eq(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return IntAbsValue.make(value==((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue neq(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return IntAbsValue.make(value!=((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThan(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return IntAbsValue.make(value<((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue lessThanEq(AbsValue other) {
	    if (other instanceof DoubleAbsValue) {
		return IntAbsValue.make(value<=((DoubleAbsValue)other).value);
	    } else {
		return TOP;
	    }
	}
	AbsValue cast(Type t) {
	    switch (t.effectiveBasetype()) {
	    case BOOLEAN: if (value!=0) return ONE; else return ZERO;
	    case BYTE: return IntAbsValue.make((byte)value);
	    case CHAR: return IntAbsValue.make((char)value);
	    case SHORT: return IntAbsValue.make((short)value);
	    case INT: return IntAbsValue.make((int)value);
	    case LONG: return new LongAbsValue((long)value);
	    case FLOAT: return new FloatAbsValue((float)value);
	    case DOUBLE: return this;
	    case POINTER: return PointerAbsValue.make((long)value);
	    default: return TOP;
	    }
	}
        AbsValue fiat(Type t) {
            assert t==Type.LONG;
            return new LongAbsValue(Double.doubleToRawLongBits(value));
        }
	boolean replaceInst(Instruction i) {
	    i.prepend(
		new SimpleInst(
		    i.di(),i.opcode()==OpCode.Ipsilon?OpCode.Ipsilon:OpCode.Mov,
		    i.lhs(),new Arg[]{DoubleConst.make(value)}));
	    i.remove();
            return true;
	}
	Arg replaceArg(Arg a) {
            if (a instanceof DoubleConst) {
                assert Double.doubleToRawLongBits(((DoubleConst)a).value())
                    == Double.doubleToRawLongBits(value);
                return a;
            } else {
                return DoubleConst.make(value);
            }
	}
	public String toString() {
	    return "DoubleAbsValue["+value+"]";
	}
    }
    
    static class ObjAbsValue extends AbsValue {
	PTSet ps;
	boolean nonzero;
	int lengthLB; // length lower bound
	ObjectLocation objLocation;
	
	ObjAbsValue(PTSet ps,
		    boolean nonzero,
		    int lengthLB,
		    ObjectLocation objLocation) {
	    assert ps.leastType().isObject();
	    assert !ps.isBottom();
	    this.ps=ps;
	    this.nonzero=nonzero;
	    this.lengthLB=lengthLB;
	    this.objLocation=objLocation;
	    assert !(lengthLB>0 && !ps.leastType().isArray()) : this;
	    if (Global.verbosity>=5) {
		Global.log.println("Making abs value: "+this);
	    }
	}
	static ObjAbsValue makeExactNonZero(Exectype t,
					    ObjectLocation objLocation) {
	    return makeExactNonZero(Type.make(t),objLocation);
	}
	static ObjAbsValue makeNonZero(Exectype t,
				       ObjectLocation objLocation) {
	    return makeNonZero(Type.make(t),objLocation);
	}
	static AbsValue make(Exectype t,
			     ObjectLocation objLocation) {
	    return make(Type.make(t),objLocation);
	}
	static ObjAbsValue makeExactNonZero(Type t,
					    ObjectLocation objLocation) {
	    return new ObjAbsValue(new TypeBound(t,TypeBoundMode.EXACT),
				   true,0,objLocation);
	}
	static ObjAbsValue makeExactNonZeroArray(Type t,
						 int length,
						 ObjectLocation objLocation) {
	    return new ObjAbsValue(new TypeBound(t,TypeBoundMode.EXACT),
				   true,length,
				   objLocation);
	}
	static ObjAbsValue makeNonZero(Type t,
				       ObjectLocation objLocation) {
	    assert t.isObject() && t.hasInstances();
	    return new ObjAbsValue(new TypeBound(t),
				   true,0,objLocation);
	}
	static AbsValue make(Type t,
			     ObjectLocation objLocation) {
	    assert t.isObject();
	    if (t.hasInstances()) {
		return new ObjAbsValue(new TypeBound(t),
				       false,0,objLocation);
	    } else {
		return NULL;
	    }
	}
	static AbsValue makeExact(Type t,
                                  ObjectLocation objLocation) {
	    assert t.isObject();
	    if (t.hasInstances()) {
		return new ObjAbsValue(new TypeBound(t,TypeBoundMode.EXACT),
				       false,0,objLocation);
	    } else {
		return NULL;
	    }
	}
	static ObjAbsValue makeNonZero(PTSet ps,
				       ObjectLocation objLocation) {
	    assert !ps.isBottom();
	    return new ObjAbsValue(ps,true,0,objLocation);
	}
	static AbsValue make(PTSet ps,
			     ObjectLocation objLocation) {
	    if (!ps.isBottom()) {
		return new ObjAbsValue(ps,false,0,objLocation);
	    } else {
		return NULL;
	    }
	}
	boolean isZero() {
	    return false;
	}
	boolean isNonZero() {
	    return nonzero;
	}
	ObjectLocation location() {
	    return objLocation;
	}
	AbsValue asZero(Exectype t) {
	    assert !nonzero;
	    return NULL;
	}
	AbsValue asHeap() {
	    return new ObjAbsValue(ps,nonzero,lengthLB,ObjectLocation.IN_HEAP);
	}
	AbsValue asNonZero() {
	    if (nonzero) {
		return this;
	    } else {
		return new ObjAbsValue(ps,true,lengthLB,objLocation);
	    }
	}
	AbsValue eq(AbsValue other) {
	    // NOTE: this could be made lots more powerful.
	    if (nonzero && other.isZero()) {
		return ZERO;
	    } else {
		return TOP;
	    }
	}
	AbsValue neq(AbsValue other) {
	    // NOTE: this could be made lots more powerful.
	    if (nonzero && other.isZero()) {
		return ONE;
	    } else {
		return TOP;
	    }
	}
	AbsValue cast(Type t) {
	    // NOTE: cast succeeds for NULL, so this does not make the
	    // value nonnull.
	    if (t==Type.POINTER) {
		if (isNonZero()) {
		    return NONZERO;
		} else {
		    return TOP;
		}
	    } else if (!t.hasInstances()) {
		// this case is not just an optimization ... it protects against
		// assertion failures if you have an interface that gets marked
		// as EXACT because it has no implementors, and we cast something
		// of an unrelated type to it.  without this check, you'd have
		// a proposed refinement of something EXACT to something that is
		// not a subtype.
		if (isNonZero()) {
		    return BOTTOM;
		} else {
		    return NULL;
		}
	    } else {
		assert t.isObject();
		PTSet newPS=ps.filter(t);
		if (Global.verbosity>=8) {
		    Global.log.println("When filtering "+this+" with "+t+": we got newPS = "+newPS);
		}
		if (newPS.isBottom()) {
		    if (isNonZero()) {
			return BOTTOM;
		    } else {
			return NULL;
		    }
		} else if (newPS==ps) {
		    return this;
		} else {
                    return new ObjAbsValue(newPS,
                                           nonzero,
                                           lengthLB,
                                           objLocation);
		}
	    }
	}
        AbsValue castExact(Type t) {
            if (t.isObject() && t.hasInstances()) {
                // FIXME this looks quite wrong.
		assert t.isObject();
                if (t==ps.leastType() && ps.isTypeExact()) {
                    return this;
                } else {
                    return new ObjAbsValue(new TypeBound(t,TypeBoundMode.EXACT),
                                           nonzero,
                                           lengthLB,
                                           objLocation);
                }
	    } else {
                return cast(t);
            }
        }
	private boolean matches(ObjAbsValue other) {
	    return ps==other.ps
		&& nonzero==other.nonzero
		&& lengthLB==other.lengthLB
		&& objLocation==other.objLocation;
	}
	AbsValue merge(AbsValue other_) {
	    if (other_==BOTTOM) {
		return this;
	    } else if (other_==NONZERO && nonzero) {
		return NONZERO;
	    } else if (other_.isZero()) {
		if (nonzero) {
		    return new ObjAbsValue(ps,false,lengthLB,objLocation);
		} else {
		    return this;
		}
	    } else if (other_ instanceof ObjAbsValue) {
		ObjAbsValue other=(ObjAbsValue)other_;
		if (matches(other)) {
		    return this;
		} else {
		    other=new ObjAbsValue(ps.union(other.ps),
					  nonzero&other.nonzero,
					  Math.min(lengthLB,
						   other.lengthLB),
					  objLocation.lub(other.objLocation));
		    if (matches(other)) {
			return this;
		    } else {
			return other;
		    }
		}
	    } else {
		return TOP;
	    }
	}
	public String toString() {
	    return "ObjAbsValue["+ps+(nonzero?" nonzero ":" ")+
		(lengthLB>0?" length>="+lengthLB:"")+" "+objLocation+"]";
	}
    }
    
    static AbsValue NONZERO = new AbsValue(){
	    boolean isNonZero() { return true; }
	    AbsValue asZero(Exectype t) { throw new Error(); }
	    AbsValue cast(Type t) { return super.cast(t).asNonZero(); }
	    AbsValue neg() { return NONZERO; }
	    AbsValue eq(AbsValue other) {
		if (other.isZero()) {
		    return ZERO;
		} else {
		    return TOP;
		}
	    }
	    AbsValue neq(AbsValue other) {
		if (other.isZero()) {
		    return ONE;
		} else {
		    return TOP;
		}
	    }
	    AbsValue merge(AbsValue other) {
		if (other==BOTTOM) {
		    return this;
		} else if (other.isNonZero()) {
		    return this;
		} else {
		    return TOP;
		}
	    }
	    public String toString() { return "NONZERO"; }
	};
    
    static AbsValue CVARADDRESS = new AbsValue(){
	    boolean isNonZero() { return true; }
	    AbsValue asZero(Exectype t) { throw new Error(); }
	    AbsValue cast(Type t) { return super.cast(t).asNonZero(); }
	    AbsValue neg() { return NONZERO; }
	    AbsValue eq(AbsValue other) {
		if (other.isZero()) {
		    return ZERO;
		} else {
		    return TOP;
		}
	    }
	    AbsValue neq(AbsValue other) {
		if (other.isZero()) {
		    return ONE;
		} else {
		    return TOP;
		}
	    }
	    AbsValue merge(AbsValue other) {
		if (other==BOTTOM) {
		    return this;
		} else if (other==CVARADDRESS) {
		    return this;
                } else if (other.isNonZero()) {
                    return NONZERO;
		} else {
		    return TOP;
		}
	    }
            AbsValue add(AbsValue other) {
                if (other instanceof PointerAbsValue) {
                    PointerAbsValue pav=(PointerAbsValue)other;
                    if (pav.value>=-4095 && pav.value<1024*1024) {
                        return NONZERO;
                    }
                }
                return TOP;
            }
	    public String toString() { return "CVARADDRESS"; }
	};
    
    static PointerAbsValue NULL = new PointerAbsValue(0);
    static IntAbsValue ZERO = new IntAbsValue(0);
    static IntAbsValue ONE = new IntAbsValue(1);
    static IntAbsValue MINUSONE = new IntAbsValue(-1);

    static AbsValue TOP = new AbsValue(){
	    AbsValue asZero(Exectype t) {
		switch (t.effectiveBasetype()) {
		case INT: return ZERO;
		case LONG: return new LongAbsValue(0);
		case FLOAT: return new FloatAbsValue(0.0f);
		case DOUBLE: return new DoubleAbsValue(0.0);
		case OBJECT:
		case NULL:
		case POINTER: return NULL;
		default: throw new Error();
		}
	    }
	    public String toString() { return "TOP"; }
	};
    
    static AbsValue BOTTOM = new AbsValue(){
	    AbsValue asZero(Exectype t) { return this; }
	    AbsValue asNonZero() { return this; }
	    AbsValue add(AbsValue other) { return this; }
	    AbsValue sub(AbsValue other) { return this; }
	    AbsValue mul(AbsValue other) { return this; }
	    AbsValue div(AbsValue other) { return this; }
	    AbsValue mod(AbsValue other) { return this; }
	    AbsValue neg() { return this; }
	    AbsValue not() { return this; }
	    AbsValue bitNot() { return this; }
	    AbsValue shl(AbsValue other) { return this; }
	    AbsValue shr(AbsValue other) { return this; }
	    AbsValue ushr(AbsValue other) { return this; }
	    AbsValue and(AbsValue other) { return this; }
	    AbsValue or(AbsValue other) { return this; }
	    AbsValue xor(AbsValue other) { return this; }
	    AbsValue compareG(AbsValue other) { return this; }
	    AbsValue compareL(AbsValue other) { return this; }
	    AbsValue eq(AbsValue other) { return this; }
	    AbsValue neq(AbsValue other) { return this; }
	    AbsValue lessThan(AbsValue other) { return this; }
	    AbsValue lessThanEq(AbsValue other) { return this; }
	    AbsValue uLessThan(AbsValue other) { return this; }
	    AbsValue uLessThanEq(AbsValue other) { return this; }
	    AbsValue cast(Type t) { return this; }
	    AbsValue castExact(Type t) { return this; }
            AbsValue fiat(Type t) { return this; }
	    boolean replaceInst(Instruction i) {
		return false;
	    }
	    Arg replaceArg(Arg a) {
		return a;
	    }
	    AbsValue merge(AbsValue other) {
		return other;
	    }
	    public String toString() { return "BOTTOM"; }
	};
    
    static final class ArrayStoreChecked {
        Arg target;
        Arg source;
        
        ArrayStoreChecked(Arg target,
                          Arg source) {
            this.target=target;
            this.source=source;
        }
        
        public int hashCode() {
            return target.structuralHashCode()+source.structuralHashCode();
        }
        
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof ArrayStoreChecked)) return false;
            ArrayStoreChecked other=(ArrayStoreChecked)other_;
            return target==other.target
                && source==other.source;
        }
    }
    
    public String toString() {
        return "IntraproceduralCFA["+mode+"]";
    }
}


