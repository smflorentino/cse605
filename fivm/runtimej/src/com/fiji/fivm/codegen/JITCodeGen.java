/*
 * JITCodeGen.java
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

import java.nio.*;

import com.fiji.fivm.r1.*;

@ExcludeUnlessSet({"CLASSLOADING"})
public abstract class JITCodeGen {
    private static final int defaultStartSize=120;
    
    private static final int defaultStartSizeIfDownsizeSupported=2040;
    
    // we will resize by a factor of resizeNum/resizeDen and then subtract resizeDiff
    private static final int resizeNum=2;
    private static final int resizeDen=1;
    private static final int resizeDiff=8; // accounts for malloc header overhead

    Pointer methodRec;
    
    int startSize;
    int flags;
    
    MachineCode code;
    
    public JITCodeGen(Pointer methodRec,int startSize,int flags) {
        this.methodRec=methodRec;
        this.startSize=startSize;
        this.flags=flags;
    }
    
    public JITCodeGen(Pointer methodRec,int flags) {
        this(methodRec,
             MachineCode.supportDownsize()
             ?defaultStartSizeIfDownsizeSupported
             :defaultStartSize,
             flags);
    }
    
    protected abstract int generate(MachineCode code);
    
    protected void cleanUpOnFail() {
        // do nothing by default
    }
    
    public final MachineCode getCode() {
        if (code==null) {
            boolean succeeded=false;
            try {
                int curSize=startSize;
                code=new MachineCode(curSize,flags,methodRec);
                for (;;) {
                    try {
                        int actualSize=generate(code);
                        code.downsize(actualSize);
                        break;
                    } catch (BufferOverflowException e) {
                        cleanUpOnFail();
                        int newSize=curSize*resizeNum/resizeDen;
                        if (newSize-resizeDiff > curSize) {
                            newSize-=resizeDiff;
                        }
                        curSize=newSize;
                        code.destroy();
                        code=new MachineCode(curSize,flags,methodRec);
                    }
                }
                succeeded=true;
            } finally {
                if (code!=null) {
                    code.poke();
                }
                if (!succeeded) {
                    code.destroy();
                    code=null;
                }
            }
        }
        return code;
    }
}

