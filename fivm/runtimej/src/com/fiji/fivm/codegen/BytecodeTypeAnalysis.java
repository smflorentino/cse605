/*
 * BytecodeTypeAnalysis.java
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

package com.fiji.fivm.codegen;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import com.fiji.asm.Label;
import com.fiji.asm.Opcodes;
import com.fiji.asm.UTF8Sequence;

import com.fiji.fivm.Constants;
import com.fiji.fivm.TypeParsing;

import com.fiji.fivm.r1.Inline;
import com.fiji.fivm.r1.NoInline;
import com.fiji.fivm.r1.NoReturn;

/**
 * The bytecode forward-flow type-inference abstract interpreter.  This analysis
 * is intended to give you the bare minimum of information that you need to
 * reason about the types in bytecode.  Currently it's just used for building GC
 * maps.
 * <p>
 * This analysis started out being very simple; it had exactly the bare minimum
 * of functionality necessary to infer GC references.  For simplicity, it's based
 * on ASM.  For speed, it uses ASM's visitor API and performs only one linear
 * pass over the bytecode.  This pass is used for constraint generation; thse
 * constraints are optimized for memory compactness and speed.  The constraints
 * are then solved; most of the solving involves comparing integers.
 * <p>
 * Almost enough power is included to reason about Pointer and Pointer[] types,
 * but this hasn't been fully implemented.  Currently the 'types' are just
 * JVM bytecode descriptors with some magic (see ExtendedTypes).  This magic is
 * already powerful enough to reason about Pointer and Pointer[] (the only
 * missing piece is that this analysis doesn't fully leverage that functionality),
 * and almost powerful enough to reason about the JVM execution type-system.
 * <p>
 * This analysis runs on the critical path of the baseline JIT.  Thus, it's
 * crucial that no features are added unless a high-performance implementation
 * can be done.  The general benchmark is that for a ~5k bytecode method, this
 * should take less than 4ms on a decently-fast Intel laptop.  And remember: no
 * crazy object orientation!  This code will be compiled with -A open and usually
 * --opt-size, and sometimes with -G hf, and yet still needs to be fast!
 */
public final class BytecodeTypeAnalysis {
    MethodBytecodeExtractor mbe;
    int verbosity;
    
    // NOTE: this code is at least slightly duplicating what's in ASM ... the point is that
    // we *do not* want to rewrite the bytecode just to get frame info.  'cause that just
    // seems retarded.
    
    private static int[] EMPTY_PUSHED=new int[0];
    private static BB[] EMPTY_SUCC=new BB[0];
    
    @Inline
    int nStack() {
        return mbe.nStack();
    }
    
    @Inline
    int nLocals() {
        return mbe.nLocals();
    }
    
    class EdgeState {
        boolean complete;
        int[] locals;
        int[] stack;
        
        EdgeState() {
            locals=new int[nLocals()];
        }
        
        public String toString() {
            String result="[EdgeState: ";
            if (!complete) {
                result+="(INCOMPLETE) ";
            }
            result+="Locals("+typesToString(locals)+") Stack(";
            if (stack!=null) {
                result+=typesToString(stack);
            }
            return result+")]";
        }
        
        void assertComplete() {
            if (!complete) {
                throw new CodeGenException("Incomplete edge state after fixpoint");
            }
        }
        
        void assign(int local,int type) {
            if (complete) {
                throw new CodeGenException(
                    "cannot make manual changes to EdgeState after it is complete");
            }
            if (!ExtendedTypes.isExplicitType(type)) {
                throw new CodeGenException("cannot set local with unknown type");
            }
            locals[local]=type;
        }
        
        void push(int type) {
            if (complete) {
                throw new CodeGenException(
                    "cannot make manual changes to EdgeState after it is complete");
            }
            if (!ExtendedTypes.isExplicitType(type)) {
                throw new CodeGenException("cannot set local with unknown type");
            }
            if (stack==null) {
                stack=new int[1];
                stack[0]=type;
            } else {
                int[] newStack=new int[stack.length+1];
                System.arraycopy(
                    stack,0,
                    newStack,0,
                    stack.length);
                newStack[stack.length]=type;
                stack=newStack;
            }
        }
        
        boolean forceComplete() {
            if (complete) {
                return false;
            } else {
                if (stack==null) {
                    stack=EMPTY_PUSHED;
                }
                complete=true;
                return true;
            }
        }
        
        boolean mergeFrom(EdgeState prev) {
            if (prev.complete) {
                boolean result=false;
                for (int i=0;i<locals.length;++i) {
                    int newLocal=ExtendedTypes.lub(locals[i],prev.locals[i]);
                    if (newLocal!=locals[i]) {
                        result=true;
                        locals[i]=newLocal;
                    }
                }
                if (stack==null) {
                    stack=new int[prev.stack.length];
                    System.arraycopy(prev.stack,0,
                                     stack,0,
                                     stack.length);
                    result=true;
                } else {
                    if (stack.length!=prev.stack.length) {
                        throw new CodeGenException("Stack heights don't match up; target has "+stack.length+" while source has "+prev.stack.length);
                    }
                    for (int i=0;i<stack.length;++i) {
                        int newStack=ExtendedTypes.lub(stack[i],prev.stack[i]);
                        if (newStack!=stack[i]) {
                            stack[i]=newStack;
                            result=true;
                        }
                    }
                }
                complete=true;
                return result;
            } else {
                return false;
            }
        }
        
        boolean mergeFrom(EdgeState atHead,BlockState body) {
            if (atHead.complete) {
                boolean result=false;
                
                // locals.  they used to be easy but then I made them hard.
                for (int i=0;i<locals.length;++i) {
                    int newLocal=body.getState(atHead,body.locals[i]);
                    if (newLocal!=locals[i]) {
                        result=true;
                        locals[i]=newLocal;
                    }
                }
                
                // the stack.  it's weird.
                if (stack==null) {
                    int height=atHead.stack.length-body.nPopped+body.nPushed;
                    if (height<0) {
                        throw new CodeGenException("Negative stack height: previous height = "+atHead.stack.length+", number popped = "+body.nPopped+", number pushed = "+body.nPushed);
                    }
                    stack=new int[height];
                }
                for (int i=0;i<atHead.stack.length-body.nPopped;++i) {
                    int newStack=ExtendedTypes.lub(stack[i],atHead.stack[i]);
                    if (newStack!=stack[i]) {
                        result=true;
                        stack[i]=newStack;
                    }
                }
                for (int i=0;i<body.nPushed;++i) {
                    int newStack=body.getState(atHead,body.pushed[i]);
                    if (newStack!=stack[atHead.stack.length-body.nPopped+i]) {
                        result=true;
                        stack[atHead.stack.length-body.nPopped+i]=newStack;
                    }
                }
                
                complete=true;
                return result;
            } else {
                return false;
            }
        }
        
        boolean mergeFrom(ThrowState throu) {
            if (throu.complete) {
                boolean result=false;
                
                if (complete) {
                    if (stack.length!=1) {
                        throw new CodeGenException(
                            "Exception handler may sometimes be jumped to with a stack that "+
                            "is not exactly one-cell high.");
                    }
                    if (stack[0]!='L') {
                        throw new CodeGenException(
                            "Exception handler may sometimes be jumped to with a stack that "+
                            "does not contain exactly one heap reference.");
                    }
                } else {
                    result=true;
                    push('L');
                }
                
                for (int i=0;i<locals.length;++i) {
                    int newLocal=ExtendedTypes.lub(locals[i],throu.locals[i]);
                    if (newLocal!=locals[i]) {
                        result=true;
                        locals[i]=newLocal;
                    }
                }
                
                complete=true;
                return result;
            } else {
                return false;
            }
        }
    }
    
    class ThrowState {
        boolean complete;
        boolean mayThrow;
        int[] locals;
        
        ThrowState() {
        }
        
        void assertComplete() {
            if (!complete) {
                throw new CodeGenException("Incomplete throw state after fixpoint");
            }
        }
        
        boolean mergeFrom(EdgeState atHead,BlockState body) {
            if (!body.mayThrow) {
                if (complete) {
                    return false;
                } else {
                    complete=true;
                    return true;
                }
            } else {
                if (atHead.complete) {
                    boolean result=false;
                    mayThrow=true;
                    if (locals==null) {
                        locals=new int[nLocals()];
                        result=true;
                    }
                    for (int i=0;i<locals.length;++i) {
                        int[] excLocals;
                        if (body.excLocals==null) {
                            excLocals=null;
                        } else {
                            excLocals=body.excLocals[i];
                        }
                        int newLocal;
                        if (excLocals==null) {
                            newLocal=body.getState(atHead,body.excLocalsSimple[i]);
                        } else {
                            newLocal=ExtendedTypes.lub(
                                body.getState(atHead,body.excLocalsSimple[i]),
                                body.getState(atHead,excLocals));
                        }
                        newLocal=ExtendedTypes.lub(atHead.locals[i],newLocal);
                        if (newLocal!=locals[i]) {
                            result=true;
                            locals[i]=newLocal;
                        }
                    }
                    complete=true;
                    return result;
                } else {
                    return false;
                }
            }
        }
    }
    
    class BlockState {
        int nPopped;
        int nPushed;
        
        int[] pushed;
        int[] locals;
        int[] excLocalsSimple;
        int[][] excLocals;
        
        boolean mayThrow;
        
        BlockState() {
            pushed=EMPTY_PUSHED;
            int nLocals=nLocals();
            locals=new int[nLocals];
            excLocalsSimple=new int[nLocals];
            for (int i=0;i<nLocals;++i) {
                locals[i]=ExtendedTypes.newLocalRef(i);
            }
        }
        
        public String toString() {
            StringBuilder buf=new StringBuilder();
            buf.append("[BlockState: nPopped("+nPopped+") nPushed("+nPushed+") ");
            buf.append("pushed("+typesToString(pushed)+")");
            buf.append(" locals("+typesToString(locals)+")");
            buf.append(" excLocalsSimple("+typesToString(excLocalsSimple)+")");
            buf.append(" excLocals(");
            if (excLocals==null) {
                buf.append("null");
            } else {
                for (int[] el : excLocals) {
                    buf.append("["+typesToString(el)+"]");
                }
            }
            buf.append(")]");
            return buf.toString();
        }
        
        int getState(EdgeState atHead,int code) {
            if (code==0) {
                throw new CodeGenException("code cannot be 0");
            }
            switch (ExtendedTypes.codeKind(code)) {
            case ExtendedTypes.EXPLICIT_TYPE:
            case ExtendedTypes.PTR_TYPE:
            case ExtendedTypes.SYMB_TYPE:
                return code;
            case ExtendedTypes.LOCAL_REFERENCE:
                return ExtendedTypes.validateNonStateRef(
                    atHead.locals[ExtendedTypes.getPayload(code)]);
            case ExtendedTypes.STACK_REFERENCE:
                return ExtendedTypes.validateNonStateRef(
                    atHead.stack[atHead.stack.length-ExtendedTypes.getPayload(code)-1]);
            default:
                throw new CodeGenException("bad code kind: "+code);
            }
        }
        
        int getState(EdgeState atHead,int[] codes) {
            int result=0;
            for (int code : codes) {
                result=ExtendedTypes.lub(result,getState(atHead,code));
            }
            return result;
        }
        
        @Inline
        void pop() {
            if (nPushed==0) {
                nPopped++;
            } else {
                nPushed--;
            }
        }
        
        @Inline
        void pop(int n) {
            nPushed-=n;
            if (nPushed<0) {
                nPopped-=nPushed;
                nPushed=0;
            }
        }

        @NoInline
        private void reallocStack() {
            int[] newPushed=new int[(pushed.length+1)<<1];
            System.arraycopy(pushed,0,
                             newPushed,0,
                             nPushed);
            pushed=newPushed;
        }
        
        @Inline
        void push(int type) {
            if (pushed.length==nPushed) {
                reallocStack();
            }
            pushed[nPushed++]=type;
        }
        
        @NoInline
        @NoReturn
        private void popOverflow() {
            throw new CodeGenException("Pop overflow");
        }
        
        @Inline
        int popValue() {
            if (nPushed==0) {
                int idx=nPopped++;
                if (idx>65535 || idx<0) {
                    popOverflow();
                    return 0; // make javac happy
                }
                return ExtendedTypes.newStackRef(idx);
            } else {
                return pushed[--nPushed];
            }
        }
        
        void assign(int local,int type) {
            locals[local]=type;
        }
        
        int load(int local) {
            return locals[local];
        }
        
        void mayThrow() {
            for (int i=0;i<locals.length;++i) {
                int curLocal=locals[i];
                int result=ExtendedTypes.tryLub(curLocal,excLocalsSimple[i]);
                if (result<0) {
                    if (excLocals==null) {
                        if (verbosity>=1) {
                            System.err.println("Inflating excLocals in "+this);
                        }
                        excLocals=new int[locals.length][];
                    }
                    int[] lubs=excLocals[i];
                    if (lubs==null) {
                        if (verbosity>=1) {
                            System.err.println("Inflating excLocal["+i+"] in "+this+
                                               " because of "+curLocal+" (simple = "+excLocalsSimple[i]+")");
                        }
                        excLocals[i]=new int[]{curLocal};
                    } else {
                        boolean found=false;
                        for (int j=0;j<lubs.length;++j) {
                            result=ExtendedTypes.tryLub(curLocal,lubs[j]);
                            if (result>=0) {
                                lubs[j]=result;
                                found=true;
                                break;
                            }
                        }
                        if (!found) {
                            if (verbosity>=1) {
                                System.err.println(
                                    "Extending inflation excLocal["+i+"] in "+this+
                                    " because of "+curLocal+" (simple = "+excLocalsSimple[i]+")");
                            }
                            int[] newLubs=new int[lubs.length+1];
                            System.arraycopy(lubs,0,
                                             newLubs,0,
                                             lubs.length);
                            newLubs[lubs.length]=curLocal;
                            excLocals[i]=newLubs;
                        }
                    }
                } else {
                    excLocalsSimple[i]=result;
                }
            }
            mayThrow=true;
        }
    }

    class BB {
        int bcOffset;
        
        EdgeState atHead;
        EdgeState atTail;
        BlockState body;
        ThrowState throu;
        
        BB[] normalSuccessors;
        int nNormalSuccessors;
        
        BB[] excSuccessors;
        int nExcSuccessors;
        
        BB(int bcOffset) {
            this.bcOffset=bcOffset;
            atHead=new EdgeState();
            atTail=new EdgeState();
            body=new BlockState();
            throu=new ThrowState();
            normalSuccessors=EMPTY_SUCC;
            excSuccessors=EMPTY_SUCC;
        }
        
        public String toString() {
            StringBuilder buf=new StringBuilder();
            buf.append("[BB: ");
            buf.append(bcOffset);
            buf.append(" Succ(");
            for (int i=0;i<nNormalSuccessors;++i) {
                if (i!=0) {
                    buf.append(", ");
                }
                buf.append(normalSuccessors[i].bcOffset);
            }
            buf.append("), ExcSucc(");
            for (int i=0;i<nExcSuccessors;++i) {
                if (i!=0) {
                    buf.append(", ");
                }
                buf.append(excSuccessors[i].bcOffset);
            }
            buf.append("), ");
            buf.append(body.toString());
            buf.append("]");
            return buf.toString();
        }
        
        void addNormalSuccessor(BB succ) {
            if (verbosity>=1) {
                System.err.println("Edge: "+bcOffset+" -> "+succ.bcOffset);
            }
            if (nNormalSuccessors==normalSuccessors.length) {
                BB[] newSucc=new BB[(nNormalSuccessors+1)<<1];
                System.arraycopy(normalSuccessors,0,
                                 newSucc,0,
                                 nNormalSuccessors);
                normalSuccessors=newSucc;
            }
            normalSuccessors[nNormalSuccessors++]=succ;
            if (verbosity>=1) {
                System.err.println(this);
            }
        }
        
        void addExcSuccessor(BB succ) {
            if (verbosity>=1) {
                System.err.println("Exc edge: "+bcOffset+" -> "+succ.bcOffset);
            }
            if (nExcSuccessors==excSuccessors.length) {
                BB[] newSucc=new BB[(nExcSuccessors+1)<<1];
                System.arraycopy(excSuccessors,0,
                                 newSucc,0,
                                 nExcSuccessors);
                excSuccessors=newSucc;
            }
            excSuccessors[nExcSuccessors++]=succ;
            if (verbosity>=1) {
                System.err.println(this);
            }
        }
        
        boolean propagate() {
            try {
                boolean result=false;
                result|=atTail.mergeFrom(atHead,body);
                result|=throu.mergeFrom(atHead,body);
                if (verbosity>=1) {
                    System.err.println(this);
                    System.err.println("atHead("+bcOffset+"): "+atHead);
                    System.err.println("atTail("+bcOffset+"): "+atTail);
                }
                if (atTail.complete) {
                    if (verbosity>=1 && nNormalSuccessors==0) {
                        System.err.println("not propagating from "+bcOffset+" because it has no successors");
                    }
                    for (int i=0;i<nNormalSuccessors;++i) {
                        if (verbosity>=1) {
                            System.err.println("propagating along "+bcOffset+" -> "+
                                               normalSuccessors[i].bcOffset);
                        }
                        try {
                            result|=normalSuccessors[i].atHead.mergeFrom(atTail);
                        } catch (CodeGenException e) {
                            throw new CodeGenException(
                                "Failed to propagate from "+bcOffset+" to "+
                                normalSuccessors[i].bcOffset,e);
                        }
                    }
                } else {
                    if (verbosity>=1) {
                        System.err.println("not propagating from "+bcOffset+" due to incompleteness");
                    }
                }
                if (throu.mayThrow) {
                    if (verbosity>=1 && nExcSuccessors==0) {
                        System.err.println("not exception-propagating from "+bcOffset+" because it has no exception-successors");
                    }
                    for (int i=0;i<nExcSuccessors;++i) {
                        if (verbosity>=1) {
                            System.err.println("propagating exceptional flow along "+bcOffset+
                                               " -> "+excSuccessors[i].bcOffset);
                        }
                        try {
                            result|=excSuccessors[i].atHead.mergeFrom(throu);
                        } catch (CodeGenException e) {
                            throw new CodeGenException(
                                "Failed to propagate exceptional flow from "+bcOffset+" to "+
                                excSuccessors[i].bcOffset,e);
                        }
                    }
                } else {
                    if (verbosity>=1) {
                        System.err.println("not exception-propagating from "+bcOffset+" because it doesn't throw");
                    }
                }
                return result;
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {
                throw new CodeGenException(
                    "Failed to propagate at bcOffset = "+bcOffset,e);
            }
        }
        
        void assertComplete() {
            try {
                atHead.assertComplete();
                atTail.assertComplete();
                throu.assertComplete();
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {
                throw new CodeGenException(
                    "Failed to assert completeness at bcOffset = "+bcOffset,e);
            }
        }
    }
    
    static class TryCatch {
        int start;
        int end;
        int target;
        boolean terminal;
        
        TryCatch(int start,
                 int end,
                 int target,
                 boolean terminal) {
            this.start=start;
            this.end=end;
            this.target=target;
            this.terminal=terminal;
        }
    }

    TreeMap< Integer, BB > blocks=new TreeMap< Integer, BB >();
    ArrayList< BB > blocksArray;
    ArrayList< TryCatch > handlers=new ArrayList< TryCatch >();
    boolean finished=false;
    int bytecodeSize;
    
    BB blockFor(int bcOffset) {
        BB result=blocks.get(bcOffset);
        if (result==null) {
            if (finished) {
                throw new CodeGenException("Could not find basic block at "+bcOffset);
            } else {
                if (verbosity>=1) {
                    System.err.println("Allocating BB("+bcOffset+")");
                }
                blocks.put(bcOffset,result=new BB(bcOffset));
                if (verbosity>=1) {
                    System.err.println("blocks["+bcOffset+"] = "+blocks.get(bcOffset));
                }
            }
        }
        return result;
    }
    
    BB blockFor(Label label) {
        return blockFor(label.getOffsetWorks());
    }
    
    boolean propForward() {
        if (verbosity>=1) {
            System.err.println("    Propagating forward...");
        }
        boolean changed=false;
        for (BB bb : blocksArray) {
            changed|=bb.propagate();
        }
        return changed;
    }
    
    boolean propBackward() {
        if (verbosity>=1) {
            System.err.println("    Propagating backward...");
        }
        boolean changed=false;
        for (int i=blocksArray.size();i-->0;) {
            changed|=blocksArray.get(i).propagate();
        }
        return changed;
    }
    
    public BytecodeTypeAnalysis(MethodBytecodeExtractor mbe) {
        this(mbe,0);
    }
    
    public BytecodeTypeAnalysis(MethodBytecodeExtractor mbe,
                                int verbosity_) {
        this.mbe=mbe;
        this.verbosity=verbosity_;
        
        try {

            if (verbosity>=1) {
                System.err.println("Beginning bytecode type analysis...");
                System.err.println("nLocals = "+nLocals()+", nStack = "+nStack());
            }
        
            mbe.extract(
                new EmptyMethodVisitor() {
                    BB currentBB;
                    BlockState current;
                    boolean terminal;
                    boolean justJumped;
                    int lastOffset;
                    boolean endSeen;
                
                        {
                            currentBB=blockFor(0);
                            current=currentBB.body;
                            terminal=false;
                        }
                
                    // FIXME: if we see a branch then we need to terminate the previous
                    // block and fire off a new one at the succeeding instruction!!
                    // i.e. need a flag that we set whenever we add a successor, and
                    // visitBCOffset needs to Do The Right thing when that flag is
                    // set...
                
                    void makeNewBlock(int offset) {
                        BB newBB=blockFor(offset);
                        if (newBB!=currentBB) {
                            if (!terminal) {
                                currentBB.addNormalSuccessor(newBB);
                            }
                            currentBB=newBB;
                            current=currentBB.body;
                            terminal=false;
                        }
                    }

                    public void visitBCOffset(int offset) {
                        if (false && verbosity>=1) {
                            System.err.println("at bcOffset = "+offset);
                        }
                        if (justJumped) {
                            makeNewBlock(offset);
                            justJumped=false;
                        }
                        lastOffset=offset;
                    }
                
                    public void visitLabel(Label l) {
                        if (false && verbosity>=1) {
                            System.err.println("visiting label with bcOffset = "+l.getOffsetWorks());
                        }
                        makeNewBlock(l.getOffsetWorks());
                    }
                    
                    public void visitEnd() {
                        if (endSeen) {
                            throw new Error("Seeing end twice!");
                        }
                        // if a method ends in a jump then we'll stupidly insert a
                        // basic block at the tail.  here we kill it if it exists.
                        if (verbosity>=1) {
                            System.err.println("Removing "+lastOffset);
                        }
                        blocks.remove(lastOffset);
                        bytecodeSize=lastOffset;
                        endSeen=true;
                    }
                
                    public void visitTryCatchBlock(Label start,
                                                   Label end,
                                                   Label handler,
                                                   UTF8Sequence type) {
                        // NOTE: because this analysis doubles as a light-weight verification,
                        // we don't want to mark a catch block as terminal just because it
                        // catches Throwable.  It turns out that javac/ecj will emit code
                        // that is unreachable in this way, presumably because they don't
                        // have accurate information about which bytecode instructions may
                        // or may not throw.  But perhaps this is something that we'll have to
                        // look into...
                        handlers.add(
                            new TryCatch(
                                start.getOffsetWorks(),
                                end.getOffsetWorks(),
                                handler.getOffsetWorks(),
                                type==null/* || type.equals(UTF8Sequence.java_lang_Throwable)*/));
                    }
                
                    public void visitInsn(int opcode) {
                        switch (opcode) {
                        case Opcodes.NOP:
                        case Opcodes.INEG:
                        case Opcodes.LNEG:
                        case Opcodes.FNEG:
                        case Opcodes.DNEG:
                        case Opcodes.I2B:
                        case Opcodes.I2C:
                        case Opcodes.I2S:
                            break;
                        case Opcodes.ACONST_NULL:
                            current.push('L');
                            break;
                        case Opcodes.ICONST_M1:
                        case Opcodes.ICONST_0:
                        case Opcodes.ICONST_1:
                        case Opcodes.ICONST_2:
                        case Opcodes.ICONST_3:
                        case Opcodes.ICONST_4:
                        case Opcodes.ICONST_5:
                            current.push('I');
                            break;
                        case Opcodes.FCONST_0:
                        case Opcodes.FCONST_1:
                        case Opcodes.FCONST_2:
                            current.push('F');
                            break;
                        case Opcodes.LCONST_0:
                        case Opcodes.LCONST_1:
                            current.push('J');
                            current.push('-');
                            break;
                        case Opcodes.DCONST_0:
                        case Opcodes.DCONST_1:
                            current.push('D');
                            current.push('-');
                            break;
                        case Opcodes.IALOAD:
                        case Opcodes.BALOAD:
                        case Opcodes.CALOAD:
                        case Opcodes.SALOAD:
                        case Opcodes.IDIV:
                        case Opcodes.IREM:
                            current.mayThrow();
                            current.pop(2);
                            current.push('I');
                            break;
                        case Opcodes.LALOAD:
                            current.mayThrow();
                            current.pop(2);
                            current.push('J');
                            current.push('-');
                            break;
                        case Opcodes.FALOAD:
                        case Opcodes.FDIV:
                        case Opcodes.FREM:
                            current.mayThrow();
                            current.pop(2);
                            current.push('F');
                            break;
                        case Opcodes.DALOAD:
                            current.mayThrow();
                            current.pop(2);
                            current.push('D');
                            current.push('-');
                            break;
                        case Opcodes.AALOAD:
                            current.mayThrow();
                            current.pop(2);
                            current.push('L');
                            break;
                        case Opcodes.IASTORE:
                        case Opcodes.FASTORE:
                        case Opcodes.AASTORE:
                        case Opcodes.BASTORE:
                        case Opcodes.CASTORE:
                        case Opcodes.SASTORE:
                            current.mayThrow();
                            current.pop(3);
                            break;
                        case Opcodes.DASTORE:
                        case Opcodes.LASTORE:
                            current.mayThrow();
                            current.pop(4);
                            break;
                        case Opcodes.ISHL:
                        case Opcodes.ISHR:
                        case Opcodes.IUSHR:
                        case Opcodes.IAND:
                        case Opcodes.IOR:
                        case Opcodes.IXOR:
                        case Opcodes.IADD:
                        case Opcodes.ISUB:
                        case Opcodes.IMUL:
                            current.pop(2);
                            current.push('I');
                            break;
                        case Opcodes.FADD:
                        case Opcodes.FSUB:
                        case Opcodes.FMUL:
                            current.pop(2);
                            current.push('F');
                            break;
                        case Opcodes.POP:
                            current.pop(1);
                            break;
                        case Opcodes.POP2:
                            current.pop(2);
                            break;
                        case Opcodes.DUP: {
                            int val=current.popValue();
                            current.push(val);
                            current.push(val);
                            break;
                        }
                        case Opcodes.DUP_X1: {
                            int val1=current.popValue();
                            int val2=current.popValue();
                            current.push(val1);
                            current.push(val2);
                            current.push(val1);
                            break;
                        }
                        case Opcodes.DUP_X2: {
                            int val1=current.popValue();
                            int val2=current.popValue();
                            int val3=current.popValue();
                            current.push(val1);
                            current.push(val3);
                            current.push(val2);
                            current.push(val1);
                            break;
                        }
                        case Opcodes.DUP2: {
                            int val1=current.popValue();
                            int val2=current.popValue();
                            current.push(val2);
                            current.push(val1);
                            current.push(val2);
                            current.push(val1);
                            break;
                        }
                        case Opcodes.DUP2_X1: {
                            int val1=current.popValue();
                            int val2=current.popValue();
                            int val3=current.popValue();
                            current.push(val2);
                            current.push(val1);
                            current.push(val3);
                            current.push(val2);
                            current.push(val1);
                            break;
                        }
                        case Opcodes.DUP2_X2: {
                            int val1=current.popValue();
                            int val2=current.popValue();
                            int val3=current.popValue();
                            int val4=current.popValue();
                            current.push(val2);
                            current.push(val1);
                            current.push(val4);
                            current.push(val3);
                            current.push(val2);
                            current.push(val1);
                            break;
                        }
                        case Opcodes.SWAP: {
                            int val1=current.popValue();
                            int val2=current.popValue();
                            current.push(val1);
                            current.push(val2);
                            break;
                        }
                        case Opcodes.LDIV:
                        case Opcodes.LREM:
                            current.mayThrow();
                            current.pop(4);
                            current.push('J');
                            current.push('-');
                            break;
                        case Opcodes.LAND:
                        case Opcodes.LOR:
                        case Opcodes.LXOR:
                        case Opcodes.LADD:
                        case Opcodes.LSUB:
                        case Opcodes.LMUL:
                            current.pop(4);
                            current.push('J');
                            current.push('-');
                            break;
                        case Opcodes.DDIV:
                        case Opcodes.DREM:
                            current.mayThrow();
                            current.pop(4);
                            current.push('D');
                            current.push('-');
                            break;
                        case Opcodes.DADD:
                        case Opcodes.DSUB:
                        case Opcodes.DMUL:
                            current.pop(4);
                            current.push('D');
                            current.push('-');
                            break;
                        case Opcodes.LSHL:
                        case Opcodes.LSHR:
                        case Opcodes.LUSHR:
                            current.pop();
                            break;
                        case Opcodes.I2L:
                        case Opcodes.F2L:
                            current.pop();
                            current.push('J');
                            current.push('-');
                            break;
                        case Opcodes.I2F:
                            current.pop();
                            current.push('F');
                            break;
                        case Opcodes.I2D:
                        case Opcodes.F2D:
                            current.pop();
                            current.push('D');
                            current.push('-');
                            break;
                        case Opcodes.L2I:
                        case Opcodes.D2I:
                            current.pop(2);
                            current.push('I');
                            break;
                        case Opcodes.L2F:
                        case Opcodes.D2F:
                            current.pop(2);
                            current.push('F');
                            break;
                        case Opcodes.L2D:
                            current.pop(2);
                            current.push('D');
                            current.push('-');
                            break;
                        case Opcodes.F2I:
                            current.pop();
                            current.push('I');
                            break;
                        case Opcodes.D2L:
                            current.pop(2);
                            current.push('J');
                            current.push('-');
                            break;
                        case Opcodes.LCMP:
                        case Opcodes.DCMPL:
                        case Opcodes.DCMPG:
                            current.pop(4);
                            current.push('I');
                            break;
                        case Opcodes.FCMPL:
                        case Opcodes.FCMPG:
                            current.pop(2);
                            current.push('I');
                            break;
                        case Opcodes.IRETURN:
                        case Opcodes.LRETURN:
                        case Opcodes.FRETURN:
                        case Opcodes.DRETURN:
                        case Opcodes.ARETURN:
                        case Opcodes.RETURN:
                            terminal=true;
                            break;
                        case Opcodes.ARRAYLENGTH:
                            current.pop();
                            current.push('I');
                            break;
                        case Opcodes.ATHROW:
                            current.mayThrow();
                            current.pop();
                            terminal=true;
                            break;
                        case Opcodes.MONITORENTER:
                            current.mayThrow();
                            current.pop();
                            break;
                        case Opcodes.MONITOREXIT:
                            current.mayThrow();
                            current.pop();
                            break;
                        default:
                            throw new CodeGenException("unrecognized opcode: "+opcode);
                        }
                    }
                
                    public void visitFieldInsn(int opcode,
                                               UTF8Sequence owner,
                                               UTF8Sequence name,
                                               UTF8Sequence desc) {
                        char type=Types.toExec((char)desc.byteAt(0));
                        current.mayThrow();
                        switch (opcode) {
                        case Opcodes.GETSTATIC:
                            current.push(type);
                            if (Types.cells(type)==2) {
                                current.push('-');
                            }
                            break;
                        case Opcodes.PUTSTATIC:
                            current.pop(Types.cells(type));
                            break;
                        case Opcodes.GETFIELD:
                            current.pop();
                            current.push(type);
                            if (Types.cells(type)==2) {
                                current.push('-');
                            }
                            break;
                        case Opcodes.PUTFIELD:
                            current.pop(1+Types.cells(type));
                            break;
                        default:
                            throw new CodeGenException("unrecognized opcode: "+opcode);
                        }
                    }
                
                    public void visitMethodInsn(int opcode,
                                                UTF8Sequence owner,
                                                UTF8Sequence name,
                                                UTF8Sequence desc) {
                        TypeParsing.MethodSigSeqs sigs=TypeParsing.splitMethodSig(desc);
                        char retType=(char)sigs.result().byteAt(0);
                        if (retType!='V') {
                            retType=Types.toExec(retType);
                        }
                        int paramCells=0;
                        for (UTF8Sequence sig : sigs.params()) {
                            paramCells+=Types.cells(Types.toExec((char)sig.byteAt(0)));
                        }
                        current.mayThrow();
                        switch (opcode) {
                        case Opcodes.INVOKEVIRTUAL:
                        case Opcodes.INVOKESPECIAL:
                        case Opcodes.INVOKEINTERFACE:
                            paramCells++;
                            break;
                        case Opcodes.INVOKESTATIC:
                            break;
                        default:
                            throw new CodeGenException("unrecognized opcode: "+opcode);
                        }
                        current.pop(paramCells);
                        if (retType!='V') {
                            current.push(retType);
                            if (Types.cells(retType)==2) {
                                current.push('-');
                            }
                        }
                    }
                
                    public void visitIincInsn(int var,int increment) {
                        current.assign(var,'I');
                    }
                
                    public void visitIntInsn(int opcode,int operand) {
                        switch (opcode) {
                        case Opcodes.BIPUSH:
                        case Opcodes.SIPUSH:
                            current.push('I');
                            break;
                        case Opcodes.NEWARRAY:
                            current.mayThrow();
                            current.pop();
                            current.push('L');
                            break;
                        default:
                            throw new CodeGenException("unrecognized opcode: "+opcode);
                        }
                    }
                
                    public void visitJumpInsn(int opcode,Label label) {
                        switch (opcode) {
                        case Opcodes.IF_ACMPEQ:
                        case Opcodes.IF_ACMPNE:
                        case Opcodes.IF_ICMPEQ:
                        case Opcodes.IF_ICMPNE:
                        case Opcodes.IF_ICMPLT:
                        case Opcodes.IF_ICMPGE:
                        case Opcodes.IF_ICMPGT:
                        case Opcodes.IF_ICMPLE:
                            current.pop(2);
                            currentBB.addNormalSuccessor(blockFor(label));
                            justJumped=true;
                            break;
                        case Opcodes.IFEQ:
                        case Opcodes.IFNE:
                        case Opcodes.IFLT:
                        case Opcodes.IFGE:
                        case Opcodes.IFGT:
                        case Opcodes.IFLE:
                        case Opcodes.IFNULL:
                        case Opcodes.IFNONNULL:
                            current.pop(1);
                            currentBB.addNormalSuccessor(blockFor(label));
                            justJumped=true;
                            break;
                        case Opcodes.GOTO:
                            currentBB.addNormalSuccessor(blockFor(label));
                            justJumped=true;
                            terminal=true; // bad terminology but I can't bring myself to care
                            break;
                        case Opcodes.JSR:
                            throw new CodeGenException("saw unexpected JSR: "+opcode);
                        default:
                            throw new CodeGenException("unrecognized opcode: "+opcode);
                        }
                    }
                
                    public void visitLdcInsn(Object cst) {
                        if (cst instanceof Integer) {
                            current.push('I');
                        } else if (cst instanceof Float) {
                            current.push('F');
                        } else if (cst instanceof Long) {
                            current.push('J');
                            current.push('-');
                        } else if (cst instanceof Double) {
                            current.push('D');
                            current.push('-');
                        } else if (cst instanceof String || cst instanceof com.fiji.asm.Type) {
                            current.push('R');
                        } else {
                            throw new CodeGenException("unexpected kind of LDC: "+cst);
                        }
                    }
                
                    public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels) {
                        current.pop();
                        currentBB.addNormalSuccessor(blockFor(dflt));
                        for (Label l : labels) {
                            currentBB.addNormalSuccessor(blockFor(l));
                        }
                        terminal=true;
                    }
                
                    public void visitMultiANewArrayInsn(UTF8Sequence desc,int dims) {
                        current.mayThrow();
                        current.pop(dims);
                        current.push('R');
                    }
                
                    public void visitTableSwitchInsn(int min,int max,Label dflt,Label[] labels) {
                        current.pop();
                        currentBB.addNormalSuccessor(blockFor(dflt));
                        for (Label l : labels) {
                            currentBB.addNormalSuccessor(blockFor(l));
                        }
                        terminal=true;
                    }
                
                    public void visitTypeInsn(int opcode,UTF8Sequence type) {
                        switch (opcode) {
                        case Opcodes.NEW:
                            current.mayThrow();
                            current.push('R');
                            break;
                        case Opcodes.ANEWARRAY:
                            current.mayThrow();
                            current.pop();
                            current.push('R');
                            break;
                        case Opcodes.CHECKCAST:
                            current.mayThrow();
                            break;
                        case Opcodes.INSTANCEOF:
                            current.pop();
                            current.push('I');
                            break;
                        default:
                            throw new CodeGenException("unexpected opcode: "+opcode);
                        }
                    }
                
                    public void visitVarInsn(int opcode,int var) {
                        switch (opcode) {
                        case Opcodes.ILOAD:
                            current.push('I');
                            break;
                        case Opcodes.LLOAD:
                            current.push('J');
                            current.push('-');
                            break;
                        case Opcodes.FLOAD:
                            current.push('F');
                            break;
                        case Opcodes.DLOAD:
                            current.push('D');
                            current.push('-');
                            break;
                        case Opcodes.ALOAD:
                            current.push(current.load(var));
                            break;
                        case Opcodes.ISTORE:
                            current.assign(var,'I');
                            current.pop();
                            break;
                        case Opcodes.LSTORE:
                            current.assign(var,'J');
                            current.pop(2);
                            break;
                        case Opcodes.FSTORE:
                            current.assign(var,'F');
                            current.pop();
                            break;
                        case Opcodes.DSTORE:
                            current.assign(var,'D');
                            current.pop(2);
                            break;
                        case Opcodes.ASTORE:
                            current.assign(var,current.popValue());
                            break;
                        case Opcodes.RET:
                            throw new CodeGenException("unexpected RET");
                        default:
                            throw new CodeGenException("unexpected opcode: "+opcode);
                        }
                    }
                });
        
            if (verbosity>=1) {
                System.err.println("Parsed code.");
            }
        
            // set up root block
            BB root=blockFor(0);
            TypeParsing.MethodSigSeqs mss=TypeParsing.splitMethodSig(mbe.descriptor());
            int offset=0;
            if ((mbe.flags()&Constants.BF_STATIC)==0) {
                root.atHead.assign(offset,'R');
                offset++;
            }
            for (int i=0;i<mss.nParams();++i) {
                root.atHead.assign(offset,Types.toExec(mss.param(i).charAt(0)));
                offset+=Types.cells(mss.param(i).charAt(0));
            }
            for (int i=offset;i<nLocals();++i) {
                root.atHead.assign(i,'.');
            }
            root.atHead.forceComplete();
        
            // indicate that no new blocks should be created
            finished=true;
        
            blocksArray=new ArrayList< BB >(blocks.values());
        
            if (verbosity>=1) {
                System.err.println("Linking try-catch blocks...");
            }
            
            // link try-catch blocks
            for (BB bb : blocks.values()) {
            loopOverHandlers:
                for (TryCatch tc : handlers) {
                    if (bb.bcOffset >= tc.start && bb.bcOffset < tc.end) {
                        bb.addExcSuccessor(blockFor(tc.target));
                        if (tc.terminal) {
                            break loopOverHandlers;
                        }
                    }
                }
            }
        
            if (verbosity>=1) {
                System.err.println("Performing fixpoint...");
            }

            // perform fixpoint
            for (int cnt=1;;cnt++) {
                if (verbosity>=1) {
                    System.err.println("  Fixpoint iteration #"+cnt);
                }
                if (!propForward()) break;
                if (!propBackward()) break;
                if (!propForward()) break;
            }
        
            if (verbosity>=1) {
                System.err.println("Fixpoint done; asserting completeness...");
            }
        
            // assert that everyone is complete
            for (BB bb : blocksArray) {
                bb.assertComplete();
            }
        
            if (verbosity>=1) {
                System.err.println("Analysis done.");
            }
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable e) {
            throw new CodeGenException("Failed to analyze "+mbe,e);
        }
    }
    
    public class ForwardHeavyCalc extends EmptyMethodVisitor {
        int[] stack;
        int[] locals;
        int nPushed;
        boolean justJumped;
        
        public ForwardHeavyCalc() {
            stack=new int[nStack()];
            locals=new int[nLocals()];
            reset(0);
        }
        
        void reset(int bcOffset) {
            if (bcOffset!=bytecodeSize) {
                BB bb=blockFor(bcOffset);
                System.arraycopy(bb.atHead.stack,0,
                                 stack,0,
                                 bb.atHead.stack.length);
                System.arraycopy(bb.atHead.locals,0,
                                 locals,0,
                                 locals.length);
                nPushed=bb.atHead.stack.length;
            }
        }
        
        void assign(int local,int type) {
            locals[local]=type;
        }
        
        public void push(int type) {
            stack[nPushed++]=type;
        }
        
        public int popValue() {
            return stack[--nPushed];
        }
        
        public void pop() {
            nPushed--;
        }
        
        public void pop(int amount) {
            nPushed-=amount;
        }
        
        // INEFFICIENT!  do not use except for debugging!
        public int[] stack() {
            int[] result=new int[nPushed];
            System.arraycopy(stack,0,
                             result,0,
                             nPushed);
            return result;
        }
        
        public int[] locals() {
            return locals;
        }
        
        public int stackHeight() {
            return nPushed;
        }
        
        public int stackAtAbsolute(int height) {
            int result=stack[height];
            if (ExtendedTypes.isStateRef(result)) {
                throw new CodeGenException("cannot have state references anymore: "+result+" at abs height "+height);
            }
            return result;
        }
        
        public int stackAt(int offset) {
            int result=stack[nPushed-offset-1];
            if (ExtendedTypes.isStateRef(result)) {
                throw new CodeGenException("cannot have state references anymore: "+result+" at height "+offset);
            }
            return result;
        }
        
        // useful for situations where you know that the stack offset is invalid,
        // but in which case you still want something returned because you intend
        // to ignore it anyway.
        public int tryStackAt(int offset) {
            int idx=nPushed-offset-1;
            if (idx<0 || idx>=stack.length) {
                return 0;
            }
            return stackAt(offset);
        }
        
        public boolean stackAtIsRef(int offset) {
            return ExtendedTypes.isRef(stackAt(offset));
        }
        
        public boolean stackAtIsNonNull(int offset) {
            return ExtendedTypes.isNonNull(stackAt(offset));
        }
        
        public int localAt(int local) {
            int result=locals[local];
            if (ExtendedTypes.isStateRef(result)) {
                throw new CodeGenException("cannot have state references anymore: "+result+" at local "+local);
            }
            return result;
        }
        
        public boolean localAtIsRef(int local) {
            return ExtendedTypes.isRef(localAt(local));
        }
        
        public boolean localAtIsNonNull(int local) {
            return ExtendedTypes.isNonNull(localAt(local));
        }
        
        public void visitBCOffset(int bcOffset) {
            if (justJumped) {
                reset(bcOffset);
                justJumped=false;
            }
        }
        
        public void visitLabel(Label l) {
            if ((l.getStatus()&Label.DEBUG)==0) {
                reset(l.getOffsetWorks());
            }
        }
        
        public void visitInsn(int opcode) {
            switch (opcode) {
            case Opcodes.NOP:
            case Opcodes.INEG:
            case Opcodes.LNEG:
            case Opcodes.FNEG:
            case Opcodes.DNEG:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                break;
            case Opcodes.ACONST_NULL:
                push('L');
                break;
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                push('I');
                break;
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
                push('F');
                break;
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
                push('J');
                push('-');
                break;
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                push('D');
                push('-');
                break;
            case Opcodes.IALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            case Opcodes.IDIV:
            case Opcodes.IREM:
                pop(2);
                push('I');
                break;
            case Opcodes.LALOAD:
                pop(2);
                push('J');
                push('-');
                break;
            case Opcodes.FALOAD:
            case Opcodes.FDIV:
            case Opcodes.FREM:
                pop(2);
                push('F');
                break;
            case Opcodes.DALOAD:
                pop(2);
                push('D');
                push('-');
                break;
            case Opcodes.AALOAD:
                pop(2);
                push('L');
                break;
            case Opcodes.IASTORE:
            case Opcodes.FASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                pop(3);
                break;
            case Opcodes.DASTORE:
            case Opcodes.LASTORE:
                pop(4);
                break;
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
                pop(2);
                push('I');
                break;
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
                pop(2);
                push('F');
                break;
            case Opcodes.POP:
                pop(1);
                break;
            case Opcodes.POP2:
                pop(2);
                break;
            case Opcodes.DUP: {
                int val=popValue();
                push(val);
                push(val);
                break;
            }
            case Opcodes.DUP_X1: {
                int val1=popValue();
                int val2=popValue();
                push(val1);
                push(val2);
                push(val1);
                break;
            }
            case Opcodes.DUP_X2: {
                int val1=popValue();
                int val2=popValue();
                int val3=popValue();
                push(val1);
                push(val3);
                push(val2);
                push(val1);
                break;
            }
            case Opcodes.DUP2: {
                int val1=popValue();
                int val2=popValue();
                push(val2);
                push(val1);
                push(val2);
                push(val1);
                break;
            }
            case Opcodes.DUP2_X1: {
                int val1=popValue();
                int val2=popValue();
                int val3=popValue();
                push(val2);
                push(val1);
                push(val3);
                push(val2);
                push(val1);
                break;
            }
            case Opcodes.DUP2_X2: {
                int val1=popValue();
                int val2=popValue();
                int val3=popValue();
                int val4=popValue();
                push(val2);
                push(val1);
                push(val4);
                push(val3);
                push(val2);
                push(val1);
                break;
            }
            case Opcodes.SWAP: {
                int val1=popValue();
                int val2=popValue();
                push(val1);
                push(val2);
                break;
            }
            case Opcodes.LDIV:
            case Opcodes.LREM:
                pop(4);
                push('J');
                push('-');
                break;
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
                pop(4);
                push('J');
                push('-');
                break;
            case Opcodes.DDIV:
            case Opcodes.DREM:
                pop(4);
                push('D');
                push('-');
                break;
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
                pop(4);
                push('D');
                push('-');
                break;
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
                pop();
                break;
            case Opcodes.I2L:
            case Opcodes.F2L:
                pop();
                push('J');
                push('-');
                break;
            case Opcodes.I2F:
                pop();
                push('F');
                break;
            case Opcodes.I2D:
            case Opcodes.F2D:
                pop();
                push('D');
                push('-');
                break;
            case Opcodes.L2I:
            case Opcodes.D2I:
                pop(2);
                push('I');
                break;
            case Opcodes.L2F:
            case Opcodes.D2F:
                pop(2);
                push('F');
                break;
            case Opcodes.L2D:
                pop(2);
                push('D');
                push('-');
                break;
            case Opcodes.F2I:
                pop();
                push('I');
                break;
            case Opcodes.D2L:
                pop(2);
                push('J');
                push('-');
                break;
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                pop(4);
                push('I');
                break;
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                pop(2);
                push('I');
                break;
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.RETURN:
                break;
            case Opcodes.ARRAYLENGTH:
                pop();
                push('I');
                break;
            case Opcodes.ATHROW:
                pop();
                break;
            case Opcodes.MONITORENTER:
                pop();
                break;
            case Opcodes.MONITOREXIT:
                pop();
                break;
            default:
                throw new CodeGenException("unrecognized opcode: "+opcode);
            }
        }
                
        public void visitFieldInsn(int opcode,
                                   UTF8Sequence owner,
                                   UTF8Sequence name,
                                   UTF8Sequence desc) {
            char type=Types.toExec((char)desc.byteAt(0));
            switch (opcode) {
            case Opcodes.GETSTATIC:
                push(type);
                if (Types.cells(type)==2) {
                    push('-');
                }
                break;
            case Opcodes.PUTSTATIC:
                pop(Types.cells(type));
                break;
            case Opcodes.GETFIELD:
                pop();
                push(type);
                if (Types.cells(type)==2) {
                    push('-');
                }
                break;
            case Opcodes.PUTFIELD:
                pop(1+Types.cells(type));
                break;
            default:
                throw new CodeGenException("unrecognized opcode: "+opcode);
            }
        }
                
        public void visitMethodInsn(int opcode,
                                    UTF8Sequence owner,
                                    UTF8Sequence name,
                                    UTF8Sequence desc) {
            TypeParsing.MethodSigSeqs sigs=TypeParsing.splitMethodSig(desc);
            char retType=Types.toExec((char)sigs.result().byteAt(0));
            int paramCells=0;
            for (UTF8Sequence sig : sigs.params()) {
                paramCells+=Types.cells(Types.toExec((char)sig.byteAt(0)));
            }
            if (Protocols.isInstance(opcode)) {
                paramCells++;
            }
            pop(paramCells);
            if (retType!='V') {
                push(retType);
                if (Types.cells(retType)==2) {
                    push('-');
                }
            }
        }
                
        public void visitIincInsn(int var,int increment) {
            assign(var,'I');
        }
                
        public void visitIntInsn(int opcode,int operand) {
            switch (opcode) {
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                push('I');
                break;
            case Opcodes.NEWARRAY:
                pop();
                push('R');
                break;
            default:
                throw new CodeGenException("unrecognized opcode: "+opcode);
            }
        }
                
        public void visitJumpInsn(int opcode,Label label) {
            switch (opcode) {
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
                pop(2);
                justJumped=true;
                break;
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                pop(1);
                justJumped=true;
                break;
            case Opcodes.GOTO:
                justJumped=true;
                break;
            case Opcodes.JSR:
                throw new CodeGenException("saw unexpected JSR: "+opcode);
            default:
                throw new CodeGenException("unrecognized opcode: "+opcode);
            }
        }
                
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Integer) {
                push('I');
            } else if (cst instanceof Float) {
                push('F');
            } else if (cst instanceof Long) {
                push('J');
                push('-');
            } else if (cst instanceof Double) {
                push('D');
                push('-');
            } else if (cst instanceof String || cst instanceof com.fiji.asm.Type) {
                push('R');
            } else {
                throw new CodeGenException("unexpected kind of LDC: "+cst);
            }
        }
                
        public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] labels) {
            pop();
            justJumped=true;
        }
                
        public void visitMultiANewArrayInsn(UTF8Sequence desc,int dims) {
            pop(dims);
            push('R');
        }
                
        public void visitTableSwitchInsn(int min,int max,Label dflt,Label[] labels) {
            pop();
            justJumped=true;
        }
                
        public void visitTypeInsn(int opcode,UTF8Sequence type) {
            switch (opcode) {
            case Opcodes.NEW:
                push('R');
                break;
            case Opcodes.ANEWARRAY:
                pop();
                push('R');
                break;
            case Opcodes.CHECKCAST:
                break;
            case Opcodes.INSTANCEOF:
                pop();
                push('I');
                break;
            default:
                throw new CodeGenException("unexpected opcode: "+opcode);
            }
        }
                
        public void visitVarInsn(int opcode,int var) {
            switch (opcode) {
            case Opcodes.ILOAD:
                push('I');
                break;
            case Opcodes.LLOAD:
                push('J');
                push('-');
                break;
            case Opcodes.FLOAD:
                push('F');
                break;
            case Opcodes.DLOAD:
                push('D');
                push('-');
                break;
            case Opcodes.ALOAD:
                push(localAt(var));
                break;
            case Opcodes.ISTORE:
                assign(var,'I');
                pop();
                break;
            case Opcodes.LSTORE:
                assign(var,'J');
                pop(2);
                break;
            case Opcodes.FSTORE:
                assign(var,'F');
                pop();
                break;
            case Opcodes.DSTORE:
                assign(var,'D');
                pop(2);
                break;
            case Opcodes.ASTORE:
                assign(var,popValue());
                break;
            case Opcodes.RET:
                throw new CodeGenException("unexpected RET");
            default:
                throw new CodeGenException("unexpected opcode: "+opcode);
            }
        }
    }
    
    public int[] localsAt(int bcOffset) {
        BB bb=blocks.get(bcOffset);
        if (bb==null) {
            throw new CodeGenException("Do not know about bytecode offset "+bcOffset);
        }
        return bb.atHead.locals;
    }
    
    public int[] stackAt(int bcOffset) {
        BB bb=blocks.get(bcOffset);
        if (bb==null) {
            throw new CodeGenException("Do not know about bytecode offset "+bcOffset);
        }
        return bb.atHead.stack;
    }
    
    public static String typesToString(int[] types) {
        if (types==null) {
            return "null";
        }
        StringBuilder buf=new StringBuilder();
        for (int code : types) {
            switch (ExtendedTypes.codeKind(code)) {
            case ExtendedTypes.EXPLICIT_TYPE:
                if (code==0) {
                    buf.append('_');
                } else {
                    buf.append((char)code);
                }
                break;
            case ExtendedTypes.LOCAL_REFERENCE:
                buf.append("(local: "+ExtendedTypes.getPayload(code)+")");
                break;
            case ExtendedTypes.STACK_REFERENCE:
                buf.append("(stack: "+ExtendedTypes.getPayload(code)+")");
                break;
            case ExtendedTypes.PTR_TYPE:
                buf.append("(pointer");
                for (int n=ExtendedTypes.getPayload(code);n-->0;) {
                    buf.append("[]");
                }
                buf.append(")");
                break;
            case ExtendedTypes.SYMB_TYPE:
                buf.append("(symbolic: "+ExtendedTypes.getPayload(code)+")");
                break;
            default:
                throw new CodeGenException("unrecognized code: "+code);
            }
        }
        return buf.toString();
    }
    
    public Set< Integer > knownPoints() {
        return blocks.keySet();
    }
}

