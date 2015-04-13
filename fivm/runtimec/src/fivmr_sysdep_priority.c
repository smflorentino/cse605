/*
 * fivmr_sysdep_priority.c
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
#include "fivmr.h"

const char *fivmr_ThreadPriority_schedulerName(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_JAVA: return "Java";
    case FIVMR_TPR_NORMAL: return "Normal";
    case FIVMR_TPR_RR: return "RR";
    case FIVMR_TPR_FIFO: return "FIFO";
    case FIVMR_TPR_FIVM: return "FIVM";
    default:
        fivmr_abortf("Invalid scheduler: %d",fivmr_ThreadPriority_scheduler(pr));
        return NULL;
    }
}

void fivmr_ThreadPriority_describe(fivmr_ThreadPriority pr,
                                   char *buf,
                                   int32_t len) {
    snprintf(buf,len,"%s%d",
             fivmr_ThreadPriority_schedulerName(pr),
             fivmr_ThreadPriority_priority(pr));
}

fivmr_ThreadPriority fivmr_ThreadPriority_parseScheduler(char const **sched) {
    /* use memcmp because it's more likely to be portable */
    
    if (!memcmp(*sched,"Java",4)) {
        (*sched)+=4;
        return FIVMR_TPR_JAVA;
    } else if (!memcmp(*sched,"Normal",6)) {
        (*sched)+=6;
        return FIVMR_TPR_NORMAL;
    } else if (!memcmp(*sched,"RR",2)) {
        (*sched)+=2;
        return FIVMR_TPR_RR;
    } else if (!memcmp(*sched,"FIFO",4)) {
        (*sched)+=4;
        return FIVMR_TPR_FIFO;
    } else {
        return FIVMR_TPR_INVALID;
    }
}

fivmr_ThreadPriority fivmr_ThreadPriority_parse(const char *prstr) {
    const char *str;
    fivmr_ThreadPriority result;
    int32_t prio;
    char buf[32];
    
    str=prstr;
    result=fivmr_ThreadPriority_parseScheduler(&str);
    
    if (result==FIVMR_TPR_INVALID) {
        LOG(2,("failed to parse %s because the scheduler wasn't valid",prstr));
        return FIVMR_TPR_INVALID;
    }
    
    if (sscanf(str,"%d",&prio)!=1 ||
        prio<0 || prio>0xffff) {
        LOG(2,("failed to parse %s because the priority wasn't valid",prstr));
        return FIVMR_TPR_INVALID;
    }
    
    snprintf(buf,sizeof(buf),"%s%d",
             fivmr_ThreadPriority_schedulerName(result),
             prio);
    
    if (strcmp(buf,prstr)) {
        LOG(2,("failed to parse %s because %s != %s",prstr,buf,prstr));
        return FIVMR_TPR_INVALID;
    }
    
    result=fivmr_ThreadPriority_withPriority(result,prio);

    if (result<fivmr_ThreadPriority_minPriority(result) ||
        result>fivmr_ThreadPriority_maxPriority(result)) {
        LOG(2,("failed to parse %s because the priority is out of range",prstr));
        return FIVMR_TPR_INVALID;
    }
    
    return result;
}

int32_t fivmr_ThreadPriority_minPriority(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_JAVA: return FIVMR_TPR_JAVA_MIN;
    case FIVMR_TPR_NORMAL: return FIVMR_TPR_NORMAL_MIN;
    case FIVMR_TPR_RR: return FIVMR_TPR_RR_MIN;
    case FIVMR_TPR_FIFO: return FIVMR_TPR_FIFO_MIN;
    default:
        fivmr_abortf("Invalid scheduler for minPriority: %d",pr);
        return 0;
    }
}

int32_t fivmr_ThreadPriority_maxPriority(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_JAVA: return FIVMR_TPR_JAVA_MAX;
    case FIVMR_TPR_NORMAL: return FIVMR_TPR_NORMAL_MAX;
    case FIVMR_TPR_RR: return FIVMR_TPR_RR_MAX;
    case FIVMR_TPR_FIFO: return FIVMR_TPR_FIFO_MAX;
    default:
        fivmr_abortf("Invalid scheduler for maxPriority: %d",pr);
        return 0;
    }
}

fivmr_ThreadPriority fivmr_ThreadPriority_withScheduler(fivmr_ThreadPriority pr,
                                                        fivmr_ThreadPriority sched) {
    fivmr_ThreadPriority oldMin=fivmr_ThreadPriority_minPriority(pr);
    fivmr_ThreadPriority newMin=fivmr_ThreadPriority_minPriority(sched);
    fivmr_ThreadPriority oldMax=fivmr_ThreadPriority_maxPriority(pr);
    fivmr_ThreadPriority newMax=fivmr_ThreadPriority_maxPriority(sched);
    fivmr_assert(pr>=oldMin && pr<=oldMax);
    return newMin+(pr-oldMin)*(newMax-newMin+1)/(oldMax-oldMin+1);
}

fivmr_ThreadPriority fivmr_ThreadPriority_fromPriority(fivmr_ThreadPriority sched,
                                                       fivmr_Priority prio) {
    fivmr_ThreadPriority oldMin=FIVMR_PR_MIN;
    fivmr_ThreadPriority newMin=fivmr_ThreadPriority_minPriority(sched);
    fivmr_ThreadPriority oldMax=FIVMR_PR_MAX;
    fivmr_ThreadPriority newMax=fivmr_ThreadPriority_maxPriority(sched);
    fivmr_assert(prio>=oldMin && prio<=oldMax);
    return newMin+(prio-oldMin)*(newMax-newMin+1)/(oldMax-oldMin+1);
}

fivmr_Priority fivmr_ThreadPriority_asPriority(fivmr_ThreadPriority prio) {
    fivmr_ThreadPriority oldMin=fivmr_ThreadPriority_minPriority(prio);
    fivmr_ThreadPriority newMin=FIVMR_PR_MIN;
    fivmr_ThreadPriority oldMax=fivmr_ThreadPriority_maxPriority(prio);
    fivmr_ThreadPriority newMax=FIVMR_PR_MAX;
    fivmr_assert(prio>=oldMin && prio<=oldMax);
    return newMin+(prio-oldMin)*(newMax-newMin+1)/(oldMax-oldMin+1);
}

fivmr_Priority fivmr_Priority_bound(fivmr_Priority prio,
                                    fivmr_ThreadPriority maxPrio) {
    fivmr_Priority result;
    if (maxPrio==FIVMR_TPR_CRITICAL) {
        result=prio;
    } else {
        fivmr_ThreadPriority sched=fivmr_ThreadPriority_scheduler(maxPrio);
        if (sched==FIVMR_TPR_RR || sched==FIVMR_TPR_FIFO) {
            result=fivmr_min(prio,fivmr_ThreadPriority_asPriority(maxPrio));
        } else if (sched==FIVMR_TPR_JAVA || sched==FIVMR_TPR_NORMAL) {
            result=FIVMR_PR_NONE;
        } else {
            fivmr_assert(!"bad priority");
            result=0; /* make GCC happy */
        }
    }
    LOG(6,("given priority %d and max prio %d returning %d",
           prio,maxPrio,result));
    return result;
}

fivmr_ThreadPriority fivmr_ThreadPriority_canonicalize(fivmr_ThreadPriority pr) {
    if (fivmr_ThreadPriority_scheduler(pr)==FIVMR_TPR_JAVA) {
        return fivmr_ThreadPriority_withScheduler(pr,FIVMR_TPR_NORMAL);
    } else {
        return pr;
    }
}

bool fivmr_ThreadPriority_eq(fivmr_ThreadPriority pr1,
                             fivmr_ThreadPriority pr2) {
    return fivmr_ThreadPriority_canonicalize(pr1)
        == fivmr_ThreadPriority_canonicalize(pr2);
}

fivmr_ThreadPriority fivmr_ThreadPriority_canonicalizeRT(fivmr_ThreadPriority pr) {
    switch (fivmr_ThreadPriority_scheduler(pr)) {
    case FIVMR_TPR_NORMAL:
    case FIVMR_TPR_JAVA: return FIVMR_TPR_MIN;
    default: return pr;
    }
}

bool fivmr_ThreadPriority_eqRT(fivmr_ThreadPriority pr1,
                               fivmr_ThreadPriority pr2) {
    return fivmr_ThreadPriority_canonicalizeRT(pr1)
        == fivmr_ThreadPriority_canonicalizeRT(pr2);
}

bool fivmr_ThreadPriority_ltRT(fivmr_ThreadPriority pr1,
                               fivmr_ThreadPriority pr2) {
    pr1=fivmr_ThreadPriority_canonicalizeRT(pr1);
    pr2=fivmr_ThreadPriority_canonicalizeRT(pr2);
    if (pr2==FIVMR_TPR_MIN) {
        return false;
    }
    if (pr1==FIVMR_TPR_MIN) {
        return true;
    }
    if (pr1==FIVMR_TPR_CRITICAL) {
        return false;
    }
    if (pr2==FIVMR_TPR_CRITICAL) {
        return true;
    }
    if (FIVMR_TPR_PRIORITY(pr1)<FIVMR_TPR_PRIORITY(pr2)) {
        return true;
    }
    /* currently we assume, for the purposes of sorting, that two priorities are
       equal if the priority numbers are equal but even if the schedulers are
       different.  this assumption is actually used throughout the system.  so
       we shouldn't change it. */
    return false && FIVMR_TPR_SCHEDULER(pr1)<FIVMR_TPR_SCHEDULER(pr2);
}

fivmr_ThreadPriority fivmr_ThreadPriority_max(fivmr_ThreadPriority pr1,
                                              fivmr_ThreadPriority pr2) {
    pr1=fivmr_ThreadPriority_canonicalize(pr1);
    pr2=fivmr_ThreadPriority_canonicalize(pr2);
    if (pr1==FIVMR_TPR_CRITICAL || pr2==FIVMR_TPR_CRITICAL) {
        return FIVMR_TPR_CRITICAL;
    }
    switch (fivmr_ThreadPriority_scheduler(pr1)) {
    case FIVMR_TPR_NORMAL:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return fivmr_max(pr1,pr2);
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return pr2;
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_RR:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr1;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr2,
                fivmr_max(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_FIFO:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr1;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr1,
                fivmr_max(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    default:
        fivmr_abortf("bad priority");
        return 0;
    }
}

fivmr_ThreadPriority fivmr_ThreadPriority_min(fivmr_ThreadPriority pr1_,
                                              fivmr_ThreadPriority pr2_) {
    fivmr_ThreadPriority pr1,pr2;
    pr1=fivmr_ThreadPriority_canonicalize(pr1_);
    pr2=fivmr_ThreadPriority_canonicalize(pr2_);
    if (pr1==FIVMR_TPR_CRITICAL) {
        return pr2;
    } else if (pr2==FIVMR_TPR_CRITICAL) {
        return pr1;
    }
    switch (fivmr_ThreadPriority_scheduler(pr1)) {
    case FIVMR_TPR_NORMAL:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return fivmr_min(pr1,pr2);
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return pr1_;
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_RR:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr2_;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr1,
                fivmr_min(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    case FIVMR_TPR_FIFO:
        switch (fivmr_ThreadPriority_scheduler(pr2)) {
        case FIVMR_TPR_NORMAL:
            return pr2_;
        case FIVMR_TPR_RR:
        case FIVMR_TPR_FIFO:
            return fivmr_ThreadPriority_withPriority(
                pr2,
                fivmr_min(fivmr_ThreadPriority_priority(pr1),
                          fivmr_ThreadPriority_priority(pr2)));
        default:
            fivmr_abortf("bad priority");
            return 0;
        }
    default:
        fivmr_abortf("bad priority");
        return 0;
    }
}

bool fivmr_ThreadPriority_isRT(fivmr_ThreadPriority pr) {
    return FIVMR_PR_SUPPORTED
        && (FIVMR_TPR_SCHEDULER(fivmr_ThreadPriority_canonicalizeRT(pr))
            != FIVMR_TPR_NORMAL);
}




