/*
 * YetAnotherClass.java
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

public class YetAnotherClass {
    static public boolean stpredicate=true;
    static public boolean sfpredicate=false;
    
    static public byte sbnumber=(byte)23;
    
    static public char scnumber1=(char)100;
    
    static public char scnumber2=(char)50000;
    
    static public short ssnumber1=(short)100;
    
    static public short ssnumber2=(short)-10000;
    
    static public int snumber=1000000;
    
    static public long slnumber=-1000000000000l;
    
    static public float sfnumber=5.525f;
    
    static public double sdnumber=10.41247642;
    
    static public Object sobject="bar";
    
    static {
        System.out.println("all static fields stored.");
    }
    
    public boolean itpredicate;
    public boolean ifpredicate;
    
    public byte ibnumber;
    
    public char icnumber1;
    public char icnumber2;
    
    public short isnumber1;
    public short isnumber2;
    
    public int inumber;
    
    public long ilnumber;
    
    public float ifnumber;
    
    public double idnumber;
    
    public Object iobject;
    
    public YetAnotherClass() {
        itpredicate=true;
        ifpredicate=false;
        ibnumber=(byte)41;
        icnumber1=(char)200;
        icnumber2=(char)55000;
        isnumber1=(short)200;
        isnumber2=(short)-20000;
	inumber=42000000;
        ilnumber=65000000000l;
        ifnumber=5.425f;
        idnumber=10.347421;
        iobject="foo";
        System.out.println("all instance fields stored.");
    }
}


