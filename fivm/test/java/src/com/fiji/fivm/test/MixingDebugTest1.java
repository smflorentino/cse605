/*
 * MixingDebugTest1.java
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

import com.fiji.util.*;

public class MixingDebugTest1 {
    public static void main(String[] v) {
        MixingDebug md=new MixingDebug(5);
        
        Util.ensureEqual(md.toString(),"[Current = []; Worst = []]");
        
        md.add(0);
        Util.ensureEqual(md.toString(),"[Current = [0]; Worst = []]");
        
        md.add(1);
        Util.ensureEqual(md.toString(),"[Current = [0, 1]; Worst = []]");
        
        md.add(2);
        Util.ensureEqual(md.toString(),"[Current = [0, 1, 2]; Worst = [0, 1, 2]]");
        
        md.add(3);
        Util.ensureEqual(md.toString(),"[Current = [0, 1, 2, 3]; Worst = [0, 1, 2, 3]]");
        
        md.add(4);
        Util.ensureEqual(md.toString(),"[Current = [0, 1, 2, 3, 4]; Worst = [0, 1, 2, 3, 4]]");
        
        md.add(3);
        Util.ensureEqual(md.toString(),"[Current = [1, 2, 3, 4, 3]; Worst = [1, 2, 3, 4, 3]]");
        
        md.add(0);
        Util.ensureEqual(md.toString(),"[Current = [2, 3, 4, 3, 0]; Worst = [2, 3, 4, 3, 0]]");
        
        md.add(1);
        Util.ensureEqual(md.toString(),"[Current = [3, 4, 3, 0, 1]; Worst = [2, 3, 4, 3, 0]]");
        
        md.add(4);
        Util.ensureEqual(md.toString(),"[Current = [4, 3, 0, 1, 4]; Worst = [2, 3, 4, 3, 0]]");
        
        md.add(5);
        Util.ensureEqual(md.toString(),"[Current = [3, 0, 1, 4, 5]; Worst = [2, 3, 4, 3, 0]]");
        
        md.add(6);
        Util.ensureEqual(md.toString(),"[Current = [0, 1, 4, 5, 6]; Worst = [0, 1, 4, 5, 6]]");
        
        md.add(0);
        Util.ensureEqual(md.toString(),"[Current = [1, 4, 5, 6, 0]; Worst = [1, 4, 5, 6, 0]]");
        
        md.add(0);
        Util.ensureEqual(md.toString(),"[Current = [4, 5, 6, 0, 0]; Worst = [4, 5, 6, 0, 0]]");
        
        md.add(1);
        Util.ensureEqual(md.toString(),"[Current = [5, 6, 0, 0, 1]; Worst = [4, 5, 6, 0, 0]]");
        
        System.out.println("That worked!");
    }
}

