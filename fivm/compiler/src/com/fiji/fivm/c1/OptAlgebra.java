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

import java.util.*;
import com.fiji.util.*;

public class OptAlgebra extends CodePhase {
    public OptAlgebra(Code c) { super(c); }
    
    /** Builds an expression for computing some commutative and reassociative operation
     * over the given list of arguments.  When given a list of arguments of the
     * form a1, a2, a3, a4 it will build an expression tree of the form:
     * Add(Add(Add(a1, a2), a3), a4)
     */
    Arg build(Instruction i,
              ArrayList< Arg > args) {
        Arg prev=args.get(0);
        for (int j=1;j<args.size();++j) {
            Var result=code.addVar(prev.type());
            i.prepend(
                new SimpleInst(
                    i.di(),i.opcode(),
                    result,new Arg[]{
                        prev,
                        args.get(j)
                    }));
            prev=result;
        }
        return prev;
    }
    
    public void visitCode() {
        assert code.isSSA();
        
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                switch (i.opcode()) {
                case Sub: {
                    Var tmp=code.addVar(i.lhs().type());
                    i.prepend(
                        new SimpleInst(
                            i.di(),OpCode.Neg,
                            tmp,new Arg[]{ i.rhs(1) }));
                    i.prepend(
                        new SimpleInst(
                            i.di(),OpCode.Add,
                            i.lhs(),new Arg[]{ i.rhs(0), tmp }));
                    i.remove();
                    setChangedCode();
                    break;
                }
                default: break;
                }
            }
        }
        
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
        
        RankCalc rc=code.getRanks();
        UseCalc uc=code.getUses();
            
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                switch (i.opcode()) {
                case Add:
                case Or:
                case Xor:
                case And:
                case Mul: {
                    if (i.lhs().type().effectiveBasetype().isInteger) {
                        boolean abort=false;
                        
                        if (uc.usedOnce(i)) {
                            Operation use=uc.theUse(i);
                            if (use.opcode()==i.opcode() &&
                                rc.rank((Instruction)use)<=rc.rank(i)) {
                                abort=true;
                            }
                        }
                        
                        if (!abort) {
                            TreeMap< Integer, ArrayList< Arg > > args=
                                new TreeMap< Integer, ArrayList< Arg > >();
                            MyStack< Arg > worklist=new MyStack< Arg >();
                            worklist.push(i.rhs(0));
                            worklist.push(i.rhs(1));
                            while (!worklist.empty()) {
                                Arg a=worklist.pop();
                                Instruction src=a.inst();
                                if (src!=null &&
                                    uc.usedOnce(src) &&
                                    src.opcode()==i.opcode() &&
                                    rc.rank(i)<=rc.rank(src)) {
                                    worklist.push(src.rhs(0));
                                    worklist.push(src.rhs(1));
                                } else {
                                    Integer rank=rc.rank(a);
                                    ArrayList< Arg > list=args.get(rank);
                                    if (list==null) {
                                        args.put(rank,list=new ArrayList< Arg >());
                                    }
                                    list.add(a);
                                }
                            }
                            
                            // construct new instructions.  the args tree, which is
                            // sorted on ascending rank, is traversed in forward order,
                            // such that the newArgs list will contain the arguments
                            // corresponding to expression trees for computing the
                            // sub-result for all original arguments of a given rank,
                            // sorted by that rank in ascending order.
                            ArrayList< Arg > newArgs=new ArrayList< Arg >();
                            
                            for (Integer rank : args.keySet()) {
                                ArrayList< Arg > list=args.get(rank);
                                
                                // sort by the other kind of rank
                                Collections.sort(
                                    list,
                                    new Comparator< Arg >(){
                                        public int compare(Arg a,Arg b) {
                                            return b.rank()-a.rank();
                                        }
                                    });
                                
                                newArgs.add(build(i,list));
                            }
                            
                            // if we didn't reverse, this would build an expression tree
                            // of the form:
                            //
                            // Add(Add(Add(... constants ...), ... vars1 ...), ... vars2 ...)
                            //
                            // when we really want:
                            //
                            // Add(Add(Add(... vars2 ...), ... vars1 ...), ... constants ...)
                            //
                            // hence, we reverse the list.
                            
                            Collections.reverse(newArgs);
                            
                            // now add that stuff together
                            i.prepend(
                                new SimpleInst(
                                    i.di(),OpCode.Mov,
                                    i.lhs(),new Arg[]{ build(i, newArgs) }));

                            i.remove();
                            setChangedCode();
                        }
                    }
                    break;
                }
                default: break;
                }
            }
        }

        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
        
        if (false) {
            // FIXME: I'm pretty sure this is boned.  we should do a simplification
            // pass after the pass above, or something.
            for (int cnt=0;;++cnt) {
                String changed=null;
            
                rc=code.getRanks();
                uc=code.getUses();
            
                for (Header h : code.headers()) {
                    for (Instruction i : h.instructions()) {
                        switch (i.opcode()) {
                        case Neg: {
                            Instruction i2=i.rhs(0).inst();
                            if (i2!=null && i2.opcode()==OpCode.Neg) {
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mov,
                                        i.lhs(),new Arg[]{
                                            i2.rhs(0)
                                        }));
                                i.remove();
                                changed="Neg(Neg) -> Mov";
                            }
                            break;
                        }
                        case Add: {
                            Instruction a=i.rhs(0).inst();
                            Instruction b=i.rhs(1).inst();
                            boolean aok=a!=null && rc.rank(a)==rc.rank(i) && uc.usedOnce(a);
                            boolean bok=b!=null && rc.rank(b)==rc.rank(i) && uc.usedOnce(b);
                            if (i.lhs().type().effectiveBasetype().isInteger &&
                                aok && bok &&
                                a.opcode()==OpCode.Mul &&
                                b.opcode()==OpCode.Mul &&
                                a.usesAnySame(b)) {
                            
                                // (a * b) + (a * c) -> a * (b + c)
                                int ai=a.sameUseIndex(b);
                                int bi=b.indexOf(a.rhs(ai));
                                assert ai>=0;
                                assert bi>=0;
                                assert a.rhs(ai)==b.rhs(bi);
                                Var sum=code.addVar(i.lhs().type());
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Add,
                                        sum,new Arg[]{
                                            a.rhs(ai^1),
                                            b.rhs(bi^1)
                                        }));
                                i.prepend(
                                    new SimpleInst(
                                        i.di(),OpCode.Mul,
                                        i.lhs(),new Arg[]{
                                            sum,
                                            a.rhs(ai)
                                        }));
                                i.remove();
                                changed="Add(Mul,Mul) -> Mul(_,Add)";
                            }
                            break;
                        }
                        default: break;
                        }
                    }
                }
            
                if (changed!=null) {
                    if (cnt>100 & Global.verbosity>=2) {
                        Global.log.println("OptAlgebra taking too long because of "+changed);
                    }
                    setChangedCode();
                    code.killIntraBlockAnalyses();
                }
                if (changed==null) break;
            }
        }

        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                switch (i.opcode()) {
                case Add: {
                    Instruction a=i.rhs(0).inst();
                    Instruction b=i.rhs(1).inst();
                    boolean aok=a!=null;
                    boolean bok=b!=null;
                    if (aok && a.opcode()==OpCode.Neg) {
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.Sub,
                                i.lhs(),new Arg[]{
                                    i.rhs(1),
                                    a.rhs(0)
                                }));
                        i.remove();
                        setChangedCode();
                    } else if (bok && b.opcode()==OpCode.Neg) {
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.Sub,
                                i.lhs(),new Arg[]{
                                    i.rhs(0),
                                    b.rhs(0)
                                }));
                        i.remove();
                        setChangedCode();
                    }
                    break;
                }
                default: break;
                }
            }
        }
        
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
    }
}

