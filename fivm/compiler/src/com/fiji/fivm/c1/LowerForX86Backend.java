/*
 * LowerForX86Backend.java
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
import com.fiji.fivm.*;

public class LowerForX86Backend extends CodePhase {
    public LowerForX86Backend(Code c) { super(c); }
    
    private Var emitGetConst(Operation before,
                             Basetype type,
                             Object value) {
        Var val=code.addVar(type.asExectype);
        
        if (Util.fiatToLong(type,value)==0) {
            switch (type) {
            case FLOAT:
                before.prepend(
                    new SimpleInst(
                        before.di(),OpCode.Float0,
                        val,Arg.EMPTY));
                break;
            case DOUBLE:
                before.prepend(
                    new SimpleInst(
                        before.di(),OpCode.Double0,
                        val,Arg.EMPTY));
                break;
            default:
                throw new Error("bad type: "+type);
            }
        } else {
            Var addr=code.addVar(Exectype.POINTER);
            
            before.prepend(
                new CFieldInst(
                    before.di(),OpCode.GetCVarAddress,
                    addr,Arg.EMPTY,
                    Util.makeFloatConst(type,value)));
            before.prepend(
                new MemoryAccessInst(
                    before.di(),OpCode.Load,
                    val,new Arg[]{ addr },
                    type.asType));
        }

        return val;
    }
    
    private Var emitGetConst(HashMap< Object, Var > alreadyLoaded,
                             Operation before,
                             Basetype type,
                             Object value) {
        Var result=alreadyLoaded.get(value);
        if (result==null) {
            alreadyLoaded.put(value,result=emitGetConst(before,type,value));
        }
        return result;
    }
    
    static final class Targ implements Comparable< Targ > {
        int value;
        Header h;
        
        Targ(int value,
             Header h) {
            this.value=value;
            this.h=h;
        }
        
        public int hashCode() {
            return value;
        }
        
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof Targ)) return false;
            Targ other=(Targ)other_;
            return value==other.value;
        }
        
        public int compareTo(Targ other) {
            if (value<other.value) {
                return -1;
            } else if (value==other.value) {
                return 0;
            } else {
                return 1;
            }
        }
    }
    
    long tableSize(Targ[] targs,int lower,int upper,int shift) {
        return (((long)targs[upper-1].value-(long)targs[lower].value)>>>shift)+1;
    }
    
    boolean doAwesomeJump(Header h,Switch s,Targ[] targs,int lower,int upper) {
        if (upper-lower<=5) {
            return false;
        }
        
        final long tableSizeLimit=((long)upper-(long)lower)*3;
        int shift=0;
        
        if (tableSize(targs,lower,upper,0) <= tableSizeLimit) {
            // awesome.  we should totally do an awesome-jump.
        } else {
            shift=32;
            
            for (int i=lower;i<upper;++i) {
                shift=Math.min(shift,IntUtil.firstSetBit(targs[i].value-targs[lower].value));
            }
            
            if (tableSize(targs,lower,upper,shift) > tableSizeLimit) {
                return false;
            }
            
            // slightly less awesome.  we should do an awesome-jump, but only after
            // doing some shifting.
        }
        
        final int tableSize=(int)tableSize(targs,lower,upper,shift);
        
        // what I need:
        //
        // - a way of conveying to the backend that I'm creating a jump table.
        // 
        // - an instruction for retrieving that jump table.
        //
        // maybe the jump table should be a special kind of Local?
        //
        // the really tough thing is that we want to do this totally differently
        // in AOT-PIC versus AOT-noPIC or JIT mode.  in AOT-PIC mode, we want to
        // do trick to emit PC-relative addresses.  moreover, the jump table
        // itself will have PC-relative offsets - so to compute the jump address
        // we'll need more stuff.  in JIT/noPIC mode it's easier: the jump table will
        // be at a known address and will contain absolute addresses of the labels.
        //
        // so do we want to abandon PIC mode?  that might be an easier solution at
        // least for now.
        //
        // another approach is to do our own PIC.  when the VM loads, we can have
        // a patch table.  this table will likely not be so large.
        
        Header[] jumpTableHeads=new Header[tableSize];
        for (int i=0;i<jumpTableHeads.length;++i) {
            jumpTableHeads[i]=s.defaultSuccessor();
        }
        for (int i=lower;i<upper;++i) {
            int j=(targs[i].value-targs[lower].value)>>>shift;
            jumpTableHeads[j]=targs[i].h;
        }
        
        JumpTable jt=JumpTable.make(code.cname(),jumpTableHeads);
        
        Var translated=code.addVar(Exectype.INT);
        Var ltPred=code.addVar(Exectype.INT);
        Var maskPred=code.addVar(Exectype.INT);
        Var shifted=code.addVar(Exectype.INT);
        Var ptrIndex=code.addVar(Exectype.POINTER);
        Var ptrOffset=code.addVar(Exectype.POINTER);
        Var jumpTable=code.addVar(Exectype.POINTER);
        Var jumpAddrPtr=code.addVar(Exectype.POINTER);
        Var jumpAddr=code.addVar(Exectype.POINTER);
        
        h.append(
            new SimpleInst(
                s.di(),OpCode.Sub,
                translated,new Arg[]{
                    s.rhs(0),
                    IntConst.make(targs[lower].value)
                }));
        h.append(
            new SimpleInst(
                s.di(),OpCode.ULessThanEq,
                ltPred,new Arg[]{
                    translated,
                    IntConst.make(targs[upper-1].value-targs[lower].value)
                }));
        
        Header cont=h.makeSimilar(s.di());

        h.setFooter(
            new Branch(
                s.di(),OpCode.BranchNonZero,
                new Arg[]{
                    ltPred
                },
                s.defaultSuccessor(),
                cont));
        
        h=cont;
        
        if (shift>0) {
            h.append(
                new SimpleInst(
                    s.di(),OpCode.And,
                    maskPred,new Arg[]{
                        translated,
                        IntConst.make((1<<shift)-1)
                    }));
            
            cont=h.makeSimilar(s.di());
            
            h.setFooter(
                new Branch(
                    s.di(),OpCode.BranchZero,
                    new Arg[]{
                        maskPred
                    },
                    s.defaultSuccessor(),
                    cont));
            
            h=cont;
            
            h.append(
                new SimpleInst(
                    s.di(),OpCode.Ushr,
                    shifted,new Arg[]{
                        translated,
                        IntConst.make(shift)
                    }));
        } else {
            shifted=translated;
        }
        
        h.append(
            new TypeInst(
                s.di(),OpCode.Cast,
                ptrIndex,new Arg[]{
                    shifted
                },
                Type.POINTER));
        h.append(
            new SimpleInst(
                s.di(),OpCode.Mul,
                ptrOffset,new Arg[]{
                    ptrIndex,
                    PointerConst.make(Global.pointerSize)
                }));
        h.append(
            new CFieldInst(
                s.di(),OpCode.GetCVarAddress,
                jumpTable,Arg.EMPTY,
                jt));
        h.append(
            new SimpleInst(
                s.di(),OpCode.Add,
                jumpAddrPtr,new Arg[]{
                    jumpTable,
                    ptrOffset
                }));
        h.append(
            new MemoryAccessInst(
                s.di(),OpCode.Load,
                jumpAddr,new Arg[]{
                    jumpAddrPtr
                },
                Type.POINTER,
                Mutability.FINAL,
                Volatility.NON_VOLATILE));
        h.setFooter(
            new AwesomeJump(
                s.di(),
                new Arg[]{
                    jumpAddr
                },
                jumpTableHeads));
        
        return true;
    }
    
    static final class LookupRange {
        int lower;
        int upper;
        int lowerCase;
        int upperCase;
        Header h;
        
        LookupRange(int lower,
                    int upper,
                    int lowerCase,
                    int upperCase,
                    Header h) {
            this.lower=lower;
            this.upper=upper;
            this.lowerCase=lowerCase;
            this.upperCase=upperCase;
            this.h=h;
        }
    }
    
    private void doEqCheck(Header h,Switch s,Targ t,Header next) {
        Var pred=code.addVar(Exectype.INT);
        
        h.append(
            new SimpleInst(
                s.di(),OpCode.Eq,
                pred,new Arg[]{
                    s.rhs(0),
                    IntConst.make(t.value)
                }));
        h.setFooter(
            new Branch(
                s.di(),OpCode.BranchNonZero,
                new Arg[]{
                    pred
                },
                next,
                t.h));
    }
    
    private Header doEqCheck(Header h,Switch s,Targ t) {
        Header result=h.makeSimilar(s.di()); // else case
        
        doEqCheck(h,s,t,result);
        
        return result;
    }
    
    Header doTreeBranch(Switch s,Targ[] targs) {
        MyStack< LookupRange > stack=new MyStack< LookupRange >();
        
        Header start=s.head().makeSimilar(s.di());
        
        stack.push(
            new LookupRange(
                0,targs.length,
                -1,-1,
                start));
        
        while (!stack.empty()) {
            LookupRange lr=stack.pop();
            
            if (doAwesomeJump(lr.h,s,targs,lr.lower,lr.upper)) {
                // done
            } else {
                switch (lr.upper-lr.lower) {
                case 0:
                    lr.h.setFooter(
                        new Jump(
                            s.di(),s.defaultSuccessor()));
                    break;
                case 1: {
                    if (lr.lowerCase>=0 && lr.upperCase>=0 &&
                        targs[lr.lowerCase].value+1 == targs[lr.lower].value &&
                        targs[lr.lower].value+1 == targs[lr.upperCase].value) {
                        lr.h.setFooter(
                            new Jump(
                                s.di(), targs[lr.lower].h));
                    } else {
                        doEqCheck(lr.h,s,targs[lr.lower],s.defaultSuccessor());
                    }
                    break;
                }
                case 2: {
                    if (lr.lowerCase>=0 && lr.upperCase>=0 &&
                        targs[lr.lowerCase].value+1 == targs[lr.lower].value &&
                        targs[lr.lower].value+1 == targs[lr.lower+1].value &&
                        targs[lr.lower+1].value+1 == targs[lr.upperCase].value) {
                        // need just a less-than comparison
                        // FIXME: not really, could get by with eq.  but it
                        // almost certainly doesn't matter.
                        
                        Var pred=code.addVar(Exectype.INT);
                        
                        lr.h.append(
                            new SimpleInst(
                                s.di(),OpCode.LessThan,
                                pred,new Arg[]{
                                    s.rhs(0),
                                    IntConst.make(targs[lr.lower+1].value)
                                }));
                        lr.h.setFooter(
                            new Branch(
                                s.di(),OpCode.BranchNonZero,
                                new Arg[]{
                                    pred
                                },
                                targs[lr.lower+1].h,
                                targs[lr.lower].h));
                    } else {
                        Header h=doEqCheck(lr.h,s,targs[lr.lower]);
                        doEqCheck(h,s,targs[lr.lower+1],s.defaultSuccessor());
                    }
                    break;
                }
                default: {
                    int diff=lr.upper-lr.lower;
                    int medI=diff/2+lr.lower;
                    
                    // balance it out
                    if ((diff&3)==2) {
                        medI--;
                    }
                    
                    Header h=doEqCheck(lr.h,s,targs[medI]);
                    
                    Var pred=code.addVar(Exectype.INT);
                    
                    h.append(
                        new SimpleInst(
                            s.di(),OpCode.LessThan,
                            pred,new Arg[]{
                                s.rhs(0),
                                IntConst.make(targs[medI].value)
                            }));
                    
                    Header left=h.makeSimilar(s.di());
                    Header right=h.makeSimilar(s.di());
                    
                    h.setFooter(
                        new Branch(
                            s.di(),OpCode.BranchNonZero,
                            new Arg[]{
                                pred
                            },
                            right,
                            left));
                    
                    stack.push(
                        new LookupRange(
                            lr.lower,medI,
                            lr.lowerCase,medI,
                            left));
                    stack.push(
                        new LookupRange(
                            medI+1,lr.upper,
                            medI,lr.upperCase,
                            right));
                }}
            }
        }
        
        return start;
    }
    
    void lowerSwitch(final Switch s) {
        final Targ[] targs=new Targ[s.numCases()];
        
        for (int i=0;i<s.numCases();++i) {
            targs[i]=new Targ(s.value(i),s.target(i));
        }
        
        Arrays.sort(targs);
        
        Header switchImpl=doTreeBranch(s,targs);
        
        s.replace(
            new Jump(
                s.di(),switchImpl));
    }
    
    public void visitCode() {
        // warning: this does some pretty clever optimizations.
        
        boolean didSwitch=false;
        
        for (Header h : code.headers3()) {
            // first do some obvious simplifications and lower CVars
            for (Instruction i : h.instructions()) {
                switch (i.opcode()) {
                case PutCVar: {
                    CField cf=((CFieldInst)i).field();
                    Var addr=code.addVar(Exectype.POINTER);
                    i.prepend(
                        new CFieldInst(
                            i.di(),OpCode.GetCVarAddress,
                            addr,Arg.EMPTY,
                            cf));
                    if (i.rhs(0) instanceof FloatConst) {
                        i.prepend(
                            new MemoryAccessInst(
                                i.di(),OpCode.Store,
                                Var.VOID,new Arg[]{
                                    addr,
                                    IntConst.make(
                                        Float.floatToRawIntBits(
                                            ((FloatConst)i.rhs(0)).value()))
                                },
                                Type.INT));
                    } else if (i.rhs(0) instanceof DoubleConst) {
                        i.prepend(
                            new MemoryAccessInst(
                                i.di(),OpCode.Store,
                                Var.VOID,new Arg[]{
                                    addr,
                                    LongConst.make(
                                        Double.doubleToRawLongBits(
                                            ((DoubleConst)i.rhs(0)).value()))
                                },
                                Type.LONG));
                    } else {
                        i.prepend(
                            new MemoryAccessInst(
                                i.di(),OpCode.Store,
                                Var.VOID,new Arg[]{
                                    addr,
                                    i.rhs(0)
                                },
                                cf.getType().asType));
                    }
                    i.remove();
                    setChangedCode();
                    break;
                }
                case GetCVar: {
                    CField cf=((CFieldInst)i).field();
                    Var addr=code.addVar(Exectype.POINTER);
                    i.prepend(
                        new CFieldInst(
                            i.di(),OpCode.GetCVarAddress,
                            addr,Arg.EMPTY,
                            cf));
                    i.prepend(
                        new MemoryAccessInst(
                            i.di(),OpCode.Load,
                            i.lhs(),new Arg[]{
                                addr
                            },
                            cf.getType().asType));
                    i.remove();
                    setChangedCode();
                    break;
                }
                case GetCArg: {
                    ArgInst ai=(ArgInst)i;
                    Var addr=code.addVar(Exectype.POINTER);
                    i.prepend(
                        new ArgInst(
                            i.di(),OpCode.GetCArgAddress,
                            addr,Arg.EMPTY,ai.getIdx()));
                    i.prepend(
                        new MemoryAccessInst(
                            i.di(),OpCode.Load,
                            ai.lhs(),new Arg[]{ addr },
                            i.lhs().type().asType()));
                    i.remove();
                    setChangedCode();
                    break;
                }
                case Store:
                    if (i.rhs(1) instanceof Arg.Const) {
                        MemoryAccessInst mai=(MemoryAccessInst)i;
                        switch (mai.effectiveBasetype()) {
                        case FLOAT:
                            mai.type=Type.INT;
                            mai.rhs[1]=IntConst.make(
                                Float.floatToRawIntBits(
                                    ((FloatConst)mai.rhs(1)).value()));
                            setChangedCode();
                            break;
                        case DOUBLE:
                            mai.type=Type.LONG;
                            mai.rhs[1]=LongConst.make(
                                Double.doubleToRawLongBits(
                                    ((DoubleConst)mai.rhs(1)).value()));
                            setChangedCode();
                            break;
                        default:
                            break;
                        }
                    }
                    break;
                default:
                    break;
                }
            }
            
            // now perform propagation
            HashMap< Object, Var > alreadyLoaded=
                new HashMap< Object, Var >();
            Var floatNegator=null;
            Var doubleNegator=null;
            
            for (Operation o : h.operations()) {
                switch (o.opcode()) {
                case Neg:
                    if (o.lhs().isFloat()) {
                        assert o.rhs(0) instanceof Var;
                    
                        Var negator;
                        switch (o.rhs(0).effectiveBasetype()) {
                        case FLOAT:
                            negator=floatNegator;
                            break;
                        case DOUBLE:
                            negator=doubleNegator;
                            break;
                        default: throw new Error();
                        }
                    
                        if (negator==null) {
                            StaticCGlobal global;
                        
                            negator=code.addVar(o.rhs(0).type());

                            switch (o.rhs(0).effectiveBasetype()) {
                            case FLOAT:
                                global=new StaticCGlobal(Basetype.INT,"FloatNegator",-2147483648);
                                floatNegator=negator;
                                break;
                            case DOUBLE:
                                global=new StaticCGlobal(Basetype.LONG,"DoubleNegator",-9223372036854775808l);
                                doubleNegator=negator;
                                break;
                            default: throw new Error();
                            }

                            Var addr=code.addVar(Exectype.POINTER);
                            o.prepend(
                                new CFieldInst(
                                    o.di(),OpCode.GetCVarAddress,
                                    addr,Arg.EMPTY,
                                    global));
                            o.prepend(
                                new MemoryAccessInst(
                                    o.di(),OpCode.Load,
                                    negator,new Arg[]{ addr },
                                    o.rhs(0).type().asType()));
                        }
                    
                        o.prepend(
                            new SimpleInst(
                                o.di(),OpCode.FXor,
                                o.lhs(),new Arg[]{
                                    o.rhs(0),
                                    negator
                                }));
                        o.remove();
                        setChangedCode();
                    }
                    break;
                case Call:
                case CallIndirect:
                    // these are special:
                    // 1) invalidate loaded constants so we don't use pretend-persistents to
                    //    store them since that's a waste
                    // 2) keep immediates intact so BuildLOp can turn them into integers
                    alreadyLoaded.clear();
                    floatNegator=null;
                    doubleNegator=null;
                    break;
                case Return:
                case RawReturn:
                    if (Global.pointerSize==4 && o.nrhs()==1) {
                        if (o.rhs(0) instanceof FloatConst) {
                            FloatConst fc=(FloatConst)o.rhs(0);
                            if (fc.value()==0.0f || fc.value()==1.0f) {
                                // leave this alone
                                break;
                            }
                        } else if (o.rhs(0) instanceof DoubleConst) {
                            DoubleConst dc=(DoubleConst)o.rhs(0);
                            if (dc.value()==0.0 || dc.value()==1.0) {
                                // leave this alone
                                break;
                            }
                        }
                    }
                default:
                    for (int i=0;i<o.nrhs();++i) {
                        if (o.rhs(i) instanceof FloatConst) {
                            o.rhs[i]=emitGetConst(
                                alreadyLoaded,o,Basetype.FLOAT,((FloatConst)o.rhs(i)).value());
                            setChangedCode();
                        } else if (o.rhs(i) instanceof DoubleConst) {
                            o.rhs[i]=emitGetConst(
                                alreadyLoaded,o,Basetype.DOUBLE,((DoubleConst)o.rhs(i)).value());
                            setChangedCode();
                        }
                    }
                    break;
                }
            }
            
            if (h.footer().opcode()==OpCode.Switch) {
                lowerSwitch((Switch)h.footer());
                setChangedCode();
                didSwitch=true;
            }
        }

        if (changedCode()) {
            if (didSwitch) {
                code.killAllAnalyses();
            } else {
                code.killIntraBlockAnalyses();
            }
        }
    }
}

