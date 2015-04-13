/*
 * fivmr_rtems_nanos.c
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

#include <fivmr_config.h>
#if FIVMR_RTEMS

#include "fivmr.h"
#include <sys/time.h>

fivmr_Nanos fivmr_nanosResolution() {
    /* FIXME: How can we get this resolution?  Doesn't seem to be
     * rtems_clock_get_ticks_per_second() */
    return 1000;
}

fivmr_Nanos fivmr_curTimePrecise(void) {
    if (true) {
        fivmr_Nanos result;
        struct timespec ts;
        rtems_status_code status;
        
        status=rtems_clock_get_uptime(&ts);
        fivmr_assert( status == RTEMS_SUCCESSFUL ); 
        
        result=ts.tv_sec;
        result*=1000;
        result*=1000;
        result*=1000;
        result+=ts.tv_nsec;
        
        return result;
    } else {
        /* this doesn't seem to work */
        
        fivmr_Nanos result;
        struct timeval tv;
        rtems_status_code status;
        status=rtems_clock_get_tod_timeval(&tv);
        fivmr_assert(status==RTEMS_SUCCESSFUL);
        
        result=tv.tv_sec;
        result*=1000;
        result*=1000;
        result+=tv.tv_usec;
        result*=1000;
        
        return result;
    }
}

fivmr_Nanos fivmr_curTime(void) {
    if (false) {
        return fivmr_curTimePrecise();
    } else {
        fivmr_Nanos result;
        struct timespec ts;
        
        rtems_interrupt_level cookie;
        rtems_interrupt_disable(cookie);
        ts=_TOD_Uptime;
        rtems_interrupt_enable(cookie);

        result=ts.tv_sec;
        result*=1000;
        result*=1000;
        result*=1000;
        result+=ts.tv_nsec;

        return result;
    }
}

#endif
