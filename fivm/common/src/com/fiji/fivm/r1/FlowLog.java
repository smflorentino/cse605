/*
 * FlowLog.java
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
package com.fiji.fivm.r1;

@UsesMagic
@NoFlowLog
public class FlowLog {
    /* These constants must be kept perfectly in sync with their
     * equivalents in fivmr_util.h */
    public static final short TYPE_VM                   = 0;
    public static final short TYPE_METHOD               = 1;
    public static final short TYPE_MONITOR              = 2;
    public static final short TYPE_PRIORITY             = 3;
    public static final short TYPE_THREAD               = 4;
    public static final short TYPE_SCOPE                = 5;
    public static final short TYPE_ALLOC                = 6;
    public static final short TYPE_REFERENCE            = 7;
    public static final short TYPE_SCJ                  = 128;

    public static final short SUBTYPE_INIT              = 1;

    public static final short SUBTYPE_ENTER             = 1;
    public static final short SUBTYPE_EXIT              = 2;
    public static final short SUBTYPE_ENTER_INLINE      = 3;
    public static final short SUBTYPE_EXIT_INLINE       = 4;

    public static final short SUBTYPE_LOCK_FAST         = 1;
    public static final short SUBTYPE_UNLOCK_FAST       = 2;
    public static final short SUBTYPE_LOCK_SLOW_BEGIN   = 3;
    public static final short SUBTYPE_UNLOCK_SLOW_BEGIN = 4;
    public static final short SUBTYPE_LOCK_SLOW_END     = 5;
    public static final short SUBTYPE_UNLOCK_SLOW_END   = 6;
    public static final short SUBTYPE_UNLOCK_COMPLETE   = 7;
    public static final short SUBTYPE_RELOCK            = 8;
    public static final short SUBTYPE_WAIT              = 9;
    public static final short SUBTYPE_NOTIFY            = 10;
    public static final short SUBTYPE_NOTIFY_ALL        = 11;
    
    public static final short SUBTYPE_CREATE            = 1;
    public static final short SUBTYPE_RUN               = 3;
    public static final short SUBTYPE_YIELD             = 4;
    public static final short SUBTYPE_SLEEP             = 5;
    public static final short SUBTYPE_PRIORITY          = 6;
    public static final short SUBTYPE_WAKE              = 12;

    public static final short SUBTYPE_ALLOC_BACKING     = 3;
    public static final short SUBTYPE_ALLOC_SCOPE       = 4;
    public static final short SUBTYPE_FREE_BACKING      = 5;
    public static final short SUBTYPE_FREE_SCOPE        = 6;
    public static final short SUBTYPE_PUSH              = 7;
    public static final short SUBTYPE_POP               = 8;
    public static final short SUBTYPE_IMMORTAL          = 9;

    public static final short SUBTYPE_OBJECT            = 1;
    public static final short SUBTYPE_ARRAY             = 2;

    public static final short SUBTYPE_PUTFIELD          = 1;
    public static final short SUBTYPE_PUTSTATIC         = 2;
    public static final short SUBTYPE_ARRAYSTORE        = 3;

    public static final short SUBTYPE_PEH_DEADLINE      = 1;
    public static final short SUBTYPE_T0                = 2;
    public static final short SUBTYPE_CYCLE             = 3;
    public static final short SUBTYPE_RELEASE           = 4;

    @RuntimeImport
    @NoSafepoint
    @NoPollcheck
    private static native void fivmr_FlowLog_log(Pointer ts, short type,
						 short subtype, long data);

    @RuntimeImport
    @NoSafepoint
    @NoPollcheck
    private static native void fivmr_FlowLog_log_fat(Pointer ts, short type,
						     short subtype, long data,
						     long extdata);

    @Inline
    @NoSafepoint
    @NoPollcheck
    public static void log(Pointer ts, short type, short subtype, long data) {
	fivmr_FlowLog_log(ts, type, subtype, data);
    }

    @Inline
    @NoSafepoint
    @NoPollcheck
    public static void log(short type, short subtype, long data) {
	fivmr_FlowLog_log(Magic.curThreadState(), type, subtype, data);
    }

    @Inline
    @NoSafepoint
    @NoPollcheck
    public static void log(short type, short subtype, long data, long extdata) {
	fivmr_FlowLog_log_fat(Magic.curThreadState(),
			      type, subtype, data, extdata);
    }
}
