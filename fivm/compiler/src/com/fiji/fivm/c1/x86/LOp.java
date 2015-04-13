/*
 * LOp.java
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

import com.fiji.fivm.*;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.*;

import java.util.*;

public class LOp extends LNode {

    LOpCode opcode;
    LType type;
    LArg[] rhs;
    
    LHeader head;
    
    public LOp(LOpCode opcode,
               LType type,
               LArg[] rhs) {
        assert opcode!=null;
        assert type!=null;
        assert rhs!=null;
	this.opcode=opcode;
        this.type=type;
	this.rhs=rhs;
    }
    
    public LType type() {
        return type;
    }
    
    public int nMems() {
        int result=0;
        for (LArg a : rhs()) {
            if (a.memory()) {
                result++;
            }
        }
        return result;
    }
    
    public int nImmediates() {
        int result=0;
        for (LArg a : rhs()) {
            if (a.immediate()) {
                result++;
            }
        }
        return result;
    }
    
    public boolean sideEffect() {
        return opcode.sideEffect() || defsMemory();
    }

    private void assertHasCarry() {
        LOp cur=(LOp)prev;
        try {
            for (;/* terminates on cast failure when it gets to the header */;cur=(LOp)cur.prev) {
                switch (opcode) {
                case Adc:
                    if (cur.opcode==LOpCode.Add || cur.opcode==LOpCode.Neg) return;
                    break;
                case Sbb:
                    if (cur.opcode==LOpCode.Sub) return;
                    break;
                default: break;
                }
                assert cur.opcode==LOpCode.Mov; // allow for spills
            }
        } catch (Throwable e) {
            throw new CompilerException(
                "Failed to find carry setting operation from "+this+"; instead found "+cur);
        }
    }
    
    public void checkSanity() {
        switch (opcode) {
        case ToSingle:
        case ToDouble:
        case SignExtL:
        case SignExtQ:
        case ZeroExtL:
        case ZeroExtQ:
        case SetNotFGT:
        case SetNotFGTEq:
        case SetFGreaterThan:
        case SetFGTEq:
        case SetFEqOrUnordered:
        case SetFNeqAndOrdered:
        case FiatToInt:
        case FiatToFloat:
            break;
        default:
            for (LArg a : rhs()) {
                assert a.compatibleWith(type);
            }
        }
        opcode.form().verifyRhs(rhs());
        assert opcode.footer()==(this instanceof LFooter);
        switch (opcode) {
        case Jump:
            assert type==LType.Void;
            break;
        case Call:
            assert type==LType.ptr();
            break;
        case ToSingle:
        case ToDouble:
            switch (type) {
            case Long:
            case Quad: /* on 32-bit targets, the source of this will be turned into a memory
                          location by Expand32. FIXME we're not doing that yet.*/
            case Single:
            case Double:
                break;
            default:
                throw new CompilerException("bad source type for "+opcode+": "+type);
            }
            assert rhs(0).compatibleWith(type);
            assert rhs(1).compatibleWith(Kind.FLOAT);
            break;
        case FiatToInt:
        case FiatToFloat:
            assert type==LType.Long || type==LType.Quad;
            break;
        case PushIntToFP:
            assert type==LType.Quad; // this is kinda retarded
            assert Global.pointerSize==4;
            break;
        case First32:
        case Second32:
            assert type==LType.Quad;
            break;
        case SignExtL:
        case ZeroExtL:
            assert type.isInt();
            assert type.size()<=4;
            assert rhs[0].compatibleWith(type);
            assert rhs[1].compatibleWith(LType.Long);
            break;
        case SignExtQ:
        case ZeroExtQ:
            assert type.isInt();
            assert rhs[0].compatibleWith(type);
            assert rhs[1].compatibleWith(LType.Quad);
            break;
        case Add:
        case Sub:
        case Shl:
        case Shld:
        case Shr:
        case Shrd:
        case Muld:
        case Ushr:
        case And:
        case Or:
        case Xor:
        case BitNot:
        case BranchULessThan:
        case BranchULTEq:
        case BranchLessThan:
        case BranchLTEq:
        case BranchUGreaterThan:
        case BranchUGTEq:
        case BranchGreaterThan:
        case BranchGTEq:
        case BranchAndZero:
        case BranchAndNotZero:
        case BranchEq:
        case BranchNeq:
        case SetULessThan:
        case SetULTEq:
        case SetUGreaterThan:
        case SetUGTEq:
        case SetLessThan:
        case SetLTEq:
        case SetGreaterThan:
        case SetGTEq:
        case SetEq:
        case SetNeq:
        case SetAndZero:
        case SetAndNotZero:
        case Div:
        case UDiv:
        case Neg:
            assert type.isInt();
            break;
        case Adc:
        case Sbb:
            assert type.isInt();
            assertHasCarry();
            break;
        case Mul:
            assert type==LType.Long || type==LType.Quad || type==LType.Half;
            break;
        case Megamul:
            assert type==LType.Long || type==LType.ptr() || type==LType.Half;
            break;
        case Cdq:
            assert type==LType.Long;
            break;
        case Lea:
            assert type.isLikeAddress();
            break;
        case FXor:
        case FAdd:
        case FSub:
        case FMul:
        case FDiv:
        case FSqrt:
        case BranchNotFGT:
        case BranchNotFGTEq:
        case SetNotFGT:
        case SetNotFGTEq:
        case BranchFGreaterThan:
        case BranchFGTEq:
        case SetFGreaterThan:
        case SetFGTEq:
        case SetFEqOrUnordered:
        case SetFNeqAndOrdered:
        case BranchFEqOrUnordered:
        case BranchFNeqAndOrdered:
            assert type.isFloat();
            assert rhs[0].compatibleWith(type);
            assert rhs[1].compatibleWith(type);
            break;
        case ResetFUnordered:
        case ResetFOrdered:
            assert rhs[0].compatibleWith(LType.Byte);
            break;
        case PushFP:
        case PopFP:
            assert type.isFloat();
            assert Global.pointerSize==4;
            break;
        case PushFP0:
        case PushFP1:
            assert type==LType.Double;
            assert Global.pointerSize==4;
            break;
        default:
            break;
        }
    }
    
    public LType typeOf(int i) {
        return opcode.typeOf(type,i);
    }
    
    public LType memType() {
        return opcode.memType(type);
    }
    
    public String asm(boolean reverseBranch) {
        StringBuilder buf=new StringBuilder();
        
        switch (opcode) {
        case SetLessThan:
        case SetLTEq:
        case SetULessThan:
        case SetULTEq:
        case SetEq:
        case SetNeq:
        case SetFEqOrUnordered:
        case SetFNeqAndOrdered:
        case BranchLessThan:
        case BranchLTEq:
        case BranchULessThan:
        case BranchULTEq:
        case BranchEq:
        case BranchNeq:
        case BranchFEqOrUnordered:
        case BranchFNeqAndOrdered:
        case SetGreaterThan:
        case SetGTEq:
        case SetNotFGT:
        case SetNotFGTEq:
        case SetFGreaterThan:
        case SetFGTEq:
        case SetUGreaterThan:
        case SetUGTEq:
        case BranchGreaterThan:
        case BranchGTEq:
        case BranchNotFGT:
        case BranchNotFGTEq:
        case BranchFGreaterThan:
        case BranchFGTEq:
        case BranchUGreaterThan:
        case BranchUGTEq: {
            
            if (type.isInt() &&
                rhs[0].variable() &&
                rhs[1]==Immediate.make(0)) {
                buf.append("\ttest"+type.asm()+" "+
                           rhs[0].asm(type,LType.ptr())+", "+
                           rhs[0].asm(type,LType.ptr())+"\n");
            } else {
                if (type.isInt()) {
                    buf.append("\tcmp");
                } else {
                    buf.append("\tucomis");
                }
                buf.append(type.asm()+" "+
                           rhs[1].asm(type,LType.ptr())+", "+
                           rhs[0].asm(type,LType.ptr())+"\n");
            }
            switch (opcode) {
            case SetLessThan:
                buf.append("\tsetl "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetGreaterThan:
                buf.append("\tsetg "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetFGreaterThan:
                buf.append("\tseta "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetLTEq:
                buf.append("\tsetle "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetGTEq:
                buf.append("\tsetge "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetFGTEq:
                buf.append("\tsetae "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetNotFGT:
                buf.append("\tsetna "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetNotFGTEq:
                buf.append("\tsetb "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetULessThan:
                buf.append("\tsetb ");
                buf.append(rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetUGreaterThan:
                buf.append("\tseta ");
                buf.append(rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetULTEq:
                buf.append("\tsetbe ");
                buf.append(rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetUGTEq:
                buf.append("\tsetae ");
                buf.append(rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetEq:
            case SetFEqOrUnordered:
                buf.append("\tsete ");
                buf.append(rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetNeq:
            case SetFNeqAndOrdered:
                buf.append("\tsetne ");
                buf.append(rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case BranchLessThan:
                if (reverseBranch) {
                    buf.append("\tjge ");
                } else {
                    buf.append("\tjl ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchGreaterThan:
                if (reverseBranch) {
                    buf.append("\tjle ");
                } else {
                    buf.append("\tjg ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchFGreaterThan:
                if (reverseBranch) {
                    buf.append("\tjna ");
                } else {
                    buf.append("\tja ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchLTEq:
                if (reverseBranch) {
                    buf.append("\tjg ");
                } else {
                    buf.append("\tjle ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchGTEq:
                if (reverseBranch) {
                    buf.append("\tjl ");
                } else {
                    buf.append("\tjge ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchFGTEq:
                if (reverseBranch) {
                    buf.append("\tjb ");
                } else {
                    buf.append("\tjae ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchNotFGT:
                if (reverseBranch) {
                    buf.append("\tja ");
                } else {
                    buf.append("\tjna ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchNotFGTEq:
                if (reverseBranch) {
                    buf.append("\tjae ");
                } else {
                    buf.append("\tjb ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchULessThan:
                if (reverseBranch) {
                    buf.append("\tjae ");
                } else {
                    buf.append("\tjb ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchUGreaterThan:
                if (reverseBranch) {
                    buf.append("\tjbe ");
                } else {
                    buf.append("\tja ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchULTEq:
                if (reverseBranch) {
                    buf.append("\tja ");
                } else {
                    buf.append("\tjbe ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchUGTEq:
                if (reverseBranch) {
                    buf.append("\tjb ");
                } else {
                    buf.append("\tjae ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchEq:
            case BranchFEqOrUnordered:
                if (reverseBranch) {
                    buf.append("\tjne ");
                } else {
                    buf.append("\tje ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchNeq:
            case BranchFNeqAndOrdered:
                if (reverseBranch) {
                    buf.append("\tje ");
                } else {
                    buf.append("\tjne ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            default: throw new CompilerException("bad opcode: "+opcode);
            }
            break;
        }
        case SetAndZero:
        case SetAndNotZero:
        case BranchAndZero:
        case BranchAndNotZero: {
            buf.append("\ttest"+type.asm()+" "+
                       rhs[1].asm(type,LType.ptr())+", "+
                       rhs[0].asm(type,LType.ptr())+"\n");
            switch (opcode) {
            case SetAndZero:
                buf.append("\tsete "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case SetAndNotZero:
                buf.append("\tsetne "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
                break;
            case BranchAndZero:
                if (reverseBranch) {
                    buf.append("\tjne ");
                } else {
                    buf.append("\tje ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            case BranchAndNotZero:
                if (reverseBranch) {
                    buf.append("\tje ");
                } else {
                    buf.append("\tjne ");
                }
                buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
                break;
            default: throw new CompilerException("bad opcode: "+opcode);
            }
            break;
        }
        case RebranchLessThan:
            if (reverseBranch) {
                buf.append("\tjge ");
            } else {
                buf.append("\tjl ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchGreaterThan:
            if (reverseBranch) {
                buf.append("\tjle ");
            } else {
                buf.append("\tjg ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchFGreaterThan:
            if (reverseBranch) {
                buf.append("\tjna ");
            } else {
                buf.append("\tja ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchLTEq:
            if (reverseBranch) {
                buf.append("\tjg ");
            } else {
                buf.append("\tjle ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchGTEq:
            if (reverseBranch) {
                buf.append("\tjl ");
            } else {
                buf.append("\tjge ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchFGTEq:
            if (reverseBranch) {
                buf.append("\tjb ");
            } else {
                buf.append("\tjae ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchNotFGT:
            if (reverseBranch) {
                buf.append("\tja ");
            } else {
                buf.append("\tjna ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchNotFGTEq:
            if (reverseBranch) {
                buf.append("\tjae ");
            } else {
                buf.append("\tjb ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchULessThan:
            if (reverseBranch) {
                buf.append("\tjae ");
            } else {
                buf.append("\tjb ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchUGreaterThan:
            if (reverseBranch) {
                buf.append("\tjbe ");
            } else {
                buf.append("\tja ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchULTEq:
            if (reverseBranch) {
                buf.append("\tjg ");
            } else {
                buf.append("\tjle ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchUGTEq:
            if (reverseBranch) {
                buf.append("\tjl ");
            } else {
                buf.append("\tjge ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchEq:
            if (reverseBranch) {
                buf.append("\tjne ");
            } else {
                buf.append("\tje ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchNeq:
            if (reverseBranch) {
                buf.append("\tje ");
            } else {
                buf.append("\tjne ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchFOrdered:
            if (reverseBranch) {
                buf.append("\tjp ");
            } else {
                buf.append("\tjnp ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case RebranchFUnordered:
            if (reverseBranch) {
                buf.append("\tjnp ");
            } else {
                buf.append("\tjp ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case LoadCAS:
            buf.append("\tlock; cmpxchg"+type.asm()+" "+
                       rhs[1].asm(type,LType.ptr())+", "+
                       rhs[0].asm(type,LType.ptr())+"\n");
            break;
        case TestCAS:
            buf.append("\tlock; cmpxchg"+type.asm()+" "+
                       rhs[1].asm(type,LType.ptr())+", "+
                       rhs[0].asm(type,LType.ptr())+"\n");
            buf.append("\tsete "+rhs[2].asm(LType.Byte,LType.ptr())+"\n");
            break;
        case BranchCASSucc:
            buf.append("\tlock; cmpxchg"+type.asm()+" "+
                       rhs[1].asm(type,LType.ptr())+", "+
                       rhs[0].asm(type,LType.ptr())+"\n");
            if (reverseBranch) {
                buf.append("\tjne ");
            } else {
                buf.append("\tje ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case BranchCASFail:
            buf.append("\tlock; cmpxchg"+type.asm()+" "+
                       rhs[1].asm(type,LType.ptr())+", "+
                       rhs[0].asm(type,LType.ptr())+"\n");
            if (reverseBranch) {
                buf.append("\tje ");
            } else {
                buf.append("\tjne ");
            }
            buf.append("FIVMR_LOCAL_SYMBOL("+conditionalSuccessor(reverseBranch).labelName()+")\n");
            break;
        case Fence:
            // do nothing for now...
            break;
        case Call:
            buf.append("\tcall "+rhs[0].callAsm(LType.ptr(),LType.ptr())+"\n");
            break;
        case Return:
            if (Settings.OMIT_FRAME_POINTER) {
                buf.append("\tret\n");
            } else {
                buf.append("\tleave\n");
                buf.append("\tret\n");
            }
            break;
        case NotReached:
            buf.append("\tint $3\n");
            break;
        case Jump:
            // this is handled by the end-of-BB handler.
            break;
        case AwesomeJump:
            buf.append("\tjmp "+rhs[0].callAsm(LType.ptr(),LType.ptr())+"\n");
            break;
        default: 
            buf.append("\t"+opcode.asm(type)+" ");
            for (int i=0;i<rhs.length;++i) {
                if (i!=0) {
                    buf.append(", ");
                }
                buf.append(rhs[i].asm(typeOf(i),memType()));
            }
            buf.append("\n");
            break;
        }
        return buf.toString();
    }
    
    public LinkableSet linkableSet() {
        LinkableSet result=null;
        for (LArg a : rhs) {
            Linkable cur=a.linkable();
            if (cur!=null) {
                if (result==null) {
                    result=new LinkableSet();
                }
                result.add(cur);
            }
        }
        return result;
    }
    
    public boolean canBeMemory(int i) {
        return form().canBeMemory(this,i);
    }
    
    public LOpCode opcode() {
	return opcode;
    }
    
    public OpForm form() {
        return opcode.form();
    }
    
    public LArg[] rhs() {
	return rhs;
    }
    
    public int nrhs() {
        return rhs().length;
    }
    
    public LArg rhs(int idx) {
	return rhs[idx];
    }
    
    public boolean footer() {
        return opcode.footer();
    }
    
    public boolean terminal() {
        throw new CompilerException("not a footer: "+this);
    }
    
    public LHeader defaultSuccessor() {
        throw new CompilerException("not a footer: "+this);
    }
    
    public LHeader conditionalSuccessor() {
        throw new CompilerException("not a footer: "+this);
    }
    
    public LHeader conditionalSuccessor(boolean reverseBranch) {
        throw new CompilerException("not a footer: "+this);
    }
    
    public LHeader[] successors() {
        throw new CompilerException("not a footer: "+this);
    }
    
    public int numSuccessors() {
        throw new CompilerException("not a footer: "+this);
    }
    
    public LHeader successor(int i) {
        throw new CompilerException("not a footer: "+this);
    }
    
    /** If it's an instruction, remove it, otherwise throw an error */
    public void remove() {
        assert !footer();
	prev.next=next;
	next.prev=prev;
    }
    
    public LHeader head() {
        return head;
    }
    
    public String label() {
	return ""+head.order+"."+order;
    }
    
    public LOp prepend(LOp i) {
        assert !i.footer();
	i.next=this;
	i.prev=prev;
	prev.next=i;
	prev=i;
        i.head=head;
	return i;
    }
    
    /** Insert the given instruction after this instruction. */
    public LOp append(LOp i) {
        assert !footer();
        assert !i.footer();
	i.next=next;
	i.prev=this;
	next.prev=i;
	next=i;
        i.head=head;
	return i;
    }
    
    public String toString() {
	StringBuffer buf=new StringBuffer();
	buf.append(opcode());
        buf.append("<");
        buf.append(type);
        buf.append("> ");
	buf.append(Util.dump(rhs()));
	return buf.toString();
    }
    
    public LOp copy() {
	LOp result=(LOp)super.copy();
	if (rhs().length!=0) {
	    result.rhs=new LArg[rhs().length];
	    System.arraycopy(rhs(),0,
			     result.rhs(),0,
			     rhs().length);
	}
	return result;
    }
    
    public void mapRhs(HashMap< ?, ? extends LArg > map) {
        for (int i=0;i<rhs().length;++i) {
            rhs[i]=rhs[i].map(map);
        }
    }
    
    public boolean usesDirectly(int i) {
        return opcode.form().directUseArg(i);
    }
    
    public boolean usesDirectly(LArg a) {
        OpForm of=opcode.form();
        for (int i=0;i<rhs().length;++i) {
            if (of.directUseArg(i) && a.equals(rhs()[i])) {
                return true;
            }
        }
        return false;
    }
    
    public boolean uses(LArg a) {
        OpForm of=opcode.form();
        for (int i=0;i<rhs().length;++i) {
            if (of.useArg(i)) {
                for (int j=rhs()[i].nUseOnUseVars();j-->0;) {
                    if (a.equals(rhs()[i].useOnUseVar(j))) {
                        return true;
                    }
                }
            }
            if (of.defArg(i)) {
                for (int j=rhs()[i].nUseOnDefVars();j-->0;) {
                    if (a.equals(rhs()[i].useOnDefVar(j))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public boolean defsDirectly(int i) {
        return opcode.form().defArg(i);
    }
    
    public boolean defsDirectly(LArg a) {
        OpForm of=opcode.form();
        for (int i=0;i<rhs().length;++i) {
            if (of.defArg(i) && a.equals(rhs()[i])) {
                return true;
            }
        }
        return false;
    }
    
    public boolean defs(LArg a) {
        OpForm of=opcode.form();
        for (int i=0;i<rhs().length;++i) {
            if (of.defArg(i) && a.equals(rhs()[i])) {
                return true;
            }
        }
        return false;
    }
    
    public LArg[] uses() {
        OpForm of=opcode.form();
        int n=0;
        for (int i=0;i<rhs.length;++i) {
            if (of.useArg(i)) {
                n+=rhs[i].nUseOnUseVars();
            }
            if (of.defArg(i)) {
                n+=rhs[i].nUseOnDefVars();
            }
        }
        LArg[] implicits=of.implicitUses(head().code());
        LArg[] result=new LArg[n+implicits.length];
        int cnt=0;
        for (int i=0;i<rhs.length;++i) {
            if (of.useArg(i)) {
                for (int j=rhs()[i].nUseOnUseVars();j-->0;) {
                    result[cnt++]=rhs[i].useOnUseVar(j);
                }
            }
            if (of.defArg(i)) {
                for (int j=rhs[i].nUseOnDefVars();j-->0;) {
                    result[cnt++]=rhs[i].useOnDefVar(j);
                }
            }
        }
        assert cnt==n;
        System.arraycopy(implicits,0,
                         result,n,
                         implicits.length);
        return result;
    }
    
    public LArg[] defs() {
        OpForm of=opcode.form();
        int n=0;
        for (int i=0;i<rhs.length;++i) {
            if (of.defArg(i) && rhs[i].variable()) {
                n++;
            }
        }
        LArg[] implicits=of.implicitDefsPretend();
        LArg[] result=new LArg[n+implicits.length];
        int cnt=0;
        for (int i=0;i<rhs.length;++i) {
            if (of.defArg(i) && rhs[i].variable()) {
                result[cnt++]=rhs[i];
            }
        }
        assert cnt==n;
        System.arraycopy(implicits,0,
                         result,n,
                         implicits.length);
        return result;
    }
    
    public boolean usesMemory() {
        for (int i=0;i<rhs().length;++i) {
            if (usesDirectly(i) && rhs()[i].memory()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean usesRegs() {
        // FIXME: this is almost certainly boned.  a Reg could appear in the
        // address expressions.
        for (int i=0;i<rhs().length;++i) {
            if (usesDirectly(i) && rhs()[i] instanceof Reg) {
                return true;
            }
        }
        if (opcode.form().implicitUses(head().code()).length!=0) {
            return true;
        }
        return false;
    }
    
    public boolean defsMemory() {
        for (int i=0;i<rhs().length;++i) {
            if (defsDirectly(i) && rhs()[i].memory()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean defsRegs() {
        for (int i=0;i<rhs().length;++i) {
            if (defsDirectly(i) && rhs[i] instanceof Reg) {
                return true;
            }
        }
        if (opcode.form().implicitDefsPretend().length!=0) {
            return true;
        }
        return false;
    }
    
    public int rhsHash() {
        int result=5;
        for (LArg a : rhs()) {
            result*=3;
            result+=a.hashCode();
        }
        return result;
    }
    
    public boolean rhsEqual(LOp other) {
        if (rhs().length!=other.rhs().length) {
            return false;
        }
        for (int i=0;i<rhs().length;++i) {
            if (!rhs()[i].equals(other.rhs()[i])) {
                return false;
            }
        }
        return true;
    }
}

