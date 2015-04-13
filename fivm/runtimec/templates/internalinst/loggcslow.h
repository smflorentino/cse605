/*
 * loggcslow.h
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
 * Example of how to use internal instrumentation to log every use of a
 * GC slow path.
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
#define FIVMR_II_THREADSTATE_FIELDS

/* Modify this macro to insert type definitions *after* the Fiji VM type
   definitions, but *before* any Fiji VM global variable declarations.  This
   allows you to define structs that include inline instances of Fiji VM
   types. */
#define FIVMR_II_TYPE_DECL_AFTER

/* Modify this macro to insert global variable declarations. */
#define FIVMR_II_GLOBALS

/* Modify this macro to insert function declarations for functions to be
   defined in the body, below. */
#define FIVMR_II_FUNCTION_DECLS

/* Modify this macro to insert variable declarations into any Fiji VM
   function that may call into any instrumentation macros. */
#define FIVMR_II_LOCAL_DECLS

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
    fprintf(stderr,"being GC store slow\n");

/* Modify this macro to insert instrumentation after the end of
   a GC store barrier slow path. */
#define FIVMR_II_AFTER_GCSTORE_SLOW(ts,frame,object) \
    fprintf(stderr,"end GC store slow\n");

/* Modify this macro to insert instrumentation before the start of
   an allocation. */
#define FIVMR_II_BEFORE_ALLOC(ts,frame,td)

/* Modify this macro to insert instrumentation after the end of a
   an allocation. */
#define FIVMR_II_AFTER_ALLOC(ts,frame,td)

/* Modify this macro to insert instrumentation before the start of
   an allocation slow path. */
#define FIVMR_II_BEFORE_ALLOC_SLOW(ts,frame,td) \
    fprintf(stderr,"begin alloc slow\n");

/* Modify this macro to insert instrumentation after the end of
   an allocation slow path. */
#define FIVMR_II_AFTER_ALLOC_SLOW(ts,frame,td) \
    fprintf(stderr,"end alloc slow\n");

/* Modify this macro to insert instrumentation before the start of
   an allocation. */
#define FIVMR_II_BEFORE_ALLOC_ARRAY(ts,frame,td,numEle)

/* Modify this macro to insert instrumentation after the end of a
   an allocation. */
#define FIVMR_II_AFTER_ALLOC_ARRAY(ts,frame,td,numEle)

/* Modify this macro to insert instrumentation before the start of
   an allocation slow path. */
#define FIVMR_II_BEFORE_ALLOC_ARRAY_SLOW(ts,frame,td,numEle) \
    fprintf(stderr,"begin alloc array slow\n");

/* Modify this macro to insert instrumentation after the end of
   an allocation slow path. */
#define FIVMR_II_AFTER_ALLOC_ARRAY_SLOW(ts,frame,td,numEle) \
    fprintf(stderr,"end alloc array slow\n");

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
#define FIVMR_II_BEFORE_PC_SLOW(ts,frame,id) \
    fprintf(stderr,"begin pollcheck slow\n");

/* Modify this macro to insert instrumentation after the end of
   a poll check slow path.  Note, this only works with
   --pollcheck-mode portable. */
#define FIVMR_II_AFTER_PC_SLOW(ts,frame,id) \
    fprintf(stderr,"end pollcheck slow\n");

#endif

#if FIVMR_INTINST_BODY
/* This code forms its own code module, compiled with the C compiler selected
   by Fiji VM and using the same compiler flags as Fiji VM.  Note that this
   code will be compiled with all warnings turned off. */

/* Add any function/global definitions here. */

/* Modify this function to have code run just before any Java code is
   invoked. */
void fivmr_ii_start(void) {
}

/* Modify this function to have code run at VM shutdown. */
void fivmr_ii_end(void) {
}

void fivmr_ii_startThread(fivmr_ThreadState *ts) {
}

void fivmr_ii_commitThread(fivmr_ThreadState *ts) {
}

#endif



