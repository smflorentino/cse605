/*
 * MachineCode.java
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

import com.fiji.fivm.*;
import com.fiji.fivm.r1.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import java.nio.*;
import java.util.*;

public final class MachineCode implements ObligatoryFinalizer, DebugIDAllocator {
    private Pointer mc;
    
    private Pointer address;
    private int size;
    
    public MachineCode(int size,
                       int flags,
                       Pointer methodRec) {
        this.size=size;
        mc=fivmr_MachineCode_create(size,flags);
        if (mc==Pointer.zero()) {
            throw new OutOfMemoryError(
                "Could not create a fivmr_MachineCode with size "+size);
        }
        log(MachineCode.class,2,
            "Created new machine code at "+mc.asLong()+" for methodRec at "+methodRec.asLong());
        CType.put(mc,
                  "fivmr_MachineCode",
                  "mr",
                  methodRec);
        address=CType.getPointer(mc,
                                 "fivmr_MachineCode",
                                 "code");
    }
    
    public static boolean supportDownsize() {
        return fivmr_supportDownsizeExec();
    }
    
    public static boolean logMachineCode() {
        return (CType.getInt(Magic.getVM(),"fivmr_VM","flags")
                & CVar.getInt("FIVMR_VMF_LOG_MACHINE_CODE"))!=0;
    }
    
    public void downsize(int newSize) {
        if (supportDownsize()) {
            if (newSize>size) {
                throw new fivmError(
                    "Cannot increase size: this.size = "+size+"; newSize = "+newSize);
            }
            fivmr_MachineCode_downsize(mc,newSize);
            size=newSize;
        }
    }
    
    public synchronized boolean destroy() {
        if (mc!=Pointer.zero()) {
            fivmr_MachineCode_down(mc);
            mc=Pointer.zero();
            address=Pointer.zero();
            size=0;
            return true;
        } else {
            return false;
        }
    }
    
    protected void finalize() {
        destroy();
    }
    
    public boolean active() {
        return mc!=Pointer.zero();
    }
    
    public Pointer getMachineCode() {
        return mc;
    }
    
    public Pointer getMethodRec() {
        return CType.getPointer(mc,"fivmr_MachineCode","mr");
    }
    
    public Pointer getRegion() {
        return getMachineCode();
    }
    
    public Pointer getCString(String s) {
        return getCStringFullRegion(getRegion(),s);
    }
    
    public Pointer regionAlloc(Pointer size) {
        return fivmRuntime.regionAlloc(getRegion(),size);
    }
    
    public Pointer regionAlloc(int size) {
        return regionAlloc(Pointer.fromInt(size));
    }
    
    public Pointer getTypeData() {
        return CType.getPointer(getMethodRec(),"fivmr_MethodRec","owner");
    }
    
    public Pointer getTypeContext() {
        return fivmr_TypeData_getContext(getTypeData());
    }
    
    public Class<?> getClazz() {
        return fivmr_TypeData_asClass(getTypeData());
    }
    
    @NoPollcheck
    @AllowUnsafe
    public ClassLoader getClassLoader() {
        return (ClassLoader)CType.getPointer(getTypeContext(),
                                             "fivmr_TypeContext",
                                             "classLoader").asObject();
    }
    
    static class DebugIDHash {
        int bytecodePC;
        int lineNumber;
        int nRefs;
        int[] refs;
        int hashCode;
        
        public DebugIDHash(int bytecodePC,
                           int lineNumber,
                           int nRefs,
                           int[] refs) {
            this.bytecodePC=bytecodePC;
            this.lineNumber=lineNumber;
            this.nRefs=nRefs;
            this.refs=refs;
            
            this.hashCode=bytecodePC+lineNumber*3+nRefs*7+Arrays.hashCode(refs);
        }
        
        public int hashCode() {
            return hashCode;
        }
        
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof DebugIDHash)) return false;
            DebugIDHash other=(DebugIDHash)other_;
            return hashCode==other.hashCode
                && bytecodePC==other.bytecodePC
                && lineNumber==other.lineNumber
                && nRefs==other.nRefs
                && Arrays.equals(refs,other.refs);
        }
    }
    
    HashMap< DebugIDHash, PointerBox > debugIDs=new HashMap< DebugIDHash, PointerBox >();
    
    public void clearDebugIDHash() {
        debugIDs.clear();
    }
    
    // constraints: call this with a refs array that has only zeroes past the nRefs point,
    // and make sure that it is a *new* array and you won't modify it after
    public Pointer allocDebugID(int bytecodePC,int lineNumber,int nRefs,int[] refs) {
        // for X86 anyway, the convention is as follows: the data on the stack
        // *preceding* the Frame is where the references are.  they are laid out
        // in reverse.
        
        // fix nRefs
        while (nRefs>=1 && !IntUtil.bit(refs,nRefs-1)) {
            nRefs--;
        }
        
        DebugIDHash hash=new DebugIDHash(bytecodePC,
                                         lineNumber,
                                         nRefs,
                                         refs);
        
        Pointer result;
        
        PointerBox resultBox=debugIDs.get(hash);
        if (resultBox==null) {
            result=regionAlloc(CType.sizeof("fivmr_DebugRec"));
            Pointer ln_rm_c;
            boolean fat;
            int lnShift=0;
            int pcShift=0;
        
            if (Pointer.size()==4) {
                if (lineNumber<0 || lineNumber>=(1<<10) ||
                    bytecodePC<0 || bytecodePC>=(1<<8) ||
                    nRefs>14) {
                    fat=true;
                } else {
                    lnShift=14;
                    pcShift=24;
                    fat=false;
                }
            } else {
                if (lineNumber<0 || lineNumber>=(1<<16) ||
                    bytecodePC<0 || bytecodePC>=(1<<15) ||
                    nRefs>31) {
                    fat=true;
                } else {
                    lnShift=33;
                    pcShift=49;
                    fat=false;
                }
            }
            if (fat) {
                Pointer fatty=regionAlloc(CType.sizeof("fivmr_FatDebugData").sub(4).add(4*((nRefs+31)/32)));
                CType.put(fatty,"fivmr_FatDebugData","lineNumber",lineNumber);
                CType.put(fatty,"fivmr_FatDebugData","bytecodePC",bytecodePC);
                CType.put(fatty,"fivmr_FatDebugData","refMapSize",(nRefs+31)/32);
                Pointer refMapPtr=fatty.add(CType.offsetof("fivmr_FatDebugData","refMap"));
                for (int i=0;i<(nRefs+31)/32;++i) {
                    refMapPtr.add(i*4).store(refs[i]);
                }
                ln_rm_c=fatty.or(Pointer.fromInt(1));
            } else {
                ln_rm_c=Pointer.fromInt(lineNumber).shl(lnShift).or(
                    Pointer.fromInt(bytecodePC).shl(pcShift).or(
                        Pointer.fromInt(refs.length==0?0:refs[0]).shl(2)));
            }
            CType.put(result,"fivmr_DebugRec","ln_rm_c",ln_rm_c);
            CType.put(result,"fivmr_DebugRec","method",getMachineCode());
            
            debugIDs.put(hash,new PointerBox(result));
        } else {
            result=resultBox.value();
        }
        
        return result;
    }
    
    public Pointer debugIDWithRootSize(Pointer did,
                                       int rootSize) {
        return fivmr_DebugRec_withRootSize(did,getRegion(),rootSize);
    }

    public void addPointer(Object o) {
        java.lang.fivmSupport.getClassLoaderRefs(getClassLoader()).add(o);
    }
    
    public void addBasepoint(int bytecodePC,
                             int stackHeight,
                             Pointer machinecodePC) {
        fivmr_MachineCode_appendBasepoint(getMachineCode(),
                                          bytecodePC,
                                          stackHeight,
                                          machinecodePC);
    }
    
    public void addBaseTryCatch(int start,
                                int end,
                                int target,
                                Pointer stub) {
        fivmr_MachineCode_appendBaseTryCatch(getMachineCode(),
                                             start,
                                             end,
                                             target,
                                             stub);
    }
    
    public void addChild(MachineCode child) {
        child.addToMC(getMachineCode());
    }
    
    static int totalSize;
    
    int upTotal() {
        synchronized (MachineCode.class) {
            totalSize+=getSize();
            return totalSize;
        }
    }
    
    public void addToMC(Pointer machineCode) {
        if (logMachineCode()) {
            logPrintFull("[JIT: registering "+getSize()+" bytes with "+
                         methodRecToString(CType.getPointer(machineCode,
                                                            "fivmr_MachineCode","mr"))+
                         " ("+upTotal()+" total)]\n");
        }
        log(MachineCode.class,1,
            "Registering machine code "+getMachineCode().asLong()+" ("+getAddress().asLong()+
            " size="+getSize()+
            ") with other machine code "+machineCode.asLong());
        fivmr_MachineCode_registerMC(machineCode,getMachineCode());
    }
    
    public void addToMR(Pointer methodRec) {
        if (logMachineCode()) {
            logPrintFull("[JIT: registering "+getSize()+" bytes with "+
                         methodRecToString(methodRec)+" ("+upTotal()+" total)]\n");
        }
        log(MachineCode.class,1,
            "Registering machine code "+getMachineCode().asLong()+" ("+getAddress().asLong()+
            " size="+getSize()+
            ") with "+methodRecToString(methodRec)+" ("+
            methodRec.asLong()+")");
        fivmr_MethodRec_registerMC(methodRec,getMachineCode());
    }

    public int getSize() {
        return size;
    }
    
    public Pointer getAddress() {
        return address;
    }
    
    public ByteBuffer getBuffer() {
        return java.nio.fivmSupport.wrap(address,size,size,0);
    }
    
    /**
     * The most important method in this class.  If you're using getMachineCode(),
     * getBuffer() or getAddress(), make sure that the lexical scope within which
     * you use the return values of those methods ends with a call to poke().
     */
    public void poke() {
        Magic.hardUse(this);
    }
}

