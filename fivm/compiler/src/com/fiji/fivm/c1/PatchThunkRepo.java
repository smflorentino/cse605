/*
 * PatchThunkRepo.java
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

import java.io.*;
import java.util.*;

public final class PatchThunkRepo {
    public static final Linkable local() {
	return new Linkable(Basetype.VOID,CTypesystemReferences.generated_patchRepo_name) {
            public boolean isLocal() {
                return true;
            }
            public void generateDeclaration(PrintWriter w) {
                w.println("void *"+name+"["+size()+"];");
            }
            public void generateDefinition(PrintWriter w) {
                for (int i=0;i<size();++i) {
                    PatchThunk pt=patchThunkArr.get(i);
                    boolean hasGCState=false;
                    for (int j=0;j<pt.stateSize();++j) {
                        if (pt.heapPointers().get(j)) {
                            hasGCState=true;
                            break;
                        }
                    }
                    if (hasGCState) {
                        w.println("static uint32_t patchThunkGCMap"+i+"["+((pt.stateSize()+31)/32)+
                                  "] = {");
                        for (int j=0;j<pt.stateSize();j+=32) {
                            int bits=0;
                            for (int k=0;k<32;++k) {
                                if (pt.heapPointers().get(j*32+k)) {
                                    bits|=(1<<k);
                                }
                            }
                            w.print("   (uint32_t)INT32_C("+bits+")");
                            if (j+32<pt.stateSize()) {
                                w.println(",");
                            } else {
                                w.println();
                            }
                        }
                        w.println("};");
                    }
                    w.println("static "+
                              pt.parent().getType().asExectype().effectiveBasetype().cType.asCCode()+
                              " patchThunk"+i+"(fivmr_ThreadState *ts) {");
                    w.println("   void **mePtr;");
                    if (Global.oneShotPayload) {
                        w.println("   mePtr = "+name+"+"+i+";");
                    } else {
                        w.println("   mePtr = ts->patchRepo+"+i+";");
                    }
                    if (hasGCState) {
                        w.println("   ts->stateBufGCMap=patchThunkGCMap"+i+";");
                    }
                    w.println("   ts->stateSize="+pt.stateSize()+";");
                    w.println("   fivmr_handlePatchPoint(ts,\""+
                              Util.cStringEscape(pt.neededClass().jniName())+"\",\""+
                              Util.cStringEscape(pt.description())+"\","+pt.bcOffset()+",mePtr,patchThunk"+i+");");
                    if (hasGCState) {
                        w.println("   ts->stateBufGCMap=NULL;");
                        w.println("   ts->stateSize=0;");
                    }
                    // from this point forward we don't need the GC map since whatever state
                    // is live will be adequately captured by the safepoint mechanisms of the
                    // function we're about to call.
                    if (pt.parent().getType()==Type.VOID) {
                        w.println("   if (ts->curException) return;");
                    } else {
                        w.println("   if (ts->curException) return 0;");
                    }
                    w.println("   return (("+
                              pt.parent().getType().asExectype().effectiveBasetype().cType.asCCode()+
                              "(*)(fivmr_ThreadState*))*mePtr)(ts);");
                    w.println("}");
                }
                w.println("void *"+name+"["+size()+"] = {");
                for (int i=0;i<size();++i) {
                    if (i!=size()-1) {
                        w.println("   patchThunk"+i+",");
                    } else {
                        w.println("   patchThunk"+i);
                    }
                }
                w.println("};");
            }
        };
    }

    static HashMap< PatchThunk, Integer > patchThunks=
        new HashMap< PatchThunk, Integer >();
    static ArrayList< PatchThunk > patchThunkArr=
        new ArrayList< PatchThunk >();
    
    public static int size() {
        assert patchThunks.size()==patchThunkArr.size();
        return patchThunks.size();
    }
    
    public static synchronized int register(PatchThunk pt) {
        Integer idx=patchThunks.get(pt);
        if (idx==null) {
            patchThunks.put(pt,idx=size());
            patchThunkArr.add(pt);
        }
        return idx;
    }
    
    public static Arg generateIRFor(Code code,Operation before,PatchThunk pt) {
        int index=register(pt);
        Var tableBase=code.addVar(Exectype.POINTER);
        if (Global.oneShotPayload) {
            before.prepend(
                new CFieldInst(
                    before.di(),OpCode.GetCVarAddress,
                    tableBase,Arg.EMPTY,
                    CTypesystemReferences.generated_patchRepo));
        } else {
            before.prepend(
                new CFieldInst(
                    before.di(),OpCode.GetCField,
                    tableBase,new Arg[]{Arg.THREAD_STATE},
                    CTypesystemReferences.ThreadState_patchRepo));
        }
        Var result=code.addVar(Exectype.POINTER);
        before.prepend(
            new SimpleInst(
                before.di(),OpCode.Add,
                result,new Arg[]{
                    tableBase,
                    PointerConst.make(
                        Global.pointerSize*index)
                }));
        return result;
    }
}


