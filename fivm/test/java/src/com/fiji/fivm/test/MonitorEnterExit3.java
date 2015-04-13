/*
 * MonitorEnterExit3.java
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

import com.fiji.fivm.r1.Monitors;

public class MonitorEnterExit3 {
    public static void main(String[] v) throws Throwable {
        Object o=new Object();
        Util.ensureEqual(Monitors.curHolder(o),null);
        Util.ensureEqual(Monitors.recCount(o),0);
        System.out.println("About to lock "+o);
        Util.ensureEqual(Monitors.curHolder(o),null);
        Util.ensureEqual(Monitors.recCount(o),0);
        synchronized (o) {
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
            System.out.println("Locked "+o);
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
            synchronized (o) {
                Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
                Util.ensureEqual(Monitors.recCount(o),2);
                System.out.println("Locked again "+o);
                Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
                Util.ensureEqual(Monitors.recCount(o),2);
            }
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
            System.out.println("Back to one lock level "+o);
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
        }
        Util.ensureEqual(Monitors.curHolder(o),null);
        Util.ensureEqual(Monitors.recCount(o),0);
        System.out.println("Unlocked "+o);
        System.out.println("About to lock a second time "+o);
        Util.ensureEqual(Monitors.curHolder(o),null);
        Util.ensureEqual(Monitors.recCount(o),0);
        synchronized (o) {
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
            System.out.println("Locked a second time "+o);
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
            synchronized (o) {
                Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
                Util.ensureEqual(Monitors.recCount(o),2);
                System.out.println("Locked a second time again "+o);
                Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
                Util.ensureEqual(Monitors.recCount(o),2);
            }
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
            System.out.println("Back to one lock level a second time "+o);
            Util.ensureEqual(Monitors.curHolder(o),Thread.currentThread());
            Util.ensureEqual(Monitors.recCount(o),1);
        }
        Util.ensureEqual(Monitors.curHolder(o),null);
        Util.ensureEqual(Monitors.recCount(o),0);
        System.out.println("Unlocked a second time "+o);
    }
}


