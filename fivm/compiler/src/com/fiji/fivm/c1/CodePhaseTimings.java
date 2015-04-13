/*
 * CodePhaseTimings.java
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

import java.io.*;
import java.util.*;
import com.fiji.util.*;

public class CodePhaseTimings {
    private CodePhaseTimings() {}
    
    private static HashMap< String, SimpleStats > statsMap=
        new HashMap< String, SimpleStats >();
    
    public static long tic() {
        if (Global.measurePhaseTimings) {
            return System.nanoTime();
        } else {
            return 0;
        }
    }
    
    public static void toc(Object phase,long before) {
        if (Global.measurePhaseTimings) {
            synchronized (CodePhaseTimings.class) {
                long after=System.nanoTime();
                String phaseStr=phase.toString();
                SimpleStats stats=statsMap.get(phaseStr);
                if (stats==null) {
                    statsMap.put(phaseStr,stats=new SimpleStats());
                }
                stats.add(((double)(after-before))/1000.0/1000.0);
                
                if (statsMap.size()>1000) {
                    throw new CompilerException(
                        "statsMap has grown unreasonably huge: "+statsMap);
                }
            }
        }
    }
    
    public static void dump(String filename) throws IOException {
        PrintWriter pw=Util.wrap(new FileOutputStream(filename));
        try {
            ArrayList< Map.Entry< String, SimpleStats > > array=
                new ArrayList< Map.Entry< String, SimpleStats > >(statsMap.entrySet());
            
            Collections.sort(
                array,
                new Comparator< Map.Entry< String, SimpleStats > >() {
                    public int compare(Map.Entry< String, SimpleStats > a,
                                       Map.Entry< String, SimpleStats > b) {
                        if (a.getValue().getSum()<b.getValue().getSum()) {
                            return 1;
                        } else if (a.getValue().getSum()>b.getValue().getSum()) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                });
            
            for (Map.Entry< String, SimpleStats > e : array) {
                pw.println(e.getKey()+" "+e.getValue());
            }
        } finally {
            pw.close();
        }
    }
}

