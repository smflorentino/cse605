/*
 * CTypesystemReferences.java
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

import java.io.PrintWriter;
import java.util.*;
import com.fiji.config.*;

/**
 * Gathers all references to Fiji VM C runtime (runtimec) types and structures
 * that originate in the compiler.  Can also be used to gather references that
 * originate in the Fji VM Java runtime (runtimej), but that requires doing
 * a static analysis of the code.  This dichotomy is intentional.  The compiler
 * reports all of its references to C code directly (through static fields
 * in this class), while the runtime is permitted to access C code ad-hoc and
 * those references must be extracted through a separate pass.
 */
public class CTypesystemReferences {
    private static boolean locked=false;
    
    static abstract class MemoTable< T > {
        HashMap< T, T > table=new HashMap< T, T >();
        
        synchronized T add(T value) {
            T result=table.get(value);
            if (result==null) {
                if (locked) {
                    throw new CompilerException(
                        "Attempt to add "+value+" after the C type system has been locked.");
                }
                table.put(value,value);
                return value;
            } else if (value instanceof CField &&
                       result instanceof CField &&
                       ((CField)value).moreSpecificThan((CField)result)) {
                if (locked) {
                    throw new CompilerException(
                        "Attempted to narrow "+result+" to "+value+" after the C type system has been locked.");
                }
                result=table.remove(result);
                assert result!=null;
                table.put(value,value);
                return value;
            } else {
                if (value instanceof CField &&
                    result instanceof CField) {
                    assert ((CField)value).sameTypesAs((CField)result);
                }
                if (value instanceof Generatedable &&
                    result instanceof Generatedable) {
                    assert ((Generatedable)value).generated()==((Generatedable)result).generated();
                }
                return result;
            }
        }
        
        Set< T > set() {
            return table.keySet();
        }
        
        abstract ConfigMapNode asConfigNode(T value);
        
        abstract void fromConfigNode(ConfigMapNode node);
        
        ConfigNode asConfigNode() {
            ConfigListNode result=new ConfigListNode();
            
            for (T value : table.keySet()) {
                result.append(asConfigNode(value));
            }
            
            return result;
        }
        
        void fromConfigNode(ConfigNode node) {
            for (ConfigNode sn : node.asList()) {
                fromConfigNode(sn.asMap());
            }
        }
    }
    
    private static MemoTable< CType > ctypes=new MemoTable< CType >() {
        ConfigMapNode asConfigNode(CType ctype) {
            return new ConfigMapNode("name",ctype.name(),
                                     "sizeof",ctype.sizeofImpl());
        }
        void fromConfigNode(ConfigMapNode node) {
            if (node.getString("name").equals("void")) {
                // oh boy, this one is super special!  the C typesystem
                // really reports this as having sizeof=1, but we want
                // to keep it at sizeof=0.
            } else {
                CType ctype=CType.forName(node.getString("name"));
                ctype.sizeof=node.getInt("sizeof");
            }
        }
    };
    private static MemoTable< CStructField > cfields=new MemoTable< CStructField >() {
        ConfigMapNode asConfigNode(CStructField field) {
            return new ConfigMapNode("basetype",""+field.getType().name(),
                                     "name",field.getName(),
                                     "struct",field.getStructName(),
                                     "offsetof",field.offsetofImpl(),
                                     "from",field.from);
        }
        void fromConfigNode(ConfigMapNode node) {
            CStructField field=CStructField.make(
                Basetype.valueOf(node.getString("basetype")),
                node.getString("name"),
                node.getString("struct"),
                CTypesystemReferences.class);
            field.offsetof=node.getInt("offsetof");
        }
    };
    private static MemoTable< KnownRemoteCGlobal > krcg=new MemoTable< KnownRemoteCGlobal >() {
        ConfigMapNode asConfigNode(KnownRemoteCGlobal global) {
            return new ConfigMapNode("name",global.getName(),
                                     "basetype",global.getType().name(),
                                     "generated",global.generated());
        }
        void fromConfigNode(ConfigMapNode node) {
            KnownRemoteCGlobal.make(Basetype.valueOf(node.getString("basetype")),
                                    node.getString("name"),
                                    node.getBoolean("generated"));
        }
    };
    private static MemoTable< KnownRemoteDataConstant > krdc=
        new MemoTable< KnownRemoteDataConstant >() {
        ConfigMapNode asConfigNode(KnownRemoteDataConstant dc) {
            return new ConfigMapNode("name",dc.getName(),
                                     "generated",dc.generated());
        }
        void fromConfigNode(ConfigMapNode node) {
            KnownRemoteDataConstant.forName(node.getString("name"),
                                            node.getBoolean("generated"));
        }
    };
    
    // NOTE: we could create a detector that figures out, for each of these,
    // if it's a variable, a #define, or something special.
    private static MemoTable< GodGivenCVar > ggcv=new MemoTable< GodGivenCVar >() {
        ConfigMapNode asConfigNode(GodGivenCVar cvar) {
            return new ConfigMapNode("basetype",cvar.getType().name(),
                                     "name",cvar.getName(),
                                     "usedForGetAddress",cvar.usedForGetAddress(),
                                     "usedForRead",cvar.usedForRead(),
                                     "usedForWrite",cvar.usedForWrite());
        }
        void fromConfigNode(ConfigMapNode node) {
            GodGivenCVar cvar=GodGivenCVar.make(Basetype.valueOf(node.getString("basetype")),
                                                node.getString("name"));
            if (node.getBoolean("usedForGetAddress")) {
                cvar.notifyGetAddress();
            }
            if (node.getBoolean("usedForRead")) {
                cvar.notifyRead();
            }
            if (node.getBoolean("usedForWrite")) {
                cvar.notifyWrite();
            }
        }
    };
    
    private static MemoTable< GodGivenFunction > ggf=new MemoTable< GodGivenFunction >() {
        ConfigMapNode asConfigNode(GodGivenFunction func) {
            return new ConfigMapNode("name",func.getName(),
                                     "signature",func.getSignature().asConfigNode(),
                                     "usedForCall",func.usedForCall());
        }
        void fromConfigNode(ConfigMapNode node) {
            GodGivenFunction func=GodGivenFunction.make(
                node.getString("name"),
                NativeSignature.fromConfigNode(node.getMap("signature")));
            if (node.getBoolean("usedForCall")) {
                func.notifyCall();
            }
        }
    };
    
    static CType addType(CType type) {
        return ctypes.add(type);
    }
    
    static CStructField addField(CStructField field) {
        return cfields.add(field);
    }
    
    static KnownRemoteCGlobal addKRCG(KnownRemoteCGlobal value) {
        return krcg.add(value);
    }
    
    static KnownRemoteDataConstant addKRDC(KnownRemoteDataConstant value) {
        return krdc.add(value);
    }
    
    static GodGivenCVar addGGCV(GodGivenCVar value) {
        return ggcv.add(value);
    }
    
    static GodGivenFunction addGGF(GodGivenFunction value) {
        return ggf.add(value);
    }
    
    public static Set< CType > ctypes() {
	return ctypes.set();
    }
    
    public static Set< CStructField > cfields() {
	return cfields.set();
    }
    
    public static Set< KnownRemoteCGlobal > krcg() {
        return krcg.set();
    }
    
    public static Set< KnownRemoteDataConstant > krdc() {
        return krdc.set();
    }
    
    public static Set< GodGivenCVar > ggcv() {
        return ggcv.set();
    }
    
    public static Set< GodGivenFunction > ggf() {
        return ggf.set();
    }
    
    public static void lock() {
	locked=true;
    }
    
    // the basetypes
    public static CType void_t    = CType.forName("void");
    public static CType int8_t    = CType.forName("int8_t");
    public static CType uint8_t   = CType.forName("uint8_t");
    public static CType int16_t   = CType.forName("int16_t");
    public static CType uint16_t  = CType.forName("uint16_t");
    public static CType int32_t   = CType.forName("int32_t");
    public static CType uint32_t  = CType.forName("uint32_t");
    public static CType int64_t   = CType.forName("int64_t");
    public static CType uint64_t  = CType.forName("uint64_t");
    public static CType float_t   = CType.forName("float");
    public static CType double_t  = CType.forName("double");
    public static CType intptr_t  = CType.forName("intptr_t");
    public static CType uintptr_t = CType.forName("uintptr_t");

    public static CType voidstar       = CType.forName("void *");
    public static CType constvoidstar  = CType.forName("const void *");

    static void linkBasetypes() {
        if (Global.verbosity>=1) {
            Global.log.println("Initailizing base-to-C type system link.");
        }
        
        // ugh .... this is ugly.
        
        void_t.basetype=Basetype.VOID;
        void_t.sizeof=0;
        Basetype.VOID.cType=void_t;
        Basetype.VOID.cTypeForCall=void_t;
        Basetype.VOID.unsignedCType=void_t;
        Basetype.VOID.signedCType=void_t;

        Basetype.BOOLEAN.cType=int8_t;
        Basetype.BOOLEAN.cTypeForCall=int8_t;
        Basetype.BOOLEAN.unsignedCType=uint8_t;
        Basetype.BOOLEAN.signedCType=int8_t;

        int8_t.basetype=Basetype.BYTE;
        Basetype.BYTE.cType=int8_t;
        Basetype.BYTE.cTypeForCall=int8_t;
        Basetype.BYTE.unsignedCType=uint8_t;
        Basetype.BYTE.signedCType=int8_t;
        
        uint8_t.basetype=Basetype.BYTE;
        
        int16_t.basetype=Basetype.SHORT;
        Basetype.SHORT.cType=int16_t;
        Basetype.SHORT.cTypeForCall=int16_t;
        Basetype.SHORT.unsignedCType=uint16_t;
        Basetype.SHORT.signedCType=int16_t;
        
        uint16_t.basetype=Basetype.CHAR;
        Basetype.CHAR.cType=uint16_t;
        Basetype.CHAR.cTypeForCall=uint16_t;
        Basetype.CHAR.unsignedCType=uint16_t;
        Basetype.CHAR.signedCType=int16_t;
        
        int32_t.basetype=Basetype.INT;
        Basetype.INT.cType=int32_t;
        Basetype.INT.cTypeForCall=int32_t;
        Basetype.INT.unsignedCType=uint32_t;
        Basetype.INT.signedCType=int32_t;
        
        uint32_t.basetype=Basetype.INT;
        
        int64_t.basetype=Basetype.LONG;
        Basetype.LONG.cType=int64_t;
        Basetype.LONG.cTypeForCall=int64_t;
        Basetype.LONG.unsignedCType=uint64_t;
        Basetype.LONG.signedCType=int64_t;
        
        uint64_t.basetype=Basetype.LONG;
        
        float_t.basetype=Basetype.FLOAT;
        Basetype.FLOAT.cType=float_t;
        Basetype.FLOAT.cTypeForCall=float_t;
        Basetype.FLOAT.signedCType=float_t;
        
        double_t.basetype=Basetype.DOUBLE;
        Basetype.DOUBLE.cType=double_t;
        Basetype.DOUBLE.cTypeForCall=double_t;
        Basetype.DOUBLE.signedCType=double_t;
        
        intptr_t.basetype=Basetype.POINTER;
        
        uintptr_t.basetype=Basetype.POINTER;
        Basetype.POINTER.cType=uintptr_t;
        Basetype.POINTER.cTypeForCall=constvoidstar;
        Basetype.POINTER.unsignedCType=uintptr_t;
        Basetype.POINTER.signedCType=intptr_t;
        
        Basetype.OBJECT.cType=uintptr_t;
        Basetype.OBJECT.cTypeForCall=constvoidstar;
        Basetype.OBJECT.unsignedCType=uintptr_t;
        Basetype.OBJECT.signedCType=intptr_t;
        
        Basetype.NULL.cType=uintptr_t;
        Basetype.NULL.cTypeForCall=constvoidstar;
        Basetype.NULL.unsignedCType=uintptr_t;
        Basetype.NULL.signedCType=intptr_t;
        
        Basetype.VM_FCPTR.cType=uintptr_t;
        Basetype.VM_FCPTR.cTypeForCall=constvoidstar;
        Basetype.VM_FCPTR.unsignedCType=uintptr_t;
        Basetype.VM_FCPTR.signedCType=intptr_t;
        
        voidstar.basetype=Basetype.POINTER;
        constvoidstar.basetype=Basetype.POINTER;
    }

    public static CType Frame_TYPE=CType.forName("fivmr_Frame");
    
    public static CStructField Frame_up=
	CStructField.make(Basetype.POINTER, "up", "fivmr_Frame");
    public static CStructField Frame_id=
	CStructField.make(Basetype.POINTER, "id", "fivmr_Frame");
    public static CStructField Frame_refs=
	CStructField.make(Basetype.VOID, "refs", "fivmr_Frame");

    public static final CStructField GCData_alloc=
	CStructField.make(Basetype.VOID, "alloc", "fivmr_GCData");

    public static CType TypeEpoch_TYPE=CType.forName("fivmr_TypeEpoch");
    
    public static CStructField TypeEpoch_buckets=
	CStructField.make(Basetype.POINTER, "buckets", "fivmr_TypeEpoch");
    public static CStructField TypeEpoch_tid=
	CStructField.make(Basetype.BYTE, "tid", "fivmr_TypeEpoch");
    public static CStructField TypeEpoch_bucket=
	CStructField.make(Basetype.CHAR, "bucket", "fivmr_TypeEpoch");
    public static CStructField TypeEpoch_itable=
	CStructField.make(Basetype.POINTER, "itable", "fivmr_TypeEpoch");

    public static final CType GCSpaceAlloc_TYPE=CType.forName("fivmr_GCSpaceAlloc");

    public static final CStructField GCSpaceAlloc_bump=
	CStructField.make(Basetype.POINTER, "bump", "fivmr_GCSpaceAlloc");

    public static final CType TypeStub_TYPE=CType.forName("fivmr_TypeStub");
    
    public static CStructField TypeStub_state=
        CStructField.make(Basetype.POINTER, "state", "fivmr_TypeStub");
    public static CStructField TypeStub_forward=
        CStructField.make(Basetype.POINTER, "forward", "fivmr_TypeStub");
    public static CStructField TypeStub_name=
        CStructField.make(Basetype.POINTER, "name", "fivmr_TypeStub");
    public static CStructField TypeStub_flags=
        CStructField.make(Basetype.INT, "flags", "fivmr_TypeStub");

    public static CStructField Handle_obj=
	CStructField.make(Basetype.POINTER, "obj", "fivmr_Handle");
    public static GodGivenFunction Handle_get=
	GodGivenFunction.make("fivmr_Handle_get",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER
                              }, SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    
    public static CType ScopeID_TYPE=CType.forName("fivmr_ScopeID");
    
    public static CStructField ScopeID_word=
	CStructField.make(Basetype.POINTER, "word", "fivmr_ScopeID");

    public static final CStructField MethodRec_entrypoint=
	CStructField.make(Basetype.POINTER, "entrypoint", "fivmr_MethodRec");
    public static final CStructField MethodRec_codePtr=
	CStructField.make(Basetype.POINTER, "codePtr", "fivmr_MethodRec");
    public static final CStructField MethodRec_flags=
	CStructField.make(Basetype.INT, "flags", "fivmr_MethodRec");
    public static final CStructField MethodRec_location=
	CStructField.make(Basetype.POINTER, "location", "fivmr_MethodRec");
    
    public static final GodGivenFunction MethodRec_logEntry=
        GodGivenFunction.make("fivmr_MethodRec_logEntry",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static final GodGivenFunction MethodRec_logExit=
        GodGivenFunction.make("fivmr_MethodRec_logExit",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static final GodGivenFunction MethodRec_logResultInt=
        GodGivenFunction.make("fivmr_MethodRec_logResultInt",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static final GodGivenFunction MethodRec_logResultLong=
        GodGivenFunction.make("fivmr_MethodRec_logResultLong",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.LONG
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static final GodGivenFunction MethodRec_logResultFloat=
        GodGivenFunction.make("fivmr_MethodRec_logResultFloat",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.FLOAT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static final GodGivenFunction MethodRec_logResultDouble=
        GodGivenFunction.make("fivmr_MethodRec_logResultDouble",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.DOUBLE
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static final GodGivenFunction MethodRec_logResultPtr=
        GodGivenFunction.make("fivmr_MethodRec_logResultPtr",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static CType Monitor_TYPE=CType.forName("fivmr_Monitor");
    
    public static CStructField Monitor_forward=
	CStructField.make(Basetype.POINTER, "forward", "fivmr_Monitor");
    public static CStructField Monitor_holder=
	CStructField.make(Basetype.POINTER, "state", "fivmr_Monitor");
    
    public static GodGivenFunction Monitor_lock=
        GodGivenFunction.make("fivmr_Monitor_lock",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              });
    public static GodGivenFunction Monitor_unlock=
        GodGivenFunction.make("fivmr_Monitor_unlock",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static CType NativeFrame_TYPE=CType.forName("fivmr_NativeFrame");

    public static CStructField NativeFrame_jni=
        CStructField.make(Basetype.VOID, "jni", "fivmr_NativeFrame");
    
    public static GodGivenFunction NativeFrame_addHandle=
        GodGivenFunction.make("fivmr_NativeFrame_addHandle",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER, // native frame
                                  Basetype.POINTER, // thread state
                                  Basetype.POINTER  // object
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static final CStructField Payload_stringTable=
	CStructField.make(Basetype.POINTER, "stringTable", "fivmr_Payload");
    public static final CStructField Payload_classTable=
	CStructField.make(Basetype.POINTER, "classTable", "fivmr_Payload");

    public static CStructField ThreadState_pollingUnion=
        CStructField.make(Basetype.VOID, "pollingUnion", "fivmr_ThreadState");
    public static CStructField ThreadState_curException=
        CStructField.make(Basetype.POINTER, "curException", "fivmr_ThreadState");
    public static CStructField ThreadState_curExceptionHandle=
        CStructField.make(Basetype.POINTER, "curExceptionHandle", "fivmr_ThreadState");
    public static CStructField ThreadState_curNF=
        CStructField.make(Basetype.POINTER, "curNF", "fivmr_ThreadState");
    public static CStructField ThreadState_curF=
        CStructField.make(Basetype.POINTER, "curF", "fivmr_ThreadState");
    public static CStructField ThreadState_stackHigh=
        CStructField.make(Basetype.POINTER, "stackHigh", "fivmr_ThreadState");
    public static CStructField ThreadState_allocFrame=
        CStructField.make(Basetype.POINTER, "allocFrame", "fivmr_ThreadState");
    public static CStructField ThreadState_gc=
        CStructField.make(Basetype.VOID, "gc", "fivmr_ThreadState");
    public static CStructField ThreadState_vm=
        CStructField.make(Basetype.POINTER, "vm", "fivmr_ThreadState");
    public static CStructField ThreadState_primFields=
        CStructField.make(Basetype.POINTER, "primFields", "fivmr_ThreadState");
    public static CStructField ThreadState_refFields=
        CStructField.make(Basetype.POINTER, "refFields", "fivmr_ThreadState");
    public static CStructField ThreadState_patchRepo=
        CStructField.make(Basetype.POINTER, "patchRepo", "fivmr_ThreadState");
    public static CStructField ThreadState_stateBuf=
        CStructField.make(Basetype.VOID, "stateBuf", "fivmr_ThreadState");
    public static CStructField ThreadState_stateBufOverflow=
        CStructField.make(Basetype.POINTER, "stateBufOverflow", "fivmr_ThreadState");
    public static CStructField ThreadState_stateBufGCMap=
        CStructField.make(Basetype.POINTER, "stateBufGCMap", "fivmr_ThreadState");
    public static CStructField ThreadState_typeList=
        CStructField.make(Basetype.POINTER, "typeList", "fivmr_ThreadState");
    public static CStructField ThreadState_stubList=
        CStructField.make(Basetype.POINTER, "stubList", "fivmr_ThreadState");
    public static CStructField ThreadState_stackLimit=
        CStructField.make(Basetype.POINTER, "stackLimit", "fivmr_ThreadState");
    public static CStructField ThreadState_typeEpoch=
        CStructField.make(Basetype.INT, "typeEpoch", "fivmr_ThreadState");

    // FIXME: is this used?
    public static GodGivenFunction ThreadState_pollcheck=
        GodGivenFunction.make("fivmr_ThreadState_pollcheck",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              });
    
    public static GodGivenFunction ThreadState_pollcheckSlow=
        GodGivenFunction.make("fivmr_ThreadState_pollcheckSlow",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              });
    public static GodGivenFunction ThreadState_goToNative=
        GodGivenFunction.make("fivmr_ThreadState_goToNative",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER
                              });
    public static GodGivenFunction ThreadState_goToJava=
        GodGivenFunction.make("fivmr_ThreadState_goToJava",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER
                              });
    public static GodGivenFunction ThreadState_get=
        GodGivenFunction.make("fivmr_ThreadState_get",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER // the VM pointer
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction ThreadState_pushAndInitNF=
        GodGivenFunction.make("fivmr_ThreadState_pushAndInitNF",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER, // thread state
                                  Basetype.POINTER, // native frame
                                  Basetype.POINTER  // method rec
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction ThreadState_popNF=
        GodGivenFunction.make("fivmr_ThreadState_popNF",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction ThreadState_addHandle=
        GodGivenFunction.make("fivmr_ThreadState_addHandle",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction ThreadState_checkHeight=
        GodGivenFunction.make("fivmr_ThreadState_checkHeight",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    
    public static GodGivenFunction arrayLength_barrier=
        GodGivenFunction.make("fivmr_arrayLength",
                              Basetype.INT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction addressOfField_barrier=
        GodGivenFunction.make("fivmr_addressOfField",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction addressOfElement_barrier=
        GodGivenFunction.make("fivmr_addressOfElement",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.POINTER
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction byteArrayLoad_barrier=
        GodGivenFunction.make("fivmr_byteArrayLoad",
                              Basetype.BYTE,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction charArrayLoad_barrier=
        GodGivenFunction.make("fivmr_charArrayLoad",
                              Basetype.CHAR,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction shortArrayLoad_barrier=
        GodGivenFunction.make("fivmr_shortArrayLoad",
                              Basetype.SHORT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction intArrayLoad_barrier=
        GodGivenFunction.make("fivmr_intArrayLoad",
                              Basetype.INT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction longArrayLoad_barrier=
        GodGivenFunction.make("fivmr_longArrayLoad",
                              Basetype.LONG,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerArrayLoad_barrier=
        GodGivenFunction.make("fivmr_pointerArrayLoad",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction floatArrayLoad_barrier=
        GodGivenFunction.make("fivmr_floatArrayLoad",
                              Basetype.FLOAT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction doubleArrayLoad_barrier=
        GodGivenFunction.make("fivmr_doubleArrayLoad",
                              Basetype.DOUBLE,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectArrayLoad_barrier=
        GodGivenFunction.make("fivmr_objectArrayLoad",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction byteArrayStore_barrier=
        GodGivenFunction.make("fivmr_byteArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.BYTE,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction charArrayStore_barrier=
        GodGivenFunction.make("fivmr_charArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.CHAR,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction shortArrayStore_barrier=
        GodGivenFunction.make("fivmr_shortArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.SHORT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction intArrayStore_barrier=
        GodGivenFunction.make("fivmr_intArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction longArrayStore_barrier=
        GodGivenFunction.make("fivmr_longArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.LONG,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerArrayStore_barrier=
        GodGivenFunction.make("fivmr_pointerArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction floatArrayStore_barrier=
        GodGivenFunction.make("fivmr_floatArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.FLOAT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction doubleArrayStore_barrier=
        GodGivenFunction.make("fivmr_doubleArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.DOUBLE,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectArrayStore_barrier=
        GodGivenFunction.make("fivmr_objectArrayStore",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction byteGetField_barrier=
        GodGivenFunction.make("fivmr_byteGetField",
                              Basetype.BYTE,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction charGetField_barrier=
        GodGivenFunction.make("fivmr_charGetField",
                              Basetype.CHAR,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction shortGetField_barrier=
        GodGivenFunction.make("fivmr_shortGetField",
                              Basetype.SHORT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction intGetField_barrier=
        GodGivenFunction.make("fivmr_intGetField",
                              Basetype.INT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction longGetField_barrier=
        GodGivenFunction.make("fivmr_longGetField",
                              Basetype.LONG,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerGetField_barrier=
        GodGivenFunction.make("fivmr_pointerGetField",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction floatGetField_barrier=
        GodGivenFunction.make("fivmr_floatGetField",
                              Basetype.FLOAT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction doubleGetField_barrier=
        GodGivenFunction.make("fivmr_doubleGetField",
                              Basetype.DOUBLE,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectGetField_barrier=
        GodGivenFunction.make("fivmr_objectGetField",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction bytePutField_barrier=
        GodGivenFunction.make("fivmr_bytePutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.BYTE,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction charPutField_barrier=
        GodGivenFunction.make("fivmr_charPutField",
                              Basetype.VOID, 
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.CHAR,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction shortPutField_barrier=
        GodGivenFunction.make("fivmr_shortPutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.SHORT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction intPutField_barrier=
        GodGivenFunction.make("fivmr_intPutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction longPutField_barrier=
        GodGivenFunction.make("fivmr_longPutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.LONG,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerPutField_barrier=
        GodGivenFunction.make("fivmr_pointerPutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction floatPutField_barrier=
        GodGivenFunction.make("fivmr_floatPutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.FLOAT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction doublePutField_barrier=
        GodGivenFunction.make("fivmr_doublePutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.DOUBLE,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectPutField_barrier=
        GodGivenFunction.make("fivmr_objectPutField",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction intArrayWeakCAS_barrier=
        GodGivenFunction.make("fivmr_intArrayWeakCAS",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerArrayWeakCAS_barrier=
        GodGivenFunction.make("fivmr_pointerArrayWeakCAS",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectArrayWeakCAS_barrier=
        GodGivenFunction.make("fivmr_objectArrayWeakCAS",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction intWeakCASField_barrier=
        GodGivenFunction.make("fivmr_intWeakCASField",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerWeakCASField_barrier=
        GodGivenFunction.make("fivmr_pointerWeakCASField",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectWeakCASField_barrier=
        GodGivenFunction.make("fivmr_objectWeakCASField",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction objectGetStatic_barrier=
        GodGivenFunction.make("fivmr_objectGetStatic",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction objectPutStatic_barrier=
        GodGivenFunction.make("fivmr_objectPutStatic",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction objectWeakCASStatic_barrier=
        GodGivenFunction.make("fivmr_objectWeakCASStatic",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction byteGetStatic2_barrier=
        GodGivenFunction.make("fivmr_byteGetStatic2",
                              Basetype.BYTE,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction charGetStatic2_barrier=
        GodGivenFunction.make("fivmr_charGetStatic2",
                              Basetype.CHAR,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction shortGetStatic2_barrier=
        GodGivenFunction.make("fivmr_shortGetStatic2",
                              Basetype.SHORT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction intGetStatic2_barrier=
        GodGivenFunction.make("fivmr_intGetStatic2",
                              Basetype.INT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction longGetStatic2_barrier=
        GodGivenFunction.make("fivmr_longGetStatic2",
                              Basetype.LONG,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerGetStatic2_barrier=
        GodGivenFunction.make("fivmr_pointerGetStatic2",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction floatGetStatic2_barrier=
        GodGivenFunction.make("fivmr_floatGetStatic2",
                              Basetype.FLOAT,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction doubleGetStatic2_barrier=
        GodGivenFunction.make("fivmr_doubleGetStatic2",
                              Basetype.DOUBLE,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectGetStatic2_barrier=
        GodGivenFunction.make("fivmr_objectGetStatic2",
                              Basetype.POINTER,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction bytePutStatic2_barrier=
        GodGivenFunction.make("fivmr_bytePutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.BYTE,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction charPutStatic2_barrier=
        GodGivenFunction.make("fivmr_charPutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.CHAR,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction shortPutStatic2_barrier=
        GodGivenFunction.make("fivmr_shortPutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.SHORT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction intPutStatic2_barrier=
        GodGivenFunction.make("fivmr_intPutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction longPutStatic2_barrier=
        GodGivenFunction.make("fivmr_longPutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.LONG,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction pointerPutStatic2_barrier=
        GodGivenFunction.make("fivmr_pointerPutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction floatPutStatic2_barrier=
        GodGivenFunction.make("fivmr_floatPutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.FLOAT,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction doublePutStatic2_barrier=
        GodGivenFunction.make("fivmr_doublePutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.DOUBLE,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction objectPutStatic2_barrier=
        GodGivenFunction.make("fivmr_objectPutStatic2",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              },
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction intWeakCASStatic2_barrier=
        GodGivenFunction.make("fivmr_intWeakCASStatic2",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT,
                                  Basetype.INT,
                                  Basetype.INT
                              });
    public static GodGivenFunction pointerWeakCASStatic2_barrier=
        GodGivenFunction.make("fivmr_pointerWeakCASStatic2",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              });
    public static GodGivenFunction objectWeakCASStatic2_barrier=
        GodGivenFunction.make("fivmr_objectWeakCASStatic2",
                              Basetype.BOOLEAN,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.POINTER,
                                  Basetype.INT
                              });
    
    public static GodGivenFunction throwException=
        GodGivenFunction.make("fivmr_throw",
                              Basetype.VOID,
                              new Basetype[]{
                                  Basetype.POINTER,
                                  Basetype.POINTER
                              },
                              SafepointMode.CANNOT_SAFEPOINT);
	    
    public static CStructField TypeData_state=
        CStructField.make(Basetype.POINTER, "state", "fivmr_TypeData");
    public static CStructField TypeData_forward=
        CStructField.make(Basetype.POINTER, "forward", "fivmr_TypeData");
    public static CStructField TypeData_inited=
        CStructField.make(Basetype.INT, "inited", "fivmr_TypeData");
    public static CStructField TypeData_curIniter=
        CStructField.make(Basetype.POINTER, "curIniter", "fivmr_TypeData");
    public static CStructField TypeData_name=
        CStructField.make(Basetype.POINTER, "name", "fivmr_TypeData");
    public static CStructField TypeData_flags=
        CStructField.make(Basetype.INT, "flags", "fivmr_TypeData");
    public static CStructField TypeData_parent=
        CStructField.make(Basetype.POINTER, "parent", "fivmr_TypeData");
    public static CStructField TypeData_nSuperInterfaces=
        CStructField.make(Basetype.CHAR, "nSuperInterfaces", "fivmr_TypeData");
    public static CStructField TypeData_superInterfaces=
        CStructField.make(Basetype.POINTER, "superInterfaces", "fivmr_TypeData");
    public static CStructField TypeData_canonicalNumber=
        CStructField.make(Basetype.INT, "canonicalNumber", "fivmr_TypeData");
    public static CStructField TypeData_epochs=
        CStructField.make(Basetype.VOID, "epochs", "fivmr_TypeData");
    public static CStructField TypeData_arrayElement=
        CStructField.make(Basetype.POINTER, "arrayElement", "fivmr_TypeData");
    public static CStructField TypeData_size=
        CStructField.make(Basetype.INT, "size", "fivmr_TypeData");
    public static CStructField TypeData_requiredAlignment=
        CStructField.make(Basetype.BYTE, "requiredAlignment", "fivmr_TypeData");
    public static CStructField TypeData_refSize=
        CStructField.make(Basetype.BYTE, "refSize", "fivmr_TypeData");
    public static CStructField TypeData_classObject=
        CStructField.make(Basetype.POINTER, "classObject", "fivmr_TypeData");
    public static CStructField TypeData_numMethods=
        CStructField.make(Basetype.CHAR, "numMethods", "fivmr_TypeData");
    public static CStructField TypeData_numFields=
        CStructField.make(Basetype.CHAR, "numFields", "fivmr_TypeData");
    public static CStructField TypeData_methods=
        CStructField.make(Basetype.POINTER, "methods", "fivmr_TypeData");
    public static CStructField TypeData_fields=
        CStructField.make(Basetype.POINTER, "fields", "fivmr_TypeData");
    public static CStructField TypeData_gcMap=
        CStructField.make(Basetype.POINTER, "gcMap", "fivmr_TypeData");
    public static CStructField TypeData_vtable=
        CStructField.make(Basetype.VOID, "vtable", "fivmr_TypeData");

    public static CStructField VM_payload=
	CStructField.make(Basetype.POINTER, "payload", "fivmr_VM");
    
    public static GodGivenFunction AH_float_to_int=
        GodGivenFunction.make("fivmr_AH_float_to_int",
                              Basetype.INT,
                              new Basetype[]{
                                  Basetype.FLOAT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction AH_double_to_int=
        GodGivenFunction.make("fivmr_AH_double_to_int",
                              Basetype.INT,
                              new Basetype[]{
                                  Basetype.DOUBLE
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction AH_float_to_long=
        GodGivenFunction.make("fivmr_AH_float_to_long",
                              Basetype.LONG,
                              new Basetype[]{
                                  Basetype.FLOAT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction AH_double_to_long=
        GodGivenFunction.make("fivmr_AH_double_to_long",
                              Basetype.LONG,
                              new Basetype[]{
                                  Basetype.DOUBLE
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    
    public static GodGivenFunction AH_long_div=
        GodGivenFunction.make("fivmr_AH_long_div",
                              Basetype.LONG,
                              new Basetype[]{
                                  Basetype.LONG,
                                  Basetype.LONG
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction AH_long_mod=
        GodGivenFunction.make("fivmr_AH_long_mod",
                              Basetype.LONG,
                              new Basetype[]{
                                  Basetype.LONG,
                                  Basetype.LONG
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);

    public static GodGivenFunction AH_float_mod=
        GodGivenFunction.make("fivmr_AH_float_mod",
                              Basetype.FLOAT,
                              new Basetype[]{
                                  Basetype.FLOAT,
                                  Basetype.FLOAT
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    public static GodGivenFunction AH_double_mod=
        GodGivenFunction.make("fivmr_AH_double_mod",
                              Basetype.DOUBLE,
                              new Basetype[]{
                                  Basetype.DOUBLE,
                                  Basetype.DOUBLE
                              },
                              SideEffectMode.PURE,
                              SafepointMode.CANNOT_SAFEPOINT);
    
    public static TrustedGodGivenFunction memcpy=
        new TrustedGodGivenFunction("memcpy",
                                    Basetype.VOID,
                                    new Basetype[]{
                                        Basetype.POINTER,
                                        Basetype.POINTER,
                                        Basetype.POINTER
                                    },
                                    SideEffectMode.CLOBBERS_WORLD,
                                    SafepointMode.CANNOT_SAFEPOINT);

    // NOTE: CLocal/GodGivenCVar/CGlobal should be treated quite differently.
    // GodGivenCVar may refer to either a local or a global - it's just a reference
    // to a C expression that would be seen from that C function.
    //
    // CGlobals, on the other hand, must be handled carefully since there are so
    // darn many of them.  we don't want a central repository for *all* of those.
    // but, CGlobals may be used at JIT time.  then, we'll want to know what
    // those are a priori.  so any CGlobals created by the compiler whose purpose is
    // not to refer to code generated by the compiler itself but to stuff from the
    // runtime should be properly captured in one place.
    
    // NOTE: GodGivenCFunction requires the same treatment as GodGivenCVars.
    
    // THUS: GodGivenCFunction and GodGivenCVar must be placed in this repository.

    public static String generated_bytecodeTable_name;
    public static String generated_classTable_name;
    public static String generated_debugTable_name;
    public static String generated_patchRepo_name;
    public static String generated_payload_name;
    public static String generated_contexts_name;
    public static String generated_stringTable_name;
    public static String generated_stringDataArray_name;
    public static String generated_stringIndex_name;
    public static String generated_typeList_name;
    public static String generated_stubList_name;

    public static KnownRemoteDataConstant generated_bytecodeTable;
    public static KnownRemoteDataConstant generated_classTable;
    public static KnownRemoteDataConstant generated_debugTable;
    public static KnownRemoteDataConstant generated_patchRepo;
    public static KnownRemoteDataConstant generated_staticPrimFields;
    public static KnownRemoteDataConstant generated_staticRefFields;
    public static Linkable generated_contexts;
    public static KnownRemoteDataConstant generated_stringTable;
    public static KnownRemoteDataConstant generated_stringDataArray;
    public static RemoteDataConstant generated_stringIndex;
    public static KnownRemoteDataConstant generated_typeList;
    public static Linkable generated_stubList;
    
    public static void addGenerated(String name) {
        // FIXME: have a way of informing the memotable that these are generated.  there
        // should be a boolean or something.
        
	generated_bytecodeTable_name=name+"_bytecodeTable";
	generated_classTable_name=name+"_classTable";
	generated_debugTable_name=name+"_degugTable";
	generated_patchRepo_name=name+"_patchRepo";
	generated_payload_name=name+"_payload";
	generated_contexts_name=name+"_contexts";
	generated_stringTable_name=name+"_stringTable";
	generated_stringDataArray_name=name+"_stringDataArray";
	generated_stringIndex_name=name+"_stringIndex";
	generated_typeList_name=Global.name+"_typeList";
	generated_stubList_name=Global.name+"_stubList";

	generated_bytecodeTable=KnownRemoteDataConstant.forName(generated_bytecodeTable_name,true);
	generated_classTable=KnownRemoteDataConstant.forName(generated_classTable_name,true);
	generated_debugTable=KnownRemoteDataConstant.forName(generated_debugTable_name,true);
	generated_patchRepo=KnownRemoteDataConstant.forName(generated_patchRepo_name,true);
	generated_staticPrimFields=KnownRemoteDataConstant.forName(name+"_staticPrimFields",true);
	generated_staticRefFields=KnownRemoteDataConstant.forName(name+"_staticRefFields",true);
        
        // FIXME: add a KnownRemote variety
	generated_contexts=new Linkable(Basetype.VOID,generated_contexts_name) {
	        public boolean isLocal() { return false; }
	        public void generateDeclaration(PrintWriter w) {
	            w.println("extern fivmr_StaticTypeContext "+generated_contexts_name+
	                      "["+Global.contextList().size()+"];");
	        }
	    };

	generated_stringTable=KnownRemoteDataConstant.forName(generated_stringTable_name,true);
	generated_stringDataArray=KnownRemoteDataConstant.forName(generated_stringDataArray_name,true);
	generated_stringIndex=new RemoteDataConstant(generated_stringIndex_name);
	generated_typeList=KnownRemoteDataConstant.forName(generated_typeList_name,true);
        
        // FIXME: add a KnownRemote variety
	generated_stubList=new Linkable(Basetype.VOID,generated_stubList_name){
	        public boolean isLocal() {
	            return false;
	        }
	        public void generateDeclaration(PrintWriter w) {
	            w.println("extern fivmr_TypeStub "+generated_stubList_name+"[];");
	        }
	    };
    }
    
    public static ConfigNode asConfigNode() {
        ConfigMapNode result=new ConfigMapNode();
        
        result.put("ctypes",ctypes.asConfigNode());
        result.put("cfields",cfields.asConfigNode());
        result.put("knownRemoteCGlobals",krcg.asConfigNode());
        result.put("knownRemoteDataConstants",krdc.asConfigNode());
        result.put("godGivenCVars",ggcv.asConfigNode());
        result.put("godGivenFunctions",ggf.asConfigNode());
        
        return result;
    }
    
    public static void fromConfigNode(ConfigNode node_) {
        ConfigMapNode node=node_.asMap();
        
        ctypes.fromConfigNode(node.getNode("ctypes"));
        cfields.fromConfigNode(node.getNode("cfields"));
        krcg.fromConfigNode(node.getNode("knownRemoteCGlobals"));
        krdc.fromConfigNode(node.getNode("knownRemoteDataConstants"));
        ggcv.fromConfigNode(node.getNode("godGivenCVars"));
        ggf.fromConfigNode(node.getNode("godGivenFunctions"));
    }
    
    public static LinkableSet godGivenIndirections() {
        LinkableSet result=new LinkableSet();
        for (GodGivenCVar cvar : ggcv()) {
            if (cvar.usedForGetAddress()) {
                result.add(cvar.makeGetAddressLocal());
            }
            if (cvar.usedForRead()) {
                result.add(cvar.makeReadLocal());
            }
            if (cvar.usedForWrite()) {
                result.add(cvar.makeWriteLocal());
            }
        }
        for (GodGivenFunction func : ggf()) {
            if (func.usedForCall()) {
                result.add(func.makeCallLocal());
            }
        }
        return result;
    }

    static boolean inited;
    
    static {
	Frame_up.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);
	Frame_id.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);

	Payload_stringTable.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);
	Payload_classTable.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);
	
	ThreadState_curF.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);
	ThreadState_allocFrame.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);
	ThreadState_vm.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);

	VM_payload.setThreadLocalMode(ThreadLocalMode.FRAME_LOCAL);
        
        if (Basetype.inited) {
            linkBasetypes();
        }
        
        if (Global.verbosity>=1) {
            Global.log.println("CTypesystemReferences initialized.");
        }
        inited=true;
    }
}
