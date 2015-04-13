/*
 * fijdk_jdkruntime.h
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

#ifndef FP_FIVMR_JDK_H
#define FP_FIVMR_JDK_H

/* functions imported from Java */

void FijiJDK_JDK_monitorWait(fivmr_ThreadState *ts,
                             fivmr_Handle *h,
                             int64_t ms);

void FijiJDK_JDK_notify(fivmr_ThreadState *ts,
                        fivmr_Handle *h);

void FijiJDK_JDK_notifyAll(fivmr_ThreadState *ts,
                           fivmr_Handle *h);

fivmr_Handle *FijiJDK_JDK_clone(fivmr_ThreadState *ts,
                                fivmr_Handle *h);

fivmr_Handle *FijiJDK_JDK_intern(fivmr_ThreadState *ts,
                                 fivmr_Handle *h);

void FijiJDK_JDK_arraycopy(fivmr_ThreadState *ts,
                           fivmr_Handle *src, int32_t srcPos,
                           fivmr_Handle *dst, int32_t dstPos,
                           int32_t length);

void FijiJDK_JDK_initProperties(fivmr_ThreadState *ts,
                                fivmr_Handle *properties);

void FijiJDK_JDK_throwModuleError(fivmr_ThreadState *ts,
                                  const char *str);

void FijiJDK_JDK_startThread(fivmr_ThreadState *ts,
                             fivmr_Handle *thread);

void FijiJDK_JDK_stopThread(fivmr_ThreadState *ts,
                            fivmr_Handle *thread);

bool FijiJDK_JDK_isThreadAlive(fivmr_ThreadState *ts,
                               fivmr_Handle *thread);

void FijiJDK_JDK_suspendThread(fivmr_ThreadState *ts,
                               fivmr_Handle *thread);

void FijiJDK_JDK_resumeThread(fivmr_ThreadState *ts,
                              fivmr_Handle *thread);

void FijiJDK_JDK_setThreadPriority(fivmr_ThreadState *ts,
                                   fivmr_Handle *thread,
                                   int32_t prio);

void FijiJDK_JDK_sleep(fivmr_ThreadState *ts,
                       int64_t millis);

fivmr_Handle *FijiJDK_JDK_currentThread(fivmr_ThreadState *ts);

int32_t FijiJDK_JDK_countStackFrames(fivmr_ThreadState *ts,
                                     fivmr_Handle *thread);

void FijiJDK_JDK_interrupt(fivmr_ThreadState *ts,
                           fivmr_Handle *thread);

bool FijiJDK_JDK_isInterrupted(fivmr_ThreadState *ts,
                               fivmr_Handle *thread,
                               bool clearInterrupted);

bool FijiJDK_JDK_holdsLock(fivmr_ThreadState *ts,
                           fivmr_Handle *object);

void FijiJDK_JDK_dumpAllStacks(fivmr_ThreadState *ts);

fivmr_Handle *FijiJDK_JDK_getAllThreads(fivmr_ThreadState *ts);

fivmr_Handle *FijiJDK_JDK_dumpThreads(fivmr_ThreadState *ts,
                                      fivmr_Handle *threads);

#endif

