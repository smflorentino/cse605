/*
 * StringRepository.java
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

import java.util.*;

public class StringRepository {
    private StringRepository() {}
    
    static DataBuilder db;
    static HashMap< String, Integer > stringToIndex=
	new HashMap< String, Integer >();
    static ArrayList< String > allStrings=
        new ArrayList< String >();
    
    public static void setDataBuilder(DataBuilder db) {
	StringRepository.db=db;
    }
    
    public static synchronized int register(String s) {
	Integer index=stringToIndex.get(s);
	if (index==null) {
	    index=stringToIndex.size();
	    stringToIndex.put(s,index);
            allStrings.add(s);
	}
	return index;
    }
    
    public static Pointerable pointerTo(String s) {
        return new DisplacedLink(
            CTypesystemReferences.generated_stringTable,
            (int)(Global.allocOffset+
                  Global.root().stringClass.alignedPayloadSize()*register(s)));
    }
    
    public static String mangledName(String s) {
        return "StrConst_"+Util.hidePunct(s.substring(0,Math.min(s.length(),12)))+"_"+Util.hash(s);
    }
    
    public static Arg generateIRFor(Code code,Operation before,String s) {
        return Util.generateIRForTableLoad(
            code,
            before,
            register(s),
            Global.root().stringClass,
            CTypesystemReferences.Payload_stringTable,
            CTypesystemReferences.generated_stringTable);
    }
    
    public static ArrayList< String > allStrings() {
	return allStrings;
    }
    
    private static LocalDataConstant stringIndexLDC() {
        ArrayList< String > strings=new ArrayList< String >(allStrings());
        Collections.sort(strings);
        ArrayList< Object > data=new ArrayList< Object >();
        for (String s : strings) {
            data.add(pointerTo(s));
        }
        return new LocalDataConstant(CTypesystemReferences.generated_stringIndex_name,data);
    }
    
    public static int numStrings() {
        return allStrings.size();
    }
    
    public static final class StringLayout {
        StringLayout() {}
        
        public int[] indices;
        public String array;
    }
    
    public static StringLayout buildLayout() {
        // this could be optimized to reduce the size of the string backing
        // store array...
        
        StringLayout result=new StringLayout();
        
        result.indices=new int[allStrings.size()];
        
        StringBuilder builder=new StringBuilder();
        
        for (int i=0;i<allStrings.size();++i) {
            String s=allStrings.get(i);
            
            result.indices[i]=builder.length();
            builder.append(s);
        }

        result.array=builder.toString();
        
        return result;
    }
    
    public static DataBuilder getDataBuilder() { return db; }
    
    public static abstract class DataBuilder {
	public abstract LinkableSet build(String stConstName,
                                          String arrayConstName);
    }
    
    public static void printStats() {
	int n=0;
	int chars=0;
	for (String s : allStrings()) {
	    n++;
	    chars+=s.length();
	}
	Global.log.println("Number of strings: "+n);
	Global.log.println("Number of characters: "+chars);
    }
    
    public static LinkableSet all() {
        LinkableSet result=new LinkableSet();
        result.addAll(db.build(CTypesystemReferences.generated_stringTable_name,
                               CTypesystemReferences.generated_stringDataArray_name));
        result.add(stringIndexLDC());
        return result;
    }
}

