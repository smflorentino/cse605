/*
 * ComparePropLate.java
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

public class ComparePropLate extends CodePhase {
    public ComparePropLate(Code c) { super(c); }
    
    public void visitCode() {
        assert !code.isSSA();
        
        for (Header h : code.headers3()) {
            switch (h.footer().opcode()) {
            case BranchNonZero:
            case BranchZero: {
                Branch b=(Branch)h.footer();
                if (b.prev() instanceof Instruction) {
                    Instruction compare=(Instruction)b.prev();
                    if (compare.lhs()==b.rhs(0) &&
                        code.getBoolResult().get(compare)) {
                        Var newTmp=code.addVar(b.rhs(0).type());
                    
                        int targValue;
                        if (b.opcode()==OpCode.BranchNonZero) {
                            targValue=1;
                        } else {
                            targValue=0;
                        }
                    
                        Header newTarg=h.makeSimilar(b.di());
                        Header newDef=h.makeSimilar(b.di());
                    
                        newTarg.append(
                            new SimpleInst(
                                b.di(),OpCode.Mov,
                                (Var)b.rhs(0),new Arg[]{ b.rhs(0).type().makeConst(targValue) }));
                        newTarg.setFooter(
                            new Jump(
                                b.di(),b.target()));
                        newDef.append(
                            new SimpleInst(
                                b.di(),OpCode.Mov,
                                (Var)b.rhs(0),new Arg[]{ b.rhs(0).type().makeConst(targValue^1) }));
                        newDef.setFooter(
                            new Jump(
                                b.di(),b.defaultSuccessor()));
                    
                        b.next=newDef;
                        b.target=newTarg;
                    
                        compare.setLhs(newTmp);
                        b.rhs[0]=newTmp;
                    
                        assert newTmp.inst()==compare;
                    
                        setChangedCode();
                    }
                }
                break;
            }
            default: break;
            }
        }
        
        if (changedCode()) {
            code.killAllAnalyses();
        }
    }
}

	
