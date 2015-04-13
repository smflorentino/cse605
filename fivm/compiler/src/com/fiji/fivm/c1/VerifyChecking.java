/*
 * VerifyChecking.java
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

public class VerifyChecking extends CodePhase {
    public VerifyChecking(Code c) { super(c); }
    
    public void visitCode() {
        boolean safe=true;
        
        // see if there are any pointers
        for (Var v : code.vars()) {
            if (v.type()==Exectype.POINTER) {
                if (Global.verbosity>=1) {
                    Global.log.println(code.shortName()+": uses pointers");
                }
                safe=false;
                break;
            }
        }
        
        // see if there are any unsafe casts
    unsafeCastFinder:
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                if (i.opcode()==OpCode.Cast) {
                    TypeInst ti=(TypeInst)i;
                    if ((ti.getType().isObject() &&
                         !ti.rhs(0).type().isSubtypeOf(ti.getType().asExectype())) ||
                        (ti.getType().isObject()!=ti.rhs(0).type().isObject())) {
                        if (Global.verbosity>=1) {
                            Global.log.println(code.shortName()+": performs unsafe casts");
                        }
                        safe=false;
                        break unsafeCastFinder;
                    }
                }
            }
        }
        
        // verify null checks and array bounds checks
        // FIXME
        
        
        // what about type checks?
        
        // FIXME!!!
        if (safe) {
            //code.verifiabilityMode=VerifiabilityMode.VERIFIABLE;
        } else {
            //code.verifiabilityMode=VerifiabilityMode.CONTAINS_UNSAFE;
        }
    }
    
    AbsValue valueFor(Arg arg,
                      HashMap< Var, AbsValue > values) {
        if (arg instanceof IntConst) {
            return IntAbsValue.make(((IntConst)arg).value());
        } else if (arg==Arg.NULL) {
            return ZERO;
        } else if (arg instanceof Var) {
            return values.get(arg);
        } else {
            return TOP;
        }
    }
    
    void makeNonZero(Arg arg,
                     HashMap< Var, AbsValue > values) {
        if (arg instanceof Var) {
            Var var=(Var)arg;
            AbsValue val=values.get(var);
            if (val instanceof ArrayAbsValue) {
                val=ArrayAbsValue.make(true,
                                       val.arrayLengthBound());
            } else if (val==TOP) {
                val=NONZERO;
            }
            values.put(var,val);
        }
    }
    
    AbsValue evaluate(Instruction i,
                      HashMap< Var, AbsValue > values) {
        switch (i.opcode()) {
        case GetArg: {
            ArgInst ai=(ArgInst)i;
            if (ai.getIdx()==0 && code.method()!=null && code.method().isInstance() &&
                code.param(0).isObject() && !code.method().noNullCheckOnAccess()) {
                return NONZERO;
            } else {
                return TOP;
            }
        }
        case GetString: return NONZERO;
        case GetField:
        case GetStatic: {
            HeapAccessInst hsi=(HeapAccessInst)i;
            if (hsi.fieldField().isNonZero()) {
                return NONZERO;
            } else {
                return TOP;
            }
        }
        case Mov:
        case Ipsilon:
        case LikelyZero:
        case LikelyNonZero:
        case SemanticallyLikelyZero:
        case SemanticallyLikelyNonZero:
        case Phi: return valueFor(i.rhs(0),values);
        case Add: return add(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Sub: return sub(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Mul: return mul(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Div: return div(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Mod: return mod(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Neg: return neg(valueFor(i.rhs(0),values));
        case Not: return not(valueFor(i.rhs(0),values));
        case BitNot: return bitNot(valueFor(i.rhs(0),values));
        case Shl: return shl(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Shr: return shr(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Ushr: return ushr(valueFor(i.rhs(0),values),
                               valueFor(i.rhs(1),values));
        case And: return and(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case Or: return or(valueFor(i.rhs(0),values),
                           valueFor(i.rhs(1),values));
        case Xor: return xor(valueFor(i.rhs(0),values),
                             valueFor(i.rhs(1),values));
        case LessThan: return lessThan(valueFor(i.rhs(0),values),
                                       valueFor(i.rhs(1),values));
        case ULessThan: return uLessThan(valueFor(i.rhs(0),values),
                                         valueFor(i.rhs(1),values));
        case CheckDivisor:
        case NullCheck:
        case ThrowRTEOnZero:
            makeNonZero(i.rhs(0),values);
            return TOP;
            // FIXME implement more cases!
        default: return TOP;
        }
    }
    
    static AbsValue add(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()+b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue sub(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()-b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue mul(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()*b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue div(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()/b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue mod(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()%b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue neg(AbsValue a) {
        if (a.intValueKnown()) {
            return IntAbsValue.make(-a.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue bitNot(AbsValue a) {
        if (a.intValueKnown()) {
            return IntAbsValue.make(~a.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue not(AbsValue a) {
        if (a.isNonZero()) {
            return ZERO;
        } else if (a.isZero()) {
            return IntAbsValue.make(1);
        } else {
            return TOP;
        }
    }
    
    static AbsValue shl(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()<<b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue shr(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()>>b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue ushr(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()>>>b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue and(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()&b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue or(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()|b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue xor(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            return IntAbsValue.make(a.getValue()^b.getValue());
        } else {
            return TOP;
        }
    }
    
    static AbsValue eq(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            if (a.getValue()==b.getValue()) {
                return IntAbsValue.make(1);
            } else {
                return IntAbsValue.make(0);
            }
        } else {
            return TOP;
        }
    }
    
    static AbsValue neq(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            if (a.getValue()!=b.getValue()) {
                return IntAbsValue.make(1);
            } else {
                return IntAbsValue.make(0);
            }
        } else {
            return TOP;
        }
    }
    
    static AbsValue lessThan(AbsValue a,AbsValue b) {
        if (a.intValueKnown() && b.intValueKnown()) {
            if (a.getValue()<b.getValue()) {
                return IntAbsValue.make(1);
            } else {
                return IntAbsValue.make(0);
            }
        } else {
            return TOP;
        }
    }
    
    static AbsValue uLessThan(AbsValue a,AbsValue b) {
        if (a.isZero() && b.isNonZero()) {
            return IntAbsValue.make(1);
        } else if (b.isZero()) {
            return IntAbsValue.make(0);
        } else if (a.intValueKnown() && b.intValueKnown()) {
            if (a.getValue()>=0) {
                if (b.getValue()>=0) {
                    if (a.getValue()<b.getValue()) {
                        return IntAbsValue.make(1);
                    } else {
                        return IntAbsValue.make(0);
                    }
                } else {
                    return IntAbsValue.make(1);
                }
            } else {
                if (b.getValue()>=0) {
                    return IntAbsValue.make(0);
                } else {
                    if (a.getValue()<b.getValue()) {
                        return IntAbsValue.make(1);
                    } else {
                        return IntAbsValue.make(0);
                    }
                }
            }
        } else {
            return TOP;
        }
    }
    
    static AbsValue cast(AbsValue a,Exectype from,Type to) {
        if (a.isZero()) {
            return a;
        } else if (a.intValueKnown()) {
            switch (to.effectiveBasetype()) {
            case BOOLEAN:
                if (a.getValue()!=0) {
                    return IntAbsValue.make(1);
                } else {
                    return IntAbsValue.make(0);
                }
            case BYTE: return IntAbsValue.make((byte)a.getValue());
            case CHAR: return IntAbsValue.make((char)a.getValue());
            case SHORT: return IntAbsValue.make((short)a.getValue());
            case INT: return a;
            default:
                if (a.isNonZero()) {
                    return NONZERO;
                } else {
                    return TOP;
                }
            }
        } else if (from.isObject() && to.isObject()) {
            return a;
        } else {
            return TOP;
        }
    }
    
    static abstract class AbsValue {
        boolean isNonZero() {
            return false;
        }
        boolean intValueKnown() {
            return false;
        }
        int getValue() {
            throw new Error();
        }
        final boolean isZero() {
            return intValueKnown() && getValue()==0;
        }
        int arrayLengthBound() {
            return 0;
        }
        AbsValue mergeImpl(AbsValue other) {
            return TOP;
        }
        final AbsValue merge(AbsValue other) {
            if (other==BOTTOM || this==other) {
                return this;
            } else if (other==TOP) {
                return TOP;
            } else {
                return mergeImpl(other);
            }
        }
    }
    
    static AbsValue TOP = new AbsValue() {
        };
    
    static AbsValue BOTTOM = new AbsValue() {
            AbsValue mergeImpl(AbsValue other) {
                return other;
            }
        };
    
    static AbsValue NONZERO = new AbsValue() {
            boolean isNonZero() {
                return true;
            }
            AbsValue mergeImpl(AbsValue other) {
                if (other.isNonZero()) {
                    return NONZERO;
                } else {
                    return TOP;
                }
            }
        };
    
    static IntAbsValue ZERO = new IntAbsValue(0);
    
    static class IntAbsValue extends AbsValue {
        private int value;
        
        IntAbsValue(int value) {
            this.value=value;
        }
        
        static IntAbsValue make(int value) {
            if (value==0) {
                return ZERO;
            } else {
                return new IntAbsValue(value);
            }
        }
        
        boolean isNonZero() {
            return value!=0;
        }
        
        boolean intValueKnown() {
            return true;
        }
        
        int getValue() {
            return value;
        }
        
        AbsValue mergeImpl(AbsValue other) {
            if (other.intValueKnown()) {
                if (other.getValue()==value) {
                    return this;
                } else if (isNonZero() && other.isNonZero()) {
                    return NONZERO;
                } else {
                    return TOP;
                }
            } else if (isNonZero() && other.isNonZero()) {
                return NONZERO;
            } else {
                return TOP;
            }
        }
    }
    
    static class ArrayAbsValue extends AbsValue {
        private boolean nonzero;
        private int length;
        
        private ArrayAbsValue(boolean nonzero,
                              int length) {
            this.nonzero=nonzero;
            this.length=length;
        }
        
        static AbsValue make(boolean nonzero,
                             int length) {
            if (length>0) {
                return new ArrayAbsValue(nonzero,
                                         length);
            } else if (nonzero) {
                return NONZERO;
            } else {
                return TOP;
            }
        }
        
        boolean isNonZero() {
            return nonzero;
        }
        
        int arrayLengthBound() {
            return length;
        }
        
        AbsValue mergeImpl(AbsValue other) {
            if (other.arrayLengthBound()>=length &&
                (other.isNonZero()&nonzero)==nonzero) {
                return this;
            } else if (other.arrayLengthBound()>0) {
                return ArrayAbsValue.make(nonzero&other.isNonZero(),
                                          Math.min(length,other.arrayLengthBound()));
            } else if (isNonZero() && other.isNonZero()) {
                return NONZERO;
            } else {
                return TOP;
            }
        }
    }
}

