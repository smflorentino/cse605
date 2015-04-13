/*
 * ExceptionThrower.java
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

/**
 * The point of this class is to make it easy to generate thunks in the Baseline JIT
 * to throw various exceptions.  The idea is that whenever the thunk is called we
 * want to throw a unique exception, but always with a given set of parameters.  So
 * you create an ExceptionThrower closure and tell the Baseline JIT to create a
 * thunk that calls it.  The JIT takes care of calling convention issues and makes
 * sure to generate code that calls this guy.
 */
@ExcludeUnlessSet({"CLASSLOADING"})
public abstract class ExceptionThrower {
    public abstract void throwException();
    
    // helper for baseline
    @Reflect
    @NoReturn
    @RuntimeExceptionThrower
    public static void throwException(ExceptionThrower et) {
        et.throwException();
        throw new fivmError("et = "+et+" failed to throw an exception.");
    }
    
    // unless you know what "safe for space" is, and how to prove it in the
    // context of Java closures, stay away from this code.
    
    static abstract class MessageExceptionThrower extends ExceptionThrower {
        String message;
        
        MessageExceptionThrower(Throwable e) {
            this.message=e.getMessage();
        }
    }
    
    static abstract class BoxingExceptionThrower< T extends Throwable >
        extends ExceptionThrower {
        
        T boxedException;
        String message;
        
        BoxingExceptionThrower(T e) {
            this.boxedException=e;
            this.message=e.getMessage();
        }
    }
    
    public static ExceptionThrower build(Throwable e) {
        if (e instanceof VirtualMachineError) {
            // if it's that serious then don't try to be fancy - just rethrow the
            // error to get a quicker fail.
            throw (VirtualMachineError)e;
        } else if (e instanceof ClassNotFoundException ||
                   e instanceof NoClassDefFoundError ||
                   e instanceof ExceptionInInitializerError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new NoClassDefFoundError(message);
                }
            };
        } else if (e instanceof ClassCircularityError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new ClassCircularityError(message);
                }
            };
        } else if (e instanceof UnsupportedClassVersionError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new UnsupportedClassVersionError(message);
                }
            };
        } else if (e instanceof ClassFormatError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new ClassFormatError(message);
                }
            };
        } else if (e instanceof UnsatisfiedLinkError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new UnsatisfiedLinkError(message);
                }
            };
        } else if (e instanceof VerifyError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new VerifyError(message);
                }
            };
        } else if (e instanceof AbstractMethodError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new AbstractMethodError(message);
                }
            };
        } else if (e instanceof IllegalAccessError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new IllegalAccessError(message);
                }
            };
        } else if (e instanceof InstantiationError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new InstantiationError(message);
                }
            };
        } else if (e instanceof NoSuchFieldError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new NoSuchFieldError(message);
                }
            };
        } else if (e instanceof NoSuchMethodError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new NoSuchMethodError(message);
                }
            };
        } else if (e instanceof IncompatibleClassChangeError) {
            return new MessageExceptionThrower(e){
                public void throwException() {
                    throw new IncompatibleClassChangeError(message);
                }
            };
        } else {
            return new BoxingExceptionThrower< Throwable >(e) {
                public void throwException() {
                    throw new fivmError(boxedException);
                }
            };
        }
    }
}

