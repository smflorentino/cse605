/*
 * InstMethodCalls.java
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

package com.fiji.fivm.test;

public class InstMethodCalls {
    static class Methods {
        int x;
        
        public Methods(int value) {
            this.x=value;
        }
        
        public float intArgs(int a,int b,int c) {
            System.out.println(""+x+" "+a+" "+b+" "+c);
            return 42.5f;
        }
        
        public double boolArgs(boolean a,boolean b,boolean c) {
            System.out.println(""+x+" "+a+" "+b+" "+c);
            return 42.7;
        }
        
        public boolean floatArgs(float a,float b,float c) {
            System.out.println(""+x+" "+a+" "+b+" "+c);
            return false;
        }
        
        public boolean iffiArgs(int a,float b,float c,int d) {
            System.out.println(""+x+" "+a+" "+b+" "+c+" "+d);
            return true;
        }
        
        public byte fiifArgs(float a,int b,int c,float d) {
            System.out.println(""+x+" "+a+" "+b+" "+c+" "+d);
            return 5;
        }
        
        public short ijjiArgs(int a,long b,long c,int d) {
            System.out.println(""+x+" "+a+" "+b+" "+c+" "+d);
            return 10000;
        }
        
        public char jiijArgs(long a,int b,int c,long d) {
            System.out.println(""+x+" "+a+" "+b+" "+c+" "+d);
            return 40000;
        }
        
        public boolean jdibjdArgs(long a,double b,int c,byte d,long e,double f) {
            System.out.println(""+x+" "+a+" "+b+" "+c+" "+d+" "+e+" "+f);
            return false;
        }
        
        public boolean zbcsiArgs(boolean a,byte b,char c,short d,int e) {
            System.out.println(""+x+" "+a+" "+b+" "+(int)c+" "+d+" "+e);
            return true;
        }
        
        public boolean jzbcsdzbcsArgs(long a,boolean b,byte c,char d,short e,
                                      double f,boolean g,byte h,char i,short j) {
            System.out.println(""+x+" "+a+" "+b+" "+c+" "+(int)d+" "+e+" "+f+" "+g+" "+h+" "+(int)i+" "+j);
            return false;
        }
    }
    
    public static void main(String[] v) {
        Util.ensureEqual(new Methods(Integer.parseInt(v[0])).intArgs(
                             Integer.parseInt(v[1]),
                             Integer.parseInt(v[2]),
                             Integer.parseInt(v[3])),
                         42.5f);
        Util.ensureEqual(new Methods(Integer.parseInt(v[4])).boolArgs(
                             Boolean.parseBoolean(v[5]),
                             Boolean.parseBoolean(v[6]),
                             Boolean.parseBoolean(v[7])),
                         42.7);
        Util.ensureEqual(new Methods(Integer.parseInt(v[8])).floatArgs(
                             Float.parseFloat(v[9]),
                             Float.parseFloat(v[10]),
                             Float.parseFloat(v[11])),
                         false);
        Util.ensureEqual(new Methods(Integer.parseInt(v[12])).iffiArgs(
                             Integer.parseInt(v[13]),
                             Float.parseFloat(v[14]),
                             Float.parseFloat(v[15]),
                             Integer.parseInt(v[16])),
                         true);
        Util.ensureEqual(new Methods(Integer.parseInt(v[17])).fiifArgs(
                             Float.parseFloat(v[18]),
                             Integer.parseInt(v[19]),
                             Integer.parseInt(v[20]),
                             Float.parseFloat(v[21])),
                         (byte)5);
        Util.ensureEqual(new Methods(Integer.parseInt(v[22])).ijjiArgs(
                             Integer.parseInt(v[23]),
                             Long.parseLong(v[24]),
                             Long.parseLong(v[25]),
                             Integer.parseInt(v[26])),
                         (short)10000);
        Util.ensureEqual(new Methods(Integer.parseInt(v[27])).jiijArgs(
                             Long.parseLong(v[28]),
                             Integer.parseInt(v[29]),
                             Integer.parseInt(v[30]),
                             Long.parseLong(v[31])),
                         (char)40000);
        Util.ensureEqual(new Methods(Integer.parseInt(v[32])).jdibjdArgs(
                             Long.parseLong(v[33]),
                             Double.parseDouble(v[34]),
                             Integer.parseInt(v[35]),
                             Byte.parseByte(v[36]),
                             Long.parseLong(v[37]),
                             Double.parseDouble(v[38])),
                         false);
        Util.ensureEqual(new Methods(Integer.parseInt(v[39])).zbcsiArgs(
                             Boolean.parseBoolean(v[40]),
                             Byte.parseByte(v[41]),
                             (char)Integer.parseInt(v[42]),
                             Short.parseShort(v[43]),
                             Integer.parseInt(v[44])),
                         true);
        Util.ensureEqual(new Methods(Integer.parseInt(v[45])).jzbcsdzbcsArgs(
                             Long.parseLong(v[46]),
                             Boolean.parseBoolean(v[47]),
                             Byte.parseByte(v[48]),
                             (char)Integer.parseInt(v[49]),
                             Short.parseShort(v[50]),
                             Double.parseDouble(v[51]),
                             Boolean.parseBoolean(v[52]),
                             Byte.parseByte(v[53]),
                             (char)Integer.parseInt(v[54]),
                             Short.parseShort(v[55])),
                         false);
        System.out.println("that seemed to work.");
    }
}

