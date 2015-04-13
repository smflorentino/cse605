/*
 * StaticProfileCounterRepo.java
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
import java.io.*;

public class StaticProfileCounterRepo {
    private StaticProfileCounterRepo() {}
    
    static HashMap< String, RemoteCGlobal > map=
	new HashMap< String, RemoteCGlobal >();
    
    public static String toVarName(String name) {
	String shortName=name;
	if (shortName.length()>30) {
	    shortName=shortName.substring(0,30);
	}
	return "fivmc_SPC_"+Util.hidePunct(shortName)+"_"+Util.hash(name);
    }
    
    public static synchronized RemoteCGlobal get(String name) {
	RemoteCGlobal result=map.get(name);
	if (result==null) {
	    map.put(name,result=new RemoteCGlobal(
			Basetype.POINTER,toVarName(name)));
	}
	return result;
    }
    
    public static void genInc(Code code,Operation before,String name) {
	RemoteCGlobal g=get(name);
	Var v1=code.addVar(Exectype.POINTER);
	Var v2=code.addVar(Exectype.POINTER);
	before.prepend(
	    new CFieldInst(
		before.di(),OpCode.GetCVar,
		v1,Arg.EMPTY,
		g));
	before.prepend(
	    new SimpleInst(
		before.di(),OpCode.Add,
		v2,new Arg[]{v1,new PointerConst(1)}));
	before.prepend(
	    new CFieldInst(
		before.di(),OpCode.PutCVar,
		Var.VOID,new Arg[]{v2},
		g));
    }
    
    public static boolean enabled() {
        return !map.isEmpty();
    }
    
    public static LinkableSet allProfileData() {
	LinkableSet result=new LinkableSet();
	for (RemoteCGlobal g : map.values()) {
	    result.add(g.asLocal());
	}
	result.add(new LocalFunction("fivmc_SPC_dump",Basetype.VOID,Basetype.EMPTY){
		public LinkableSet subLinkables() {
		    LinkableSet result=new LinkableSet();
		    result.addAll(map.values());
		    return result;
		}
		public void generateCode(PrintWriter w) {
		    if (!map.isEmpty()) {
			ArrayList< String > keys=new ArrayList< String >(map.keySet());
			Collections.sort(keys);
			w.println("   fivmr_Log_lock();");
			w.println("   fivmr_Log_printf(\"Compiler Static Profile Counters:\\n\");");
			for (String name : keys) {
			    w.println("   fivmr_Log_printf(\"%30s: %\" PRIuPTR "+
				      "\"\\n\",\""+Util.cStringEscape(name)+"\","+
				      toVarName(name)+");");
			}
			w.println("   fivmr_Log_unlock();");
		    }
		}
	    });
        result.add(new LocalFunction("fivmc_SPC_numCounts",Basetype.INT,Basetype.EMPTY){
                public void generateCode(PrintWriter w) {
                    w.println("   return "+map.size()+";");
                }
            });
        result.add(new LocalFunction("fivmc_SPC_getCounts",
                                     Basetype.VOID,
                                     new Basetype[]{Basetype.POINTER}){
                public LinkableSet subLinkables() {
                    LinkableSet result=new LinkableSet();
                    result.addAll(map.values());
                    return result;
                }
                public void generateCode(PrintWriter w) {
                    if (!map.isEmpty()) {
			ArrayList< String > keys=new ArrayList< String >(map.keySet());
			Collections.sort(keys);
                        w.println("   uintptr_t **buffer;");
                        w.println("   int32_t idx;");
                        w.println("   buffer=(uintptr_t**)arg0;");
                        w.println("   idx=0;");
                        for (String name : keys) {
                            w.println("   buffer[idx++]=&"+toVarName(name)+";");
                        }
                    }
                }
            });
        result.add(new LocalFunction("fivmc_SPC_getNames",
                                     Basetype.VOID,
                                     new Basetype[]{Basetype.POINTER}){
                public void generateCode(PrintWriter w) {
                    if (!map.isEmpty()) {
			ArrayList< String > keys=new ArrayList< String >(map.keySet());
			Collections.sort(keys);
                        w.println("   char const **buffer;");
                        w.println("   int32_t idx;");
                        w.println("   buffer=(uintptr_t**)arg0;");
                        w.println("   idx=0;");
                        for (String name : keys) {
                            w.println("   buffer[idx++]=\""+Util.cStringEscape(name)+"\";");
                        }
                    }
                }
            });
	return result;
    }
}


