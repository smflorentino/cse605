/*
 * FragmentedClasspathObjectModel.java
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

import com.fiji.fivm.*;
import java.io.*;
import java.util.*;

public class FragmentedClasspathObjectModel {
    private FragmentedClasspathObjectModel() {}
    
    public static void init(RootsRepo rootsRepo) {
	assert Global.om==ObjectModel.FRAGMENTED;
	assert Global.hm.numHeaderWords()==1;
	
	// this matches fivmr_main.c.  we put this here instead of, for example,
	// RootsRepo's constructor, because this is very much Classpath-specific.
	// FIXME: move this to a Classpath-specific, but not object-model-specifc,
	// class.
	rootsRepo.rootRoots().use("Ljava/lang/System;");
	rootsRepo.rootRoots().use("Ljava/lang/String;");
	rootsRepo.rootRoots().use("Ljava/security/VMAccessController;");
	rootsRepo.rootRoots().use("Ljava/lang/Character;");
	rootsRepo.rootRoots().use("Ljava/lang/Math;");

	if (Global.verbosity>=1) {
	    Global.log.println("Using fragmented classpath object model.");
	}
        
	StringRepository.setDataBuilder(new StringRepository.DataBuilder() {
                public LinkableSet build(String stConstName,
                                         String arrayConstName) {
                    LinkableSet result=new LinkableSet();
                    
                    StringRepository.StringLayout layout=
                        StringRepository.buildLayout();
                    
                    ArrayList< Object > arrData=new ArrayList< Object >();
                    
                    for (int i=0;i<layout.array.length();++i) {
                        arrData.add(
                            new Character(layout.array.charAt(i)));
                    }
                    
                    Pointerable arrPtr=
                        Util.buildArraylet(arrayConstName,
                                           arrData,
                                           Type.CHAR.makeArray());
                    result.add(arrPtr);
                    
                    ArrayList< Object > totalStrData=new ArrayList< Object >();
                    
                    ArrayList< String > strings=StringRepository.allStrings();
                    for (int i=0;i<strings.size();++i) {
                        ArrayList< Object > objData=new ArrayList< Object >();
                        
                        String s=strings.get(i);
                        int charIndex=layout.indices[i];
                        
                        Util.buildHeader(objData,Global.root().stringType);

                        // final char[] value
                        objData.add(arrPtr);
		    
                        // final int count
                        objData.add(
                            new Integer(s.length()));
		    
                        // int hashCode
                        // computed based on the JDK documentation.
                        // it's valid for Classpath and OpenJDK, but apparently
                        // not for Harmony.
                        int hash=0;
                        int multiplier=1;
                        for (int j=s.length(); j-->0;) {
                            hash+=multiplier*s.charAt(j);
                            multiplier*=31;
                        }
                        objData.add(
                            new Integer(hash));
		    
                        // final int offset
                        objData.add(
                            new Integer(charIndex));
                        
                        totalStrData.addAll(
                            Util.buildFragmentedObjData(
                                new DisplacedLink(
                                    new RemoteDataConstant(stConstName),
                                    Global.root().stringClass.alignedPayloadSize()*i),
                                objData));
                    }
                    
                    result.add(new LocalDataConstant(stConstName,
                                                     totalStrData));
                    
                    return result;
                }
	    });
	
	ClassRepository.setDataBuilder(new ClassRepository.DataBuilder() {
                public LocalDataConstant build(String name) {
                    boolean hasSigners=false;
                    boolean hasPD=false;
                    boolean hasConstructor=false;
                    
                    if (Global.lib==Library.GLIBJ) {
                        hasSigners=
                            Global.root().resolveField(
                                "Ljava/lang/Class;/signers/[Ljava/lang/Object;").shouldExist();
                        hasPD=
                            Global.root().resolveField(
                                "Ljava/lang/Class;/pd/Ljava/security/ProtectionDomain;").shouldExist();
                        hasConstructor=
                            Global.root().resolveField(
                                "Ljava/lang/Class;/constructor/Ljava/lang/reflect/Constructor;").shouldExist();
                    }
                    
                    ArrayList< Object > data=new ArrayList< Object >();

                    int cnt=0;
                    for (Type t : Global.allResolvedTypesUsedAtRuntime()) {
                        ArrayList< Object > objData=new ArrayList< Object >();
                        
                        Util.buildHeader(objData,Global.root().classType); // three words
                        
                        if (hasSigners) objData.add(Pointer.NULL); // signers
                        if (hasPD) objData.add(Pointer.NULL); // protection domain
                        
                        objData.add(TypeData.forType(t)); // vmdata
                        
                        if (hasConstructor) objData.add(Pointer.NULL); // constructor
                        
                        // NOTE: assuming (correctly) that we're pointer-aligned up to
                        // this point.  but this assumption may break if you have some
                        // other kind of fields in here.
                        data.addAll(
                            Util.buildFragmentedObjData(
                                new DisplacedLink(
                                    new RemoteDataConstant(name),
                                    Global.root().classClass.alignedPayloadSize()*(cnt++)),
                                objData));
                    }
                    
                    return new LocalDataConstant(name,data);
                }
	    });
	
        BytecodeRepository.setDataBuilder(new BytecodeRepository.DataBuilder() {
                public Linkable build(String symName) {
                    return new Linkable(Basetype.VOID,symName){
                        public boolean isLocal() {
                            return true;
                        }
                        
                        public LinkableSet subLinkables() {
                            LinkableSet result=new LinkableSet();
                            result.add(TypeData.forType(Type.BYTE.makeArray()));
                            return result;
                        }
                        
                        public void generateDeclaration(PrintWriter w) {
                            w.println("extern uintptr_t "+getName()+"["+
                                      (BytecodeRepository.curOffset()/Global.pointerSize)+
                                      "];");
                        }
                        
                        public void generateDefinition(PrintWriter w) {
                            w.print("uintptr_t "+getName()+"["+
                                    (BytecodeRepository.curOffset()/Global.pointerSize)+
                                    "] = {");
                            boolean first=true;
                            for (VisibleClass c : BytecodeRepository.classesInOrder()) {
                                if (first) {
                                    first=false;
                                } else {
                                    w.print(",");
                                }
                                
                                w.print(
                                    new DisplacedLink(
                                        this,
                                        FragmentedObjectRepresentation.arrayHeaderSize).asCCode());
                                w.print(",");
                                if (Global.gc.hasCMHeader()) {
                                    w.print(new Pointer(
                                                ((long)Constants.CMR_GC_ALWAYS_MARKED)
                                                << (Global.pointerSize*8-2)).asCCode());
                                } else {
                                    w.print(new Pointer((long)-1).asCCode());
                                }
                                w.print(',');
                                if (Global.hm==HeaderModel.POISONED) {
                                    w.print(
                                        new TaggedLink(
                                            TypeData.forType(Type.BYTE.makeArray()),
                                            1).asCCode());
                                } else {
                                    w.print(
                                        TypeData.forType(Type.BYTE.makeArray()).asCCode());
                                }
                                
                                byte[] bytecode=c.purifiedBytecode();
                                
                                PackedNumberPrinter pnp=
                                    PackedNumberPrinter.make(w);
                                pnp.addInt(bytecode.length);
                                for (int i=0;i<bytecode.length;++i) {
                                    pnp.addByte(bytecode[i]);
                                }
                                pnp.flush();
                            }
                            w.println("};");
                        }
                    };
                }
                
                public long size(VisibleClass klass) {
                    return Global.pointerSize*3+4+
                        ((((long)klass.purifiedBytecode().length)+Global.pointerSize-1)
                         &~(Global.pointerSize-1));
                }
            });

	rootsRepo.rootRoots().access("Ljava/lang/Class;/vmdata/Ljava/lang/Object;");
	rootsRepo.rootRoots().access("Ljava/lang/String;/value/[C");
	rootsRepo.rootRoots().access("Ljava/lang/String;/count/I");
	rootsRepo.rootRoots().access("Ljava/lang/String;/cachedHashCode/I");
	rootsRepo.rootRoots().access("Ljava/lang/String;/offset/I");
    }
}


