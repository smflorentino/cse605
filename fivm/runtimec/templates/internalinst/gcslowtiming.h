/*
 * gcslowtiming.h
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

/*
 * Example of how to use internal instrumentation to measure execution time
 * of every GC slow path.
 *
 * This file has no warranty, and is public domain.  Using it does not
 * imply any contract, license, or obligation from or to Fiji Systems LLC.
 */

#if FIVMR_INTINST_HEADER
/* The code within this #if block is included in the fivmr.h header file,
   which is included globally by all of Fiji VM, including code that Fiji VM
   generates (i.e. Java code). */

/* Place any globally-visible declarations here (including constants, for example)
   here, so long as they don't rely on any of Fiji VM's data structures being
   defined.. */

/* Modify this macro to insert type definitions *before* the Fiji VM type
   definitions, but *after* the Fiji VM type declarations.  I.e. if you'd like
   to have a struct that has pointers to Fiji VM data structures, define it
   here.  You can also declare it here, but it would be better to declare it
   before the macro. */
#define FIVMR_II_TYPE_DECL_BEFORE

/* Modify this macro to insert fields in the fivmr_Frame structure, which
   contains call-frame-local data and is always rapidly accessible, both
   from the function that uses it and from a stack walk. */
#define FIVMR_II_FRAME_FIELDS

/* Modify this macro to insert fields in the fivmr_ThreadState structure,
   which contains thread-local data and is always rapidly accessible. */
#define FIVMR_II_THREADSTATE_FIELDS \
    fivmr_Nanos ii_barrier;         \
    fivmr_Nanos ii_alloc;           \
    fivmr_Nanos ii_pollcheck;       \
    uint64_t ii_barrierCnt;         \
    uint64_t ii_allocCnt;           \
    uint64_t ii_pollcheckCnt;

/* Modify this macro to insert type definitions *after* the Fiji VM type
   definitions, but *before* any Fiji VM global variable declarations.  This
   allows you to define structs that include inline instances of Fiji VM
   types. */
#define FIVMR_II_TYPE_DECL_AFTER

/* Modify this macro to insert global variable declarations. */
#define FIVMR_II_GLOBALS                        \
    extern fivmr_Nanos fivmr_ii_barrier;        \
    extern fivmr_Nanos fivmr_ii_alloc;          \
    extern fivmr_Nanos fivmr_ii_pollcheck;      \
    extern uint64_t fivmr_ii_barrierCnt;        \
    extern uint64_t fivmr_ii_allocCnt;          \
    extern uint64_t fivmr_ii_pollcheckCnt;

/* Modify this macro to insert function declarations for functions to be
   defined in the body, below. */
#define FIVMR_II_FUNCTION_DECLS

/* Modify this macro to insert variable declarations into any Fiji VM
   function that may call into any instrumentation macros. */
#define FIVMR_II_LOCAL_DECLS \
    fivmr_Nanos ii_before;

/* The following macros will be called by Fiji VM in generated code.
   Note that they all take two arguments: a pointer to the ThreadState
   and a pointer to the Frame. */

/* Modify this macro to insert instrumentation whenever an exception is
   thrown.  Note that 'exception' is the actual variable that holds the
   exception. */
#define FIVMR_II_THROW(ts,frame,exception)

/* Modify this macro to insert instrumentation preceding a method call.
   Note that 'method' is the actual pointer to the method. */
#define FIVMR_II_BEFORE_INVOKE(ts,frame,method)

/* Modify this macro to insert instrumentation preceding a method call.
   Note that 'method' is the actual pointer to the method. */
#define FIVMR_II_AFTER_INVOKE(ts,frame,method)

/* Modify this macro to insert instrumentation before the start of an
   array bounds check. */
#define FIVMR_II_BEFORE_ABC(ts,frame,array,index)

/* Modify this macro to insert instrumentation after the end of a
   non-failing array bounds check. */
#define FIVMR_II_AFTER_ABC(ts,frame,array,index)

/* Modify this macro to insert instrumentation before the start of a
   null check. */
#define FIVMR_II_BEFORE_NC(ts,frame,object)

/* Modify this macro to insert instrumentation after the end of a
   non-failing null check. */
#define FIVMR_II_AFTER_NC(ts,frame,object)

/* Modify this macro to insert instrumentation before the start of a
   type check. */
#define FIVMR_II_BEFORE_TC(ts,frame,object,typedata)

/* Modify this macro to insert instrumentation after the end of a
   non-failing type check. */
#define FIVMR_II_AFTER_TC(ts,frame,object,typedata)

/* Modify this macro to insert instrumentation before the start of
   a GC store barrier. */
#define FIVMR_II_BEFORE_GCSTORE(ts,frame,target,fieldAddr,source)

/* Modify this macro to insert instrumentation after the end of a
   GC store barrier. */
#define FIVMR_II_AFTER_GCSTORE(ts,frame,target,fieldAddr,source)

/* Modify this macro to insert instrumentation before the start of
   a GC store barrier slow path. */
#define FIVMR_II_BEFORE_GCSTORE_SLOW(ts,frame,object) \
    ii_before=fivmr_curTime();                        \
    ((fivmr_ThreadState*)ts)->ii_barrierCnt++;

/* Modify this macro to insert instrumentation after the end of
   a GC store barrier slow path. */
#define FIVMR_II_AFTER_GCSTORE_SLOW(ts,frame,object) \
    ((fivmr_ThreadState*)ts)->ii_barrier+=fivmr_curTime()-ii_before;

/* Modify this macro to insert instrumentation before the start of
   an allocation. */
#define FIVMR_II_BEFORE_ALLOC(ts,frame,td)

/* Modify this macro to insert instrumentation after the end of a
   an allocation. */
#define FIVMR_II_AFTER_ALLOC(ts,frame,td)

/* Modify this macro to insert instrumentation before the start of
   an allocation slow path. */
#define FIVMR_II_BEFORE_ALLOC_SLOW(ts,frame,td) \
    ii_before=fivmr_curTime();                  \
    ((fivmr_ThreadState*)ts)->ii_allocCnt++;

/* Modify this macro to insert instrumentation after the end of
   an allocation slow path. */
#define FIVMR_II_AFTER_ALLOC_SLOW(ts,frame,td) \
    ((fivmr_ThreadState*)ts)->ii_alloc+=fivmr_curTime()-ii_before;

/* Modify this macro to insert instrumentation before the start of
   an allocation. */
#define FIVMR_II_BEFORE_ALLOC_ARRAY(ts,frame,td,numEle)

/* Modify this macro to insert instrumentation after the end of a
   an allocation. */
#define FIVMR_II_AFTER_ALLOC_ARRAY(ts,frame,td,numEle)

/* Modify this macro to insert instrumentation before the start of
   an allocation slow path. */
#define FIVMR_II_BEFORE_ALLOC_ARRAY_SLOW(ts,frame,td,numEle) \
    ii_before=fivmr_curTime();                               \
    ((fivmr_ThreadState*)ts)->ii_allocCnt++;

/* Modify this macro to insert instrumentation after the end of
   an allocation slow path. */
#define FIVMR_II_AFTER_ALLOC_ARRAY_SLOW(ts,frame,td,numEle) \
    ((fivmr_ThreadState*)ts)->ii_alloc+=fivmr_curTime()-ii_before;

/* Modify this macro to insert instrumentation before the start of
   an array access slow path. */
#define FIVMR_II_BEFORE_AA_SLOW(ts,frame,object,index)

/* Modify this macro to insert instrumentation after the end of an
   array access slow path.  Note that this will occur *before* the
   actual load or store, but *after* the location in the array has
   been resolved. */
#define FIVMR_II_AFTER_AA_SLOW(ts,frame,object,index)

/* Modify this macro to insert instrumentation before the start of
   a poll check slow path.  Note, this only works with
   --pollcheck-mode portable. */
#define FIVMR_II_BEFORE_PC_SLOW(ts,frame,id)            \
    ii_before=fivmr_curTime();                          \
    ((fivmr_ThreadState*)ts)->ii_pollcheckCnt++;        

/* Modify this macro to insert instrumentation after the end of
   a poll check slow path.  Note, this only works with
   --pollcheck-mode portable. */
#define FIVMR_II_AFTER_PC_SLOW(ts,frame,id) \
    ((fivmr_ThreadState*)ts)->ii_pollcheck+=fivmr_curTime()-ii_before;

#endif

#if FIVMR_INTINST_BODY
/* This code forms its own code module, compiled with the C compiler selected
   by Fiji VM and using the same compiler flags as Fiji VM.  Note that this
   code will be compiled with all warnings turned off. */

/* Add any function/global definitions here. */
fivmr_Nanos fivmr_ii_barrier;
fivmr_Nanos fivmr_ii_alloc;
fivmr_Nanos fivmr_ii_pollcheck;
uint64_t fivmr_ii_barrierCnt;
uint64_t fivmr_ii_allocCnt;
uint64_t fivmr_ii_pollcheckCnt;

/* Modify this function to have code run just before any Java code is
   invoked. */
void fivmr_ii_start(void) {
    fivmr_ii_barrier=0;
    fivmr_ii_alloc=0;
    fivmr_ii_pollcheck=0;
    fivmr_ii_barrierCnt=0;
    fivmr_ii_allocCnt=0;
    fivmr_ii_pollcheckCnt=0;
}

/* Modify this function to have code run at VM shutdown. */
void fivmr_ii_end(void) {
    fprintf(stderr,"Total time spent in GC slow paths\n");
    fprintf(stderr,"     Barrier: %" PRIu64 " ns   (count: %" PRIu64 ")\n",
            fivmr_ii_barrier,fivmr_ii_barrierCnt);
    fprintf(stderr,"       Alloc: %" PRIu64 " ns   (count: %" PRIu64 ")\n",
            fivmr_ii_alloc,fivmr_ii_allocCnt);
    fprintf(stderr,"   Pollcheck: %" PRIu64 " ns   (count: %" PRIu64 ")\n",
            fivmr_ii_pollcheck,fivmr_ii_pollcheckCnt);
}

void fivmr_ii_startThread(fivmr_ThreadState *ts) {
    ts->ii_barrier=0;
    ts->ii_alloc=0;
    ts->ii_pollcheck=0;
    ts->ii_barrierCnt=0;
    ts->ii_allocCnt=0;
    ts->ii_pollcheckCnt=0;
}

void fivmr_ii_commitThread(fivmr_ThreadState *ts) {
    fivmr_Lock_lock(&fivmr_lock);
    fivmr_ii_barrier+=ts->ii_barrier;
    fivmr_ii_alloc+=ts->ii_alloc;
    fivmr_ii_pollcheck+=ts->ii_pollcheck;
    fivmr_ii_barrierCnt+=ts->ii_barrierCnt;
    fivmr_ii_allocCnt+=ts->ii_allocCnt;
    fivmr_ii_pollcheckCnt+=ts->ii_pollcheckCnt;
    fivmr_Lock_unlock(&fivmr_lock);
    ts->ii_barrier=0;
    ts->ii_alloc=0;
    ts->ii_pollcheck=0;
    ts->ii_barrierCnt=0;
    ts->ii_allocCnt=0;
    ts->ii_pollcheckCnt=0;
}

#endif



