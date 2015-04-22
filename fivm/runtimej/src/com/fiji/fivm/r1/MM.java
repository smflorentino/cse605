/*
 * MM.java
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

package com.fiji.fivm.r1;

import static com.fiji.fivm.Constants.*;
import com.fiji.fivm.Settings;
import com.fiji.fivm.r1.edu.buffalo.cse605.LOG;

import static com.fiji.fivm.r1.fivmOptions.*;
import static com.fiji.fivm.r1.fivmRuntime.*;
import static com.fiji.fivm.om.OMData.*;

/**
 * The memory management entrypoint implementation.  Memory management is mostly
 * implemented in runtimec, but two areas of it are implemented in Java:
 * <ul>
 * <li>Entrypoints used by the compiler for allocation and barriers.
 * <li>Generic allocation entrypoints used by everyone, including C code (for example,
 *     JNI code).</li>
 * </ul>
 * Note that this code relies heavily on inlining and constant folding for performance.
 * When inspecting this code, you will see long sequences of seemingly complex code
 * that at first glance cannot be performant.  However, this code is written with
 * intimate knowledge of the optimization capabilities of c1 and gcc, and it has been
 * confirmed, based on looking at both emitted C code and emitted assembly, that all
 * of the fast path code found herein is reduced to highly optimal machine code.  As
 * such, any modifications to this code should be made with the compilers' capabilities
 * in mind!
 * <p>
 * Furthermore, it should be noted that this code is implementing support for a number
 * of different GCs, allocation styles, and object models.  And by object models we do
 * not mean just the structure of the object header -- this code is actually considering
 * both the case where objects are contiguous in memory and the case where they are not;
 * moreover, it has the ability to understand different styles of object references.
 * For example, under one model supported herein, object references point to the beginning
 * of the array payload or the beginning of the object payload minus 4.  In another
 * style, object references point to the beginning of the header.  As such, this code is
 * at times subtle and complex; this is neither a bug nor a lack of software engineering
 * foresight, but rather a careful and meticulous attempt at ensuring that this code
 * is simultaneously optimized for all of the object models, allocation styles, and GCs
 * that we support.
 */
@UsesMagic
@NoFlowLog
public class MM {
    private static final boolean EXTREME_ASSERTS = false;
    
    private MM() {}

    /**
     * Determines whether allocSpace indicates the heap memory area
     * @return true of allocSpace is the heap
     */
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static boolean heapArea(int allocSpace) {
	return (allocSpace==objectSpace()&&
		CType.getPointer(getGCData(),"fivmr_GCData","currentArea")==
		getGCData().add(CType.offsetof("fivmr_GCData",
					       "heapMemoryArea")));
    }

    /**
     * Determines whether allocSpace indicates a shared memory area
     * in the object alloc space.
     * @return true if allocSpace is shared, false otherwise
     */
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static boolean sharedArea(int allocSpace) {
	return Settings.HAVE_SHARED_SCOPES 
            && (allocSpace==objectSpace()
		&&CType.getPointer(CType.getPointer(getGCData(),
						    "fivmr_GCData",
						    "currentArea"),
				   "fivmr_MemoryArea","shared")
		!=Pointer.zero());
    }

    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int allocOffset() {
        if (Settings.OM_CONTIGUOUS) {
            return objectGCOffset();
        } else if (Settings.OM_FRAGMENTED) {
            return 0;
        } else {
            throw abort("unknown object model");
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int objectPayloadOffset() {
        if (Settings.OM_CONTIGUOUS) {
            return -4;
        } else if (Settings.OM_FRAGMENTED) {
            return totalHeaderSize();
        } else {
            throw abort("unknown object model");
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int objectAlignmentOffset() {
	if (Settings.OM_CONTIGUOUS) {
	    return -4;
	} else if (Settings.OM_FRAGMENTED) {
	    return 0;
	} else {
	    throw abort("unknown object model");
	}
    }

    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int sharedAreaObjectAlignmentOffset() {
	if (Settings.NOGC) {
	    return objectAlignmentOffset();
	} else {
	    return objectAlignmentOffset() + Pointer.size();
	}
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int arrayAlignmentOffset() {
        if (Settings.OM_CONTIGUOUS) {
            if (Pointer.size()==4) {
                /* in 32-bit land contiguous arrays have three word prior to
                   the object pointer and the payload following it.  thus if
                   we align the object pointer (alignment offset = 0) then
                   we achieve alignment for both the payload and all of the
                   header words. */
                return 0;
            } else {
                /* in 64-bit land contiguous arrays have two 64-bit words
                   followed by a 32-bit word, prior to the object pointer, and
                   the payload following it.  we align the payload such that
                   it sits on the same boundaries as the 64-bit header words.
                   thus we need to align with respect to the 64-bit header
                   words so - so we have an alignment offset of -4 (though
                   it could be +4, or -12, or +12, or so on). */
                return -4;
            }
        } else if (Settings.OM_FRAGMENTED) {
            return 0;
        } else {
            throw abort("unknown object model");
        }
    }

    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int sharedAreaArrayAlignmentOffset() {
	if (Settings.NOGC) {
	    return arrayAlignmentOffset();
	} else {
	    return arrayAlignmentOffset() + Pointer.size();
	}
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static Pointer requiredSizeAlignment() {
        if (Settings.OM_CONTIGUOUS) {
            return Pointer.fromInt(Pointer.size());
        } else {
            return Pointer.fromInt(chunkWidth());
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static int logRequiredObjectAlignment() {
        if (Settings.OM_CONTIGUOUS) {
            if (Pointer.size()==4) {
                return 2;
            } else if (Pointer.size()==8) {
                return 3;
            } else {
                throw abort("bad pointer size");
            }
        } else {
            return logChunkWidth();
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    static Pointer gcHeader(Pointer object) {
        return object.sub(objectGCOffset());
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    static Pointer objectHeader(Pointer object) {
        return object.sub(objectTDOffset());
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    static Pointer readObjectHeader(Pointer objectHeader) {
        Pointer result = objectHeader.loadPointer();
        if (Settings.HM_POISONED) {
            result=result.sub(1);
        }
        return result;
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    static Pointer fhHeader(Pointer object) {
        return object.sub(objectFHOffset());
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    public static Pointer getGCforVM(Pointer vm) {
        return vm.add(CType.offsetof("fivmr_VM","gc"));
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    public static Pointer getVMforGC(Pointer gc) {
        return gc.sub(CType.offsetof("fivmr_VM","gc"));
    }
    
    /**
     * Tells you which garbage collector instance the currently executing Java
     * code is using.  There is a one-to-one mapping between the current VM and
     * the current GC.
     */
    @NoPollcheck
    @NoSafepoint
    @Inline
    public static Pointer getGC() {
        return getGCforVM(Magic.getVM());
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    static void stampGCBits(int gcSpace,
                            Pointer frame,
                            Pointer object) {
        if (gcSpace==stackAllocSpace()) {
            gcHeader(object).store(frame.ushr(2));
	} else if (Settings.SCOPED_MEMORY) {
	    gcHeader(object).store(
                CType.getPointer(CType.getPointer(
                                     getGCData(),"fivmr_GCData","currentArea"),
                                 "fivmr_MemoryArea",
                                 "scopeID"));
        } else {
            if (Settings.NOGC) {
                gcHeader(object).store(Pointer.fromIntSignExtend(-1));
            } else {
                gcHeader(object).store(CType.getPointer(
                                           getGCData(),"fivmr_GCData","curShadedAlloc"));
            }
        }
    }
    
    @NoPollcheck
    @NoSafepoint
    @Inline
    static void stampObjHeader(Pointer object,
                               Pointer td) {
        if (Settings.HM_POISONED) {
            td=td.add(1);
        }
        objectHeader(object).store(td);
    }
    
    @Inline
    @NoSafepoint
    @NoPollcheck
    public static boolean willNeverMove(Object o) {
	if (nonMovingGC()) {
	    return true;
	} else {
	    throw abort("expecting a non-moving GC");
	}
    }
    
    /**
     * Checks if the given array is contiguous.  If the object is not
     * an array, the result is undefined.
     */
    @Inline
    @NoSafepoint
    @NoPollcheck
    public static boolean contiguousArray(Object o) {
	if (Settings.OM_CONTIGUOUS) {
	    return true;
	} else if (Settings.OM_FRAGMENTED) {
	    Pointer ptr=Pointer.fromObject(o);
            if (ptr.add(objectPayloadOffset()).loadInt()==0) {
                // if it is an arraylet then it is only "contiguous" for sure
                // if it has 0 or 1 elements.
                return fhHeader(ptr).loadPointer().sub(4).loadInt()<=1;
            } else {
                return true;
            }
	} else {
            throw abort("unrecognized object model");
        }
    }
    
    /**
     * Get the address of an array element.  This is the type-unsafe version of
     * the methods of the same name in Magic.  In order to work, it needs the
     * element size of the array.  This method is designed to be fast and
     * correct for all object models, but is totally unsafe - if you give it
     * an element size that does not match actual size of elements in the array,
     * you will just get a bogus result (or it'll crash).
     * <p>
     * Note that it is only safe for ask for the address of the "tail" element
     * (i.e. index = array.length) if the array is contiguous.
     */
    @Inline
    @NoSafepoint
    @NoPollcheck
    public static Pointer addressOfElement(Object o,
                                           int index,
                                           Pointer eleSize) {
        Pointer ptr=Pointer.fromObject(o);
        if (Settings.OM_CONTIGUOUS || ptr.add(objectPayloadOffset()).loadInt()!=0) {
            return ptr.add(arrayPayloadOffset(eleSize)).add(eleSize.mul(index));
        } else if (Settings.OM_FRAGMENTED) {
            Pointer offset=eleSize.mul(index);
            return fhHeader(ptr).loadPointer().add(offset.shr(logChunkWidth())).loadPointer().sub(offset.and(Pointer.fromInt(chunkWidth()-1)));
        } else {
            throw abort("unrecognized object model");
        }
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static Pointer indexableStartOfArray(Object o,Pointer eleSize) {
        if (contiguousArray(o)) {
            return Pointer.fromObject(o).add(arrayPayloadOffset(eleSize));
        } else {
            throw new fivmError("Array isn't contiguous");
        }
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static Pointer indexableStartOfArray(byte[] array) {
        return indexableStartOfArray(array,Pointer.fromInt(1));
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static Pointer indexableStartOfArray(char[] array) {
        return indexableStartOfArray(array,Pointer.fromInt(2));
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static Pointer indexableStartOfArray(short[] array) {
        return indexableStartOfArray(array,Pointer.fromInt(2));
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static Pointer indexableStartOfArray(int[] array) {
        return indexableStartOfArray(array,Pointer.fromInt(4));
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static Pointer indexableStartOfArray(long[] array) {
        return indexableStartOfArray(array,Pointer.fromInt(8));
    }
    
    @NoPollcheck
    @AllowUnsafe
    public static Pointer indexableStartOfArray(Pointer[] array) {
        return indexableStartOfArray(array,Pointer.fromInt(Pointer.size()));
    }
    
    @Inline
    @NoPollcheck
    @Pure
    public static int arrayLength(Object o) {
        Pointer ptr=Pointer.fromObject(o);
        if (Settings.OM_CONTIGUOUS) {
            return ptr.add(objectPayloadOffset()).loadInt();
        } else if (Settings.OM_FRAGMENTED) {
            return fhHeader(ptr).loadPointer().add(-4).loadInt();
        } else {
            throw abort("unrecognized object model");
        }
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    @Pure
    public static int hashCode(Object o) {
        return Pointer.fromObject(o).castToInt()>>logRequiredObjectAlignment();
    }
    
    @NoInline
    @NoReturn
    @Reflect
    private static void throwOOME() {
        LOG.info(LOG.DEBUG_MM, "Throwing OOME from MM.java");
        throwOutOfMemoryError_inJava();
    }
    
    @NoInline
    @NoReturn
    @Reflect
    private static void throwNASE() {
        throwNegativeSizeRTE();
    }
    
    @NoInline
    @NoReturn
    @Reflect
    private static void throwOOMEOrNASE(int numEle) {
        if (numEle<0) {
            throwNASE();
        } else {
            LOG.info(LOG.DEBUG_MM, "Threw OOME from MM.java:485");
            throwOOME();
        }
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    private static Pointer computeArrayPayloadSize(int allocSpace,
                                                   int numEle,
                                                   Pointer eleSize) {
        Pointer payloadSize=eleSize.mul(numEle);
        
        if (Settings.OM_FRAGMENTED && Settings.FORCE_ARRAYLETS &&
            allocSpace!=stackAllocSpace()) {
            payloadSize=payloadSize.add(chunkWidth()-1).and(Pointer.fromInt(chunkWidth()-1).not());
        }
        
        return payloadSize;
    }
    
    /**
     * Compute an array size given a payload size.
     */
    @Inline
    @NoSafepoint
    @NoPollcheck
    private static Pointer computeArraySizeWithPayloadSize(int allocSpace,
                                                           Pointer payloadSize,
                                                           Pointer eleSize) {
        Pointer size;
        
        if (Settings.OM_FRAGMENTED && Settings.HFGC && Settings.FORCE_ARRAYLETS) {
            throw abort("cannot compute array size if using HFGC and FORCE_ARRAYLETS");
        }

        if (Settings.OM_FRAGMENTED &&
            Settings.FORCE_ARRAYLETS &&
            allocSpace!=stackAllocSpace()) {
            size=
                align(Pointer.fromInt(totalHeaderSize()+4+4)
                      .add(payloadSize.add(chunkWidth()-1).shr(logChunkWidth())
                           .mul(Pointer.size())),
                      eleSize).add(payloadSize);
        } else {
            return alignRaw(Pointer.fromInt(totalHeaderSize()+4),eleSize).add(payloadSize);
        }
        
        return size;
    }
    
    /**
     * Compute an array size, which may not be pointer-size-aligned.
     */
    @Inline
    @NoSafepoint
    @NoPollcheck
    private static Pointer computeArraySize(int allocSpace,
                                            int numEle,
                                            Pointer eleSize) {
        return computeArraySizeWithPayloadSize(
            allocSpace,
            computeArrayPayloadSize(allocSpace,numEle,eleSize),
            eleSize);
    }
    
    @Inline
    @NoSafepoint
    @NoPollcheck
    private static Pointer computeSpineLength(Pointer payloadSize) {
        if (Settings.HFGC) {
            return payloadSize.add(chunkWidth()-1).ushr(logChunkWidth());
        } else {
            throw abort("wrong GC");
        }
    }
    
    @Inline
    @NoSafepoint
    @NoPollcheck
    private static Pointer computeSpineSize(Pointer spineLength) {
        if (Settings.HFGC) {
            return spineLength.mul(Pointer.size()).add(2*Pointer.size());
        } else {
            throw abort("wrong GC");
        }
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    private static Pointer getGCData() {
        return Magic.curThreadState().add(CType.offsetof("fivmr_ThreadState","gc"));
    }
    
    @Inline
    @NoSafepoint
    @NoPollcheck
    private static Pointer getAlloc(int allocSpace) {
        return getGCData().add(
            CType.offsetof("fivmr_GCData","alloc").add(
                CType.sizeof("fivmr_GCSpaceAlloc").mul(allocSpace)));
    }
    
    @NoInline
    @NoPollcheck
    @AllowUnsafe
    private static Pointer allocSpineSlow(Pointer spineLength,
                                          int numEle,
                                          Pointer description) {
        return fivmr_GC_allocSSSlow(
            Magic.curThreadState(),spineLength,numEle,
            description);
    }
    
    // spine allocation methods for HFGC
    @NoPollcheck
    @AllowUnsafe
    @Inline
    private static Pointer allocSpine(Pointer spineLength,
                                      int numEle,
                                      Pointer typeData,
                                      Pointer description) {
        if (Settings.HFGC) {
            Pointer alloc=getAlloc(objectSpace());
            
            Pointer spineSize=computeSpineSize(spineLength);
            
            Pointer result=
                CType.getPointer(
                    alloc,"fivmr_GCSpaceAlloc","ssBump").sub(spineSize);
            
            if (CType.getPointer(
                    alloc,"fivmr_GCSpaceAlloc","ssEnd").sub(result)
                .greaterThan(
                    Settings.HFGC_FAIL_FAST_PATHS
                    ?CType.getPointer(alloc,"fivmr_GCSpaceAlloc","zero")
                    :CType.getPointer(alloc,"fivmr_GCSpaceAlloc","ssSize"))) {
                Magic.unlikely();
                if (Settings.INTERNAL_INST && Settings.HFGC_ALL_ARRAYLETS) {
                    FIVMR_II_BEFORE_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
                                                     Magic.curFrame(),
                                                     typeData,
                                                     numEle);
                }
                result=allocSpineSlow(spineLength,numEle,description);
                if (Settings.INTERNAL_INST && Settings.HFGC_ALL_ARRAYLETS) {
                    FIVMR_II_AFTER_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
                                                    Magic.curFrame(),
                                                    typeData,
                                                    numEle);
                }
            } else {
                CType.put(alloc,"fivmr_GCSpaceAlloc","ssBump",result);
            }
            
            result.add(spineForwardOffset()).store(result);
            result.add(spineArrayLengthOffset()).store(numEle);
            
            return result;
        } else {
            throw abort("wrong GC");
        }
    }
    
    @NoPollcheck
    @NoInline
    @AllowUnsafe
    private static Pointer allocChunkSlow(Pointer requiredAlignment,
                                          Pointer description) {
	if (!Settings.HFGC) {
	    throw abort("wrong GC");
	}

	return fivmr_GC_allocRawSlow(
	    Magic.curThreadState(),
	    objectSpace(),
	    Pointer.fromInt(chunkWidth()),
	    Pointer.fromInt(0),
	    requiredAlignment,
            AE_MUST_SUCCEED,
            description);
    }
    
    @NoPollcheck
    @AllowUnsafe
    @Inline
    private static Pointer allocChunk(Pointer requiredAlignment,
                                      Pointer typeData,
                                      int arrayNumEle,
                                      Pointer description) {
	if (!Settings.HFGC) {
	    throw abort("wrong GC");
	}

        Pointer alloc=getAlloc(objectSpace());
        
        Pointer result=
            align(CType.getPointer(
                      alloc,"fivmr_GCSpaceAlloc","bump"),
		  requiredAlignment);
        
	Pointer newBump=result.add(chunkWidth());
	
        if (newBump.sub(CType.getPointer(alloc,"fivmr_GCSpaceAlloc","start"))
            .greaterThan(
                Settings.HFGC_FAIL_FAST_PATHS
                ?CType.getPointer(alloc,"fivmr_GCSpaceAlloc","zero")
                :CType.getPointer(alloc,"fivmr_GCSpaceAlloc","size"))) {
            Magic.unlikely();
            if (Settings.INTERNAL_INST && Settings.HFGC_ALL_ARRAYLETS && arrayNumEle>=0) {
                FIVMR_II_BEFORE_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
                                                 Magic.curFrame(),
                                                 typeData,
                                                 arrayNumEle);
            }
            result=allocChunkSlow(requiredAlignment,description);
            if (Settings.INTERNAL_INST && Settings.HFGC_ALL_ARRAYLETS && arrayNumEle>=0) {
                FIVMR_II_AFTER_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
                                                Magic.curFrame(),
                                                typeData,
                                                arrayNumEle);
            }
        } else {
            CType.put(alloc,"fivmr_GCSpaceAlloc","bump",newBump);
        }
        
        if (EXTREME_ASSERTS && Settings.ASSERTS_ON) {
            if (result.and(Pointer.fromInt(chunkWidth()-1))!=Pointer.zero()) {
                throw abort("Misaligned chunk.");
            }
        }

        return result;
    }
    
    // methods for "stamping" objects and arrays after allocation, the common
    // way (i.e. the fast way)
    @Inline
    @NoPollcheck
    private static Object stampObject(Pointer result,
                                      int allocSpace,
                                      Pointer allocFrame,
                                      Pointer typeData,
                                      Pointer size) {
        if (Settings.OM_FRAGMENTED) {
            Pointer cur=result;
            for (int n=(size.castToInt()-1)>>logChunkWidth();
                 n-->0;) {
                Pointer next=cur.add(chunkWidth());
                cur.store(next.or(Pointer.fromInt(1)));
                cur=next;
            }
        }

        stampGCBits(allocSpace,allocFrame,result);
        stampObjHeader(result,typeData);
        
        return result.asObject();
    }
    
    @Inline
    @NoPollcheck
    private static Object stampArray(Pointer result,
                                     int allocSpace,
                                     Pointer allocFrame,
                                     Pointer typeData,
                                     int numEle,
                                     Pointer eleSize) {
        stampGCBits(allocSpace,allocFrame,result);
        stampObjHeader(result,typeData);
        
        if (Settings.OM_FRAGMENTED && Settings.FORCE_ARRAYLETS &&
            allocSpace!=stackAllocSpace()) {
            
            // leave the pseudo array length "unintialized" to zero
            
            // set the array length in the spine
            result.add(arrayLengthOffset()+4).store((int)numEle);
            
            // initialize the spine
            Pointer spineCur=result.add(arrayLengthOffset()+4+4);
            Pointer blockCur=align(spineCur,eleSize).add(Pointer.fromInt(spineLength(numEle,eleSize)).mul(Pointer.size()));
            for (int n=spineLength(numEle,eleSize);n-->0;) {
                spineCur.store(blockCur.add(chunkWidth()).sub(eleSize));
                spineCur=spineCur.add(Pointer.size());
                blockCur=blockCur.add(chunkWidth());
            }
            
            // store the pointer to the spine
            fhHeader(result).store(result.add(arrayLengthOffset()+4+4));
        } else {
            result.add(arrayLengthOffset()).store((int)numEle);
            
            if (Settings.OM_FRAGMENTED) {
                fhHeader(result).store(result.add(arrayLengthOffset()+4));
            }
        }
        
        return result.asObject();
    }
    
    // DO NOT use the slow paths directly - they should only be called from
    // the MM.allocXYZ methods.
    
    @NoInline
    @NoPollcheck
    @AllowUnsafe
    @AllocateAsCaller
    private static Object allocSlow(int allocSpace,
                                    Pointer td) {
        if (Settings.PROFILE_GC) {
            fivmr_SPC_incAllocSlowPath();
        }
        
        Pointer size=
            alignRaw(Pointer.fromInt(fivmr_TypeData_size(td)),
                     requiredSizeAlignment());
	
	if (Settings.SCOPED_MEMORY&&sharedArea(allocSpace)) {
	    Pointer area=CType.getPointer(getGCData(),
					  "fivmr_GCData","currentArea");
	    Pointer bumpPtr=area.add(CType.offsetof("fivmr_MemoryArea","bump"));
	    size=size.add(Pointer.size());
	    for (;;) {
		Pointer bump=bumpPtr.loadPointer();
		Pointer align=Pointer.fromInt(
		    fivmr_TypeData_requiredAlignment(td));
		/* Asymmetry is intentional */
		Pointer result=align(bump.add(
		    sharedAreaObjectAlignmentOffset()),align)
		    .sub(objectAlignmentOffset());
		Pointer newBump=result.add(size);

		if (newBump.sub(CType.getPointer(area,
						 "fivmr_MemoryArea","start"))
		    .greaterThan(CType.getPointer(area,
						  "fivmr_MemoryArea","size"))) {
		    Magic.unlikely();
            LOG.info(LOG.DEBUG_MM, "Threw OOME from MM.java:843 - Scope full when allocating Pointer?");
		    throwOOME();
		}
		if (bumpPtr.weakCAS(bump,newBump)) {
		    libc.bzero(bump.sub(allocOffset()),
			       newBump.sub(bump));
		    Object object=stampObject(result,allocSpace,
					      Magic.curAllocFrame(),
					      td,size);
		    if (!Settings.NOGC) {
			Pointer headPtr=area.add(CType.offsetof("fivmr_MemoryArea",
								"objList"));
			Pointer link=result.sub(allocOffset()).sub(Pointer.size());
			Pointer head;
			do {
			    head=headPtr.loadPointer();
			    link.store(head);
			} while(!headPtr.weakCAS(head,link));
		    }
		    return object;
		}
	    }
	}

	if (Settings.SCOPED_MEMORY && !heapArea(allocSpace)) {
        LOG.info(LOG.DEBUG_MM, "Threw OOME from MM.java:848");
	    throwOOME();
	}

	Pointer requiredAlignment=
	    Pointer.fromInt(fivmr_TypeData_requiredAlignment(td));
        
        Pointer name=fivmr_TypeData_name(td);
        
	if (allocSpace == stackAllocSpace() ||
	    CType.getBoolean(getGC(),"fivmr_GC","noMoreHeapAlloc")) {
        LOG.info(LOG.DEBUG_MM, "Threw OOME from MM.java:859");
	    throwOOME();
	    return null; // not reached
        } else if (Settings.HFGC) {
            Object result=null;
            Pointer lastChunk=Pointer.zero();
            
            for (Pointer alloced=Pointer.zero();
                 alloced.lessThan(size);
                 alloced=alloced.add(chunkWidth())) {
                Pointer curChunk=allocChunk(requiredAlignment,td,-1,name);
                
                if (lastChunk!=Pointer.zero()) {
                    lastChunk.store(curChunk);
                } else {
                    stampGCBits(allocSpace,Magic.curAllocFrame(),curChunk);
                    stampObjHeader(curChunk,td);
                    result=curChunk.asObject();
                }
                
                lastChunk=curChunk;
                
                /* this pollcheck is necessary.  and really sneaky.  it means
                   that even though the allocator allocates black, it does not
                   necessarily return black objects!  Awesome! */
                Magic.pollcheck();
            }
            
            return result;
	} else {
            Pointer result=fivmr_GC_allocRawSlow(
                Magic.curThreadState(),
                objectSpace(),
                size,
                Pointer.fromInt(objectAlignmentOffset()),
                requiredAlignment,
                AE_MUST_SUCCEED,
                name);
            
            return stampObject(result,allocSpace,Magic.curAllocFrame(),
                               td,size);
	}
    }
    
    @NoInline
    @NoPollcheck
    @AllowUnsafe
    @AllocateAsCaller
    @Reflect
    private static Object allocArraySlow(int allocSpace,
                                         Pointer td,
                                         int numEle) {
        if (Settings.PROFILE_GC) {
            fivmr_SPC_incAllocSlowPath();
        }
	if (Settings.SCOPED_MEMORY && !heapArea(allocSpace)
	    && !sharedArea(allocSpace)) {
        LOG.info(LOG.DEBUG_MM, "Threw OOME from MM.java:917");
	    throwOOME();
	}
        
	if (allocSpace == stackAllocSpace() ||
	    CType.getBoolean(getGC(),"fivmr_GC","noMoreHeapAlloc")) {
        LOG.info(LOG.DEBUG_MM, "Threw OOME from MM.java:920");
	    throwOOME();
	    return null; // not reached
        }

        Pointer eleTD=fivmr_TypeData_arrayElement(td);
        Pointer eleSize=Pointer.fromInt(fivmr_TypeData_refSize(eleTD));
	Pointer payloadSize=computeArrayPayloadSize(allocSpace,numEle,eleSize);

        return allocArraySlowImpl(allocSpace,td,numEle,eleSize,payloadSize);
    }
    
    @NoPollcheck
    @AllocateAsCaller
    private static Object allocArraySlowImpl(int allocSpace,
                                             Pointer td,
                                             int numEle,
                                             Pointer eleSize,
                                             Pointer payloadSize) {
        return allocArraySlowImpl(allocSpace,td,numEle,eleSize,payloadSize,false);
    }
    
    @NoPollcheck
    @AllowUnsafe
    @AllocateAsCaller
    private static Object allocArraySlowImpl(int allocSpace,
                                             Pointer td,
                                             int numEle,
                                             Pointer eleSize,
                                             Pointer payloadSize,
                                             boolean forceContiguous) {
        Pointer unalignedSize=computeArraySizeWithPayloadSize(allocSpace,payloadSize,eleSize);
        Pointer size=alignCoeff(unalignedSize,
                                requiredSizeAlignment(),
                                eleSize);
        
        Pointer name=fivmr_TypeData_name(td);
        
	if (Settings.SCOPED_MEMORY&&sharedArea(allocSpace)) {
	    Pointer area=CType.getPointer(getGCData(),
					  "fivmr_GCData","currentArea");
	    Pointer bumpPtr=area.add(CType.offsetof("fivmr_MemoryArea",
						    "bump"));
	    Object object;
	    size=size.add(Pointer.size());
	    for (;;) {
		Pointer bump=bumpPtr.loadPointer();
		/* Asymmetry is intentional */
		Pointer result=
		    align(bump.add(sharedAreaArrayAlignmentOffset()),
			  eleSize)
		    .sub(arrayAlignmentOffset());
		Pointer newBump=result.add(size);

		if (newBump.sub(CType.getPointer(area,
						 "fivmr_MemoryArea","start"))
		    .greaterThan(CType.getPointer(area,"fivmr_MemoryArea",
						  "size"))) {
		    Magic.unlikely();
            LOG.info(LOG.DEBUG_MM, "Threw OOME from MM.java:979 - Shared Memory Area Exhausted?");
		    throwOOME();
		}
		if (bumpPtr.weakCAS(bump,newBump)) {
		    libc.bzero(bump.sub(allocOffset()),
			       newBump.sub(bump));
		    object=stampArray(result,allocSpace,Magic.curAllocFrame(),
				      td,numEle,eleSize);
		    if (!Settings.NOGC) {
			Pointer headPtr=area.add(CType.offsetof("fivmr_MemoryArea",
								"objList"));
			Pointer link=result.sub(allocOffset())
			    .sub(Pointer.size());
			Pointer head;
			do {
			    head=headPtr.loadPointer();
			    link.store(head);
			} while(!headPtr.weakCAS(head,link));
		    }
		    break;
		}
	    }
	    return object;
	}

        if (Settings.HFGC &&
            payloadSize.add(totalHeaderSize()+4).greaterThan(chunkWidth()) &&
            !forceContiguous) {
            if (Settings.HFGC_TRY_CONTIGUOUS_ARRAYS) {
                // first try to allocate it on a page, if one is available...

                Pointer contResult=fivmr_GC_allocRawSlow(
                    Magic.curThreadState(),
                    objectSpace(),
                    size,
                    Pointer.fromInt(arrayAlignmentOffset()),
                    eleSize,
                    AE_CAN_FAIL,
                    name);
            
                if (contResult!=Pointer.zero()) {
                    return stampArray(contResult,allocSpace,Magic.curAllocFrame(),
                                      td,numEle,eleSize);
                }
            }
            
            // ok that failed ... try fragmenting.
            
            // first allocate sentinel
            
            Pointer sentinel=allocChunk(Pointer.fromInt(Pointer.size()),td,numEle,name);
            
	    Object result=null;
            
	    stampGCBits(allocSpace,Magic.curAllocFrame(),sentinel);
	    stampObjHeader(sentinel,td);
            
	    sentinel.add(arrayLengthOffset()).store((int)0);
	    
	    Pointer spineLength=computeSpineLength(payloadSize);
	    
	    if (spineLength.mul(Pointer.size()).add(totalHeaderSize()+4+4)
		.lessThanOrEqual(chunkWidth())) {
		// easy case: spine is small enough to fit into the sentinel.
                
		sentinel.add(arrayLengthOffset()+4).store((int)numEle);
		
		Pointer spine=sentinel.add(arrayLengthOffset()+4+4);
		sentinel.store(spine);
                
		result=sentinel.asObject();
		
		for (Pointer cur=Pointer.zero();
		     cur.lessThan(spineLength);
		     cur=cur.add(1)) {
		    Pointer curChunk=allocChunk(eleSize,td,numEle,name);
		    Pointer chunkPtr=curChunk.add(chunkWidth()).sub(eleSize);
                        
		    spine.add(cur.mul(Pointer.size())).store(chunkPtr);
                        
		    Magic.pollcheck();
		}
	    } else {
		// slightly more complicated case: need to allocate a spine
		// in the semi-space.
                    
		// make sure the GC sees that we've got a sentinel, and
		// doesn't free it.
		result=sentinel.asObject();
                    
		// now allocate the spine...
		sentinel.store(allocSpine(spineLength,numEle,td,name));
                    
		for (Pointer cur=Pointer.zero();
		     cur.lessThan(spineLength);
		     cur=cur.add(1)) {
		    Pointer curChunk=allocChunk(eleSize,td,numEle,name);
                        
		    Pointer oldSpine=sentinel.loadPointer();
		    Pointer newSpine=oldSpine.add(spineForwardOffset()).loadPointer();
                        
		    Pointer chunkPtr=curChunk.add(chunkWidth()).sub(eleSize);
                        
		    oldSpine.add(cur.mul(Pointer.size())).store(chunkPtr);
		    Magic.fence();
		    newSpine.add(cur.mul(Pointer.size())).store(chunkPtr);
                        
		    Magic.pollcheck();
		}
	    }
	    
	    return result;
	} else {
            if (Settings.INTERNAL_INST && Settings.HFGC_ALL_ARRAYLETS) {
                FIVMR_II_BEFORE_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
                                                 Magic.curFrame(),
                                                 td,
                                                 numEle);
            }
	    Pointer result=fivmr_GC_allocRawSlow(
                Magic.curThreadState(),
                objectSpace(),
                size,
                Pointer.fromInt(arrayAlignmentOffset()),
                eleSize,
                AE_MUST_SUCCEED,
                name);
            if (Settings.INTERNAL_INST && Settings.HFGC_ALL_ARRAYLETS) {
                FIVMR_II_AFTER_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
                                                Magic.curFrame(),
                                                td,
                                                numEle);
            }
            
            return stampArray(result,allocSpace,Magic.curAllocFrame(),
                              td,numEle,eleSize);
	}
    }
    
    // generic entrypoints for allocation, if you don't have all of the information
    // available.
    
    /**
     * Allocate an object.  This requires an integer identifier of the space in which
     * you wish to allocate, and a pointer to the native TypeData structure that
     * describes the class of the object you are allocating.  This method never calls
     * the constructor of the resulting object; the object is instantiated with all
     * fields being zero.  This method may throw any of the errors that Java would
     * throw when doing a 'new'.
     * @param allocSpace The space in which you'd like the object to be allocated.
     *                   Pass either Constants.GC_OBJ_SPACE for allocating in the
     *                   heap, or MM.stackAllocSpace() for allocating on the stack.
     * @param td A pointer to the TypeData native structure for the class of the
     *           object you're allocating.
     * @return An instantiated, zero-initialized, but not constructed, object.
     */
    @AllocateAsCaller // needed for Magic.curAllocFrame() to work
    public static Object alloc(int allocSpace,
                               Pointer td) {
        if (Settings.ASSERTS_ON) {
            if (td==Pointer.zero()) {
                throw new fivmError("td is zero");
            }
            switch (fivmr_TypeData_flags(td)&TBF_TYPE_KIND) {
            case TBF_VIRTUAL:
            case TBF_FINAL:
                break; // ok
            default: throw new fivmError("bad TypeData for allocation: td = "+td.asLong()+
                                         ", td->flags = "+fivmr_TypeData_flags(td));
            }
        }
        return alloc(allocSpace,Magic.curAllocFrame(),td,
		     Pointer.fromInt(fivmr_TypeData_size(td)),
		     Pointer.fromInt(fivmr_TypeData_requiredAlignment(td)));
    }
    
    /**
     * Allocate an array.  This requires an integer identifier of the space in which
     * you wish to allocate, a pointer to the native TypeData structure that
     * describes the "type" (or "class" - whichever terminology makes you feel warm
     * happy fuzzies) of the array you are allocating, and the array length.  In
     * particular, the TypeData is not for the type of the array element - it's for
     * the type of the array that gets returned by this method.  So to allocate a
     * unidimensional array of integers this would be the TypeData that corresponds
     * to int[].  The array will be zero-initialized.  This method may throw any of
     * of the errors that Java would throw when doing a 'new' for an array.
     * @param allocSpace The space in which you'd like the object to be allocated.
     *                   Pass either Constants.GC_OBJ_SPACE for allocating in the
     *                   heap, or MM.stackAllocSpace() for allocating on the stack.
     * @param td A pointer to the TypeData native structure for the type of the
     *           array you're allocating.
     * @param numEle The number of elements that the array should have.
     * @return An instantiated and zero-initialized array of the given type and
     *         length.
     */
    @AllocateAsCaller // needed for Magic.curAllocFrame() to work
    public static Object allocArray(int allocSpace,
                                    Pointer td,
                                    int numEle) {
        if (Settings.ASSERTS_ON) {
            if (td==Pointer.zero()) {
                throw new fivmError("td is zero");
            }
            switch (fivmr_TypeData_flags(td)&TBF_TYPE_KIND) {
            case TBF_ARRAY:
                break; // ok
            default: throw new fivmError("bad TypeData for allocation: td = "+td.asLong()+
                                         ", td->flags = "+fivmr_TypeData_flags(td));
            }
        }
        return allocArray(allocSpace,Magic.curAllocFrame(),td,
			  numEle,
			  Pointer.fromInt(fivmr_TypeData_elementSize(td)));
    }
    
    /**
     * Implementation of MULTIANEWARRAY, to be used exclusively by the baseline
     * compiler.
     */
    @Reflect
    @SuppressWarnings("unused")
    @NoSafetyChecks // FIXME: is this right?
    private static Object multianewarray(Pointer td,
                                         int dims,
                                         Pointer lengthPtr) {
        if (!Settings.X86 || dims==0) {
            Magic.notReached();
        }
        
        Object result=allocArray(0,td,lengthPtr.loadInt());
        
        if (dims!=1) {
            Object[] array=(Object[])result;
            Pointer eleTD=fivmr_TypeData_arrayElement(td);
            
            Pointer eleLengthPtr;
            
            if (Settings.X86) {
                eleLengthPtr=lengthPtr.sub(4);
            } else {
                Magic.notReached();
                return null; // make javac happy
            }

            for (int i=0;i<array.length;++i) {
                array[i]=multianewarray(eleTD,dims-1,eleLengthPtr);
            }
        }
        
        return result;
    }
    
    /* change the allocator to:
       if remaining >= size {
       result = sentinel - remaining
       remaining -= size
       return result;
       thanks Daniel!
    */

    // FIXME: we could get some win if we didn't inline these thingies on paths
    // that have low execution probability....

    /**
     * Fast path compiler entrypoint for object allocation.  You could use this
     * directly, if you somehow knew the size, allocation frame, size, and alignment
     * of the object.  But unless you have these values as constants, rather than
     * having to load them from the TypeData, this method is of no direct use, since
     * the public object allocation entrypoint (see above) does not require these
     * parameters and instead computes them for you.  As
     * such this method is never called directly except by the compiler.
     */
    @Inline
    @NoPollcheck
    @AllowUnsafe
    @Reflect
    @AllocateAsCaller
    static Object alloc(int allocSpace,
                        Pointer allocFrame,
                        Pointer typeData,
                        Pointer size,
                        Pointer align) {
        if (Settings.PROFILE_GC_HEAVY) {
            fivmr_SPC_incAlloc();
        }
        if (Settings.INTERNAL_INST) {
            FIVMR_II_BEFORE_ALLOC(Magic.curThreadState(),
                                  Magic.curFrame(),
                                  typeData);
        }
        
        size=alignRaw(size,requiredSizeAlignment());

	Object object;
	if (Settings.SCOPED_MEMORY&&sharedArea(allocSpace)) {
	    /* This isn't the best way to get this on the slow path, but
	     * we do want to push this off to the slow path somehow.
	     * FIXME: revisit this */
	    object=allocSlow(allocSpace,typeData);
	} else {
	    Pointer alloc=getAlloc(allocSpace);
	    
	    Pointer oldBump=
		CType.getPointer(
		    alloc,"fivmr_GCSpaceAlloc","bump");
        
	    Pointer result=
		align(oldBump.add(objectAlignmentOffset()),
		      align)
		.sub(objectAlignmentOffset());
	    
	    Pointer newBump=result.add(size);
	    
	    if (newBump.sub(CType.getPointer(alloc,"fivmr_GCSpaceAlloc","start"))
		.greaterThan(
                    (Settings.HFGC_FAIL_FAST_PATHS && allocSpace!=stackAllocSpace())
                    ?CType.getPointer(alloc,"fivmr_GCSpaceAlloc","zero")
                    :CType.getPointer(alloc,"fivmr_GCSpaceAlloc","size"))) {
		Magic.unlikely();
		if (Settings.INTERNAL_INST) {
		    FIVMR_II_BEFORE_ALLOC_SLOW(Magic.curThreadState(),
					       Magic.curFrame(),
					       typeData);
		}
		object=allocSlow(allocSpace,typeData);
		if (Settings.INTERNAL_INST) {
		    FIVMR_II_AFTER_ALLOC_SLOW(Magic.curThreadState(),
					      Magic.curFrame(),
					      typeData);
		}
	    } else {
		if (allocSpace==stackAllocSpace()) {
		    libc.bzero(oldBump.sub(allocOffset()),
			       newBump.sub(oldBump));
		}
		CType.put(alloc,"fivmr_GCSpaceAlloc","bump",newBump);

		object=stampObject(result,allocSpace,allocFrame,typeData,size);
	    }
	}
        
        if (EXTREME_ASSERTS &&
            Settings.ASSERTS_ON &&
            Settings.HFGC &&
            allocSpace!=stackAllocSpace()) {
            if (Pointer.fromObject(object).and(Pointer.fromInt(chunkWidth()-1))!=Pointer.zero()) {
                throw abort("Misaligned object.");
            }
        }
        
        if (Settings.INTERNAL_INST) {
            FIVMR_II_AFTER_ALLOC(Magic.curThreadState(),
                                 Magic.curFrame(),
                                 typeData);
        }
	if (Settings.FLOW_LOGGING) {
	    /* This assumes that size is << 2^32.  I think we make that
	     * assumption elsewhere, but ... */
	    FlowLog.log(FlowLog.TYPE_ALLOC, FlowLog.SUBTYPE_OBJECT,
			(((long)CType.getInt(typeData, "fivmr_TypeData",
					     "uniqueID")) << 32) | size.castToInt(),
			Pointer.fromObject(object).asLong());
	}
        return object;
    }

    @Inline
    @NoPollcheck
    private static int spineLength(int numEle,Pointer eleSize) {
        Pointer payloadSize=eleSize.mul(numEle);
        return payloadSize.add(chunkWidth()-1).shr(logChunkWidth()).castToInt();
    }
    
    @Inline
    @NoPollcheck
    public static int maxArrayLength(int allocSpace,Pointer eleSize) {
        if (Pointer.size()==4 && eleSize.greaterThan(1)) {
            if (Settings.OM_FRAGMENTED && (Settings.FORCE_ARRAYLETS || Settings.HFGC) &&
                allocSpace!=stackAllocSpace()) {
                // This special check is *REQUIRED* for the FORCE_ARRAYLETS case,
                // and highly desirable for the HFGC case.  In the HFGC case, it's
                // desirable because it prevents arrays that would be unallocatable in
                // the fragmented case from being sometimes allocatable in the
                // contiguous case - thus increasing determinism.
                
                // formula from Mathematica, based on solving for numEle in
                // the expression in computeArraySize.  the thing we start with was:
                //
                //   headerSize + 4 + 4 + eleSize*numEle +
                //      eleSize*numEle/chunkWidth*pointerSize == maxMem
                //
                // and solved for numEle to get:
                //
                //     chunkWidth * (8 + headerSize - maxMem)
                //   - --------------------------------------
                //      eleSize * (chunkWidth + pointerSize)
                //
                // and then refactored it so as to ensure that we don't get a negative
                // number in the numerator, and that we don't overflow, either:
                //
                //   let denom = eleSize * (chunkWidth + pointerSize)
                //
                //                  maxMem   8 + headerSize
                //   chunkWidth * ( ------ - -------------- )
                //                   denom       denom
                //
                // but then observed that these divisions may do harmful rounding, so
                // we made them conservative:
                //
                //                        maxMem            8 + headerSize
                //   chunkWidth * ( floor(------) - ceiling(--------------) )
                //                         denom                denom
                //
                // ... and that's what we implement below, where maxMem is just
                // Pointer.fromIntSignExtend(-1) and headerSize is
                // totalHeaderSize().
                //
                // Note that even though we have the conservatism due to the floor
                // and ceiling, it never "loses" us more than 300 bytes of potential
                // array size.  Note furthermore, that the logic of only doing this
                // check if eleSize>1 is still correct; the above expression yields
                // a value greater than Integer.MAX_VALUE for eleSize==1.
                
                Pointer denom=
                    eleSize.mul(chunkWidth()+Pointer.size());
                
                return
                    Pointer.fromInt(chunkWidth()).mul(
                        Pointer.fromIntSignExtend(-1).div(denom).sub(
                            Pointer.fromInt(8+totalHeaderSize()).add(denom).sub(1)
                            .div(denom))).castToInt();
            } else {
                return
                    Pointer.fromIntSignExtend(-1).sub(
                        totalHeaderSize()+4).div(eleSize).castToInt();
            }
        } else {
            return Integer.MAX_VALUE;
        }
    }
    
    /**
     * Fast path compiler entrypoint for array allocation.  You could use this
     * directly, if you somehow knew the size, allocation frame and element size
     * of the array.  But unless you have these values as constants, rather than
     * having to load them from the TypeData, this method is of no direct use, since
     * the public array allocation entrypoint (see above) does not require these
     * parameters and instead computes them for you.  As
     * such this method is never called directly except by the compiler.
     */
    @Inline
    @NoPollcheck
    @AllowUnsafe
    @Reflect
    @AllocateAsCaller
    static Object allocArray(int allocSpace,
                             Pointer allocFrame,
                             Pointer typeData,
                             int numEle,
                             Pointer eleSize) {
        // for the HFGC case, array allocation has the following modes (assuming 32-bit):
        // length == 0      single chunk, structured like an arraylet with an
        //                  empty spine; spine points at right past the length
        // payload <= 16    always contiguous (single chunk)
        // payload > 16     contiguous if possible, otherwise do the following:
        // numChunks <= 3   sentinel contains spine
        // numChunks > 3    allocate a spine in the semispace
        //
        // and for 64-bit assuming eleSize<=4:
        // length == 0      simple chunk like above
        // payload <= 36    always contiguous (single chunk)
        // payload > 36     contiguous if possible, otherwise do the following:
        // numChunks <= 4   sentinel contains spine
        // numChunks > 4    allocate a spine in the semispace
        //
        // and for 64-bit assuming eleSize>4:
        // length == 0      simple chunk like above
        // payload <= 32    always contiguous (single chunk)
        // payload > 32     contiguous if possible, otherwise do the following:
        // numChunks <= 4   sentinel contains spine
        // numChunks > 4    allocate a spine in the semispace
        
        if (Settings.PROFILE_GC_HEAVY) {
            fivmr_SPC_incAlloc();
        }
        if (Settings.INTERNAL_INST) {
            FIVMR_II_BEFORE_ALLOC_ARRAY(Magic.curThreadState(),
                                        Magic.curFrame(),
                                        typeData,
                                        numEle);
        }
        
        Pointer alloc=getAlloc(allocSpace);

        int maxNumEle=maxArrayLength(allocSpace,eleSize);
        if (maxNumEle<Integer.MAX_VALUE) {
            if (Magic.semanticallyUnlikely(Magic.uLessThan(maxNumEle,numEle))) {
                throwOOMEOrNASE(numEle);
            }
        } else {
            if (Magic.semanticallyUnlikely(numEle<0)) {
                throwNASE();
            }
        }
        
        Object object;
	if (Settings.HFGC_ALL_ARRAYLETS && allocSpace!=stackAllocSpace()
	    && !(Settings.SCOPED_MEMORY && !heapArea(allocSpace))) {
            object=allocArraySlowImpl(
                allocSpace,typeData,numEle,eleSize,
                computeArrayPayloadSize(allocSpace,numEle,eleSize));
	} else if (Settings.SCOPED_MEMORY&&sharedArea(allocSpace)) {
//        LOG.info(LOG.DEBUG_MM, "Attempting Slow ArrayAlloc - MM.java:1494");
	    object=allocArraySlow(allocSpace,typeData,numEle);
        } else {
            Pointer unalignedSize=computeArraySize(allocSpace,numEle,eleSize);
            Pointer size=alignCoeff(unalignedSize,requiredSizeAlignment(),eleSize);

	    Pointer oldBump=
		CType.getPointer(
		    alloc,"fivmr_GCSpaceAlloc","bump");

	    Pointer result=align(oldBump.add(arrayAlignmentOffset()),
				 eleSize).sub(arrayAlignmentOffset());

	    Pointer newBump=result.add(size);

	    if (newBump.sub(CType.getPointer(alloc,"fivmr_GCSpaceAlloc","start"))
		.greaterThan(
		    (Settings.HFGC_FAIL_FAST_PATHS && allocSpace!=stackAllocSpace())
		    ?CType.getPointer(alloc,"fivmr_GCSpaceAlloc","zero")
		    :CType.getPointer(alloc,"fivmr_GCSpaceAlloc","size"))) {
		Magic.unlikely();
		if (Settings.INTERNAL_INST && !Settings.HFGC_ALL_ARRAYLETS) {
		    FIVMR_II_BEFORE_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
						     Magic.curFrame(),
						     typeData,
						     numEle);
		}
//        LOG.info(LOG.DEBUG_MM, "Attempting slow array alloc - MM.java:1520");
		object=allocArraySlow(allocSpace,typeData,numEle);
		if (Settings.INTERNAL_INST && !Settings.HFGC_ALL_ARRAYLETS) {
		    FIVMR_II_AFTER_ALLOC_ARRAY_SLOW(Magic.curThreadState(),
						    Magic.curFrame(),
						    typeData,
						    numEle);
		}
	    } else {
		if (allocSpace==stackAllocSpace()) {
		    libc.bzero(oldBump.sub(allocOffset()),
			       newBump.sub(oldBump));
		}
		
		CType.put(alloc,"fivmr_GCSpaceAlloc","bump",newBump);
		
		object=stampArray(result,allocSpace,allocFrame,typeData,
				  numEle,eleSize);
	    }
	}
        
        if (EXTREME_ASSERTS &&
            Settings.ASSERTS_ON &&
            Settings.HFGC &&
            allocSpace!=stackAllocSpace()) {
            if (Pointer.fromObject(object).and(Pointer.fromInt(chunkWidth()-1))!=Pointer.zero()) {
                throw abort("Misaligned object.");
            }
        }

        if (Settings.INTERNAL_INST) {
            FIVMR_II_AFTER_ALLOC_ARRAY(Magic.curThreadState(),
                                       Magic.curFrame(),
                                       typeData,
                                       numEle);
        }
	if (Settings.FLOW_LOGGING) {
	    FlowLog.log(FlowLog.TYPE_ALLOC, FlowLog.SUBTYPE_ARRAY,
			(((long)CType.getInt(typeData, "fivmr_TypeData",
					     "uniqueID")) << 32) | numEle,
			Pointer.fromObject(object).asLong());
	}
        return object;
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    static Pointer destructorSize() {
        return alignRaw(
            alignRaw(CType.sizeof("fivmr_Destructor"),Pointer.fromInt(Pointer.size())),
            requiredSizeAlignment());
    }
    
    @NoInline
    @NoPollcheck
    @AllowUnsafe
    private static Pointer allocDestructorSlow(Object o) {
        Pointer ts=Magic.curThreadState();
        ts.add(CType.offsetof("fivmr_ThreadState","roots")).store(Pointer.fromObject(o));
        Pointer result=fivmr_allocDestructorSlow(ts);
        ts.add(CType.offsetof("fivmr_ThreadState","roots")).store(Pointer.zero());
        return result;
    }
    
    private static final boolean DEBUG_DESTRUCTOR=false;
    
    // returns the object for convenience...
    @Inline
    @NoPollcheck
    @AllowUnsafe
    @Reflect
    public static Object addDestructor(int allocSpace,Object o) {
        if (Settings.FINALIZATION_SUPPORTED && allocSpace==objectSpace()) {
            if (Settings.SCOPED_MEMORY || DEBUG_DESTRUCTOR) {
                // let the native code deal with it
                fivmr_addDestructor(Magic.curThreadState(),o);
            } else {
                Pointer alloc=getAlloc(objectSpace());

                Pointer d=CType.getPointer(alloc,"fivmr_GCSpaceAlloc","bump");
            
                Pointer newBump=d.add(destructorSize());
            
                if (newBump.sub(CType.getPointer(alloc,"fivmr_GCSpaceAlloc","start")).greaterThan(
                        CType.getPointer(alloc,"fivmr_GCSpaceAlloc","size"))) {
                    // this may throw an exception, and it may safepoint.  but to help
                    // the baseline JIT, this contains the super special provision
                    // to save o in a place where the GC will find it.  the opt compiler
                    // could take advantage of this also.
                    d=allocDestructorSlow(o);
                } else {
                    CType.put(alloc,"fivmr_GCSpaceAlloc","bump",newBump);
                    d=d.sub(allocOffset());
                }
                
                CType.put(d,"fivmr_Destructor","object",Pointer.fromObject(o));
                CType.put(d,"fivmr_Destructor","next",
                          CType.getPointer(getGCData(),"fivmr_GCData","destructorHead"));
                CType.put(getGCData(),"fivmr_GCData","destructorHead",d);
                Pointer dTail=CType.getPointer(getGCData(),"fivmr_GCData","destructorTail");
                if (dTail==Pointer.zero()) {
                    CType.put(getGCData(),"fivmr_GCData","destructorTail",d);
                }
            }
        }
        return o;
    }
    
    @Inline
    @NoPollcheck
    public static int markBitsShift() {
        return Pointer.size()*8-2;
    }
    
    @Inline
    @NoPollcheck
    private static Pointer getInvCurShaded() {
        if (Settings.HFGC_FAIL_FAST_PATHS) {
            return CType.getPointer(getGCData(),"fivmr_GCData","zeroCurShaded");
        } else {
            return CType.getPointer(getGCData(),"fivmr_GCData","invCurShaded");
        }
    }
    
    @Inline
    @NoPollcheck
    private static boolean shouldMark(Object object,
                                      Pointer invCurShaded) {
        if (Settings.HFGC_FAIL_FAST_PATHS) {
            if (!Settings.HM_NARROW) {
                // FIXME: not sure this is necessary anymore.
                throw abort("Must use narrow header model to use HFGC_FAIL_FAST_PATHS");
            }
            return gcHeader(Pointer.fromObject(object)).loadPointer().ushr(markBitsShift())
                != invCurShaded;
        } else {
            return gcHeader(Pointer.fromObject(object)).loadPointer().ushr(markBitsShift())
                == invCurShaded;
        }
    }
    
    @Inline
    @NoPollcheck
    private static boolean shouldMark(Object object) {
        return shouldMark(object,getInvCurShaded());
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    @NoThrow
    private static void markImpl(Object object,
                                 Pointer invCurShaded) {
	if (needCMStoreBarrier()) {
	    if (object!=null && shouldMark(object,invCurShaded)) {
		Magic.unlikely();
		fivmr_GC_markSlow(Magic.curThreadState(),object);
	    }
	}
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    @NoThrow
    public static void mark(Object object) {
	if (needCMStoreBarrier()) {
            /* FIXME: have a way to force failure here.  or even better, just
               always run the barrier fully ... including the CAS. */
            
	    if (object!=null && shouldMark(object)) {
		Magic.unlikely();
                if (Settings.INTERNAL_INST) {
                    FIVMR_II_BEFORE_GCSTORE_SLOW(Magic.curThreadState(),
                                                 Magic.curFrame(),
                                                 Pointer.fromObject(object));
                }
		fivmr_GC_markSlow(Magic.curThreadState(),object);
                if (Settings.INTERNAL_INST) {
                    FIVMR_II_AFTER_GCSTORE_SLOW(Magic.curThreadState(),
                                                Magic.curFrame(),
                                                Pointer.fromObject(object));
                }
	    }
	}
    }
    
    @Inline
    @NoPollcheck
    private static Pointer canonicalizeScope(Pointer header) {
        if (header.greaterThanOrEqual(Pointer.fromInt(1).shl(Pointer.size()*8-2))) {
            return Pointer.fromIntSignExtend(-1);
        } else {
            return header;
        }
    }
    
    /**
     * Scope store check.  Note that this isn't particularly optimal.  It's
     * meant to be used "casually" not by the compiler.
     */
    @Inline
    @NoPollcheck
    @AllowUnsafe
    public static void scopeStoreCheck(Object target,
                                       Object source) {
        if (Settings.HAVE_SCOPE_CHECKS && source!=null) {
            Pointer th = gcHeader(Pointer.fromObject(target)).loadPointer();
            Pointer sh = gcHeader(Pointer.fromObject(source)).loadPointer();
            if (canonicalizeScope(sh).lessThan(canonicalizeScope(th))) {
                throw new javax.realtime.IllegalAssignmentError();
            }
        }
    }
    @Inline
    
    // FIXME: this is a complete misnomer.
    @NoPollcheck
    @AllowUnsafe
    public static boolean isInHeap(Object source) {
        return source!=null
            && (canonicalizeScope(gcHeader(Pointer.fromObject(source)).loadPointer())
                == Pointer.fromIntSignExtend(-1));
    }
    
    @Inline
    @NoPollcheck
    @AllowUnsafe
    public static boolean isInScope(Object source) {
        return source!=null
            && (canonicalizeScope(gcHeader(Pointer.fromObject(source)).loadPointer())
                != Pointer.fromIntSignExtend(-1));
    }
    
    @Inline
    @NoPollcheck
    @AllowUnsafe
    public static void inHeapCheck(Object source) {
        if (Settings.HAVE_SCOPE_CHECKS && source!=null) {
            if (canonicalizeScope(gcHeader(Pointer.fromObject(source)).loadPointer())
                != Pointer.fromIntSignExtend(-1)) {
                throw new javax.realtime.IllegalAssignmentError();
            }
        }
    }
    
    /**
     * Method called by application code, via calls inserted by the compiler,
     * whenever a store of a reference occurs.  Note that the target argument
     * will be null for static fields and guaranteed non-null for instance
     * fields - thus it is safe and efficient to determine if the field is
     * static based on a null check on target (though such a check could be
     * equivalently implemented by masking on the flags argument).  Note that
     * this method should never throw exceptions, allocate objects, or perform
     * field accesses.  Attempts to do so will result in either an internal
     * compiler error or else difficult-to-track runtime errors.
     * @param target   The target object under the store.  This is null for
     *                 static field accesses, and guaranteed non-null (as in,
     *                 the compiler will know it to be non-null) for instance
     *                 field accesses.
     * @param fieldPtr A pointer to the field.  Note that this pointer may
     *                 be in an entirely different region of memory than the
     *                 target pointer in the case of a fragmented object model.
     *                 Furthermore, this barrier will be used for array accesses
     *                 as well as field accesses; as such, this pointer is also
     *                 and "elementPtr" in addition to being a "fieldPtr".
     * @param newValue The new object reference being stored into fieldPtr.
     *                 This may be null.
     * @param flags    Flags describing the field.  Will be zero for array
     *                 elements.
     */
    // there is an efficiency concern here, sort of.  not for now, but there
    // will be, when we have a barrier that cares about for example
    // forward(target).add(fieldOffset).
    @Inline
    @NoPollcheck
    @NoSafepoint
    @Reflect
    @NoThrow
    public static void storeBarrier(Object target,
                                    Pointer fieldPtr,
                                    Object newValue,
                                    int flags) {
        if (Settings.PROFILE_GC_HEAVY) {
            fivmr_SPC_incBarrierFastPath();
        }
        if (Settings.INTERNAL_INST) {
            FIVMR_II_BEFORE_GCSTORE(Magic.curThreadState(),
                                    Magic.curFrame(),
                                    Pointer.fromObject(target),
                                    fieldPtr,
                                    Pointer.fromObject(newValue));
        }
	if (needCMStoreBarrier() &&
            (!Settings.FILTERED_CM_STORE_BARRIERS ||
             CType.getBoolean(getGCData(),"fivmr_GCData","tracing"))) {
	    if (Settings.GC_BLACK_STACK) {
		Pointer invCurShaded=getInvCurShaded();
		markImpl(fieldPtr.loadPointer().asObject(),invCurShaded);
		markImpl(newValue,invCurShaded);
	    } else {
		mark(newValue);
	    }
	}
        if (Settings.INTERNAL_INST) {
            FIVMR_II_AFTER_GCSTORE(Magic.curThreadState(),
                                   Magic.curFrame(),
                                   Pointer.fromObject(target),
                                   fieldPtr,
                                   Pointer.fromObject(newValue));
        }
    }
    
    @Inline
    @NoPollcheck
    @NoSafepoint
    @Reflect
    @NoThrow
    public static void assertMarked(Object value) {
        if (value!=null && shouldMark(value)) {
            Magic.notReached();
        }
    }
    
    /**
     * Barrier invoked by reads of Reference objects (i.e. java.lang.ref.*).
     * Not to be confused with reading a reference field or a reference array;
     * those do not have barriers.
     */
    @NoPollcheck
    public static Object referenceRead(Object o) {
        if (Settings.GC_BLACK_STACK) {
            mark(o);
        }
        return o;
    }

    @SuppressWarnings("unused")
    @Export
    @UseObjectsNotHandles
    @NoExecStatusTransition
    private static Object allocForNative(int allocSpace,
                                         Pointer td) {
	return alloc(allocSpace,td);
    }
    
    @SuppressWarnings("unused")
    @Export
    @UseObjectsNotHandles
    @NoExecStatusTransition
    private static Object allocArrayForNative(int allocSpace,
                                              Pointer td,
                                              int numEle) {
	return allocArray(allocSpace,td,numEle);
    }
    
    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_SPC_incAlloc();
    
    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_SPC_incBarrierFastPath();
    
    @RuntimeImport
    @NoSafepoint
    private static native void fivmr_SPC_incAllocSlowPath();
    
    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_BEFORE_GCSTORE(Pointer ts,
                                                       Pointer frame,
                                                       Pointer target,
                                                       Pointer fieldAddr,
                                                       Pointer source);
    
    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_AFTER_GCSTORE(Pointer ts,
                                                      Pointer frame,
                                                      Pointer target,
                                                      Pointer fieldAddr,
                                                      Pointer source);
    
    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_BEFORE_GCSTORE_SLOW(Pointer ts,
                                                            Pointer frame,
                                                            Pointer source);
    
    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_AFTER_GCSTORE_SLOW(Pointer ts,
                                                           Pointer frame,
                                                           Pointer source);

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_BEFORE_ALLOC(Pointer ts,
                                                     Pointer frame,
                                                     Pointer td);
    

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_AFTER_ALLOC(Pointer ts,
                                                    Pointer frame,
                                                    Pointer td);
    

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_BEFORE_ALLOC_SLOW(Pointer ts,
                                                          Pointer frame,
                                                          Pointer td);
    

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_AFTER_ALLOC_SLOW(Pointer ts,
                                                         Pointer frame,
                                                         Pointer td);
    

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_BEFORE_ALLOC_ARRAY(Pointer ts,
                                                           Pointer frame,
                                                           Pointer td,
                                                           int numEle);
    

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_AFTER_ALLOC_ARRAY(Pointer ts,
                                                          Pointer frame,
                                                          Pointer td,
                                                          int numEle);
    

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_BEFORE_ALLOC_ARRAY_SLOW(Pointer ts,
                                                                Pointer frame,
                                                                Pointer td,
                                                                int numEle);
    

    @RuntimeImport
    @NoSafepoint
    @UnsupUnlessSet({"INTERNAL_INST"})
    private static native void FIVMR_II_AFTER_ALLOC_ARRAY_SLOW(Pointer ts,
                                                               Pointer frame,
                                                               Pointer td,
                                                               int numEle);
}


