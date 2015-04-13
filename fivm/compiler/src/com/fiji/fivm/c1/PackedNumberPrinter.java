/*
 * PackedNumberPrinter.java
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

public abstract class PackedNumberPrinter {
    protected PrintWriter w;
    
    protected PackedNumberPrinter(PrintWriter w) {
        this.w=w;
    }

    public abstract void flush();
    
    public abstract void addInt(int x);
    
    public abstract void addByte(byte x);
    
    public static PackedNumberPrinter make(PrintWriter w) {
        if (Global.pointerSize==4) {
            return new _32Bit(w);
        } else if (Global.pointerSize==8) {
            return new _64Bit(w);
        } else {
            assert false;
            return null;
        }
    }
    
    static class _32Bit extends PackedNumberPrinter {
        int current;
        int n;
        
        _32Bit(PrintWriter w) {
            super(w);
        }
        
        private void flushImpl() {
            int value;
            if (Settings.IS_BIG_ENDIAN) {
                value=
                    ((current&0x000000ff)<<24) |
                    ((current&0x0000ff00)<<8)  |
                    ((current&0x00ff0000)>>>8) |
                    ((current&0xff000000)>>>24);
            } else {
                value=current;
            }
            
            w.print(",INT32_C(");
            w.print(value);
            w.print(")");
            
            current=0;
            n=0;
        }
        
        public void flush() {
            if (n!=0) {
                flushImpl();
            }
        }
        
        public void addInt(int x) {
            assert n==0;
            current=x;
            n=4;
            flushImpl();
        }
        
        public void addByte(byte x) {
            assert n>=0 && n<=3;
            current|=((((int)x)&0xff)<<(n<<3));
            n++;
            if (n==4) {
                flushImpl();
            }
        }
    }
    
    static class _64Bit extends PackedNumberPrinter {
        long current;
        int n;
        
        _64Bit(PrintWriter w) {
            super(w);
        }
        
        private void flushImpl() {
            long value;
            if (Settings.IS_BIG_ENDIAN) {
                value=
                    ((current&0x00000000000000ffl)<<56)  |
                    ((current&0x000000000000ff00l)<<40)  |
                    ((current&0x0000000000ff0000l)<<24)  |
                    ((current&0x00000000ff000000l)<<8)   |
                    ((current&0x000000ff00000000l)>>>8)  |
                    ((current&0x0000ff0000000000l)>>>24) |
                    ((current&0x00ff000000000000l)>>>40) |
                    ((current&0xff00000000000000l)>>>56);
            } else {
                value=current;
            }
            
            w.print(",INT64_C(");
            w.print(value);
            w.print(")");
            
            current=0;
            n=0;
        }
        
        public void flush() {
            if (n!=0) {
                flushImpl();
            }
        }
        
        public void addInt(int x) {
            assert n==0 || n==4;
            current|=((((long)x)&0xffffffffl)<<(n<<3));
            n+=4;
            if (n==8) {
                flushImpl();
            }
        }
        
        public void addByte(byte x) {
            assert n>=0 && n<=7;
            current|=((((long)x)&0xff)<<(n<<3));
            n++;
            if (n==8) {
                flushImpl();
            }
        }
    }
}

