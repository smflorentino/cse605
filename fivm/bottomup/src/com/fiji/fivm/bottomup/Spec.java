/*
 * Spec.java
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

package com.fiji.fivm.bottomup;

import java.util.*;
import java.io.*;
import org.antlr.runtime.*;

public class Spec {
    public static File directory;
    
    public String fullClassname;

    public LinkedHashMap< String, Production > prods=
        new LinkedHashMap< String, Production >();
    
    private static Spec parse(File file) {
        System.err.println("Parsing "+file);
        
        try {
            ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(file));
            BottomUpVisitorSpecLexer lexer = new BottomUpVisitorSpecLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            BottomUpVisitorSpecParser parser = new BottomUpVisitorSpecParser(tokens);
            
            Spec s=parser.spec();
            
            Token t=tokens.LT(1);
            if (t!=Token.EOF_TOKEN) {
                System.err.println("Parsing failed; junk at end: "+t);
                System.exit(1);
            }
            
            return s;
        } catch (Throwable e) {
            throw new Error(e);
        }
    }
    
    public static Spec parseClass(String name) {
        File file=new File(name);
        directory=file.getParentFile();
        
        Spec s=parse(file);
        
        if (s.fullClassname==null) {
            System.err.println(name+" is not a class");
        }
        
        return s;
    }
    
    public static Spec parseModule(String name) {
        File f=new File(directory,name+".bu");
        Spec s=parse(f);
        
        if (s.fullClassname!=null) {
            System.err.println(f+" is not a module");
        }
        
        return s;
    }
    
    public void include(Spec other) {
        for (Map.Entry< String, Production > e : other.prods.entrySet()) {
            prods.put(e.getKey(),e.getValue());
        }
    }
    
    public void include(String name) {
        include(parseModule(name));
    }
}

