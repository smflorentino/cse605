/*
 * InterfaceCollisionTest.java
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

import com.fiji.fivm.util.CLStats;

public class InterfaceCollisionTest2 {
    static interface I01 {
        public void foo01();
    }
    
    static interface I02 {
        public void foo02();
    }
    
    static interface I03 {
        public void foo03();
    }
    
    static interface I04 {
        public void foo04();
    }
    
    static interface I05 {
        public void foo05();
    }
    
    static interface I06 {
        public void foo06();
    }
    
    static interface I07 {
        public void foo07();
    }
    
    static interface I08 {
        public void foo08();
    }
    
    static interface I09 {
        public void foo09();
    }
    
    static interface I10 {
        public void foo10();
    }
    
    static interface I11 {
        public void foo11();
    }
    
    static interface I12 {
        public void foo12();
    }
    
    static interface I13 {
        public void foo13();
    }
    
    static interface I14 {
        public void foo14();
    }
    
    static interface I15 {
        public void foo15();
    }
    
    static interface I16 {
        public void foo16();
    }
    
    static interface I17 {
        public void foo17();
    }
    
    static interface I18 {
        public void foo18();
    }
    
    static interface I19 {
        public void foo19();
    }
    
    static interface I20 {
        public void foo20();
    }
    
    static class C01 implements I01, I02, I03, I04, I05, I06, I07, I08, I09 {
        public void foo01() {
            System.out.println("C01.foo01 called");
        }
        public void foo02() {
            System.out.println("C01.foo02 called");
        }
        public void foo03() {
            System.out.println("C01.foo03 called");
        }
        public void foo04() {
            System.out.println("C01.foo04 called");
        }
        public void foo05() {
            System.out.println("C01.foo05 called");
        }
        public void foo06() {
            System.out.println("C01.foo06 called");
        }
        public void foo07() {
            System.out.println("C01.foo07 called");
        }
        public void foo08() {
            System.out.println("C01.foo08 called");
        }
        public void foo09() {
            System.out.println("C01.foo09 called");
        }
    }
    
    static class C02 implements I02, I03, I04, I05, I06, I07, I08, I09, I10 {
        public void foo02() {
            System.out.println("C02.foo02 called");
        }
        public void foo03() {
            System.out.println("C02.foo03 called");
        }
        public void foo04() {
            System.out.println("C02.foo04 called");
        }
        public void foo05() {
            System.out.println("C02.foo05 called");
        }
        public void foo06() {
            System.out.println("C02.foo06 called");
        }
        public void foo07() {
            System.out.println("C02.foo07 called");
        }
        public void foo08() {
            System.out.println("C02.foo08 called");
        }
        public void foo09() {
            System.out.println("C02.foo09 called");
        }
        public void foo10() {
            System.out.println("C02.foo10 called");
        }
    }
    
    static class C03 implements I01, I02, I03, I04, I05, I06, I07, I08, I09, I11, I12, I13, I14, I15, I16, I17, I18, I19 {
        public void foo01() {
            System.out.println("C03.foo01 called");
        }
        public void foo02() {
            System.out.println("C03.foo02 called");
        }
        public void foo03() {
            System.out.println("C03.foo03 called");
        }
        public void foo04() {
            System.out.println("C03.foo04 called");
        }
        public void foo05() {
            System.out.println("C03.foo05 called");
        }
        public void foo06() {
            System.out.println("C03.foo06 called");
        }
        public void foo07() {
            System.out.println("C03.foo07 called");
        }
        public void foo08() {
            System.out.println("C03.foo08 called");
        }
        public void foo09() {
            System.out.println("C03.foo09 called");
        }
        public void foo11() {
            System.out.println("C03.foo11 called");
        }
        public void foo12() {
            System.out.println("C03.foo12 called");
        }
        public void foo13() {
            System.out.println("C03.foo13 called");
        }
        public void foo14() {
            System.out.println("C03.foo14 called");
        }
        public void foo15() {
            System.out.println("C03.foo15 called");
        }
        public void foo16() {
            System.out.println("C03.foo16 called");
        }
        public void foo17() {
            System.out.println("C03.foo17 called");
        }
        public void foo18() {
            System.out.println("C03.foo18 called");
        }
        public void foo19() {
            System.out.println("C03.foo19 called");
        }
    }
    
    static class C04 implements I02, I03, I04, I05, I06, I07, I08, I09, I10, I12, I13, I14, I15, I16, I17, I18, I19, I20 {
        public void foo02() {
            System.out.println("C04.foo02 called");
        }
        public void foo03() {
            System.out.println("C04.foo03 called");
        }
        public void foo04() {
            System.out.println("C04.foo04 called");
        }
        public void foo05() {
            System.out.println("C04.foo05 called");
        }
        public void foo06() {
            System.out.println("C04.foo06 called");
        }
        public void foo07() {
            System.out.println("C04.foo07 called");
        }
        public void foo08() {
            System.out.println("C04.foo08 called");
        }
        public void foo09() {
            System.out.println("C04.foo09 called");
        }
        public void foo10() {
            System.out.println("C04.foo10 called");
        }
        public void foo12() {
            System.out.println("C04.foo12 called");
        }
        public void foo13() {
            System.out.println("C04.foo13 called");
        }
        public void foo14() {
            System.out.println("C04.foo14 called");
        }
        public void foo15() {
            System.out.println("C04.foo15 called");
        }
        public void foo16() {
            System.out.println("C04.foo16 called");
        }
        public void foo17() {
            System.out.println("C04.foo17 called");
        }
        public void foo18() {
            System.out.println("C04.foo18 called");
        }
        public void foo19() {
            System.out.println("C04.foo19 called");
        }
        public void foo20() {
            System.out.println("C04.foo20 called");
        }
    }
    
    public static void main(String[] v) {
        Object o01=new C01();
        System.out.println("First allocation: "+o01);
        Object o02=new C02();
        System.out.println("Second allocation: "+o02);
        Util.ensureEqual(o01.getClass(),C01.class);
        Util.ensureEqual(o02.getClass(),C02.class);
        Util.ensure(o01 instanceof C01);
        Util.ensure(!(o01 instanceof C02));
        Util.ensure(o01 instanceof I01);
        Util.ensure(o01 instanceof I02);
        Util.ensure(o01 instanceof I03);
        Util.ensure(o01 instanceof I04);
        Util.ensure(o01 instanceof I05);
        Util.ensure(o01 instanceof I06);
        Util.ensure(o01 instanceof I07);
        Util.ensure(o01 instanceof I08);
        Util.ensure(o01 instanceof I09);
        Util.ensure(!(o01 instanceof I10));
        Util.ensure(o02 instanceof C02);
        Util.ensure(!(o02 instanceof C01));
        Util.ensure(!(o02 instanceof I01));
        Util.ensure(o02 instanceof I02);
        Util.ensure(o02 instanceof I03);
        Util.ensure(o02 instanceof I04);
        Util.ensure(o02 instanceof I05);
        Util.ensure(o02 instanceof I06);
        Util.ensure(o02 instanceof I07);
        Util.ensure(o02 instanceof I08);
        Util.ensure(o02 instanceof I09);
        Util.ensure(o02 instanceof I10);
        System.out.println("(1) Assertions all succeeded.");
        Object o03=new C03();
        System.out.println("Third allocation: "+o03);
        Object o04=new C04();
        System.out.println("Fourth allocation: "+o04);
        Util.ensureEqual(o01.getClass(),C01.class);
        Util.ensureEqual(o02.getClass(),C02.class);
        Util.ensure(o01 instanceof C01);
        Util.ensure(!(o01 instanceof C02));
        Util.ensure(o01 instanceof I01);
        Util.ensure(o01 instanceof I02);
        Util.ensure(o01 instanceof I03);
        Util.ensure(o01 instanceof I04);
        Util.ensure(o01 instanceof I05);
        Util.ensure(o01 instanceof I06);
        Util.ensure(o01 instanceof I07);
        Util.ensure(o01 instanceof I08);
        Util.ensure(o01 instanceof I09);
        Util.ensure(!(o01 instanceof I10));
        Util.ensure(o02 instanceof C02);
        Util.ensure(!(o02 instanceof C01));
        Util.ensure(!(o02 instanceof I01));
        Util.ensure(o02 instanceof I02);
        Util.ensure(o02 instanceof I03);
        Util.ensure(o02 instanceof I04);
        Util.ensure(o02 instanceof I05);
        Util.ensure(o02 instanceof I06);
        Util.ensure(o02 instanceof I07);
        Util.ensure(o02 instanceof I08);
        Util.ensure(o02 instanceof I09);
        Util.ensure(o02 instanceof I10);
        Util.ensure(!(o01 instanceof C03));
        Util.ensure(!(o01 instanceof C04));
        Util.ensure(!(o01 instanceof I11));
        Util.ensure(!(o01 instanceof I12));
        Util.ensure(!(o01 instanceof I13));
        Util.ensure(!(o01 instanceof I14));
        Util.ensure(!(o01 instanceof I15));
        Util.ensure(!(o01 instanceof I16));
        Util.ensure(!(o01 instanceof I17));
        Util.ensure(!(o01 instanceof I18));
        Util.ensure(!(o01 instanceof I19));
        Util.ensure(!(o01 instanceof I20));
        Util.ensure(!(o02 instanceof C03));
        Util.ensure(!(o02 instanceof C04));
        Util.ensure(!(o02 instanceof I11));
        Util.ensure(!(o02 instanceof I12));
        Util.ensure(!(o02 instanceof I13));
        Util.ensure(!(o02 instanceof I14));
        Util.ensure(!(o02 instanceof I15));
        Util.ensure(!(o02 instanceof I16));
        Util.ensure(!(o02 instanceof I17));
        Util.ensure(!(o02 instanceof I18));
        Util.ensure(!(o02 instanceof I19));
        Util.ensure(!(o02 instanceof I20));
        Util.ensureEqual(o03.getClass(),C03.class);
        Util.ensureEqual(o04.getClass(),C04.class);
        Util.ensure(o03 instanceof C03);
        Util.ensure(!(o03 instanceof C04));
        Util.ensure(o03 instanceof I01);
        Util.ensure(o03 instanceof I02);
        Util.ensure(o03 instanceof I03);
        Util.ensure(o03 instanceof I04);
        Util.ensure(o03 instanceof I05);
        Util.ensure(o03 instanceof I06);
        Util.ensure(o03 instanceof I07);
        Util.ensure(o03 instanceof I08);
        Util.ensure(o03 instanceof I09);
        Util.ensure(!(o03 instanceof I10));
        Util.ensure(!(o03 instanceof C01));
        Util.ensure(!(o03 instanceof C02));
        Util.ensure(o03 instanceof I11);
        Util.ensure(o03 instanceof I12);
        Util.ensure(o03 instanceof I13);
        Util.ensure(o03 instanceof I14);
        Util.ensure(o03 instanceof I15);
        Util.ensure(o03 instanceof I16);
        Util.ensure(o03 instanceof I17);
        Util.ensure(o03 instanceof I18);
        Util.ensure(o03 instanceof I19);
        Util.ensure(!(o03 instanceof I20));
        Util.ensure(o04 instanceof C04);
        Util.ensure(!(o04 instanceof C03));
        Util.ensure(!(o04 instanceof I01));
        Util.ensure(o04 instanceof I02);
        Util.ensure(o04 instanceof I03);
        Util.ensure(o04 instanceof I04);
        Util.ensure(o04 instanceof I05);
        Util.ensure(o04 instanceof I06);
        Util.ensure(o04 instanceof I07);
        Util.ensure(o04 instanceof I08);
        Util.ensure(o04 instanceof I09);
        Util.ensure(o04 instanceof I10);
        Util.ensure(!(o04 instanceof C01));
        Util.ensure(!(o04 instanceof C02));
        Util.ensure(!(o04 instanceof I11));
        Util.ensure(o04 instanceof I12);
        Util.ensure(o04 instanceof I13);
        Util.ensure(o04 instanceof I14);
        Util.ensure(o04 instanceof I15);
        Util.ensure(o04 instanceof I16);
        Util.ensure(o04 instanceof I17);
        Util.ensure(o04 instanceof I18);
        Util.ensure(o04 instanceof I19);
        Util.ensure(o04 instanceof I20);
        System.out.println("(2) Assertions all succeeded.");
        ((I01)o01).foo01();
        ((I02)o01).foo02();
        ((I03)o01).foo03();
        ((I04)o01).foo04();
        ((I05)o01).foo05();
        ((I06)o01).foo06();
        ((I07)o01).foo07();
        ((I08)o01).foo08();
        ((I09)o01).foo09();
        ((I02)o02).foo02();
        ((I03)o02).foo03();
        ((I04)o02).foo04();
        ((I05)o02).foo05();
        ((I06)o02).foo06();
        ((I07)o02).foo07();
        ((I08)o02).foo08();
        ((I09)o02).foo09();
        ((I10)o02).foo10();
        ((I01)o03).foo01();
        ((I02)o03).foo02();
        ((I03)o03).foo03();
        ((I04)o03).foo04();
        ((I05)o03).foo05();
        ((I06)o03).foo06();
        ((I07)o03).foo07();
        ((I08)o03).foo08();
        ((I09)o03).foo09();
        ((I02)o04).foo02();
        ((I03)o04).foo03();
        ((I04)o04).foo04();
        ((I05)o04).foo05();
        ((I06)o04).foo06();
        ((I07)o04).foo07();
        ((I08)o04).foo08();
        ((I09)o04).foo09();
        ((I10)o04).foo10();
        ((I11)o03).foo11();
        ((I12)o03).foo12();
        ((I13)o03).foo13();
        ((I14)o03).foo14();
        ((I15)o03).foo15();
        ((I16)o03).foo16();
        ((I17)o03).foo17();
        ((I18)o03).foo18();
        ((I19)o03).foo19();
        ((I12)o04).foo12();
        ((I13)o04).foo13();
        ((I14)o04).foo14();
        ((I15)o04).foo15();
        ((I16)o04).foo16();
        ((I17)o04).foo17();
        ((I18)o04).foo18();
        ((I19)o04).foo19();
        ((I20)o04).foo20();
        System.out.println("Num bucket collisions: "+CLStats.numBucketCollisions());
        System.out.println("Num itable collisions: "+CLStats.numItableCollisions());
        System.out.println("Done!");
    }
}

