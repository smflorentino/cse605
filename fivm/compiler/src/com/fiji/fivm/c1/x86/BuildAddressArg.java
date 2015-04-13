/*
 * BuildAddressArg.java
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

package com.fiji.fivm.c1.x86;

import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.AbsIndexMem;
import com.fiji.fivm.c1.x86.arg.AbsMem;
import com.fiji.fivm.c1.x86.arg.AbsSymMem;
import com.fiji.fivm.c1.x86.arg.IndexMem;
import com.fiji.fivm.c1.x86.arg.LArg;
import com.fiji.fivm.c1.x86.arg.OffMem;
import com.fiji.fivm.c1.x86.arg.OffScratchSlot;
import com.fiji.fivm.c1.x86.arg.OffSymMem;

public class BuildAddressArg extends AddressModeBU {
    ToLIR context;
    LArg result;
    
    public BuildAddressArg(ToLIR context) {
        this.context=context;
    }
    
    // FIXME: everywhere that we do AbsSymMem, we should have a mode where we do
    // AbsMem in case we're in JIT mode.
    // NO: we should convert GetCVarAddress to an immediate at MIR.

    /** cVar := GetCVarAddress() */
    protected boolean visitCVar(Operation getCVarAddress) {
        CFieldInst cfi=(CFieldInst)getCVarAddress;
        if (cfi.field() instanceof CLocal) {
            result=new OffScratchSlot(context.localMap.get(cfi.field()),0);
        } else {
            result=new AbsSymMem((Linkable)context.translateField(cfi.field()),0);
        }
        return true;
    }
    /** addCVar := Add(GetCVarAddress(), a) */
    protected boolean visitAddCVar(Operation add,
                                   Instruction getCVarAddress) {
        CFieldInst cfi=(CFieldInst)getCVarAddress;
        if (add.rhs(1) instanceof PointerConst) {
            PointerConst pc=(PointerConst)add.rhs(1);
            if (pc.is32()) {
                if (cfi.field() instanceof CLocal) {
                    result=new OffScratchSlot(context.localMap.get(cfi.field()),pc.value32());
                } else {
                    result=new AbsSymMem((Linkable)context.translateField(cfi.field()),
                                         pc.value32());
                }
                return true;
            } else {
                return false;
            }
        } else {
            assert add.rhs(1) instanceof Var;
            if (cfi.field() instanceof CLocal ||
                context.blo.skipped.contains(add.rhs(1))) {
                return false;
            } else {
                result=new OffSymMem((Linkable)context.translateField(cfi.field()),
                                     0,
                                     context.tmpForVar(add.rhs(1)));
                return true;
            }
        }
    }
    /** cArg := GetCArgAddress() */
    protected boolean visitCArg(Operation getCArgAddress) {
        result=context.argSlot(getCArgAddress);
        return true;
    }
    /** addCArg := Add(GetCArgAddress(),$a) */
    protected boolean visitAddCArg(Operation add,
                                   Instruction getCArgAddress) {
        Arg.IntConst ic=(Arg.IntConst)add.rhs(1);
        if (ic.is32()) {
            result=context.argSlot(getCArgAddress,ic.value32());
            return true;
        } else {
            return false;
        }
    }
    /** addAddCVar := add1=Add(add2=Add(GetCVarAddress(), %a), $b) */
    protected boolean visitAddAddCVar(Operation add1,
                                      Instruction add2,
                                      Instruction getCVarAddress) {
        if (context.blo.skipped.contains(add2.rhs(1))) {
            return false;
        } else {
            CFieldInst cfi=(CFieldInst)getCVarAddress;
            if (cfi.field() instanceof CLocal) {
                return false;
            } else {
                PointerConst pc=(PointerConst)add1.rhs(1);
                if (pc.is32()) {
                    result=new OffSymMem((Linkable)context.translateField(cfi.field()),
                                         pc.value32(),
                                         context.tmpForVar(add2.rhs(1)));
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /** addAddAdd := add1=Add(add2=Add(add3=Add(!%a, $b), $c), $d) */
    protected boolean visitAddAddAdd(Operation add1,
                                     Instruction add2,
                                     Instruction add3) {
        if (!add2.lhs().isInteger() ||
            context.blo.skipped.contains(add3.rhs(0))) {
            return false;
        }
        Arg.IntConst offset1=(Arg.IntConst)add1.rhs(1);
        Arg.IntConst offset2=(Arg.IntConst)add2.rhs(1);
        Arg.IntConst offset3=(Arg.IntConst)add3.rhs(1);
        long offset=offset1.longValue()+offset2.longValue()+offset3.longValue();
        if (offset>=Integer.MIN_VALUE && offset<=Integer.MAX_VALUE) {
            result=new OffMem((int)offset,
                              context.tmpForVar(add3.rhs(0)));
            return true;
        } else {
            return false;
        }
    }

    private boolean doIndexMem(Arg shiftAmount_,
                               Arg offset_,
                               Arg base,
                               Arg index) {
        if (context.blo.skipped.contains(base) ||
            context.blo.skipped.contains(index)) {
            return false;
        } else {
            IntConst shiftAmount=(IntConst)shiftAmount_;
            if (Scale.hasShift(shiftAmount.value())) {
                Arg.IntConst offset=(Arg.IntConst)offset_;
                if (offset.is32()) {
                    result=new IndexMem(offset.value32(),
                                        context.tmpForVar(base),
                                        context.tmpForVar(index),
                                        Scale.shift(shiftAmount.value()));
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
    
    /** addAddShl1 := add1=Add(add2=Add(Shl(!%a, $b), %c), $d) */
    protected boolean visitAddAddShl1(Operation add1,
                                      Instruction add2,
                                      Instruction shl) {
        return doIndexMem(shl.rhs(1),
                          add1.rhs(1),
                          add2.rhs(1),
                          shl.rhs(0));
    }
    /** addAddShl2 := add1=Add(add2=Add(!%a, Shl(!%b, $c)), $d) */
    protected boolean visitAddAddShl2(Operation add1,
                                      Instruction add2,
                                      Instruction shl) {
        return doIndexMem(shl.rhs(1),
                          add1.rhs(1),
                          add2.rhs(0),
                          shl.rhs(0));
    }
    /** addAddShl3 := add1=Add(add2=Add(!%a, $b), Shl(!%c, $d)) */
    protected boolean visitAddAddShl3(Operation add1,
                                      Instruction add2,
                                      Instruction shl) {
        return doIndexMem(shl.rhs(1),
                          add2.rhs(1),
                          add2.rhs(0),
                          shl.rhs(0));
    }
    
    /** addShl1 := Add(Shl(!%a, $b), $c) */
    protected boolean visitAddShl1(Operation add,
                                   Instruction shl) {
        if (context.blo.skipped.contains(shl.rhs(0))) {
            return false;
        } else {
            IntConst shiftAmount=(IntConst)shl.rhs(1);
            if (Scale.hasShift(shiftAmount.value())) {
                Arg.IntConst offset=(Arg.IntConst)add.rhs(1);
                if (offset.is32()) {
                    result=new AbsIndexMem(offset.value32(),
                                           context.tmpForVar(shl.rhs(0)),
                                           Scale.shift(shiftAmount.value()));
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
    /** addAdd1 := add1=Add(add2=Add(!%a, %b), $c) */
    protected boolean visitAddAdd1(Operation add1,
                                   Instruction add2) {
        if (!add2.lhs().isInteger() ||
            context.blo.skipped.contains(add2.rhs(0)) ||
            context.blo.skipped.contains(add2.rhs(1))) {
            return false;
        }
        Arg.IntConst offset=(Arg.IntConst)add1.rhs(1);
        if (offset.is32()) {
            result=new IndexMem(offset.value32(),
                                context.tmpForVar(add2.rhs(0)),
                                context.tmpForVar(add2.rhs(1)),
                                Scale.ONE);
            return true;
        } else {
            return false;
        }
    }
    /** addShl2 := Add(Shl(!%a, $b), %c) */
    protected boolean visitAddShl2(Operation add,
                                   Instruction shl) {
        if (context.blo.skipped.contains(add.rhs(1)) ||
            context.blo.skipped.contains(shl.rhs(0))) {
            return false;
        }
        IntConst shiftAmount=(IntConst)shl.rhs(1);
        if (Scale.hasShift(shiftAmount.value())) {
            result=new IndexMem(0,
                                context.tmpForVar(add.rhs(1)),
                                context.tmpForVar(shl.rhs(0)),
                                Scale.shift(shiftAmount.value()));
            return true;
        } else {
            return false;
        }
    }
    /** addAdd2 := add1=Add(add2=Add(!%a, $b), $c) */
    protected boolean visitAddAdd2(Operation add1,
                                   Instruction add2) {
        if (!add2.lhs().isInteger() ||
            context.blo.skipped.contains(add2.rhs(0))) {
            return false;
        }
        Arg.IntConst offset1=(Arg.IntConst)add1.rhs(1);
        Arg.IntConst offset2=(Arg.IntConst)add2.rhs(1);
        long offset=offset1.longValue()+offset2.longValue();
        if (offset>=Integer.MIN_VALUE && offset<=Integer.MAX_VALUE) {
            result=new OffMem((int)offset,
                              context.tmpForVar(add2.rhs(0)));
            return true;
        } else {
            return false;
        }
    }
    /** addShl3 := Add(!%a, Shl(!%b, $c)) */
    protected boolean visitAddShl3(Operation add,
                                   Instruction shl) {
        if (context.blo.skipped.contains(add.rhs(0)) ||
            context.blo.skipped.contains(shl.rhs(0))) {
            return false;
        }
        IntConst shiftAmount=(IntConst)shl.rhs(1);
        if (Scale.hasShift(shiftAmount.value())) {
            result=new IndexMem(0,
                                context.tmpForVar(add.rhs(0)),
                                context.tmpForVar(shl.rhs(0)),
                                Scale.shift(shiftAmount.value()));
            return true;
        } else {
            return false;
        }
    }        
    /** add := Add(a, b) */
    protected boolean visitAdd(Operation add) {
        if (!add.lhs().isInteger()) {
            return false;
        }
        if (add.rhs(1) instanceof Arg.IntConst) {
            if (context.blo.skipped.contains(add.rhs(0))) {
                return false;
            }
            Arg.IntConst offset=(Arg.IntConst)add.rhs(1);
            if (offset.is32()) {
                result=new OffMem(offset.value32(),
                                  context.tmpForVar(add.rhs(0)));
                return true;
            }
        } else {
            assert add.rhs(1) instanceof Var;
            if (context.blo.skipped.contains(add.rhs(0)) ||
                context.blo.skipped.contains(add.rhs(1))) {
                return false;
            }
            result=new IndexMem(0,
                                context.tmpForVar(add.rhs(0)),
                                context.tmpForVar(add.rhs(1)),
                                Scale.ONE);
            return true;
        }
        return false;
    }
    /** shl := Shl(!%a, $b) */
    protected boolean visitShl(Operation shl) {
        if (context.blo.skipped.contains(shl.rhs(0))) {
            return false;
        }
        IntConst shiftAmount=(IntConst)shl.rhs(1);
        if (Scale.hasShift(shiftAmount.value())) {
            result=new AbsIndexMem(0,
                                   context.tmpForVar(shl.rhs(0)),
                                   Scale.shift(shiftAmount.value()));
            return true;
        } else {
            return false;
        }
    }
    
    public LArg build(Operation o) {
        assert result==null;
        if (accept(o)) {
            assert result!=null;
            LArg result=this.result;
            this.result=null;
            return result;
        } else {
            assert result==null;
            return null;
        }
    }
    
    public LArg build(Arg a) {
        assert result==null;
        if (a instanceof PointerConst) {
            PointerConst pc=(PointerConst)a;
            if (pc.is32()) {
                return new AbsMem(((PointerConst)a).value32());
            } else {
                return null;
            }
        } else {
            if (accept(a)) {
                assert result!=null;
                LArg result=this.result;
                this.result=null;
                return result;
            } else {
                assert result==null;
                return null;
            }
        }
    }
}

