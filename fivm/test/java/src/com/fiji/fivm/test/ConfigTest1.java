/*
 * ConfigTest1.java
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

import com.fiji.config.*;
import com.fiji.util.*;
import com.fiji.fivm.*;
import java.io.*;

public class ConfigTest1 {
    public static void main(String[] v) throws Exception {
        testMap1();
        testMap2();
        testList1();
        testList2();
        testList3();
        testList4();
        testList5();
        testList6();
        testList7();
        testParse1();
        testParse2();
        testParse3();
        testParse4();
        testParse5();
        testParse6();
        testAtom1();
    }
    
    public static void testMap1() {
        ConfigMapNode conf=new ConfigMapNode();
        conf.put("this","that");
        Util.ensure(!conf.isEmpty());
        Util.ensureEqual(conf.size(),1);
        Util.ensureEqual(conf.getString("this"),"that");
        Util.ensure(conf.has("this"));
        Util.ensure(!conf.has("that"));
        System.out.println(conf.toString());
        Util.ensureEqual(conf.toString(),
                         "{this=that;}");
        System.out.println(conf.toPrettyString());
        ConfigMapNode conf2=ConfigMapNode.parse(conf.toPrettyString());
        Util.ensureEqual(conf,conf2);
        Util.ensureEqual(conf2,conf);
        Util.ensureEqual(conf.toString(),conf2.toString());
        Util.ensureEqual(conf.toPrettyString(),conf2.toPrettyString());
        Util.ensure(!conf2.isEmpty());
        Util.ensureEqual(conf2.size(),1);
        Util.ensureEqual(conf2.getString("this"),"that");
        Util.ensure(conf2.has("this"));
        Util.ensure(!conf2.has("that"));
        System.out.println(conf2.toString());
        System.out.println(conf2.toPrettyString());
        ConfigMapNode conf3=ConfigMapNode.parse(conf.toString());
        Util.ensureEqual(conf,conf3);
        Util.ensureEqual(conf3,conf);
        Util.ensureEqual(conf2,conf3);
        Util.ensureEqual(conf3,conf2);
    }
    
    public static void testMap2() {
        ConfigMapNode conf=new ConfigMapNode();
        conf.put("foo bar",42);
        conf.put("this","that");
        conf.put("awesome",true);
        conf.put("something","something else");
        conf.put("stuff","Just some random text, thanks.\n");
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testList1() {
        ConfigListNode conf=new ConfigListNode();
        for (int i=0;i<10;++i) {
            conf.append(i);
        }
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testList2() {
        ConfigListNode conf=new ConfigListNode();
        for (int i=0;i<30;++i) {
            conf.append(i);
        }
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testList3() {
        ConfigListNode conf=new ConfigListNode();
        for (int i=0;i<100;++i) {
            conf.append(i);
        }
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testList4() {
        ConfigListNode conf=new ConfigListNode();
        for (int i=0;i<50;++i) {
            ConfigListNode conf2=new ConfigListNode();
            for (int j=0;j<20;++j) {
                conf2.append(i+j);
            }
            conf.append(conf2);
        }
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testList5() {
        ConfigListNode conf=new ConfigListNode();
        for (int i=0;i<50;++i) {
            ConfigListNode conf2=new ConfigListNode();
            for (int j=0;j<5;++j) {
                conf2.append(i+j);
            }
            conf.append(conf2);
        }
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testList6() {
        ConfigListNode conf=new ConfigListNode();
        for (int i=0;i<50;++i) {
            ConfigListNode conf2=new ConfigListNode();
            for (int j=0;j<2;++j) {
                conf2.append(i+j);
            }
            conf.append(conf2);
        }
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testList7() {
        ConfigListNode conf=new ConfigListNode();
        for (int i=0;i<50;++i) {
            ConfigMapNode conf2=new ConfigMapNode();
            conf2.put("foo","bar");
            conf.append(conf2);
        }
        System.out.println(conf.toPrettyString());
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
    }
    
    public static void testParse1() {
        ConfigMapNode conf=ConfigMapNode.parse(
            "# this is a comment\n"+
            "{\n"+
            "  1 = 2  # this is another comment\n"+
            "  this = that\n"+
            "}");
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
        Util.ensure(conf.has("1"));
        Util.ensure(conf.has("this"));
        Util.ensure(!conf.isEmpty());
        Util.ensureEqual(conf.size(),2);
        Util.ensureEqual(conf.getString("1"),"2");
        Util.ensureEqual(conf.getInt("1"),2);
        Util.ensureEqual(conf.getString("this"),"that");
        System.out.println(conf.toPrettyString());
    }
    
    public static void testParse2() {
        ConfigAtomNode conf=ConfigAtomNode.parse(
            "\"hello, world!\"            ");
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
        Util.ensureEqual(conf.getString(),"hello, world!");
        System.out.println(conf.toPrettyString());
    }
    
    public static void testParse3() {
        ConfigMapNode conf=ConfigMapNode.parse(
            "{"+
            "  message = \"\\150\\x65llo, wor\\154\\x64\\041\""+
            "}");
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
        Util.ensureEqual(conf.getString("message"),"hello, world!");
        System.out.println(conf.toPrettyString());
    }
    
    public static void testParse4() {
        ConfigMapNode conf=ConfigMapNode.parse(
            "{"+
            "  message = \"\\001\\xde\\364\""+
            "}");
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
        System.out.println(conf.toPrettyString());
    }
    
    public static void testParse5() {
        ConfigAtomNode conf=ConfigAtomNode.parse("\"\\n\\r\\t\"");
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
        Util.ensureEqual(conf.getString(),"\n\r\t");
        System.out.println(conf.toPrettyString());
    }
    
    public static void testParse6() {
        ConfigAtomNode conf=ConfigAtomNode.parse("\"\\N\\R\\T\"");
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
        Util.ensureEqual(conf.getString(),"\n\r\t");
        System.out.println(conf.toPrettyString());
    }
    
    public static void testAtom1() throws IOException {
        String origStr=
            new String(
                UTF8.decode(
                    FileUtils.readCompletely("test/data/testfile-utf8.txt")));
        ConfigAtomNode conf=new ConfigAtomNode(origStr);
        Util.ensureEqual(conf,ConfigNode.parse(conf.toString()));
        Util.ensureEqual(conf,ConfigNode.parse(conf.toPrettyString()));
        ConfigAtomNode conf2=ConfigAtomNode.parse(conf.toString());
        Util.ensureEqual(conf2.getString(),origStr);
        ConfigAtomNode conf3=ConfigAtomNode.parse(conf.toPrettyString());
        Util.ensureEqual(conf3.getString(),origStr);
        System.out.println(conf.toPrettyString());
    }
}

