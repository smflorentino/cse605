/*
 * TrivialUseDef.java
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

package com.fiji.fivm.c1.x86;

import java.util.*;

import com.fiji.fivm.c1.x86.arg.LArg;
import com.fiji.fivm.c1.x86.arg.Tmp;

public class TrivialUseDef {
    static class Info {
        boolean multiUse,multiDef;
        LOp use,def;

        void addUse(LOp o) {
            if (!multiUse) {
                if (use==null) {
                    use=o;
                } else {
                    use=null;
                    multiUse=true;
                }
            }
        }
        
        void addDef(LOp o) {
            if (!multiDef) {
                if (def==null) {
                    def=o;
                } else {
                    def=null;
                    multiDef=true;
                }
            }
        }
    }
    
    HashMap< Tmp, Info > infos=new HashMap< Tmp, Info >();
    
    Info ensureInfo(Tmp t) {
        Info i=infos.get(t);
        if (i==null) {
            infos.put(t,i=new Info());
        }
        return i;
    }
    
    public TrivialUseDef(LCode c) {
        for (LHeader h : c.headers()) {
            for (LOp o : h.operations()) {
                for (LArg a : o.uses()) {
                    if (a instanceof Tmp) {
                        ensureInfo((Tmp)a).addUse(o);
                    }
                }
                for (LArg a : o.defs()) {
                    if (a instanceof Tmp) {
                        ensureInfo((Tmp)a).addDef(o);
                    }
                }
            }
        }
    }
    
    public LOp oneUse(LArg a) {
        Info i=infos.get(a);
        if (i==null) {
            return null;
        } else {
            return i.use;
        }
    }
    
    public LOp oneDef(LArg a) {
        Info i=infos.get(a);
        if (i==null) {
            return null;
        } else {
            return i.def;
        }
    }
}

