/*
 * OptReload.java
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

public class OptReload extends CodePhase {
    public OptReload(Code c) { super(c); }
    
    NormalDominatorCalc dc;
    HeaderProximityCalc hpc;
    
    HashMap< Header, HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > > valuesAtHead;
    HashMap< Header, HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > > valuesAtTail;
    
    public void visitCode() {
        dc=code.getNormalDominators();
        hpc=code.getHeaderProximity();

        code.computeTotalOrder();
        
        if (Global.verbosity>=5) {
            Global.log.println("starting OptReload on "+code.shortName());
        }
        
        valuesAtHead=new HashMap< Header, HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > >();
        valuesAtTail=new HashMap< Header, HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > >();
        
        Worklist worklist=new Worklist();
        
        valuesAtHead.put(code.root(),new HashMap< LocationKey, HashMap< Location, HashSet< Arg > > >());
        worklist.push(code.root());
        for (Header h : code.handlerHeaders()) {
            valuesAtHead.put(h,new HashMap< LocationKey, HashMap< Location, HashSet< Arg > > >());
            worklist.push(h);
        }
        
        while (!worklist.empty()) {
            Header h=worklist.pop();
            
            HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues=
                copy(valuesAtHead.get(h));
            
            if (Global.verbosity>=5) {
                Global.log.println("in "+h+" with "+curValues);
            }
            
            for (Operation o : h.operations()) {
                invalidate(curValues,o);
                if (o instanceof Instruction) {
                    addLocation(curValues,(Instruction)o);
                }
                if (Global.verbosity>=6) {
                    Global.log.println("after "+o+": "+curValues);
                }
            }
            
            HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > otherValues=
                valuesAtTail.get(h);
            if (otherValues==null || !otherValues.equals(curValues)) {
                valuesAtTail.put(h,copy(curValues));
                for (Header h2 : h.normalSuccessors()) {
                    if (merge(curValues, h2)) {
                        worklist.push(h2);
                        if (Global.verbosity>=6) {
                            Global.log.println("changed "+h2);
                        }
                    }
                }
            }
        }
        
        ResultTypeCalc resultType=code.getResultType();
        
        // do transformation
        for (Header h : code.headers()) {
            HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues=
                copy(valuesAtHead.get(h));
            
            for (Instruction i : h.instructions()) {
                invalidate(curValues,i);
                Arg replacement=get(curValues,i);
                if (replacement!=null) {
                    if (resultType.getExact(i).isAssignableFrom(resultType.getExact(replacement))) {
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.Mov,
                                i.lhs(),new Arg[]{ replacement }));
                    } else if (resultType.getExact(i)==Type.BOOLEAN) {
                        i.prepend(
                            new SimpleInst(
                                i.di(),OpCode.Boolify,
                                i.lhs(),new Arg[]{ replacement }));
                    } else {
                        i.prepend(
                            new TypeInst(
                                i.di(),OpCode.Cast,
                                i.lhs(),new Arg[]{ replacement },
                                resultType.getExact(i)));
                    }
                    i.remove();
                    if (false) Global.log.println("RCE: "+i.opcode()+" "+i.di().shortName());
                    if (Global.verbosity>=5) {
                        Global.log.println("removed "+i+" at "+i.di().shortName());
                    }
                    setChangedCode("replaced "+i.opcode());
                }
                addLocation(curValues,i);
            }
        }
        
        code.recomputeOrder();
        if (changedCode()) {
            code.killIntraBlockAnalyses();
        }
        
        dc=null;
        valuesAtHead=null;
        valuesAtTail=null;
    }
    
    HashMap< LocationKey, HashMap< Location, HashSet< Arg > > >
        copy(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues) {
        
        HashMap< LocationKey, HashMap< Location, HashSet< Arg > > >
            otherValues=new HashMap< LocationKey, HashMap< Location, HashSet< Arg > > >();
        for (Map.Entry< LocationKey, HashMap< Location, HashSet< Arg > > > e : curValues.entrySet()) {
            HashMap< Location, HashSet< Arg > > otherSubValues=new HashMap< Location, HashSet< Arg > >();
            otherValues.put(e.getKey(),otherSubValues);
            for (Map.Entry< Location, HashSet< Arg > > e2 : e.getValue().entrySet()) {
                Location l=e2.getKey();
                HashSet< Arg > aSet=e2.getValue();
                otherSubValues.put(l,new HashSet< Arg >(aSet));
            }
        }
        
        return otherValues;
    }
    
    boolean merge(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues,
                  Header h2) {
        HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > otherValues=valuesAtHead.get(h2);
        if (otherValues==null) {
            otherValues=new HashMap< LocationKey, HashMap< Location, HashSet< Arg > > >();
            for (Map.Entry< LocationKey, HashMap< Location, HashSet< Arg > > > e : curValues.entrySet()) {
                HashMap< Location, HashSet< Arg > > otherSubValues=
                    new HashMap< Location, HashSet< Arg > >();
                otherValues.put(e.getKey(),otherSubValues);
                for (Map.Entry< Location, HashSet< Arg > > e2 : e.getValue().entrySet()) {
                    Location l=e2.getKey();
                    HashSet< Arg > aSet=e2.getValue();
                    HashSet< Arg > newASet=new HashSet< Arg >();
                    for (Arg a : aSet) {
                        if (validIn(a,h2)) {
                            newASet.add(a);
                        }
                    }
                    if (!newASet.isEmpty()) {
                        otherSubValues.put(l,newASet);
                    }
                }
            }
            valuesAtHead.put(h2,otherValues);
            return true;
        } else {
            HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > newOtherValues=
                new HashMap< LocationKey, HashMap< Location, HashSet< Arg > > >();
            for (Map.Entry< LocationKey, HashMap< Location, HashSet< Arg > > > e : curValues.entrySet()) {
                LocationKey key=e.getKey();
                HashMap< Location, HashSet< Arg > > curSubValues=e.getValue();
                HashMap< Location, HashSet< Arg > > otherSubValues=otherValues.get(key);
                if (otherSubValues!=null) {
                    HashMap< Location, HashSet< Arg > > newOtherSubValues=
                        new HashMap< Location, HashSet< Arg > >();
                    for (Map.Entry< Location, HashSet< Arg > > e2 : curSubValues.entrySet()) {
                        Location l=e2.getKey();
                        HashSet< Arg > aSet1=e2.getValue();
                        HashSet< Arg > aSet2=otherSubValues.get(l);
                        if (aSet2!=null) {
                            HashSet< Arg > newASet=new HashSet< Arg >();
                            for (Arg a : aSet1) {
                                if (aSet2.contains(a) &&
                                    validIn(a,h2)) {
                                    newASet.add(a);
                                }
                            }
                            if (!newASet.isEmpty()) {
                                newOtherSubValues.put(l,newASet);
                            }
                        }
                    }
                    if (!newOtherSubValues.isEmpty()) {
                        newOtherValues.put(key,newOtherSubValues);
                    }
                }
            }
            if (!otherValues.equals(newOtherValues)) {
                valuesAtHead.put(h2,newOtherValues);
                return true;
            } else {
                return false;
            }
        }
    }
    
    void addLocation(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues,
                     Location l, Arg a) {
        HashMap< Location, HashSet< Arg > > subValues=curValues.get(l.locationKey());
        if (subValues==null) {
            curValues.put(l.locationKey(),subValues=new HashMap< Location, HashSet< Arg > >());
        }
        HashSet< Arg > aSet=subValues.get(l);
        if (aSet==null) {
            subValues.put(l,aSet=new HashSet< Arg >());
        }
        aSet.add(a);
    }
    
    HashSet< Arg > get(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues,
                       Location l) {
        HashMap< Location, HashSet< Arg > > subValues=curValues.get(l.locationKey());
        if (subValues!=null) {
            HashSet< Arg > result=subValues.get(l);
            if (result==null) {
                return Arg.EMPTY_SET;
            } else {
                return result;
            }
        }
        return Arg.EMPTY_SET;
    }
    
    Arg get(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues,
            Location l,
            Operation o) {
        HashSet< Arg > result=get(curValues,l);
        Arg bestRepl=null;
        int bestDistance=Integer.MAX_VALUE;
        for (Arg a : result) {
            if (validIn(a, o)) {
                int dist=distance(a,o);
                if (dist<bestDistance) {
                    bestRepl=a;
                    bestDistance=dist;
                }
            }
        }
        return bestRepl;
    }
    
    void addLocation(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues,
                     Instruction i) {
        if (i instanceof Volatilable &&
            ((Volatilable)i).volatility().isVolatile()) {
            return;
        }
        
        switch (i.opcode()) {
        case GetField: {
            HeapAccessInst hai=(HeapAccessInst)i;
            addLocation(curValues,
                        new FieldLocation(hai.rhs(0),hai.fieldField()),
                        i.lhs());
            break;
        }
        case PutField: {
            HeapAccessInst hai=(HeapAccessInst)i;
            addLocation(curValues,
                        new FieldLocation(hai.rhs(0),hai.fieldField()),
                        i.rhs(1));
            break;
        }
        case GetStatic: {
            HeapAccessInst hai=(HeapAccessInst)i;
            addLocation(curValues,
                        new StaticFieldLocation(hai.fieldField()),
                        i.lhs());
            break;
        }
        case PutStatic: {
            HeapAccessInst hai=(HeapAccessInst)i;
            addLocation(curValues,
                        new StaticFieldLocation(hai.fieldField()),
                        i.rhs(0));
            break;
        }
        case GetCField: {
            CFieldInst cfi=(CFieldInst)i;
            addLocation(curValues,
                        new CStructFieldLocation(cfi.rhs(0),cfi.structField()),
                        i.lhs());
            break;
        }
        case PutCField: {
            CFieldInst cfi=(CFieldInst)i;
            addLocation(curValues,
                        new CStructFieldLocation(cfi.rhs(0),cfi.structField()),
                        i.rhs(1));
            break;
        }
        case GetCVar: {
            CFieldInst cfi=(CFieldInst)i;
            addLocation(curValues,
                        new CVarLocation(cfi.field()),
                        i.lhs());
            break;
        }
        case PutCVar: {
            CFieldInst cfi=(CFieldInst)i;
            addLocation(curValues,
                        new CVarLocation(cfi.field()),
                        i.rhs(0));
            break;
        }
        case Load: {
            addLocation(curValues,
                        new PointerLocation(i.rhs(0)),
                        i.lhs());
            break;
        }
        case Store: {
            // FIXME: what if the value being stored is a small type (byte, boolean, short, char)?
            // shouldn't we worry about the fact that the store/load combo is a cast?
            // Answer: no, since conversion from bytecode will ensure that the store is preceded
            // by a cast if necessary (i.e. if the value had turned into something where the
            // high bits were non-zero).
            addLocation(curValues,
                        new PointerLocation(i.rhs(0)),
                        i.rhs(1));
            break;
        }
        case ArrayLoad: {
            addLocation(curValues,
                        new ArrayLocation(i.rhs(0),i.rhs(1)),
                        i.lhs());
            break;
        }
        case ArrayStore: {
            addLocation(curValues,
                        new ArrayLocation(i.rhs(0),i.rhs(1)),
                        i.rhs(2));
            break;
        }
        default: break;
        }
    }
    
    Arg get(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues,
            Instruction i) {
        if (i instanceof Volatilable &&
            ((Volatilable)i).volatility().isVolatile()) {
            return null;
        }
        
        switch (i.opcode()) {
        case GetField: {
            HeapAccessInst hai=(HeapAccessInst)i;
            return get(curValues,
                       new FieldLocation(hai.rhs(0),hai.fieldField()),
                       i);
        }
        case GetStatic: {
            HeapAccessInst hai=(HeapAccessInst)i;
            return get(curValues,
                       new StaticFieldLocation(hai.fieldField()),
                       i);
        }
        case GetCField: {
            CFieldInst cfi=(CFieldInst)i;
            return get(curValues,
                       new CStructFieldLocation(cfi.rhs(0),cfi.structField()),
                       i);
        }
        case GetCVar: {
            CFieldInst cfi=(CFieldInst)i;
            return get(curValues,
                       new CVarLocation(cfi.field()),
                       i);
        }
        case Load: {
            return get(curValues,
                       new PointerLocation(i.rhs(0)),
                       i);
        }
        case ArrayLoad: {
            return get(curValues,
                       new ArrayLocation(i.rhs(0),i.rhs(1)),
                       i);
        }
        default:
            return null;
        }
    }
    
    boolean validIn(Arg a,Operation o) {
        if (a instanceof Var) {
            Var v=(Var)a;
            return dc.dominates(v.inst(),o)
                && hpc.proximityIsFinite(v.inst().head(),o.head())
                && distance(v.inst(),o)<Global.rceDistance;
        } else {
            return true;
        }
    }
    
    boolean validIn(Arg v,Header h) {
        return validIn(v,h.first());
    }

    static int distance(Operation a,
                        Operation b) {
        return Math.abs(a.order-b.order);
    }
    
    static int distance(Arg a,
                        Operation b) {
        if (a instanceof Var) {
            return distance(((Var)a).inst(),b);
        } else {
            return 0;
        }
    }
    
    void invalidate(HashMap< LocationKey, HashMap< Location, HashSet< Arg > > > curValues,
                    Operation o) {
        if (o instanceof Volatilable &&
            ((Volatilable)o).volatility().isVolatile()) {
            curValues.clear();
            return;
        }
        
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
        case Invoke:
        case InvokeStatic:
        case InvokeDynamic:
        case InvokeResolved:
        case InvokeIndirect:
        case Call:
        case CallIndirect:
        case MonitorEnter:
        case MonitorExit:
        case CheckInit:
        case Store:
        case PutCField:
            curValues.clear();
            break;
        case PollCheck:
            if (code.typeLowered) {
                curValues.clear();
            }
            break;
        case PutCVar:
            curValues.remove(LocationKey.NATIVE);
            break;
        case PutField:
        case PutStatic:
            curValues.remove(((HeapAccessInst)o).fieldField());
            curValues.remove(LocationKey.NATIVE);
            break;
        case ArrayStore:
            curValues.remove(((HeapAccessInst)o).fieldType().effectiveBasetype());
            curValues.remove(LocationKey.NATIVE);
            break;
        default:
            break;
        }
    }
    
    // OK: here's the intuition.
    // - a Location in this analysis is "like" a variable in a normal analysis
    // - so first do a liveness analysis - find where different peeps will be
    //   interested in Locations.  note, any side-effect will count as a Def
    // - then do forward flow with liveness pruning.
    
    // or .... do it without liveness?  what we really need is a compact way
    // of tracking these states: (a) value not available, (b) value available
    // from a variable, (c) value available from multiple variables
    // the tricky one is "value not available" - do we need to distinguish
    // between value not available because we don't know about it yet, or
    // value not available because it's been invalidated?
    
    static abstract class Location {
        Location() {
        }
        
        abstract LocationKey locationKey();
    }
    
    static final class FieldLocation extends Location {
        Arg recv;
        VisibleField field;
        
        FieldLocation(Arg recv,
                      VisibleField field) {
            this.recv=recv;
            this.field=field;
        }
        
        LocationKey locationKey() {
            return field;
        }
        
        public int hashCode() {
            return recv.hashCode()+field.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof FieldLocation)) return false;
            FieldLocation other=(FieldLocation)other_;
            return recv==other.recv
                && field==other.field;
        }
    }
    
    static final class StaticFieldLocation extends Location {
        VisibleField field;
        
        StaticFieldLocation(VisibleField field) {
            this.field=field;
            assert field.isStatic();
        }
        
        LocationKey locationKey() {
            return field;
        }
        
        public int hashCode() {
            return field.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof StaticFieldLocation)) return false;
            StaticFieldLocation other=(StaticFieldLocation)other_;
            return field==other.field;
        }
    }
    
    static final class CStructFieldLocation extends Location {
        Arg recv;
        CStructField field;
        
        CStructFieldLocation(Arg recv,
                             CStructField field) {
            this.recv=recv;
            this.field=field;
        }
        
        LocationKey locationKey() {
            return LocationKey.NATIVE;
        }
        
        boolean frameLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.FRAME_LOCAL;
        }
        
        boolean threadLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.THREAD_LOCAL;
        }
        
        public int hashCode() {
            return recv.hashCode()+field.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof CStructFieldLocation)) return false;
            CStructFieldLocation other=(CStructFieldLocation)other_;
            return recv==other.recv
                && field==other.field;
        }
    }
    
    static final class CVarLocation extends Location {
        CField field;
        
        CVarLocation(CField field) {
            this.field=field;
        }
        
        LocationKey locationKey() {
            return LocationKey.NATIVE;
        }
        
        boolean frameLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.FRAME_LOCAL;
        }
        
        boolean threadLocal() {
            return field.getThreadLocalMode()==ThreadLocalMode.THREAD_LOCAL;
        }
        
        public int hashCode() {
            return field.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof CVarLocation)) return false;
            CVarLocation other=(CVarLocation)other_;
            return field==other.field;
        }
    }
    
    static final class PointerLocation extends Location {
        Arg ptr;
        
        PointerLocation(Arg ptr) {
            this.ptr=ptr;
        }
        
        LocationKey locationKey() {
            return LocationKey.NATIVE;
        }
        
        public int hashCode() {
            return ptr.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof PointerLocation)) return false;
            PointerLocation other=(PointerLocation)other_;
            return ptr==other.ptr;
        }
    }
    
    static final class ArrayLocation extends Location {
        Arg array;
        Arg index;
        
        ArrayLocation(Arg array,
                      Arg index) {
            this.array=array;
            this.index=index;
        }
        
        LocationKey locationKey() {
            if (array.type().isArray()) {
                return array.type().arrayElement().effectiveBasetype();
            } else {
                return Basetype.BOTTOM;
            }
        }
        
        public int hashCode() {
            return array.hashCode()+index.structuralHashCode()*3;
        }
        
        public boolean equals(Object other_) {
            if (!(other_ instanceof ArrayLocation)) return false;
            ArrayLocation other=(ArrayLocation)other_;
            return array==other.array
                && index.structuralEquals(other.index);
        }
    }
}

