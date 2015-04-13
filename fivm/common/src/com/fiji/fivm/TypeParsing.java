/*
 * TypeParsing.java
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

package com.fiji.fivm;

import java.util.*;
import com.fiji.asm.Opcodes;
import com.fiji.asm.UTF8Sequence;
import static com.fiji.fivm.Constants.*;

public final class TypeParsing {
    private TypeParsing() {}
    
    public static class Failed extends RuntimeException {
        public Failed(String s) {
            super(s);
        }
        public Failed(String s,Throwable cause) {
            super(s,cause);
        }
    }
    
    public static class MethodSigStrings {
        String[] params;
        String result;
        
        MethodSigStrings() {}
        
        public MethodSigStrings(String[] params,
                                String result) {
            this.params=params;
            this.result=result;
        }
        
        public String[] params() {
            return params;
        }
        
        public int nParams() {
            return params.length;
        }
        
        public String param(int i) {
            return params[i];
        }
        
        public String result() {
            return result;
        }
        
        public int hashCode() {
            return Arrays.hashCode(params)+result.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof MethodSigStrings)) return false;
            MethodSigStrings other=(MethodSigStrings)other_;
            return Arrays.equals(params,other.params)
                && result.equals(other.result);
        }
        
        public String toString() {
            StringBuilder buf=new StringBuilder();
            buf.append("(");
            for (String param : params) {
                buf.append(param);
            }
            buf.append(")");
            buf.append(result);
            return buf.toString();
        }
    }
    
    public static int skipType(String str,int index) {
        try {
            switch (str.charAt(index)) {
            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
            case 'P':
            case 'f':
            case 'V':
                return index+1;
            case 'L': {
                int endIndex=str.indexOf(';',index);
                if (endIndex==-1) {
                    throw new Failed(
                        "Invalid object type descriptor; no semicolon at the end: "+
                        str.substring(index,str.length()));
                }
                return endIndex+1;
            }
            case '[':
                while (str.charAt(index)=='[') {
                    index++;
                }
                return skipType(str,index);
            default:
                throw new Failed("Invalid base type descriptor: "+
                                 str.charAt(index)+" in "+
                                 str.substring(index,str.length()));
            }
        } catch (IndexOutOfBoundsException e) {
            throw new Failed("Invalid type descriptor: "+
                             str.substring(index,str.length()),e);
        }
    }
    
    public static String purifyRefOnlyType(String str) {
        if (str.charAt(0)=='[') {
            return str;
        } else {
            return "L"+str+";";
        }
    }
    
    public static MethodSigStrings splitMethodSig(String methodSig) {
        try {
            if (methodSig.charAt(0)!='(') {
                throw new Failed(
                    "Invalid method signature descriptor; no leading paranthesis: "+
                    methodSig);
            }
            MethodSigStrings result=new MethodSigStrings();
            ArrayList< String > params=new ArrayList< String >();
            int index=1;
            while (methodSig.charAt(index)!=')') {
                int nextI=skipType(methodSig,index);
                params.add(methodSig.substring(index,nextI));
                index=nextI;
            }
            result.params=new String[params.size()];
            for (int i=0;i<result.params.length;++i) {
                result.params[i]=params.get(i);
            }
            index++;
            result.result=methodSig.substring(index,methodSig.length());
            return result;
        } catch (IndexOutOfBoundsException e) {
            throw new Failed("Invalid method signature descriptor: "+methodSig,e);
        }
    }
    
    public static String getMethodReturn(String methodSig) {
        if (methodSig.charAt(0)!='(') {
            throw new Failed(
                "Invalid method signature descriptor; no leading paranthesis: "+
                methodSig);
        }
        int end=methodSig.indexOf(')');
        if (end<0) {
            throw new Failed(
                "Invalid method signature descriptor; no ending paranthesis: "+
                methodSig);
        }
        if (end==methodSig.length()-1) {
            throw new Failed(
                "Invalid method signature descriptor; no return type: "+
                methodSig);
        }
        return methodSig.substring(end+1,methodSig.length());
    }
    
    public static class MethodSigSeqs {
        UTF8Sequence[] params;
        UTF8Sequence result;
        
        MethodSigSeqs() {}
        
        public MethodSigSeqs(UTF8Sequence[] params,
                             UTF8Sequence result) {
            this.params=params;
            this.result=result;
        }
        
        public UTF8Sequence[] params() {
            return params;
        }
        
        public int nParams() {
            return params.length;
        }
        
        public UTF8Sequence param(int i) {
            return params[i];
        }
        
        public UTF8Sequence result() {
            return result;
        }
        
        public int hashCode() {
            return Arrays.hashCode(params)+result.hashCode();
        }
        
        public boolean equals(Object other_) {
            if (this==other_) return true;
            if (!(other_ instanceof MethodSigSeqs)) return false;
            MethodSigSeqs other=(MethodSigSeqs)other_;
            return Arrays.equals(params,other.params)
                && result.equals(other.result);
        }
        
        public String toString() {
            StringBuilder buf=new StringBuilder();
            buf.append("(");
            for (UTF8Sequence param : params) {
                buf.append(param);
            }
            buf.append(")");
            buf.append(result);
            return buf.toString();
        }
    }
    
    public static int skipType(UTF8Sequence str,int index) {
        try {
            switch (str.byteAt(index)) {
            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
            case 'P':
            case 'f':
            case 'V':
                return index+1;
            case 'L': {
                int endIndex=str.indexOf((byte)';',index);
                if (endIndex==-1) {
                    throw new Failed(
                        "Invalid object type descriptor; no semicolon at the end: "+
                        str.subseq(index,str.byteLength()).toString());
                }
                return endIndex+1;
            }
            case '[':
                while (str.byteAt(index)=='[') {
                    index++;
                }
                return skipType(str,index);
            default:
                throw new Failed("Invalid base type descriptor: "+
                                 (char)str.byteAt(index)+" in "+
                                 str.subseq(index,str.byteLength()).toString());
            }
        } catch (IndexOutOfBoundsException e) {
            throw new Failed("Invalid type descriptor: "+
                             str.subseq(index,str.byteLength()).toString(),e);
        }
    }
    
    public static UTF8Sequence purifyRefOnlyType(UTF8Sequence str) {
        if (str.byteAt(0)=='[') {
            return str;
        } else {
            return UTF8Sequence.L.plus(str,UTF8Sequence.SEMI);
        }
    }
    
    public static MethodSigSeqs splitMethodSig(UTF8Sequence methodSig) {
        try {
            if (methodSig.byteAt(0)!='(') {
                throw new Failed(
                    "Invalid method signature descriptor; no leading paranthesis: "+
                    methodSig);
            }
            MethodSigSeqs result=new MethodSigSeqs();
            ArrayList< UTF8Sequence > params=new ArrayList< UTF8Sequence >();
            int index=1;
            while (methodSig.byteAt(index)!=')') {
                int nextI=skipType(methodSig,index);
                params.add(methodSig.subseq(index,nextI));
                index=nextI;
            }
            result.params=new UTF8Sequence[params.size()];
            for (int i=0;i<result.params.length;++i) {
                result.params[i]=params.get(i);
            }
            index++;
            result.result=methodSig.subseq(index,methodSig.byteLength());
            return result;
        } catch (IndexOutOfBoundsException e) {
            throw new Failed("Invalid method signature descriptor: "+methodSig,e);
        }
    }
    
    public static UTF8Sequence getMethodReturn(UTF8Sequence methodSig) {
        if (methodSig.byteAt(0)!='(') {
            throw new Failed(
                "Invalid method signature descriptor; no leading paranthesis: "+
                methodSig);
        }
        int end=methodSig.indexOf((byte)')');
        if (end<0) {
            throw new Failed(
                "Invalid method signature descriptor; no ending paranthesis: "+
                methodSig);
        }
        if (end==methodSig.byteLength()-1) {
            throw new Failed(
                "Invalid method signature descriptor; no return type: "+
                methodSig);
        }
        return methodSig.subseq(end+1,methodSig.byteLength());
    }
    
    public static int methodBindingFlagsFromBytecodeFlags(int access) {
        int binding=0;
        int kind=MBF_VIRTUAL;
        int sync=0;
        int impl=MBF_BYTECODE;
        int visibility=BF_PACKAGE;
                    
        if ((access&Opcodes.ACC_STATIC)!=0) {
            binding=BF_STATIC;
        }
        if ((access&Opcodes.ACC_PUBLIC)!=0) {
            visibility=BF_PUBLIC;
        }
        if ((access&Opcodes.ACC_PROTECTED)!=0) {
            visibility=BF_PROTECTED;
        }
        if ((access&Opcodes.ACC_PRIVATE)!=0) {
            visibility=BF_PRIVATE;
        }
        if ((access&Opcodes.ACC_FINAL)!=0) {
            kind=MBF_FINAL;
        }
        if ((access&Opcodes.ACC_ABSTRACT)!=0) {
            kind=MBF_ABSTRACT;
            impl=MBF_STUB;
        }
        if ((access&Opcodes.ACC_SYNCHRONIZED)!=0) {
            sync=MBF_SYNCHRONIZED;
        }
        if ((access&Opcodes.ACC_NATIVE)!=0) {
            impl=MBF_JNI;
        }
                    
        return binding|kind|sync|impl|visibility|MBF_EXISTS|(impl!=MBF_STUB?MBF_HAS_CODE:0);
    }
}


