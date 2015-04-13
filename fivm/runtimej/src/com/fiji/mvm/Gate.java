/*
 * Gate.java
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

package com.fiji.mvm;

import java.util.*;
import com.fiji.fivm.r1.*;

/**
 * Defines interfaces for VM-to-VM gate-based communication.  This class cannot
 * be instantiated; its main purpose is to contain the
 * @Export annotation, the Import interface, and the Message base class, which
 * you can use to enable VM-to-VM communcation.
 * <p>
 * A "gate" is a method that gets exported from one VM to another.  This allows
 * the importing VM to make cross-VM method calls ("gate calls") to the exporting
 * VM.  In the exporting VM, the gate appears as a static method which is marked
 * with the @Export annotation.  To call a gate in a remote VM, you must have:
 * (1) a reference to the VM (see the VM class) and (2) the name and signature
 * of the method you wish to call.  You provide the latter statically, by creating
 * an interface that extends Gate.Import, and placing one or more methods in that
 * interface.  Calling openGate and passing your interface (inherited from
 * Gate.Import) as an argument will result in an instance of that interface being
 * returned.  Each of the methods you supplied will be implemented as cross-VM
 * calls.
 * <p>
 * Note that this results in an asymmetry in the definition (export) and use
 * (import) of gates.  The definition is static, indicating that there is only one
 * in that VM instance.  On the other hand, when using gates, you must have an
 * instance of a Gate.Import interface, which in turn requires having an instance of
 * a VM.  This is because multiple VMs can export the same gates, and accurate
 * resolution requires having some way of identifying which of the VMs in the
 * system you wish to import the gate from.
 * <p>
 * The parameter and return types of gate methods (i.e. methods marked @Gate.Export and
 * methods placed in subinterfaces of Gate.Import) are restricted.  The following
 * types are always allowed: primitive types, String, VM, arrays of primitive types,
 * arrays of Strings, and arrays of VMs.  To define custom types that can be passed
 * as arguments or returned, you must sub-class the Message class and ensure that you
 * follow these rules; failure to follow them will result in a compiler exception
 * if the invalid Message subclass is used in the signature of any method marked
 * @Gate.Export or any method declared in a subclass of Gate.Import.  The fields of
 * your Message subclass may only have the following types: primitive type, String, VM,
 * validated Message subclass, or arrays of the previous.  Additionally for a subclass
 * C of Message to be valid, it must not be possible for any fields in C or reachable
 * from C to have type D where C is a subclass of C.  The simplest way to ensure this
 * is to: (a) make all subclasses of Message final, (b) never have fields or arrays that
 * use either the Object or Message types or any interface types, and (c) make sure
 * that all subclasses of Message in your program can be ordered in such a way that
 * each subclass only refers to Message subclasses that precede it in the order.
 * <p>
 * Gate calls that involve objects (as opposed to primitives) are tricky with
 * respect to memory allocation.  By default, message objects (including arrays,
 * Strings, and subclasses of Message, but excluding VM) are copied from the memory
 * space of the VM that instantiated them to the memory space of the VM that receives
 * them.  While this is a reasonable default for gate calls that occur during
 * system initialization or in VMs that use garbage collection, it is not acceptable
 * during mission execution in VMs that lack a collector.  It also runs the risk of
 * incurring execution time overhead, since every call must incur the cost of
 * copying the messages.  Two facilities are provided for dealing with this problem.
 * The first is to only perform gate calls with primitive arguments.  This is the
 * easiest solution but is not particularly general.  The second facility, which only
 * works for parameters (i.e. it does not directly help with return values), is to
 * mark the parameters with the @Borrowed annotation.  This indicates that the VM
 * will manage the memory of the passed messages and reclaim that memory when the
 * gate method returns.  This imposes a number of safety restrictions on any
 * argument marked @Borrowed.  First, borrowed arguments cannot "escape" the
 * called method: you cannot store them into fields, arrays, or pass them as
 * parameters to procedure calls.  One exception to the latter is if the parameter
 * that you are passing the borrowed message to is also marked @Borrowed.  Note
 * that any objects loaded from a borrowed argument object effectively behave as
 * if they were borrowed.  One outcome of using @Borrowed is that if both VMs
 * involved in the gate call (i.e. the caller and the callee) use the same object
 * model then the messages are not copied.
 */
public class Gate {
    /**
     * Opens a gate to another VM.  This method performs identically to calling
     * vm.openGate(gateClass).
     */
    @SuppressWarnings({"unchecked"})
    public static < T extends Gate.Import > T openGate(VM vm,
                                                       Class< T > gateClass) {
        return (T)builders.get(gateClass).openGate(vm);
    }
    
    /**
     * If a gate call is on-going (i.e. a stack walk from the this stack frame
     * ends up in a gate call) then the VM corresponding to the caller is
     * returned.  Otherwise, this returns null.
     */
    public static VM getCaller() {
        return null; // FIXME
    }
    
    /**
     * Annotation to be used on static methods, indicating that the method
     * will be exported as a gate.
     * FIXME consider making this an interface like Import.
     */
    public @interface Export {}
    
    /**
     * Base interface to use for defining a set of gates to import from another
     * VM.
     */
    public interface Import {}
    
    /**
     * Base class for objects that can be passed across gate calls.
     * FIXME consider making this an annotation
     */
    public static abstract class Message {}

    /**
     * Annotation on arguments indicating that their memory is managed by the
     * VM.
     */
    public @interface Borrowed {}
    
    public static class CallFailedException extends RuntimeException {
        public CallFailedException() {
            super();
        }
        
        public CallFailedException(String msg) {
            super(msg);
        }
    }
    
    // ... and the implementation
    
    abstract static class Builder {
        abstract Gate build(VM vm);
        
        TreeMap< VM, Gate > memo=new TreeMap< VM, Gate >();
        
        synchronized Gate openGate(VM vm) {
            Gate result=memo.get(vm);
            if (result==null) {
                memo.put(vm,result=openGate(vm));
            }
            return result;
        }
    }
    
    private static TreeMap< Class<?>, Builder > builders=
        new TreeMap< Class<?>, Builder >();
    
    @Intrinsic
    private static native void initializeBuilders(TreeMap< Class<?>, Builder > builders);
    
    static {
        initializeBuilders(builders);
    }
    
    VM target;
    Pointer[] fieldOffsets; // their field offsets, in the order that the compiler expects
    Pointer[] typeDatas; // their type datas, in the order that the compiler expects
    Pointer[] myTypeDatasSorted; // our type datas, sorted
    Pointer[] typeDataMap; // their type datas, according to *our* sorted order
    
    Gate(VM target,
         Pointer[] myTypes,
         int nExpectedFields) {
        this.target=target;
        // FIXME
    }
}

