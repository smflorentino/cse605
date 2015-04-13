/*
 * Payload.java
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

import com.fiji.fivm.Settings;
import com.fiji.fivm.SysDepSetting;
import com.fiji.fivm.IntUtil;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class Payload {
    private Payload() {}
    
    public static final String[] baseExportSigs=new String[]{
        "fivmRuntime_boot",
        "fivmRuntime_initSystemClassLoaders",
        "fivmRuntime_notifyInitialized",
        "allocForNative",
        "allocArrayForNative",
        "throwInternalError",
        "throwNoClassDefFoundError_inNative",
        "throwNoClassDefFoundError_inJava",
        "throwLinkageError_inJava",
        "throwNullPointerRTE",
        "throwNullPointerRTE_inJava",
        "throwArithmeticRTE",
        "throwArithmeticRTE_inJava",
        "throwStackOverflowRTE",
        "throwStackOverflowRTE_inJava",
        "throwClassChangeRTE_inJava",
        "throwArrayBoundsRTE_inJava",
        "throwArrayStoreRTE_inJava",
        "throwNegativeSizeRTE_inJava",
        "throwAbstractMethodError_inJava",
        "throwClassCastRTE_inJava",
        "throwUnsatisfiedLinkErrorForLoad",
        "throwNoSuchFieldError",
        "throwNoSuchMethodError",
        "throwNoSuchMethodError_inJava",
        "throwExceptionInInitializerError_inJava",
        "throwReflectiveException_inJava",
        "throwIllegalMonitorStateException_inJava",
        "throwOutOfMemoryError_inJava",
        "throwIllegalAssignmentError",
        "throwIllegalAssignmentError_inJava",
        "describeExceptionImpl",
        "fromCStringInHeap",
        "fromCStringFullInHeap",
        "fromUTF16Sequence",
        "stringLength",
        "cstringLength",
        "getUTF16Sequence",
        "getCString",
        "getCStringFull",
        "getStringRegion",
        "getStringUTFRegion",
        "String_getArrayPointer",
        "String_getOffset",
        "getBooleanElements",
        "getByteElements",
        "getCharElements",
        "getShortElements",
        "getIntElements",
        "getLongElements",
        "getFloatElements",
        "getDoubleElements",
        "returnBooleanElements",
        "returnByteElements",
        "returnCharElements",
        "returnShortElements",
        "returnIntElements",
        "returnLongElements",
        "returnFloatElements",
        "returnDoubleElements",
        "getBooleanRegion",
        "getByteRegion",
        "getCharRegion",
        "getShortRegion",
        "getIntRegion",
        "getLongRegion",
        "getFloatRegion",
        "getDoubleRegion",
        "setBooleanRegion",
        "setByteRegion",
        "setCharRegion",
        "setShortRegion",
        "setIntRegion",
        "setLongRegion",
        "setFloatRegion",
        "setDoubleRegion",
        "returnBuffer",
        "DirectByteBuffer_wrap",
        "DirectByteBuffer_address",
        "DirectByteBuffer_capacity",
        "VMThread_create",
        "VMThread_createRT",
        "VMThread_setThreadState",
        "VMThread_getThreadState",
        "VMThread_starting",
        "VMThread_run",
        "VMThread_setUncaughtException",
        "VMThread_die",
        "VMThread_isDaemon",
        "VMThread_getPriority",
        "DumpStackCback_cback",
        "makeJNIFuncName",
        "runRunnable",
        "MemoryArea_doRun",
        "MemoryArea_getBSID",
        "fivmRuntime_loadClass",
        "handlePatchPointImpl",
        "handleLoadThunk",
        "allocateClass",
        "handleFieldResolution",
        "handleMethodResolution",
        "handleArrayAlloc",
        "handleObjectAlloc",
        "handleInstanceof",
        "BackingStoreID_create",
        "processArgs",
        "javaExit"
    };
    
    public static final LinkedHashSet< String > baseExportSigSet=
        new LinkedHashSet< String >(Arrays.asList(baseExportSigs));

    static class ExportedMethodRec {
        VisibleMethod method;
        RemoteFunction func;
        
        ExportedMethodRec(VisibleMethod method,
                          RemoteFunction func) {
            this.method=method;
            this.func=func;
        }
    }
    
    static LinkedHashMap< String, ExportedMethodRec > exportedMethods=
        new LinkedHashMap< String, ExportedMethodRec >();
    
    static HashSet< String > exportResolutionCanceled=
        new HashSet< String >();
    
    public static synchronized void exportMethod(VisibleMethod m,
                                                 RemoteFunction func) {
        if (exportedMethods.containsKey(m.getName())) {
            ExportedMethodRec emr=exportedMethods.get(m.getName());
            if (emr.method==m && emr.func.equals(func)) {
                // ok - just ignore (this'll happen if we've reparsed a
                // class)
            } else {
                throw new CompilerException(
                    "Method with name "+m.getName()+" has already been exported; "+
                    "cannot export it more than once.");
            }
        } else {
            exportedMethods.put(m.getName(),
                                new ExportedMethodRec(m,func));
        }
    }
    
    public static synchronized void exportMethodResolutionCanceled(VisibleMethod m) {
        exportResolutionCanceled.add(m.getName());
    }
    
    static LinkedList< String > subPayloads=new LinkedList< String >();
    
    public static void addSubPayload(String name) {
        subPayloads.add(name);
    }

    public static LinkableSet payload() {
        LinkableSet result=new LinkableSet();
        result.add(new Linkable(Basetype.VOID,CTypesystemReferences.generated_payload_name) {
                public boolean isLocal() { return true; }
                public void generateDeclaration(PrintWriter w) {
                    w.println("extern fivmr_Payload "+CTypesystemReferences.generated_payload_name+";");
                }
                public void generateDefinition(PrintWriter w) {
                    try {
                        w.println("static uint32_t usedTids["+(Type.numBuckets*256/32)+"] = {");
                        int[] bucketOcc=new int[Type.numBuckets*256/32];
                        for (Type t : Type.allUsedTypes()) {
                            if (t.hasConcreteSupertypes() && t.isObject()) {
                                IntUtil.setBit(bucketOcc,t.bucket*256+t.tid);
                            }
                        }
                        for (int i=0;i<bucketOcc.length;++i) {
                            w.print("   (uint32_t)INT32_C("+bucketOcc[i]+")");
                            if (i!=bucketOcc.length-1) {
                                w.println(",");
                            } else {
                                w.println();
                            }
                        }
                        w.println("};");
                        w.println("static int32_t itableOcc["+(Global.maxImethodIndex+1)+"] = {");
                        int[] itableOcc=new int[Global.maxImethodIndex+1];
                        Collection< Integer > indicesUsed;
                        if (Settings.ITABLE_COMPRESSION) {
                            indicesUsed=Global.imethodSigIndex.values();
                        } else {
                            indicesUsed=Global.imethodIndex.values();
                        }
                        for (int idx : indicesUsed) {
                            itableOcc[idx]++;
                        }
                        for (int i=0;i<itableOcc.length;++i) {
                            w.print("   INT32_C("+itableOcc[i]+")");
                            if (i!=itableOcc.length-1) {
                                w.println(",");
                            } else {
                                w.println();
                            }
                        }
                        w.println("};");
                        w.println("fivmr_Payload "+CTypesystemReferences.generated_payload_name+" = {");
                        w.println("   \""+Util.cStringEscape(Global.revision)+"\",");
                        w.println("   \""+Util.cStringEscape(Global.fivmcHomeDir)+"\",");
                        w.println("   {"); // settings
                        w.println("      {");
                        int cnt=0;
                        long accumulator=0;
                        boolean first=true;
                        for (Field f : Settings.class.getFields()) {
                            if (!f.isSynthetic() &&
                                !f.isAnnotationPresent(SysDepSetting.class)) {
                                if (f.getBoolean(null)) {
                                    accumulator|=(1<<(cnt%32));
                                }
                                cnt++;
                                if (cnt==32) {
                                    if (first) {
                                        first=false;
                                        w.print("         ");
                                    } else {
                                        w.print(",        ");
                                    }
                                    w.println("UINT32_C("+accumulator+")");
                                    accumulator=0;
                                    cnt=0;
                                }
                            }
                        }
                        if (cnt!=0) {
                            if (first) {
                                first=false;
                                w.print("         ");
                            } else {
                                w.print(",        ");
                            }
                            w.println("UINT32_C("+accumulator+")");
                        }
                        w.println("      }");
                        w.println("   },");
                        w.println("   &"+Global.name+"_config,");
                        if (Global.oneShotPayload) {
                            w.println("   FIVMR_PL_IMMORTAL_ONESHOT,");
                        } else {
                            w.println("   FIVMR_PL_IMMORTAL,");
                        }
                        w.println("   NULL,"); // ownedBy
                        w.println("   (int32_t)"+((StaticFieldRepo.primFieldsSize()+7)/8)+",");
                        w.println("   (int32_t)"+(StaticFieldRepo.refFieldsSize()/Global.pointerSize)+",");
                        if (Global.oneShotPayload) {
                            w.println("   "+Global.name+"_staticPrimFields,");
                            w.println("   "+Global.name+"_staticRefFields,");
                        } else {
                            w.println("   NULL,");
                            w.println("   NULL,");
                        }
                        w.println("   (int32_t)"+DebugIDRepository.size()+",");
                        w.println("   (int32_t)"+StringRepository.numStrings()+",");
                        w.println("   (uintptr_t)"+BytecodeRepository.curOffset()+",");
                        w.println("   (int32_t)"+Global.allResolvedTypesUsedAtRuntime().size()+",");
                        w.println("   (int32_t)"+Global.allUnresolvedTypesUsedAtRuntime().size()+",");
                        w.println("   (int32_t)"+Global.contextList().size()+",");
                        w.println("   (int32_t)"+VisibleClass.maxCanonicalNumber()+",");
                        w.println("   (fivmr_TypeData**)"+CTypesystemReferences.generated_typeList.asCCode()+",");
                        w.println("   (fivmr_TypeStub*)"+CTypesystemReferences.generated_stubList.asCCode()+",");
                        w.println("   "+Global.name+"_contexts,");
                        w.println("   (fivmr_DebugRec*)"+CTypesystemReferences.generated_debugTable.asCCode()+",");
                        w.println("   (uintptr_t*)"+CTypesystemReferences.generated_stringTable.asCCode()+",");
                        w.println("   (uintptr_t*)"+CTypesystemReferences.generated_stringDataArray.asCCode()+",");
                        if (Settings.CLASSLOADING) {
                            w.println("   (uintptr_t*)"+CTypesystemReferences.generated_bytecodeTable.asCCode()+",");
                        } else {
                            w.println("   NULL,");
                        }
                        w.println("   (fivmr_Object*)(void*)"+CTypesystemReferences.generated_stringIndex.asCCode()+",");
                        w.println("   (uintptr_t*)"+CTypesystemReferences.generated_classTable.asCCode()+",");
                        w.println("   (uintptr_t)(intptr_t)"+Global.om.locationToOffset(Global.root().classClass.getFieldByName("vmdata").location())+",");
                        w.println("   (uintptr_t)(intptr_t)"+Global.om.locationToOffset(Global.root().stringClass.getFieldByName("value").location())+",");
                        w.println("   \""+Util.cStringEscape(Global.name)+"\",");
                        w.println("   "+TypeData.forType(Global.entrypoint.getClazz().asType()).asCCode()+",");
                        w.println("   usedTids,");
                        w.println("   (uintptr_t)"+Type.numBuckets+",");
                        w.println("   itableOcc,");
                        w.println("   (uintptr_t)"+(Global.maxImethodIndex+1)+",");
                        if (PatchThunkRepo.size()>0) {
                            w.println("   "+CTypesystemReferences.generated_patchRepo_name+",");
                            w.println("   "+PatchThunkRepo.size()+",");
                        } else {
                            w.println("   NULL,");
                            w.println("   0,");
                        }
                        if (subPayloads.isEmpty()) {
                            w.println("   NULL,"); // subPayloads
                        } else {
                            w.println("   &"+Global.name+"_subPayloads,");
                        }
                        w.println("   "+Global.name+"_postThreadInitCback");
                        for (Type t : Type.fundamentalsInOrder()) {
                            if (t==null) {
                                w.println(",  NULL");
                            } else {
                                w.println(",  (fivmr_TypeData*)"+TypeData.forType(t).asCCode());
                            }
                        }
                        for (String name : baseExportSigs) {
                            ExportedMethodRec emr=exportedMethods.get(name);
                            if (emr==null) {
                                if (exportResolutionCanceled.contains(name)) {
                                    if (Global.verbosity>=1) {
                                        Global.log.println(
                                            "NOTE: will not export "+name+
                                            " because its resolution was canceled.");
                                    }
                                } else {
                                    throw new CompilerException(
                                        "Expecting to be able to export "+name+" but could not "+
                                        "find it.");
                                }
                            }
                            if (emr==null) {
                                w.println(",  NULL  /* resolution canceled for "+name+" */");
                            } else {
                                w.println(",  "+emr.func.getName());
                            }
                        }
                        w.println("};");
                    } catch (Throwable e) {
                        Util.rethrow(e);
                    }
                }
                public LinkableSet subLinkables() {
                    LinkableSet result=new LinkableSet();
                    if (PatchThunkRepo.size()>0) {
                        result.add(PatchThunkRepo.local());
                    }
                    result.add(CTypesystemReferences.generated_stringIndex);
                    if (Settings.CLASSLOADING) {
                        result.add(CTypesystemReferences.generated_bytecodeTable);
                    }
                    result.add(TypeData.forType(Global.entrypoint.getClazz().asType()));
                    result.add(new Linkable(Basetype.VOID,Global.name+"_contexts"){
                            public boolean isLocal() { return true; }
                            public void generateDeclaration(PrintWriter w) {
                                w.println("fivmr_StaticTypeContext "+Global.name+
                                          "_contexts["+Global.contextList().size()+"];");
                            }
                            public void generateDefinition(PrintWriter w) {
                                w.println("fivmr_StaticTypeContext "+Global.name+
                                          "_contexts["+Global.contextList().size()+"] = {");
                                int typeOffset=0,stubOffset=0;
                                for (int i=0;i<Global.contextList().size();++i) {
                                    Context c=Global.contextList().get(i);
                                    w.print("   { "+typeOffset+", "+c.allResolvedTypesUsedAtRuntime().size()+", "+stubOffset+", "+c.allUnresolvedTypesUsedAtRuntime().size()+", &"+Global.name+"_payload }");
                                    if (i==Global.contextList().size()-1) {
                                        w.println();
                                    } else {
                                        w.println(",");
                                    }
                                    typeOffset+=c.allResolvedTypesUsedAtRuntime().size();
                                    stubOffset+=c.allUnresolvedTypesUsedAtRuntime().size();
                                }
                                w.println("};");
                            }
                        });
                    result.add(new Linkable(Basetype.VOID,Global.name+"_config"){
                            public boolean isLocal() { return false; }
                            public void generateDeclaration(PrintWriter w) {
                                w.println(
                                    "extern fivmr_Configuration "+Global.name+"_config;");
                            }
                        });
                    if (Global.oneShotPayload) {
                        result.add(new Linkable(Basetype.VOID,Global.name+"_staticPrimFields") {
                                public boolean isLocal() { return true; }
                                public void generateDeclaration(PrintWriter w) {
                                    w.println("int64_t "+Global.name+"_staticPrimFields["+
                                              ((StaticFieldRepo.primFieldsSize()+7)/8)+"];");
                                }
                            });
                        result.add(new Linkable(Basetype.VOID,Global.name+"_staticRefFields") {
                                public boolean isLocal() { return true; }
                                public void generateDeclaration(PrintWriter w) {
                                    w.println("fivmr_Object "+Global.name+"_staticRefFields["+
                                              (StaticFieldRepo.refFieldsSize()/
                                               Global.pointerSize)+"];");
                                }
                            });
                    }
                    result.add(new Linkable(Basetype.VOID,Global.name+"_postThreadInitCback"){
                            public boolean isLocal() { return false; }
                            public void generateDeclaration(PrintWriter w) {
                                w.println(
                                    "bool "+Global.name+"_postThreadInitCback("+
                                    "fivmr_ThreadState *ts);");
                            }
                        });
                    if (!subPayloads.isEmpty()) {
                        for (String pn_ : subPayloads) {
                            final String pn=pn_;
                            result.add(new Linkable(Basetype.VOID,pn+"_payload"){
                                    public boolean isLocal() { return false; }
                                    public void generateDeclaration(PrintWriter w) {
                                        w.println("extern fivmr_Payload "+pn+"_payload;");
                                    }
                                });
                        }
                        result.add(new Linkable(Basetype.VOID,Global.name+"_subPayloads"){
                                public boolean isLocal() { return true; }
                                public void generateDeclaration(PrintWriter w) {
                                    w.println("extern fivmr_PayloadList "+Global.name+"_subPayloads;");
                                }
                                public void generateDefinition(PrintWriter w) {
                                    w.println("fivmr_Payload *"+Global.name+"_subPayloadsArray["+
                                              subPayloads.size()+"] = {");
                                    for (Iterator< String > i=subPayloads.iterator();
                                         i.hasNext();) {
                                        String pn=i.next();
                                        w.print("   &"+pn+"_payload");
                                        if (i.hasNext()) {
                                            w.println(",");
                                        } else {
                                            w.println("");
                                        }
                                    }
                                    w.println("};");
                                    w.println("fivmr_PayloadList "+Global.name+"_subPayloads = {");
                                    w.println("   "+subPayloads.size()+",");
                                    w.println("   "+Global.name+"_subPayloadsArray");
                                    w.println("};");
                                }
                            });
                    }
                    result.add(CTypesystemReferences.generated_typeList);
                    result.add(CTypesystemReferences.generated_stubList);
                    result.add(CTypesystemReferences.generated_debugTable);
                    result.add(CTypesystemReferences.generated_stringTable);
                    result.add(CTypesystemReferences.generated_stringDataArray);
                    result.add(CTypesystemReferences.generated_classTable);
                    for (Type t : Type.fundamentals()) {
                        result.add(TypeData.forType(t));
                    }
                    for (ExportedMethodRec emr : exportedMethods.values()) {
                        result.add(emr.func);
                    }
                    return result;
                }
            });
        return result;
    }
}

