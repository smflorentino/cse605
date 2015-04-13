/*
 * StrengthReduce.java
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

public class StrengthReduce extends CodePhase {
    public StrengthReduce(Code c) { super(c); }
    
    public void visitCode() {
        assert !code.isSSA();
        
	for (Header h : code.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case Div:
		    if (false && i.rhs(1) instanceof IntConst) {
			IntConst ic=(IntConst)i.rhs(1);
			if (IntUtil.countOneBits(ic.value())==1) {
			    i.prepend(
				new SimpleInst(
				    i.di(),OpCode.Shr,
				    i.lhs(),new Arg[]{
					i.rhs(0),
					IntConst.make(IntUtil.logBase2(ic.value()))
				    }));
			    i.remove();
			    setChangedCode();
			}
		    }
		    break;
		case Mod:
		    if (false && i.rhs(1) instanceof IntConst) {
			IntConst ic=(IntConst)i.rhs(1);
			if (IntUtil.countOneBits(ic.value())==1) {
			    i.prepend(
				new SimpleInst(
				    i.di(),OpCode.And,
				    i.lhs(),new Arg[]{
					i.rhs(0),
					IntConst.make(IntUtil.logBase2(ic.value())-1)
				    }));
			    i.remove();
			    setChangedCode();
			}
		    }
		    break;
		case Mul:
		    if (i.rhs(1) instanceof Arg.IntConst) {
			Arg.IntConst ic=(Arg.IntConst)i.rhs(1);
                        
                        long b=ic.longValue();
                        boolean negate;
                        if (b<0) {
                            b=-b;
                            negate=true;
                        } else {
                            negate=false;
                        }
                        
                        if (b==0) {
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{
                                        i.lhs().type().makeZero()
                                    }));
                            i.remove();
                            setChangedCode();
                        } else if (b==1) {
                            if (negate) {
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Neg,
                                        i.lhs(),new Arg[]{
                                            i.rhs(0)
                                        }));
                                i.remove();
                                setChangedCode();
                            } else {
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mov,
                                        i.lhs(),new Arg[]{
                                            i.rhs(0)
                                        }));
                                i.remove();
                                setChangedCode();
                            }
			} else {
                            // detect cases where i.rhs(1) can be decomposed to
                            // (1<<a) +- (1<<b) + {1,0,-1}
                            
                            // this may not be the world's fastest implementation.
                            // but it works, damn it.
                            
                            int numBits=ic.numBits();
                            
                            // FIXME: this will sometimes fail to find the
                            // most optimal reduction.  for example it'll turn
                            // x*7 into (x<<2) + (x<<1) + x instead of
                            // (x<<3) - x.
                            
                        mainLoop:
                            for (int j=1;j<numBits;++j) {
                                long candidate=1l<<j;
                                if (candidate==b) {
                                    // result is (i<<j)
                                    reduceMulToShifts(i,j,0,0,negate);
                                    break mainLoop;
                                } else if (candidate<b) {
                                    if (b-candidate==1) {
                                        // result is (1<<j) + 1
                                        reduceMulToShifts(i,j,0,1,negate);
                                        break mainLoop;
                                    }
                                    for (int k=1;k<=j;++k) {
                                        if (k!=j) {
                                            long candidate2=candidate+(1l<<k);
                                            long diff=candidate2-b;
                                            if (diff>=-1 && diff<=1) {
                                                // result is (1<<j) + (1<<k) - 1
                                                reduceMulToShifts(i,j,k,(int)-diff,negate);
                                                break mainLoop;
                                            }
                                        }
                                    }
                                } else {
                                    if (b-candidate==-1) {
                                        // result is (1<<j) - 1
                                        reduceMulToShifts(i,j,0,-1,negate);
                                        break mainLoop;
                                    }
                                    for (int k=1;k<=j;++k) {
                                        if (k!=j) {
                                            long candidate2=candidate-(1l<<k);
                                            long diff=candidate2-b;
                                            if (diff>=-1 && diff<=1) {
                                                // result is (1<<j) - (1<<k) - 1
                                                reduceMulToShifts(i,j,-k,(int)-diff,negate);
                                                break mainLoop;
                                            }
                                        }
                                    }
                                }
                            }
                        }
		    }
		    break;
		default: break;
		}
	    }
	}
	
	if (changedCode()) code.killIntraBlockAnalyses();
    }
    
    private void reduceMulToShifts(Instruction i,
                                   int mainShift,
                                   int secondShift,
                                   int thirdTerm,
                                   boolean negate) {
        Var res3;
        if (negate) {
            res3=code.addVar(i.lhs().type());
        } else {
            res3=i.lhs();
        }
        
        Var res2;
        if (thirdTerm==0) {
            res2=res3;
        } else {
            res2=code.addVar(i.lhs().type());
        }
        
        Var res1;
        if (secondShift==0) {
            res1=res2;
        } else {
            res1=code.addVar(i.lhs().type());
        }
        
        i.prepend(
            new SimpleInst(
                i.di(),OpCode.Shl,
                res1,new Arg[]{
                    i.rhs(0),
                    IntConst.make(mainShift)
                }));
        
        if (secondShift!=0) {
            Var tmp=code.addVar(i.lhs().type());
            
            i.prepend(
                new SimpleInst(
                    i.di(),OpCode.Shl,
                    tmp,new Arg[]{
                        i.rhs(0),
                        IntConst.make(Math.abs(secondShift))
                    }));
            
            i.prepend(
                new SimpleInst(
                    i.di(),secondShift<0?OpCode.Sub:OpCode.Add,
                    res2,new Arg[]{
                        res1,
                        tmp
                    }));
        }
        
        if (thirdTerm!=0) {
            i.prepend(
                new SimpleInst(
                    i.di(),thirdTerm<0?OpCode.Sub:OpCode.Add,
                    res3,new Arg[]{
                        res2,
                        i.rhs(0)
                    }));
        }
        
        if (negate) {
            i.prepend(
                new SimpleInst(
                    i.di(),OpCode.Neg,
                    i.lhs(),new Arg[]{ res3 }));
        }
        
        i.remove();
        setChangedCode();
    }
}

