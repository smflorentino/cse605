/*
 * LowerMethodResolution.java
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

package com.fiji.fivm.c1;

import com.fiji.fivm.Settings;

public class LowerMethodResolution extends CodePhase {
    public LowerMethodResolution(Code c) { super(c); }
    
    public void visitCode() {
        for (Header h : code.headers()) {
            for (Instruction i : h.instructions()) {
                if (i instanceof MethodInst) {
		    if (Global.profileInvokes) {
			StaticProfileCounterRepo.genInc(code,i,"Num uses of MethodInst");
		    }
		    MethodInst mi=(MethodInst)i;
		    VisibleMethod vm=mi.method();
		    assert !vm.isIntrinsic() : vm;
		    Arg receiverish=null; /* the receiver in the case of instance
					     methods, or garbage otherwise.  we
					     capture it like this so that we have
					     the receiver at its appropriate type.
					     this is a simple and stupid optimization
					     for object models where assigning from
					     one type to another requires code.
                                             (NOTE: we currently have no such object
                                             models, and though we once thought we
                                             would, it's unlikely at this point ...
                                             the FRAGMENTED object model seems to
                                             perform well enough as it is) */
                    
                    if (mi.method().isInstance()) {
                        receiverish=mi.rhs(0);
                    }

		    // FIXME1: we sometimes see dead calls here.  shouldn't dead
		    // calls be eliminated by OptConst because the receiver will
		    // be NULL and would therefore fail the null check?
		    // FIXME2: dead calls should be NotReached-ified by Simplify.
		    // NOTE: we haven't seen a dead call here recently.

                    if (Global.verbosity>=4 && Global.analysis().closed()) {
                        Global.log.println("Possible targets for "+mi+" in "+h+":");
                        for (VisibleMethod m : mi.possibleTargets()) {
                            Global.log.println("   "+m.jniName());
                        }
                        Global.log.println("   (end list)");
                    }

                    if (true) {
                        assert !mi.deadCall() : "Oh noes, we saw a dead call!  In "+mi+" in "+h;
                    } else {
                        // TMI!!
                        assert !mi.deadCall() : "Oh noes, we saw a dead call!  In "+mi+" in "+h+", with dispatch = "+mi.method().getClazz().dispatch(mi.refinement().mode());
                    }
                    
		    if (mi.dynamicCall()) {
			Arg recvTypeData=loadTypeData(receiverish,mi);
			Var fptr=code.addVar(Exectype.POINTER);
			if (mi.refinement().clazz().isInterface() && 
			    !Global.root().objectClass.hasMethod(vm.getSignature())) {
			    try {
                                Var epochs=code.addVar(Exectype.POINTER);
                                Var itablePtr=code.addVar(Exectype.POINTER);
                                mi.prepend(
                                    new CFieldInst(
                                        mi.di(),OpCode.GetCFieldAddress,
                                        epochs,new Arg[]{recvTypeData},
                                        CTypesystemReferences.TypeData_epochs));
                                if (Settings.USE_TYPE_EPOCHS) {
                                    Var methodRecAddr=code.addVar(Exectype.POINTER);
                                    Var locationAddr=code.addVar(Exectype.POINTER);
                                    Var typeEpochIdxInt=code.addVar(Exectype.INT);
                                    Var typeEpochIdx=code.addVar(Exectype.POINTER);
                                    Var typeEpochLocOffset=code.addVar(Exectype.POINTER);
                                    Var typeEpochEpochOffset=code.addVar(Exectype.POINTER);
                                    Var locationForEpochAddr=code.addVar(Exectype.POINTER);
                                    Var locationInt=code.addVar(Exectype.INT);
                                    Var location=code.addVar(Exectype.POINTER);
                                    Var epochSize=code.addVar(Exectype.POINTER);
                                    Var epoch=code.addVar(Exectype.POINTER);
                                    Var itableOff=code.addVar(Exectype.POINTER);
                                    Var itableOffPtr=code.addVar(Exectype.POINTER);
                                    mi.prepend(
                                        new GetMethodInst(
                                            mi.di(),OpCode.GetMethodRec,
                                            methodRecAddr,Arg.EMPTY,
                                            vm));
                                    mi.prepend(
                                        new CFieldInst(
                                            mi.di(),OpCode.GetCFieldAddress,
                                            locationAddr,new Arg[]{methodRecAddr},
                                            CTypesystemReferences.MethodRec_location));
                                    mi.prepend(
                                        new CFieldInst(
                                            mi.di(),OpCode.GetCField,
                                            typeEpochIdxInt,new Arg[]{Arg.THREAD_STATE},
                                            CTypesystemReferences.ThreadState_typeEpoch));
                                    mi.prepend(
                                        new TypeInst(
                                            mi.di(),OpCode.Cast,
                                            typeEpochIdx,new Arg[]{typeEpochIdxInt},
                                            Type.POINTER));
                                    mi.prepend(
                                        new SimpleInst(
                                            mi.di(),OpCode.Mul,
                                            typeEpochLocOffset,new Arg[]{
                                                typeEpochIdx,
                                                PointerConst.make(
                                                    Global.pointerSize/2)
                                            }));
                                    mi.prepend(
                                        new SimpleInst(
                                            mi.di(),OpCode.Add,
                                            locationForEpochAddr,new Arg[]{
                                                locationAddr,
                                                typeEpochLocOffset
                                            }));
                                    mi.prepend(
                                        new MemoryAccessInst(
                                            mi.di(),OpCode.Load,
                                            locationInt,new Arg[]{locationForEpochAddr},
                                            Global.pointerSize==4?Type.CHAR:Type.INT));
                                    mi.prepend(
                                        new TypeInst(
                                            mi.di(),OpCode.Cast,
                                            location,new Arg[]{locationInt},
                                            Type.POINTER));
                                    mi.prepend(
                                        new SimpleInst(
                                            mi.di(),OpCode.Mul,
                                            itableOff,new Arg[]{
                                                location,
                                                PointerConst.make(Global.pointerSize)
                                            }));
                                    mi.prepend(
                                        new CTypeInst(
                                            mi.di(),OpCode.GetCTypeSize,
                                            epochSize,Arg.EMPTY,
                                            CTypesystemReferences.TypeEpoch_TYPE));
                                    mi.prepend(
                                        new SimpleInst(
                                            mi.di(),OpCode.Mul,
                                            typeEpochEpochOffset,new Arg[]{
                                                typeEpochIdx,
                                                epochSize
                                            }));
                                    mi.prepend(
                                        new SimpleInst(
                                            mi.di(),OpCode.Add,
                                            epoch,new Arg[]{
                                                epochs,
                                                typeEpochEpochOffset
                                            }));
                                    mi.prepend(
                                        new CFieldInst(
                                            mi.di(),OpCode.GetCField,
                                            itablePtr,new Arg[]{epoch},
                                            CTypesystemReferences.TypeEpoch_itable));
                                    mi.prepend(
                                        new SimpleInst(
                                            mi.di(),OpCode.Add,
                                            itableOffPtr,new Arg[]{
                                                itablePtr,
                                                itableOff
                                            }));
                                    mi.prepend(
                                        new MemoryAccessInst(
                                            mi.di(),OpCode.Load,
                                            fptr,new Arg[]{itableOffPtr},
                                            Type.POINTER));
                                } else {
                                    mi.prepend(
                                        new CFieldInst(
                                            mi.di(),OpCode.GetCField,
                                            itablePtr,new Arg[]{epochs},
                                            CTypesystemReferences.TypeEpoch_itable));
                                    loadAtOffset(
                                        fptr,mi,itablePtr,
                                        (vm.getItableIndex()*Global.pointerSize),
                                        Type.POINTER);
                                }
			    } catch (Throwable e) {
				throw new CompilerException(
				    "Could not load itable index for call to "+vm+
				    " in "+code+", "+h+", "+i);
			    }
			} else {
			    int index=
				mi.refinement().clazz().getVtableIndex(
				    vm.getSignature());
			    Var vtablePtr=code.addVar(Exectype.POINTER);
			    mi.prepend(
				new CFieldInst(
				    mi.di(),OpCode.GetCFieldAddress,
				    vtablePtr,new Arg[]{recvTypeData},
				    CTypesystemReferences.TypeData_vtable));
			    loadAtOffset(
				fptr,mi,vtablePtr,
				(index*Global.pointerSize),
				Type.POINTER);
			}
			Arg[] rhs=new Arg[mi.rhs().length+1];
			System.arraycopy(mi.rhs(),0,
					 rhs,1,
					 mi.rhs().length);
			rhs[0]=fptr;
			mi.prepend(
			    new IndirectMethodInst(
				mi.di(),OpCode.InvokeIndirect,
				mi.lhs(),rhs,vm.getTypeSignature()));
		    } else {
                        VisibleMethod staticTarget=mi.staticTarget();
                        if (Settings.INTERNAL_INST) {
                            Var ptr=code.addVar(Exectype.POINTER);
                            mi.prepend(
                                new CFieldInst(
                                    mi.di(),OpCode.GetCVarAddress,
                                    ptr,Arg.EMPTY,
                                    staticTarget.asRemoteFunction()));
                            mi.prepend(
                                new CFieldInst(
                                    mi.di(),OpCode.Call,
                                    Var.VOID,new Arg[]{
                                        Arg.THREAD_STATE,
                                        Arg.FRAME,
                                        ptr
                                    },
                                    Inst.beforeInvoke));
                            mi.append(
                                new CFieldInst(
                                    mi.di(),OpCode.Call,
                                    Var.VOID,new Arg[]{
                                        Arg.THREAD_STATE,
                                        Arg.FRAME,
                                        ptr
                                    },
                                    Inst.afterInvoke));
                        }
                        if (Settings.CLASSLOADING) {
                            Arg[] args=new Arg[mi.rhs().length+1];
                            
                            // arg 0 is the function pointer; emit code to
                            // resolve it
                            Var methodRecPtr=code.addVar(Exectype.POINTER);
                            Var entrypoint=code.addVar(Exectype.POINTER);
                            
                            mi.prepend(
                                new GetMethodInst(
                                    mi.di(),OpCode.GetMethodRec,
                                    methodRecPtr,Arg.EMPTY,
                                    staticTarget));
                            mi.prepend(
                                new CFieldInst(
                                    mi.di(),OpCode.GetCField,
                                    entrypoint,new Arg[]{methodRecPtr},
                                    CTypesystemReferences.MethodRec_entrypoint));
                            
                            args[0]=entrypoint;
                            
                            System.arraycopy(mi.rhs(),0,
                                             args,1,
                                             mi.rhs().length);
                            if (mi.method().isInstance() &&
                                !args[1].type().isSubtypeOf(
                                    staticTarget.getClazz().asExectype())) {
                                // inlining.  it makes type-related stuff weird.
                                
                                Var tmp=code.addVar(staticTarget.getClazz().asExectype());
                                mi.prepend(
                                    new TypeInst(
                                        mi.di(),OpCode.Cast,
                                        tmp,new Arg[]{args[1]},
                                        staticTarget.getClazz().asType()));
                                args[1]=tmp;
                            }
                            mi.prepend(
                                new IndirectMethodInst(
                                    mi.di(),OpCode.InvokeIndirect,
                                    mi.lhs(),args,
                                    staticTarget.getTypeSignature()));
                        } else {
                            Arg[] args=new Arg[mi.rhs().length];
                            System.arraycopy(mi.rhs(),0,
                                             args,0,
                                             args.length);
                            if (mi.method().isInstance() &&
                                !args[0].type().isSubtypeOf(
                                    staticTarget.getClazz().asExectype())) {
                                // inlining.  it makes type-related stuff weird.
                                
                                Var tmp=code.addVar(staticTarget.getClazz().asExectype());
                                mi.prepend(
                                    new TypeInst(
                                        mi.di(),OpCode.Cast,
                                        tmp,new Arg[]{args[0]},
                                        staticTarget.getClazz().asType()));
                                args[0]=tmp;
                            }
                            mi.prepend(
                                new ResolvedMethodInst(
                                    mi.di(),OpCode.InvokeResolved,
                                    mi.lhs(),args,
                                    staticTarget.getTypeSignature(),
                                    staticTarget.asRemoteFunction()));
                        }
		    }
		    mi.remove();
		    setChangedCode();
		}
            }
        }
        if (changedCode()) code.killIntraBlockAnalyses();
    }
}


