/*
 * VerifiabilityRepo.java
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

import com.fiji.config.*;
import java.util.*;

public class VerifiabilityRepo {
    private VerifiabilityRepo() {}
    
    private static LinkedHashMap< CodeOrigin, VerifiabilityMode > map=
        new LinkedHashMap< CodeOrigin, VerifiabilityMode >();
    
    private static LinkedHashMap< CodeOrigin, VerifiabilityReport > reports=
        new LinkedHashMap< CodeOrigin, VerifiabilityReport >();
    
    public static synchronized void put(CodeOrigin origin,
                                        VerifiabilityMode mode,
                                        VerifiabilityReport report) {
        assert mode!=null;
        if (report==null) {
            if (mode!=VerifiabilityMode.VERIFIABLE) {
                throw new CompilerException("For "+origin.shortName()+": supplying "+mode+" requires a non-null report.");
            }
        } else {
            if (mode==VerifiabilityMode.VERIFIABLE) {
                throw new CompilerException("For "+origin.shortName()+": supplying "+mode+" requires a null report.");
            }
        }
        if (map.containsKey(origin)) {
            throw new CompilerException(origin.shortName()+" already has verifiability mode");
        }
        map.put(origin,mode);
        if (report!=null) {
            assert !reports.containsKey(origin);
            reports.put(origin,report);
        }
    }
    
    public static void put(Code c,
                           VerifiabilityMode mode,
                           VerifiabilityReport report) {
        put(c.origin(),mode,report);
    }
    
    public static ConfigNode results() {
        ConfigMapNode result=new ConfigMapNode();
        
        for (Context ctx : Global.contextList()) {
            ConfigMapNode mapForCtx=new ConfigMapNode();
            result.put(ctx.description(),mapForCtx);
            
            for (VerifiabilityMode mode : VerifiabilityMode.class.getEnumConstants()) {
                if (mode!=VerifiabilityMode.VERIFIABLE) {
                    mapForCtx.put(mode.toString(),new ConfigMapNode());
                }
            }
            
            mapForCtx.put("VERIFIABLE",new ConfigListNode());
        }
        
        for (Map.Entry< CodeOrigin, VerifiabilityMode > e : map.entrySet()) {
            CodeOrigin origin=e.getKey();
            VerifiabilityMode vmode=e.getValue();
            if (vmode==VerifiabilityMode.VERIFIABLE) {
                result
                    .getMap(origin.getContext().description())
                    .getList("VERIFIABLE")
                    .append(origin.jniName());
            } else {
                result
                    .getMap(origin.getContext().description())
                    .getMap(vmode.toString())
                    .put(origin.jniName(),reports.get(origin).report());
            }
        }
        
        return result;
    }
}

