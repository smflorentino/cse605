/*
 * OptRCE.java
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

import com.fiji.fivm.om.OMField;

/**
 * Redundant Code Elimination.  This implements both RCE over non-heap-accessing
 * and non-side-effecting instructions, as well as heap loads (for both the Java
 * and C heaps).
 * FIXME: kill this.
 */
public class OptRCE extends TimedCodePhase {
    public OptRCE(Code c) { super(c); }
    
    static volatile int removalCnt;
    
    static volatile long totalDoms;
    static volatile long totalNumbering;
    static volatile long totalPreProp;
    static volatile long totalTotal;
    
    NormalDominatorCalc dc;
    
    int[] codes;
    Instruction[] insts;
    ValueSource[] values;
    ConcreteVar[] vars;
    
    HashMap< Header, AbstractState > valuesAtHead;
    Worklist worklist;
    
    Header h;
    AbstractState curValues;
    
    public void visitCode() {
        if (Global.verbosity>=3) {
            Global.log.println("OptRCE starting on "+code.shortName());
        }
        
        long before=System.currentTimeMillis();
        
        dc=code.getNormalDominators();
        poll();
        dc.makeDomMap();
        poll();
        
        if (Global.verbosity>=4) {
            long timeSpent=System.currentTimeMillis()-before;
            synchronized (OptRCE.class) {
                totalDoms+=timeSpent;
            }
            Global.log.println("in "+code.shortName()+" spent "+timeSpent+" on dominators (total: "+totalDoms+")");
        }
        
        valuesAtHead=new HashMap< Header, AbstractState >();
        HashMap< Header, AbstractState > valuesAtTail = new HashMap< Header, AbstractState >();
        HashMap< Header, AbstractState > valuesForHandler = new HashMap< Header, AbstractState >();
        
        int cnt=code.computeTotalOrder();
        codes=new int[cnt+1];
        insts=new Instruction[cnt+1];
        values=new ValueSource[cnt+1];
        vars=new ConcreteVar[cnt+1];
        
        // assign numbers
        HashMap< InstNumbering, Integer > numberSet=
            new HashMap< InstNumbering, Integer >();
        for (Header h : code.headers()) {
            poll();
            valuesAtHead.put(h,new AbstractState());
            valuesAtTail.put(h,new AbstractState());
            valuesForHandler.put(h,new AbstractState());
            for (Instruction i : h.instructions()) {
                if (!code.getSideEffects().get(
                        i,CallSideEffectMode.ALL_CALLS_ARE_SIDE_EFFECTS)) {
                    InstNumbering in=new InstNumbering(i);
                    Integer code=numberSet.get(in);
                    if (code==null) {
                        numberSet.put(in,i.order);
                        insts[i.order]=i;
                        codes[i.order]=i.order;
                        values[i.order]=produceVS(i,i.order);
                    } else {
                        codes[i.order]=code;
                        values[i.order]=values[code];
                    }
                    vars[i.order]=new ConcreteVar(i.lhs(),h);
                }
            }
        }
        
        if (Global.verbosity>=4) {
            long timeSpent=System.currentTimeMillis()-before;
            synchronized (OptRCE.class) {
                totalNumbering+=timeSpent;
            }
            Global.log.println("in "+code.shortName()+" spent "+timeSpent+" numbering (total: "+totalNumbering+")");
        }
        
        // propagate pure values according to dominance and distance
        for (Header h : code.headers()) {
            poll();
            this.h=h;
            ArrayList< Instruction > pures=new ArrayList< Instruction >();
            for (Instruction i : h.instructions()) {
                if (pure(i)) {
                    pures.add(i);
                }
            }
            for (Header h2 : dc.dominates(h)) {
                if (h2!=h) {
                    AbstractState curValues=valuesAtHead.get(h2);
                    if (distance(h,h2)<Global.rceDistance) {
                        for (Instruction i : pures) {
                            //if (vars[i.order].inst()
                            curValues.putPermPure(values[i.order],vars[i.order]);
                        }
                    } else {
                        for (Instruction i : pures) {
                            curValues.putKnown(values[i.order]);
                        }
                    }
                }
            }
        }
        
        if (Global.verbosity>=4) {
            long timeSpent=System.currentTimeMillis()-before;
            synchronized (OptRCE.class) {
                totalPreProp+=timeSpent;
            }
            Global.log.println("in "+code.shortName()+" spent "+timeSpent+" pre propagation (total: "+totalPreProp+")");
        }
        
        Spectrum< Header > counts=new Spectrum< Header >();
        
        worklist=new Worklist();
        
        worklist.push(code.root());
        valuesAtHead.put(code.root(),new AbstractState());

        // do forward flow.
        while (!worklist.empty()) {
            poll();
            h = worklist.pop();
            
            if (Global.verbosity>=1) {
                counts.add(h);
                if (counts.count(h)>code.headers().size()*100) {
                    Global.log.println("seeing "+h+" for the "+counts.count(h)+"th time in "+code.shortName());
                }
            }
            
            curValues=valuesAtHead.get(h);
            assert curValues!=null;
            curValues=curValues.copy();
            
            AbstractState curForHandler=new AbstractState();
            
            for (Operation o : h.operations()) {
                if (code.getThrows().get(o)) {
                    // this is hard to handle precisely unless we "split unhappy blocks".
                    // so we fudge it, since it's prolly not worth it.
                    curForHandler.addKnown(curValues);
                }
                update(o);
            }
            
            curValues.clearLocal();
            
            if (!valuesAtTail.get(h).equals(curValues)) {
                valuesAtTail.put(h,curValues);
                for (Header h2 : h.normalSuccessors()) {
                    mergeWith(h2);
                }
            }
            
            if (!valuesForHandler.get(h).equals(curForHandler)) {
                valuesForHandler.put(h,curForHandler);
                curValues=curForHandler;
                
                for (Header h2 : h.exceptionalSuccessors()) {
                    mergeWith(h2);
                }
            }
        }
        
        // don't need tail values anymore or the worklist; help GC
        worklist=null;
        
        // perform transformation
        for (Header h : code.headers()) {
            this.h=h;
            this.curValues=valuesAtHead.get(h);
            assert curValues!=null; // we should not see unreachable code.
            for (Instruction i : h.instructions()) {
                Var replacement=update(i);
                if (replacement!=null) {
                    i.prepend(
                        new SimpleInst(
                            i.di(),OpCode.Mov,
                            i.lhs(),new Arg[]{replacement}));
                    i.remove();
                    if (Global.verbosity>=2) {
                        removalCnt++;
                        Global.log.println("removed redundancy in "+code.shortName()+": "+i+" (total "+removalCnt+")");
                    }
                    setChangedCode();
                }
            }
        }
        
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
        
        // done.  help GC.
        valuesAtHead=null;
        h=null;
        curValues=null;
        vars=null;
        dc=null;
        codes=null;
        insts=null;
        values=null;
        
        if (Global.verbosity>=3) {
            long timeSpent=System.currentTimeMillis()-before;
            synchronized (OptRCE.class) {
                totalTotal+=timeSpent;
            }
            Global.log.println("OptRCE for "+code.shortName()+" took "+
                                timeSpent+" (total: "+totalTotal+")");
        }
    }
    
    static int distance(Header a,
                        Header b) {
        int ai=a.first().order;
        int aa=a.getFooter().order;
        int bi=b.first().order;
        int ba=b.getFooter().order;
        
        if (ai < bi) {
            assert aa < bi;
            return bi-aa;
        } else {
            assert ai > ba;
            return ai-ba;
        }
    }

    static int distance(Operation a,
                        Operation b) {
        return Math.abs(a.order-b.order);
    }
    
    boolean pure(Operation o) {
        if (!code.getSideEffects().get(
                o,CallSideEffectMode.ALL_CALLS_ARE_SIDE_EFFECTS) &&
            o instanceof Instruction) {
            switch (o.opcode()) {
            case GetField:
            case GetStatic:
            case GetCField:
            case GetCVar:
            case Load:
            case ArrayLoad:
            case GetException:
            case Ipsilon: // not really sure why this is here. ;-)
            case GetAllocSpace: // this operation is *really* bizarre
            case GetDebugID:
                return false;
            case Cast: {
                TypeInst ti=(TypeInst)o;
                return ti.rhs(0).type().isObject()==ti.getType().isObject();
            }
            // FIXME: are there any other non-side-effecting, non-throwing instructions that
            // cannot be turned into CanonicalInsts?
            default: return !code.getThrows().get(o);
            }
        } else {
            return false;
        }
    }
    
    ValueSource produceVS(Instruction i,int code) {
        assert i.lhs()!=Var.VOID;
        
        // compute a potential value source
        switch (i.opcode()) {
        case GetField: {
            HeapAccessInst hai=(HeapAccessInst)i;
            return new FieldLocation(code,hai.rhs(0),hai.fieldField());
        }
        case GetStatic: {
            HeapAccessInst hai=(HeapAccessInst)i;
            return new StaticFieldLocation(code,hai.fieldField());
        }
        case GetCField: {
            CFieldInst cfi=(CFieldInst)i;
            return new CStructFieldLocation(code,cfi.rhs(0),cfi.structField());
        }
        case GetCVar: {
            CFieldInst cfi=(CFieldInst)i;
            return new CVarLocation(code,cfi.field());
        }
        case Load: {
            return new PointerLocation(code,i.rhs(0));
        }
        case ArrayLoad: {
            return new ArrayLocation(code,i.rhs(0),i.rhs(1));
        }
        case GetException: {
            return new GetExceptionInst(code);
        }
            
        case Cast: {
            // need to be *very* careful!!
            TypeInst ti=(TypeInst)i;
            if (ti.rhs(0).type().isObject()==ti.getType().isObject()) {
                return new CanonicalInst(code);
            }
            break;
        }
            
        case Ipsilon: // not really sure why this is here. ;-)
        case GetAllocSpace: // this operation is *really* bizarre
        case GetDebugID:
            break;
            // FIXME: are there any other non-side-effecting, non-throwing instructions that
            // cannot be turned into CanonicalInsts?
        default: {
            if (!this.code.getThrows().get(i)) {
                return new CanonicalInst(code);
            }
            break;
        }}
        return null;
    }
    
    /**
     * Performs a state update for the current operation assuming that curValues
     * and h are set.  If the instruction is redundant with one we know about,
     * return the Var corresponding to that instruction.  If it's not redundant,
     * return null.
     */
    Var update(Operation o) {
        if (code.getSideEffects().get(
                o,CallSideEffectMode.ALL_CALLS_ARE_SIDE_EFFECTS)) {
            curValues.invalidateBy(o,code);
        } else if (o instanceof Instruction) {
            Instruction i=(Instruction)o;
            
            ValueSource src=values[i.order];
            
            if (Global.verbosity>=5) {
                Global.log.println("for "+i+": "+src);
            }
            
            if (src!=null) {
                VarValue oldVar=curValues.get(src);
                
                if (Global.verbosity>=5) {
                    Global.log.println("old for "+i+": "+oldVar);
                }
                
                if (oldVar==null ||
                    oldVar.isTop()) {
                    curValues.put(src,vars[i.order]);
                } else if (oldVar.var()!=i.lhs() &&
                           // this is a *really* annoying case!! >:-(
                           oldVar.var().type().isSubtypeOf(i.lhs().type())) {
                    VarValue filtered=oldVar.distExpire(o);
                    if (filtered.hasVar()) {
                        return filtered.var();
                    }
                }
            }
        }
        
        return null; // assume not redundant by default.
    }
    
    void mergeWith(Header h2) {
        if (Global.verbosity>=4) {
            Global.log.println("merging from "+h+" to "+h2);
        }
        
        boolean changed=false;

        AbstractState otherValues=valuesAtHead.get(h2);
        assert otherValues!=null;
        
        changed|=otherValues.mergeAgainst(curValues,h2);

        if (changed) {
            worklist.push(h2);
        }
    }

    static class AbstractState {
        HashMap< ValueSource, VarValue > values=new HashMap< ValueSource, VarValue >();
        HashMap< ValueSource, VarValue > localValues=new HashMap< ValueSource, VarValue >();
        HashMap< ValueSource, VarValue > invalidables=new HashMap< ValueSource, VarValue >();
        BitSet known=new BitSet();
        
        AbstractState() {
        }
        
        void clearLocal() {
            localValues.clear();
        }
        
        AbstractState copy() {
            AbstractState result=new AbstractState();
            result.values=values; // not mutable
            result.invalidables.putAll(invalidables);
            result.known.or(known);
            return result;
        }
        
        void putPermPure(ValueSource value,
                         VarValue var) {
            if (!known.get(value.code)) {
                known.set(value.code);
                values.put(value,var);
            }
        }

        void put(ValueSource value,
                 VarValue var) {
            boolean wasKnown=known.get(value.code);
            if (value.invalidable()) {
                known.set(value.code);
                if (var.isTop()) {
                    if (wasKnown) {
                        invalidables.remove(value);
                    }
                } else {
                    invalidables.put(value,var);
                }
            } else {
                known.set(value.code);
                localValues.put(value,var);
            }
        }
        
        void putKnown(ValueSource value) {
            known.set(value.code);
        }
        
        void invalidate(ValueSource value) {
            known.set(value.code);
            invalidables.remove(value);
        }
        
        void invalidateBy(Operation o,
                          Code code) {
            ArrayList< ValueSource > toInvalidate=new ArrayList< ValueSource >();
            for (ValueSource src : invalidables.keySet()) {
                if (src.invalidatedBy(o,code)) {
                    toInvalidate.add(src);
                }
            }
            for (ValueSource src : toInvalidate) {
                invalidate(src);
            }
        }
        
        VarValue get(ValueSource value) {
            if (known.get(value.code)) {
                if (!value.invalidable()) {
                    VarValue result=localValues.get(value);
                    if (result==null) {
                        result=values.get(value);
                        if (result==null) {
                            return TOP;
                        }
                    }
                    return result;
                } else {
                    VarValue result=invalidables.get(value);
                    if (result==null) {
                        return TOP;
                    } else {
                        return result;
                    }
                }
            } else {
                return null;
            }
        }
        
        private VarValue getInvalidable(ValueSource value) {
            if (known.get(value.code)) {
                VarValue result=invalidables.get(value);
                if (result==null) {
                    return TOP;
                } else {
                    return result;
                }
            } else {
                return null;
            }
        }
        
        boolean mergeAgainst(AbstractState other,
                             Header myHeader) {
            boolean changed=false;
            int oldCard=known.cardinality();
            for (ValueSource curSrc : other.invalidables.keySet()) {
                VarValue curVar=
                    other.invalidables.get(curSrc).strictlyExpire(myHeader).distExpire(myHeader);
                VarValue oldVar=getInvalidable(curSrc);
                if (oldVar==null) {
                    changed=true;
                    put(curSrc,curVar);
                } else {
                    VarValue newVar=oldVar.merge(curVar);
                    // wtf?  does this ever happen?
                    // yup!
                    //
                    // x = foo.field
                    // if (predicate) {
                    //    foo.field = y
                    // } else {
                    //    b = 2 + 2
                    // }
                    // z = foo.field
                    //
                    // one of the incoming edges to the last instuction will have
                    // foo.field -> TOP, while the other will have foo.field -> x,
                    // and we lub to TOP.
                    if (newVar!=oldVar) {
                        put(curSrc,curVar);
                        changed=true;
                    }
                }
            }
            for (Iterator< ValueSource > i=invalidables.keySet().iterator();
                 i.hasNext();) {
                ValueSource src=i.next();
                if (!other.invalidables.containsKey(src) && other.known.get(src.code)) {
                    i.remove();
                    changed=true;
                }
            }
            known.or(other.known);
            return changed || known.cardinality()!=oldCard;
        }
        
        void addKnown(AbstractState other) {
            known.or(other.known);
        }
        
        boolean equals(AbstractState other) {
            if (invalidables.size()!=other.invalidables.size() ||
                !known.equals(other.known)) {
                return false;
            }
            return invalidables.equals(other.invalidables);
        }
    }

    static class VarValue {
        boolean hasVar() { return false; }
        Var var() { throw new Error(); }
        Header header() { throw new Error(); }
        Instruction source() { throw new Error(); }

        final VarValue distExpire(Header h) {
            return distExpire(h.first());
        }
        
        VarValue distExpire(Operation o) { return TOP; }

        VarValue expire(Header h) { return TOP; }
        VarValue strictlyExpire(Header h) { return TOP; }
        
        boolean isTop() { return false; }
        
        VarValue merge(VarValue other) { return this; }
    }
    
    static VarValue TOP = new VarValue() {
            boolean isTop() { return true; }
            public String toString() { return "TOP"; }
        };

    class ConcreteVar extends VarValue {
        Var v;
        Header source;
        ConcreteVar(Var v,
                    Header source) {
            this.v=v;
            this.source=source;
        }
        boolean hasVar() { return true; }
        Var var() { return v; }
        Instruction source() { return v.inst(); }
        Header header() { return source; }
        
        VarValue distExpire(Operation o) {
            if (distance(source(),o)>=Global.rceDistance) {
                return TOP;
            } else {
                return this;
            }
        }
        
        VarValue expire(Header h) {
            if (dc.dominates(source,h)) {
                return this;
            } else {
                return TOP;
            }
        }
        
        VarValue strictlyExpire(Header h) {
            if (source==h) {
                return TOP;
            } else {
                return expire(h);
            }
        }
        
        VarValue merge(VarValue other) {
            if (other==null || equals(other)) {
                return this;
            } else {
                return TOP;
            }
        }
        
        public int hashCode() {
            return v.hashCode();
        }
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof ConcreteVar)) return false;
            ConcreteVar other=(ConcreteVar)other_;
            return v==other.v;
        }
        public String toString() {
            return v.toString();
        }
    }
    
    static class ValueSource {
        int code;
        
        ValueSource(int code) {
            this.code=code;
        }
        
        /**
         * Checks if the source is invalidated by the given operation.
         * Note that the fixpoint will automatically invalidate any
         * ValueSource attached to the LHS of an assignment when that
         * assignment is encountered a second time, or when not all
         * incoming edges have a ValueSource.
         */
        boolean invalidatedBy(Operation o,
                              Code code) { return false; }
        
        boolean invalidable() { return false; }
        
        public int hashCode() {
            return code;
        }
        
        public boolean equals(Object other) {
            return this==other;
        }
    }

    // anything that isn't a side-effect and that doesn't load from the heap
    // is treated as a CanonicalInst.  that includes ArrayLength
    class CanonicalInst extends ValueSource {
        CanonicalInst(int code) {
            super(code);
        }
        
        public String toString() {
            return "[CanonicalInst #"+code+" = "+insts[code].toString()+"]";
        }
    }
    
    // this is special
    class GetExceptionInst extends CanonicalInst {
        GetExceptionInst(int code) {
            super(code);
        }
        
        boolean invalidatedBy(Operation o,
                              Code code) {
            switch (o.opcode()) {
            case ClearException:
                return true;
            default:
                return code.getThrows().get(o);
            }
        }
        
        boolean invalidable() { return true; }
    }

    static class Location extends ValueSource {
        
        Location(int code) { super(code); }
        
        boolean frameLocal() {
            return false;
        }
        
        boolean threadLocal() {
            return false;
        }
        
        boolean locationInvalidatedBy(Operation o) {
            return false;
        }
        
        final boolean invalidatedBy(Operation o,
                                    Code code) {
            switch (o.opcode()) {
            case WeakCASStatic:
            case WeakCASField:
            case WeakCASElement:
            case WeakCAS:
            case StrongLoadCAS:
            case StrongCAS:
            case StrongVoidCAS:
            case CompilerFence:
            case HardCompilerFence:
            case Fence:
            case PollcheckFence:
            case HardUse:
                return true;
            case PollCheck:
                return code.typeLowered;
            case Invoke:
            case InvokeStatic:
            case InvokeDynamic:
            case InvokeResolved:
            case InvokeIndirect:
            case Call:
            case CallIndirect:
                return !frameLocal();
            case MonitorEnter:
            case MonitorExit:
            case CheckInit:
                return !threadLocal() && !frameLocal();
            default:
                return locationInvalidatedBy(o);
            }
        }
        
        boolean invalidable() { return true; }
    }
    
    static class FieldLocation extends Location {
        Arg recv;
        OMField field;
        
        FieldLocation(int code,
                      Arg recv,
                      OMField field) {
            super(code);
            this.recv=recv;
            this.field=field;
        }
        
        boolean locationInvalidatedBy(Operation o) {
            switch (o.opcode()) {
            case PutField:
                return ((HeapAccessInst)o).field()==field;
            default:
                return false;
            }
        }
    }
    
    static class StaticFieldLocation extends Location {
        OMField field;
        
        StaticFieldLocation(int code,
                            VisibleField field) {
            super(code);
            this.field=field;
            assert field.isStatic();
        }
        
        boolean locationInvalidatedBy(Operation o) {
            switch (o.opcode()) {
            case PutStatic:
                return ((HeapAccessInst)o).field()==field;
            default:
                return false;
            }
        }
    }
    
    static class CStructFieldLocation extends Location {
        Arg recv;
        CStructField field;
        
        CStructFieldLocation(int code,
                             Arg recv,
                             CStructField field) {
            super(code);
            this.recv=recv;
            this.field=field;
        }
        
        boolean frameLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.FRAME_LOCAL;
        }
        
        boolean threadLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.THREAD_LOCAL;
        }
        
        boolean locationInvalidatedBy(Operation o) {
            switch (o.opcode()) {
            case Store:
                return true;
            case PutCField: {
                if (false) {
                    CFieldInst cfi=(CFieldInst)o;
                    CStructField otherField=cfi.structField();
                    return field==otherField;
                } else {
                    // for now assume that we don't have type-safe C field
                    // accesses....
                    return true;
                }
            }
            default:
                return false;
            }
        }
    }
    
    static class CVarLocation extends Location {
        CField field;
        
        CVarLocation(int code,
                     CField field) {
            super(code);
            this.field=field;
        }
        
        boolean frameLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.FRAME_LOCAL;
        }
        
        boolean threadLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.THREAD_LOCAL;
        }
        
        boolean locationInvalidatedBy(Operation o) {
            switch (o.opcode()) {
            case Store:
                return true;
            case PutCVar: {
                if (false) {
                    CFieldInst cfi=(CFieldInst)o;
                    return field==cfi.field();
                } else {
                    // for now assume that we don't have type-safe C field
                    // accesses....
                    return true;
                }
            }
            default:
                return false;
            }
        }
    }
    
    static class PointerLocation extends Location {
        Arg ptr;
        
        PointerLocation(int code,
                        Arg ptr) {
            super(code);
            this.ptr=ptr;
        }
        
        boolean locationInvalidatedBy(Operation o) {
            switch (o.opcode()) {
            case Store:
            case PutCField:
                return true;
            default:
                return false;
            }
        }
    }
    
    static class ArrayLocation extends Location {
        Arg array;
        Arg index;
        Type type;
        
        ArrayLocation(int code,
                      Arg array,
                      Arg index) {
            super(code);
            this.array=array;
            this.index=index;
        }
        
        boolean locationInvalidatedBy(Operation o) {
            switch (o.opcode()) {
            case ArrayStore:
                return ((HeapAccessInst)o).fieldType()==array.type().arrayElement();
            default:
                return false;
            }
        }
    }
    
    // used for value numbering
    static class InstNumbering {
        Instruction inst;
        int hashCode;
        
        InstNumbering(Instruction inst) {
            this.inst=inst;
            this.hashCode=OpHashCode.getNoLhs(inst);
        }

        public int hashCode() {
            return hashCode;
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof InstNumbering)) return false;
            InstNumbering other=(InstNumbering)other_;
            return inst.opcode()==other.inst.opcode()
                && OpEquals.getNoLhs(inst,other.inst);
        }
    }
}

