/*
 * Simplify.java
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

public class SimplifyConditionals extends CodePhase {
    public SimplifyConditionals(Code c) { super(c); }
    
    OpCode assignOp() {
        if (code.isSSA()) {
            return OpCode.Ipsilon;
        } else {
            return OpCode.Mov;
        }
    }
    
    boolean isReturn(Header h) {
        return h.first()==h.footer()
            && (h.footer().opcode()==OpCode.Return ||
                h.footer().opcode()==OpCode.RawReturn)
            && h.footer().nrhs()==1
            && h.footer().rhs(0) instanceof Arg.IntConst;
    }
    
    OpCode retOp(Header h) {
        return h.footer().opcode();
    }
    
    Arg.IntConst retArg(Header h) {
        return (Arg.IntConst)h.footer().rhs(0);
    }
    
    long retValue(Header h) {
        return retArg(h).longValue();
    }
    
    boolean isAssign(Header h) {
        return h.first().opcode()==assignOp()
            && h.first().rhs(0) instanceof Arg.IntConst
            && h.first().next()==h.footer()
            && h.footer().opcode()==OpCode.Jump;
    }
    
    Arg.IntConst assArg(Header h) {
        return (Arg.IntConst)h.first().rhs(0);
    }
    
    long assValue(Header h) {
        return arg(h).longValue();
    }
    
    Arg.IntConst arg(Header h) {
        if (h.first()==h.footer()) {
            return retArg(h);
        } else {
            return assArg(h);
        }
    }
    
    long value(Header h) {
        return arg(h).longValue();
    }
    
    Var var(Header h) {
        return ((Instruction)h.first()).lhs();
    }
    
    Header jumpTarg(Header h) {
        return h.footer().defaultSuccessor();
    }
    
    public void setChangedCode(Object why) {
        if (false) {
            Global.log.println("in "+code.shortName()+": "+why);
        }
        super.setChangedCode(why);
    }
    
    public void visitCode() {
        for (Header h : code.headers()) {
            if (h.footer() instanceof Branch) {
                Header a=h.footer().defaultSuccessor();
                Header b=((Branch)h.footer()).target();
                
                if ((isAssign(a) && isAssign(b) &&
                     jumpTarg(a) == jumpTarg(b) &&
                     var(a) == var(b)) ||
                    (isReturn(a) && isReturn(b) &&
                     retOp(a) == retOp(b))) {
                    long aVal=value(a);
                    long bVal=value(b);

                    if (aVal==bVal) {
                        if (isAssign(a)) {
                            h.prepend(
                                new SimpleInst(
                                    h.footer().di(),assignOp(),
                                    var(a),new Arg[]{ arg(a) }));
                            h.setFooter(
                                new Jump(
                                    h.footer().di(),
                                    jumpTarg(a)));
                            if (Global.verbosity>=5) {
                                Global.log.println("eliminating redundant branch on diamond "+h+" to ("+a+", "+b+") to "+jumpTarg(a));
                            }
                            setChangedCode("eliminated redundant branch (assign)");
                        } else {
                            h.setFooter(
                                new Terminal(
                                    h.footer().di(),retOp(a),
                                    new Arg[]{ arg(a) }));
                            if (Global.verbosity>=5) {
                                Global.log.println("eliminating redundant branch on diamond "+h+" to ("+a+", "+b+") to "+jumpTarg(a));
                            }
                            setChangedCode("eliminated redundant branch (return)");
                        }
                    } else if (IntUtil.countOneBits(Math.abs(aVal-bVal))==1) {
                        long base;
                        boolean invert;
                        if (aVal<bVal) {
                            base=aVal;
                            switch (h.footer().opcode()) {
                            case BranchNonZero:
                                invert=false;
                                break;
                            case BranchZero:
                                invert=true;
                                break;
                            default:
                                throw new Error("bad opcode: "+h.footer().opcode());
                            }
                        } else {
                            base=bVal;
                            switch (h.footer().opcode()) {
                            case BranchNonZero:
                                invert=true;
                                break;
                            case BranchZero:
                                invert=false;
                                break;
                            default:
                                throw new Error("bad opcode: "+h.footer().opcode());
                            }
                        }

                        Var pred=code.addVar(Exectype.INT);
                        if (invert) {
                            h.append(
                                new SimpleInst(
                                    h.footer().di(),OpCode.Eq,
                                    pred,new Arg[]{
                                        h.footer().rhs(0),
                                        h.footer().rhs(0).type().effectiveBasetype().makeZero()
                                    }));
                        } else {
                            h.append(
                                new SimpleInst(
                                    h.footer().di(),OpCode.Neq,
                                    pred,new Arg[]{
                                        h.footer().rhs(0),
                                        h.footer().rhs(0).type().effectiveBasetype().makeZero()
                                    }));
                        }

                        Arg predTyped;
                        if (arg(a).type()==Exectype.INT) {
                            predTyped=pred;
                        } else {
                            Var predTypedVar=code.addVar(arg(a).type());
                            h.append(
                                new TypeInst(
                                    h.footer().di(),OpCode.Cast,
                                    predTypedVar,new Arg[]{
                                        pred
                                    },
                                    arg(a).type().asType()));
                            predTyped=predTypedVar;
                        }

                        long diff=Math.abs(aVal-bVal);
                        Arg scaled;
                        if (diff==1) {
                            scaled=predTyped;
                        } else {
                            Var scaledVar=code.addVar(arg(a).type());
                            h.append(
                                new SimpleInst(
                                    h.footer().di(),OpCode.Shl,
                                    scaledVar,new Arg[]{
                                        predTyped,
                                        IntConst.make(IntUtil.logBase2(diff))
                                    }));
                            scaled=scaledVar;
                        }
                        
                        Var value=code.addVar(arg(a).type());
                        h.append(
                            new SimpleInst(
                                h.footer().di(),OpCode.Add,
                                value,new Arg[]{
                                    scaled,
                                    arg(a).makeSimilar(base)
                                }));
                        
                        if (isAssign(a)) {
                            h.append(
                                new SimpleInst(
                                    h.footer().di(),assignOp(),
                                    var(a),new Arg[]{
                                        value
                                    }));
                            
                            h.setFooter(
                                new Jump(
                                    h.footer().di(),jumpTarg(a)));
                        
                            if (Global.verbosity>=5) {
                                Global.log.println("converted branch to arithmetic on diamond "+h+" to ("+a+", "+b+") to "+jumpTarg(a));
                            }
                            setChangedCode("converted branch to arithmetic (assign)");
                        } else {
                            h.setFooter(
                                new Terminal(
                                    h.footer().di(),retOp(a),
                                    new Arg[]{ value }));
                            
                            if (Global.verbosity>=5) {
                                Global.log.println("converted branch to arithmetic on diamond "+h+" to ("+a+", "+b+") to "+jumpTarg(a));
                            }
                            setChangedCode("converted branch to arithmetic (return)");
                        }
                    }
                }
            }
        }
        
        if (changedCode()) {
            code.killAllAnalyses();
        }
    }
}

