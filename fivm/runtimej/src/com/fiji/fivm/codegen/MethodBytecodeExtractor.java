/*
 * MethodBytecodeExtractor.java
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

import static com.fiji.fivm.r1.fivmRuntime.*;

import com.fiji.asm.ClassReader;
import com.fiji.asm.MethodVisitor;
import com.fiji.asm.UTF8Sequence;
import com.fiji.asm.commons.EmptyVisitor;

import com.fiji.fivm.TypeParsing;
import com.fiji.fivm.Constants;
import com.fiji.fivm.r1.CType;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.StackAllocation;
import com.fiji.fivm.r1.libc;

public final class MethodBytecodeExtractor {
    private Pointer methodRec;
    private UTF8Sequence expectedSig;
    private byte[] bytecode;
    private ClassReader reader;
    private int nStack;
    private int nLocals;
    private int nArgs;
    private UTF8Sequence name;
    private UTF8Sequence descriptor;
    private int flags;
    
    public MethodBytecodeExtractor(Pointer methodRec) {
        this.methodRec=methodRec;
        Pointer td=CType.getPointer(methodRec,"fivmr_MethodRec","owner");
        bytecode=java.lang.fivmSupport.getBytecode(td);
        if (2<=logLevel) {
            log(MethodBytecodeExtractor.class,2,
                "Got bytecode for "+getTypeName(td)+", and it has length = "+bytecode.length);
        }
        reader=new ClassReader(bytecode);
        nStack=CType.getChar(methodRec,"fivmr_MethodRec","nStack");
        nLocals=CType.getChar(methodRec,"fivmr_MethodRec","nLocals");
        name=fromCStringToSeq(fivmr_MethodRec_name(methodRec));
        descriptor=fromCStringToSeq(fivmr_MethodRec_descriptor(methodRec));
        flags=fivmr_MethodRec_flags(methodRec);
        if ((flags&Constants.MBF_ABSTRACT)!=0) {
            throw new CodeGenException(
                "Cannot parse bytecode of an abstract method");
        }
        if ((flags&Constants.MBF_JNI)!=0) {
            throw new CodeGenException(
                "Cannot parse bytecode of a JNI method");
        }
        if ((flags&Constants.BF_STATIC)==0) {
            nArgs++;
        }
        for (int i=fivmr_MethodRec_nparams(methodRec);i-->0;) {
            char typeCode=(char)
                fivmr_TypeData_name(fivmr_MethodRec_param(methodRec,i)).loadByte();
            if (typeCode=='P') {
                throw new CodeGenException("Pointer arguments are currently not supported");
            }
            nArgs+=Types.cells(typeCode);
        }
    }
    
    // this constructor is *only* for offline testing
    public MethodBytecodeExtractor(byte[] bytecode_,
                                   UTF8Sequence expectedSig_) {
        this.expectedSig=expectedSig_;
        this.bytecode=bytecode_;
        reader=new ClassReader(bytecode_);
        int lparen=expectedSig.indexOf((byte)'(');
        if (lparen<0) {
            throw new CodeGenException("Bad signature: "+expectedSig);
        }
        name=expectedSig.subseq(0,lparen);
        descriptor=expectedSig.subseq(lparen,expectedSig.byteLength());
        final boolean[] seen=new boolean[1];
        reader.accept(
            new EmptyVisitor() {
                public MethodVisitor visitMethod(int access,
                                                 UTF8Sequence name,
                                                 UTF8Sequence desc,
                                                 UTF8Sequence signature,
                                                 UTF8Sequence[] exceptions) {
                    UTF8Sequence fulldesc=name.plus(desc);
                    if (expectedSig.equals(fulldesc)) {
                        if (seen[0]) {
                            throw new CodeGenException("Seeing the same method more than once.");
                        }
                        seen[0]=true;
                        MethodBytecodeExtractor.this.flags=
                            TypeParsing.methodBindingFlagsFromBytecodeFlags(access);
                        if ((flags&Constants.MBF_ABSTRACT)!=0) {
                            throw new CodeGenException(
                                "Cannot parse bytecode of an abstract method");
                        }
                        if ((flags&Constants.MBF_JNI)!=0) {
                            throw new CodeGenException(
                                "Cannot parse bytecode of a JNI method");
                        }
                        return new EmptyVisitor() {
                            public void visitMaxs(int maxStack,
                                                  int maxLocals) {
                                MethodBytecodeExtractor.this.nStack=maxStack;
                                MethodBytecodeExtractor.this.nLocals=maxLocals;
                            }
                        };
                    } else {
                        return null;
                    }
                }
            },
            ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG);
        if (!seen[0]) {
            throw new CodeGenException("Failed to find requested code: "+expectedSig);
        }
        if ((flags&Constants.BF_STATIC)==0) {
            nArgs++;
        }
        for (UTF8Sequence s : TypeParsing.splitMethodSig(descriptor).params()) {
            nArgs+=Types.cells((char)s.byteAt(0));
        }
    }
    
    public byte[] bytecode() {
        return bytecode;
    }
    
    public int nStack() {
        return nStack;
    }
    
    public int nLocals() {
        return nLocals;
    }
    
    public int nArgs() {
        return nArgs;
    }
    
    public UTF8Sequence name() {
        return name;
    }
    
    public UTF8Sequence descriptor() {
        return descriptor;
    }
    
    public int flags() {
        return flags;
    }
    
    public boolean isSynchronized() {
        return (flags()&Constants.MBF_SYNCHRONIZED)==Constants.MBF_SYNCHRONIZED;
    }
    
    public boolean isInstance() {
        return (flags()&Constants.BF_STATIC)==0;
    }
    
    public Pointer typeContext() {
        return fivmr_TypeData_getContext(fivmr_MethodRec_owner(methodRec));
    }
    
    public static boolean doSuper(Pointer methodRec) {
        return (CType.getInt(
                    CType.getPointer(methodRec,"fivmr_MethodRec","owner"),
                    "fivmr_TypeData","flags") & Constants.TBF_NEW_SUPER_MODE) != 0;
    }
    
    public boolean doSuper() {
        return doSuper(methodRec);
    }
    
    public void extract(final MethodVisitor mv) {
        extract(mv,ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG);
    }
    
    public void extract(final MethodVisitor mv,
                        int asmFlags) {
        final boolean[] seen=new boolean[1];
        reader.accept(
            new EmptyVisitor() {
                @StackAllocation
                public MethodVisitor visitMethod(int access,
                                                 UTF8Sequence name,
                                                 UTF8Sequence desc,
                                                 UTF8Sequence signature,
                                                 UTF8Sequence[] exceptions) {
                    boolean predicate;
                    if (expectedSig!=null) {
                        UTF8Sequence fulldesc=name.plus(desc);
                        predicate=expectedSig.equals(fulldesc);
                    } else if (methodRec!=Pointer.zero()) {
                        if (2<=logLevel) {
                            log(MethodBytecodeExtractor.class,2,
                                "Testing "+fromCStringFull(fivmr_MethodRec_describe(methodRec))+
                                " against "+name+desc);
                        }
                        predicate=
                            libc.strcmp(
                                fivmr_MethodRec_name(methodRec),
                                getCStringFullStack(name))==0 &&
                            fivmr_MethodRec_matchesSig(
                                methodRec,
                                getCStringFullStack(desc));
                    } else {
                        throw new CodeGenException("This should not happen.");
                    }
                    if (predicate) {
                        if (seen[0]) {
                            throw new CodeGenException("Seeing the same method more than once.");
                        }
                        seen[0]=true;
                        return mv;
                    } else {
                        return null;
                    }
                }
            },
            asmFlags);
        if (!seen[0]) {
            throw new CodeGenException(
                "Failed to find requested code: "+
                (expectedSig==null?fromCStringFull(fivmr_MethodRec_describe(methodRec)):expectedSig));
        }
    }
    
    public static void extract(final Pointer methodRec,
                               final MethodVisitor mv,
                               int asmFlags) {
        new MethodBytecodeExtractor(methodRec).extract(mv,asmFlags);
    }
    
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append("MethodBytecodeExtractor[");
        buf.append("L"+reader.getClassName()+";/");
        if (expectedSig!=null) {
            buf.append(expectedSig);
        } else if (methodRec!=Pointer.zero()) {
            buf.append(fromCStringFull(fivmr_MethodRec_describe(methodRec)));
        }
        buf.append(" nStack="+nStack);
        buf.append(" nLocals="+nLocals);
        buf.append(" nArgs="+nArgs);
        buf.append("]");
        return buf.toString();
    }
}

