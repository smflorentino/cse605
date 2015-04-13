/*
 * CodeParser.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fiji.asm.ClassReader;
import com.fiji.asm.Opcodes;
import com.fiji.asm.tree.AbstractInsnNode;
import com.fiji.asm.tree.ClassNode;
import com.fiji.asm.tree.FieldInsnNode;
import com.fiji.asm.tree.FrameNode;
import com.fiji.asm.tree.IincInsnNode;
import com.fiji.asm.tree.IntInsnNode;
import com.fiji.asm.tree.JumpInsnNode;
import com.fiji.asm.tree.LabelNode;
import com.fiji.asm.tree.LdcInsnNode;
import com.fiji.asm.tree.LineNumberNode;
import com.fiji.asm.tree.LookupSwitchInsnNode;
import com.fiji.asm.tree.MethodInsnNode;
import com.fiji.asm.tree.MethodNode;
import com.fiji.asm.tree.MultiANewArrayInsnNode;
import com.fiji.asm.tree.TableSwitchInsnNode;
import com.fiji.asm.tree.TryCatchBlockNode;
import com.fiji.asm.tree.TypeInsnNode;
import com.fiji.asm.tree.VarInsnNode;

import com.fiji.fivm.Constants;
import com.fiji.fivm.ReturnMode;

import com.fiji.util.MyStack;

/**
 * Parses bytecode to create a properly typed IR representation.
 * <p>
 * Features:
 * <ul>
 * <li>JSR inlining using ASM's inliner.  We hope they got it right; from reading
 *     their code that seems to be the case.</li>
 * <li>Full support for polymorphic variables (both on stack and in locals).</li>
 * <li>Tries to reduce the number of variables and assignments introduced,
 *     but more importantly, tries to create a representation that will be
 *     amenable to further transformations.</li>
 * </ul>
 * How it works:
 * <ol>
 * <li>Run ASM's JSR inliner.</li>
 * <li>Do an abstract interpretation (AI) over the bytecode, where the only state is the
 *     PC.  We need AI to get the stack heights only.  During the AI, generate the
 *     IR.  After generation, variables will be types BOTTOM, and each variable may
 *     take on multiple types during its different live ranges.</li>
 * <li>Do an AI on the resulting IR for the purpose of renaming variables according
 *     to the type they contain.  The state will be the mapping of old to new
 *     variables.</li>
 * </ol>
 * The resulting code will be "correct" but will have:
 * <ul>
 * <li>Stores to variables that are shortly used as an argument and then
 *     immediately die.
 * <li>More basic blocks and exception handling blocks than necessary.
 * </ul>
 * Both can be removed by applying simple optimizations.
 */
public class CodeParser {
    
    public static ArrayList< Code >
	parseMethods(VisibleClass c) {
	return parseMethods(c,null);
    }
    
    public static ArrayList< Code >
	parseMethods(VisibleClass c,
		     Set< MethodSignature > toParse) {
	try {
	    CodeParser cp=new CodeParser();
	    ClassNode cn=new ClassNode();
            new ClassReader(c.purifiedBytecode()).accept(cn,0);
	    ArrayList< Code > result=new ArrayList< Code >();
	    if (toParse!=null) toParse=Util.copy(toParse);
            HashMap< MethodSignature, MethodNode > bytecodes=
                new HashMap< MethodSignature, MethodNode >();
	    for (Iterator< ? > i=cn.methods.iterator();i.hasNext();) {
		MethodNode mn=(MethodNode)i.next();
		MethodSignature sig=
		    MethodSignature.parse(c.getContext(),mn.desc,mn.name);
                bytecodes.put(sig,mn);
            }
            for (VisibleMethod vm : c.methods()) {
                long beforeCPT=CodePhaseTimings.tic();
                
                MethodSignature sig=vm.getSignature();
		if (toParse==null || toParse.remove(sig)) {
		    if (Global.verbosity>=3)
			Global.log.println("Want to parse "+sig+" in "+c);
		    int oldVerbosity=Global.verbosity;
		    if (Global.noisyMethods.contains(vm)) {
			Global.verbosity=100;
		    }
		    try {
			Code code=null;
			try {
			    long before=System.currentTimeMillis();
			    try {
				if (vm.shouldHaveBytecode()) {
                                    MethodNode mn=bytecodes.get(sig);
                                    assert mn!=null;
				    code=cp.parseMethod(vm,mn);
				} else if (vm.shouldHaveNativeGlue()) {
				    code=cp.wrapNative(vm);
				} else if (vm.shouldHaveUnsupStub()) {
				    code=cp.makeUnsupStub(vm);
				} else if (vm.shouldHaveSyntheticImpl()) {
                                    code=cp.handleSynthetic(vm);
                                }
			    } catch (ResolutionFailed e) {
				if (Global.verbosity>=1) {
				    Global.log.println(
					"Got unrecoverable resolution failure while parsing "+
					"method: "+vm+": "+e);
				}
                                vm.getContext().resolutionReport.addUse(
                                    e.getResolutionID(),
                                    vm.getResolutionID());
				code=cp.makePatchThunk(vm,e);
			    }
			    long after=System.currentTimeMillis();
			    if (Global.verbosity>=3) {
				Global.log.println("parsed "+vm+" in "+(after-before)+" ms");
			    }
			} catch (Throwable e) {
			    throw new CompilerException("While parsing "+vm,e);
			}
			if (code!=null) {
			    result.add(code);
			}
			if (vm.shouldExport()) {
			    try {
				long before=System.currentTimeMillis();
				try {
				    result.add(cp.makeExport(vm));
				} catch (ResolutionFailed e) {
                                    // FIXME: we may have to insert patch thunks here, at
                                    // some point.  but it'll be hard since we have to ensure
                                    // that the patch thunk is correctly surrounded by exec
                                    // status transitions and handlifications
                                    throw e;
				}
				long after=System.currentTimeMillis();
				if (Global.verbosity>=3) {
				    Global.log.println("Wrapped for export "+vm+" in "+(after-before)+" ms");
				}
			    } catch (Throwable e) {
				throw new CompilerException("While wrapping for export "+vm,e);
			    }
			}
		    } finally {
			Global.verbosity=oldVerbosity;
		    }
		}
                
                CodePhaseTimings.toc(CodeParser.class.getName(),beforeCPT);
	    }
	    assert toParse==null || toParse.isEmpty() : toParse;
	    return result;
	} catch (Throwable e) {
            if (toParse==null) {
                throw new CompilerException("While parsing "+c,e);
            } else {
                throw new CompilerException("While parsing "+toParse+" in "+c,e);
            }
	}
    }
    
    VisibleMethod vm;
    MethodNode mn;
    Code c;
    
    SubstResultTypeCalc rtc;
    
    HashMap< Integer, Header > handlerForTarget;
    ArrayList< HandlerData > handlers; // parse these from bottom to top
    
    HashMap< Integer, Header > headers;
    HashMap< Integer, AbstractInsnNode > insnNodes;
    
    HashMap< Integer, MyStack< Arg > > seenPC;
    MyStack< Integer > worklistPC;

    // variable keys here are always the original variables
    HashMap< Var, HashMap< Exectype, Var > > substGlobal;
    HashMap< Header, HashMap< Var, Var > > substAtHead;
    HashMap< Header, HashMap< Var, Var > > substAtTail;
    HashMap< Header, HashMap< Var, Var > > substForHandler;

    Code parseMethod(VisibleMethod vm,
                     MethodNode mn) {
	this.vm=vm;
	this.mn=mn;
	if (Global.verbosity>=3) Global.log.println("Parsing "+vm+" from "+mn);
	c=new Code(vm);

        try {
            parseMethodImpl();
        } catch (ResolutionFailed e) {
            throw e;
        } catch (Throwable e) {
            Global.log.println("Got an unexpected exception while parsing "+vm+":");
            e.printStackTrace(Global.log);
            Global.log.println("Code at time of error:");
            CodeDumper.dump(c,Global.log);
            Util.rethrow(e);
        }
        
        return c;
    }

    private Code parseMethodImpl() {
	// set up local variables
        Var receiver=c.addVar();
        
	Var[] vars=new Var[mn.maxLocals];
	for (int i=0;i<mn.maxLocals;++i) {
	    vars[i]=c.addVar();
	}
	
	// figure out line numbers
	HashMap< Integer, Integer > pcToLineNumber=
	    new HashMap< Integer, Integer >();
	
	int lastLineNumber=0;
	boolean sawLineNumberNode=false;
	
	for (AbstractInsnNode ain=mn.instructions.getFirst();
	     ain!=null;ain=ain.getNext()) {
	    if (ain instanceof LineNumberNode) {
		lastLineNumber=((LineNumberNode)ain).line;
		sawLineNumberNode=true;
	    }
	    pcToLineNumber.put(ain.getBCOffset(),lastLineNumber);
	}

	assert pcToLineNumber.containsKey(0);
	
	if (!sawLineNumberNode && Global.verbosity>=2) {
	    Global.log.println("Warning: no line number info for "+vm);
	}
	
	// figure out basic blocks
	headers=new HashMap< Integer, Header >();
	handlerForTarget=new HashMap< Integer, Header >();
	handlers=new ArrayList< HandlerData >();
        insnNodes=new HashMap< Integer, AbstractInsnNode >();
	
	HashMap< Integer, Header > excGetters=new HashMap< Integer, Header >();
	
	for (Iterator< ? > i=mn.tryCatchBlocks.iterator();i.hasNext();) {
	    TryCatchBlockNode tcbn=(TryCatchBlockNode)i.next();
            int targetPC=tcbn.handler.getBCOffset();
	    if (Global.verbosity>=5) {
		Global.log.println("Creating catch block for "+targetPC);
	    }
	    VisibleClass handles=Global.root().throwableClass;
	    if (tcbn.type!=null) {
		handles=vm.getContext().getClass(tcbn.type);
	    }
	    DebugInfo di=new DebugInfo(c,targetPC,pcToLineNumber.get(targetPC));
	    if (headers.get(targetPC)==null) {
		headers.put(targetPC,c.addHeader(di));
                insnNodes.put(targetPC,tcbn.handler);
	    }
	    if (excGetters.get(targetPC)==null) {
		Header eg=c.addHeader(di);
		eg.setFooter(new Jump(di,headers.get(targetPC)));
		excGetters.put(targetPC,eg);
	    }
	    HandlerData hd=
		new HandlerData(
		    targetPC,
                    tcbn.start.getBCOffset(),
                    tcbn.end.getBCOffset(),
		    handles,
		    excGetters.get(targetPC),
		    headers.get(targetPC));
	    handlers.add(hd);
	    handlerForTarget.put(targetPC,hd.handler);
	}
	
	for (AbstractInsnNode ain=mn.instructions.getFirst();
	     ain!=null;ain=ain.getNext()) {
	    // hopefully asm does the Right Thing in choosing where to
	    // put these labels.
	    if (ain instanceof LabelNode ||
		ain.getPrevious() instanceof JumpInsnNode ||
		ain.getPrevious() instanceof TableSwitchInsnNode ||
		ain.getPrevious() instanceof LookupSwitchInsnNode) {
                int pc=ain.getBCOffset();
		if (headers.get(pc)==null) {
		    headers.put(pc,
				c.addHeader(
				    new DebugInfo(c,pc,pcToLineNumber.get(pc))));
                    insnNodes.put(pc,ain);
		}
		// find limit
		int limit=handlers.size();
		for (int i=0;i<handlers.size();++i) {
		    HandlerData hd=handlers.get(i);
		    if (pc>=hd.startPC && pc<hd.endPC &&
			hd.handles==Global.root().throwableClass) {
			limit=i+1;
			break;
		    }
		}
		if (Global.verbosity>=6) {
		    Global.log.println(
			"For pc = "+pc+": limit = "+limit);
		}
		// from the limit, going up, figure out the handlers, create
		// them, and link them in
		Header h=headers.get(pc);
		assert pc==h.di().pc() : h;
		ExceptionHandler last=null;
		for (int i=limit;i-->0;) {
		    HandlerData hd=handlers.get(i);
		    if (Global.verbosity>=7) {
			Global.log.println("  Considering "+hd);
		    }
		    if (pc>=hd.startPC && pc<hd.endPC) {
			if (Global.verbosity>=7) {
			    Global.log.println("   Accepting "+hd);
			}
			DebugInfo di=
			    new DebugInfo(c,hd.targetPC,
					  pcToLineNumber.get(hd.targetPC));
			Header merger=c.addHeader(di);
			last=c.addHandler(di,
					  hd.handles,
					  last,
					  merger);
			merger.setFooter(new Jump(di,hd.excGetter));
		    }
		}
		h.setHandler(last);
	    }
	}
	
	// set up root block and exception handlers
	DebugInfo rootdi=new DebugInfo(c,0,pcToLineNumber.get(0));
	if (headers.get(0)==null) {
	    headers.put(0,c.addHeader(rootdi));
            insnNodes.put(0,mn.instructions.getFirst());
	}
	c.setRoot(c.addHeader(rootdi));
	c.root().setFooter(new Jump(rootdi,headers.get(0)));
        
        // all variables start out NIL
        c.root().append(
            new SimpleInst(
                rootdi,OpCode.Mov,
                receiver,new Arg[]{Arg.NIL}));
        
        for (int i=0;i<vars.length;++i) {
            c.root().append(
                new SimpleInst(
                    rootdi,OpCode.Mov,
                    vars[i],new Arg[]{Arg.NIL}));
        }
        
        if (vm.saveReceiver()) {
            c.root().append(
                new ArgInst(rootdi,OpCode.GetArg,receiver,Arg.EMPTY,0));
        }
        
	Type[] allParams=vm.getAllParams();
	for (int i=0,j=0;i<allParams.length;++i) {
	    c.root().append(
                new ArgInst(rootdi,OpCode.GetArg,vars[j],Arg.EMPTY,i));
	    j+=allParams[i].effectiveBasetype().cells;
	}
	
	// hack alert: we're lubbing as Exectype, but then converting back to
	// Type.  that is "fine" in the sense that lubbing is undefined for
	// Types, but it would be cleaner if we just had an ExectypeInst.
	HashMap< Header, Exectype > lubbedException=
	    new HashMap< Header, Exectype >();
	for (HandlerData hd : handlers) {
	    Exectype oldType=lubbedException.get(hd.excGetter);
	    if (oldType==null) {
		lubbedException.put(
		    hd.excGetter,
		    Exectype.make(hd.handles));
	    } else {
		lubbedException.put(
		    hd.excGetter,
		    Exectype.lub(Exectype.make(hd.handles),
				 oldType));
	    }
	}
	
	// set up header ordering.  useful for debugging.
	c.recomputeOrder();
	
	if (Global.verbosity>=6) {
	    for (HandlerData hd : handlers) {
		Global.log.println(hd);
		assert hd.excGetter.getFooter().next()==hd.handler;
	    }
	    
	    for (Header h : c.headers()) {
		Global.log.print("For "+h+" ("+h.di()+"): handler = "+h.handler());
		if (h.handler()==null) {
		    Global.log.println();
		} else {
		    Global.log.println(" -> "+h.handler().next()+" -> "+
					((Header)h.handler().next()).getFooter().next());
		}
	    }
	}
	
	// parse the code.  this is a single pass.  only insert type
	// info where it's obvious, and where we're not generating
	// instructions that already must be handled by the second
	// pass below.  as such, most of the type inference happens
	// only after we parse the code.
	seenPC=new HashMap< Integer, MyStack< Arg > >();
	worklistPC=new MyStack< Integer >();
	
	// enqueue start
	seenPC.put(0,new MyStack< Arg >());
	worklistPC.push(0);

	// deal with exception handlers
	for (Map.Entry< Header, Exectype > e : lubbedException.entrySet()) {
	    Var excVar=c.addVar(e.getValue());
	    e.getKey().append(
		new TypeInst(
		    e.getKey().di(),OpCode.GetException,
		    excVar,Arg.EMPTY,Type.make(e.getValue())));
	    e.getKey().append(
		new SimpleInst(
		    e.getKey().di(),OpCode.ClearException,
		    Var.VOID,Arg.EMPTY));
	    MyStack< Arg > curStack=new MyStack< Arg >();
	    curStack.push(excVar);
	    int pc=e.getKey().di().pc();
	    if (Global.verbosity>=6) {
		Global.log.println(
		    "Exception handler at "+e.getKey());
		//Context.log.println("pc = "+pc);
	    }
	    seenPC.put(pc,curStack);
	    worklistPC.push(pc);
	}
        
        if (Global.verbosity>=3) {
            Global.log.println("We have headers at: "+headers.keySet());
            Global.log.println("We have instruction nodes at: "+insnNodes.keySet());
        }
	
	while (!worklistPC.empty()) {
	    int pc=worklistPC.pop();
	    Header h=headers.get(pc);
            
            assert h!=null : pc;

	    MyStack< Arg > stack=seenPC.get(pc).copy();
            
            if (Global.verbosity>=3) {
                Global.log.println("Starting block with pc = "+pc);
            }
	    
	    // parse the basic block
	    boolean done=false;
	    int firstPC=pc;
	    for (AbstractInsnNode ain=insnNodes.get(pc);
		 !done;ain=ain.getNext()) {
                assert ain!=null : pc;
		pc=ain.getBCOffset();
                if (Global.verbosity>=3) {
                    Global.log.println("At pc = "+pc+"; ain = "+ain);
                }
		DebugInfo di=new DebugInfo(c,pc,pcToLineNumber.get(pc));
		if (headers.containsKey(pc) && pc!=firstPC) {
                    if (Global.verbosity>=3) {
                        Global.log.println("Skipping pc = "+pc+" because: headers.containsKey(pc) = "+headers.containsKey(pc)+", firstPC = "+firstPC);
                    }
		    h.setFooter(new Jump(di,headers.get(pc)));
		    mergeWith(pc,h,stack);
		    done=true;
		    break;
		}
		if (ain instanceof LabelNode ||
		    ain instanceof FrameNode ||
		    ain instanceof LineNumberNode) continue;
		try {
		    switch (ain.getOpcode()) {
		    case Opcodes.AALOAD:
		    case Opcodes.BALOAD:
		    case Opcodes.CALOAD:
		    case Opcodes.DALOAD:
		    case Opcodes.FALOAD:
		    case Opcodes.IALOAD:
		    case Opcodes.LALOAD:
		    case Opcodes.SALOAD: {
			Arg index=stack.pop();
			Arg array=stack.pop();
			Var result=c.addVar();
			stack.push(result);
			h.append(
			    new HeapAccessInst(
				di,OpCode.ArrayLoad,
				result,new Arg[]{array,index},
				ArrayElementField.INSTANCE));
			if (ain.getOpcode()==Opcodes.DALOAD ||
			    ain.getOpcode()==Opcodes.LALOAD) {
			    stack.push(Arg.NIL);
			}
			break;
		    }
		    case Opcodes.AASTORE:
		    case Opcodes.BASTORE:
		    case Opcodes.CASTORE:
		    case Opcodes.DASTORE:
		    case Opcodes.FASTORE:
		    case Opcodes.IASTORE:
		    case Opcodes.LASTORE:
		    case Opcodes.SASTORE: {
			if (ain.getOpcode()==Opcodes.DASTORE ||
			    ain.getOpcode()==Opcodes.LASTORE) {
			    stack.pop();
			}
			Arg value=stack.pop();
			Arg index=stack.pop();
			Arg array=stack.pop();
			h.append(
			    new HeapAccessInst(
				di,OpCode.ArrayStore,
				Var.VOID,new Arg[]{array,index,value},
				ArrayElementField.INSTANCE));
			break;
		    }
		    case Opcodes.ACONST_NULL: {
			stack.push(Arg.NULL);
			break;
		    }
		    case Opcodes.ALOAD: 
		    case Opcodes.DLOAD:
		    case Opcodes.FLOAD:
		    case Opcodes.ILOAD: 
		    case Opcodes.LLOAD: {
			int var=((VarInsnNode)ain).var;
			Var result=c.addVar();
			h.append(
			    new SimpleInst(
				di,OpCode.Mov,
				result,new Arg[]{vars[var]}));
			stack.push(result);
			if (ain.getOpcode()==Opcodes.DLOAD ||
			    ain.getOpcode()==Opcodes.LLOAD) {
			    stack.push(Arg.NIL);
			}
			break;
		    }
		    case Opcodes.ANEWARRAY: {
			Arg size=stack.pop();
			Type type=Type.parseRefOnly(c.getContext(),
						    ((TypeInsnNode)ain).desc).makeArray();
			type.checkResolved();
			Var result=c.addVar();
			h.append(
			    new TypeInst(
				di,OpCode.NewArray,
				result,new Arg[]{size},
				type));
			stack.push(result);
			break;
		    }
		    case Opcodes.ARETURN:
		    case Opcodes.ATHROW:
		    case Opcodes.DRETURN:
		    case Opcodes.FRETURN:
		    case Opcodes.IRETURN:
		    case Opcodes.LRETURN:
		    case Opcodes.RETURN: {
			if (ain.getOpcode()==Opcodes.DRETURN ||
			    ain.getOpcode()==Opcodes.LRETURN) {
			    stack.pop();
			}
			Arg a=null;
			if (ain.getOpcode()!=Opcodes.RETURN) a=stack.pop();
			OpCode opcode;
			if (ain.getOpcode()==Opcodes.ATHROW) {
			    opcode=OpCode.Throw;
			} else {
			    opcode=OpCode.Return;
			}
			h.setFooter(
			    new Terminal(
				di,
				opcode,
				ain.getOpcode()==Opcodes.RETURN
				?Arg.EMPTY:new Arg[]{a}));
			done=true;
			break;
		    }
		    case Opcodes.ARRAYLENGTH: {
			Var result=c.addVar();
			h.append(
			    new SimpleInst(
				di,OpCode.ArrayLength,
				result,new Arg[]{stack.pop()}));
			stack.push(result);
			break;
		    }
		    case Opcodes.ASTORE:
		    case Opcodes.DSTORE:
		    case Opcodes.FSTORE:
		    case Opcodes.ISTORE:
		    case Opcodes.LSTORE: {
			int var=((VarInsnNode)ain).var;
			if (ain.getOpcode()==Opcodes.DSTORE ||
			    ain.getOpcode()==Opcodes.LSTORE) {
			    stack.pop();
			}
			h.append(
			    new SimpleInst(
				di,OpCode.Mov,
				vars[var],
				new Arg[]{stack.pop()}));
                        if (ain.getOpcode()==Opcodes.DSTORE ||
                            ain.getOpcode()==Opcodes.LSTORE) {
                            h.append(
                                new SimpleInst(
                                    di,OpCode.Mov,
                                    vars[var+1],
                                    new Arg[]{Arg.NIL}));
                        }
			break;
		    }
		    case Opcodes.BIPUSH:
		    case Opcodes.SIPUSH: {
			stack.push(new IntConst(((IntInsnNode)ain).operand));
			break;
		    }
		    case Opcodes.CHECKCAST: {
			Var result=c.addVar();
			if (vm.noSafetyChecks()) {
			    h.append(
				new TypeInst(
				    di,OpCode.Cast,
				    result,new Arg[]{stack.pop()},
				    Type.parseRefOnly(c.getContext(),((TypeInsnNode)ain).desc).checkResolved()));
			} else {
			    h.append(
				new TypeCheckInst(
				    di,
				    result,new Arg[]{stack.pop()},
				    Type.parseRefOnly(c.getContext(),((TypeInsnNode)ain).desc).checkResolved(),
				    Runtime.classCastException.asType()));
			}
			stack.push(result);
			break;
		    }
		    case Opcodes.F2L:
		    case Opcodes.F2I:
		    case Opcodes.D2F:
		    case Opcodes.D2I: 
		    case Opcodes.D2L:
		    case Opcodes.F2D:
		    case Opcodes.I2B:
		    case Opcodes.I2C:
		    case Opcodes.I2D:
		    case Opcodes.I2F:
		    case Opcodes.I2L:
		    case Opcodes.I2S:
		    case Opcodes.L2D:
		    case Opcodes.L2F:
		    case Opcodes.L2I: {
			if (ain.getOpcode()==Opcodes.D2F ||
			    ain.getOpcode()==Opcodes.D2I ||
			    ain.getOpcode()==Opcodes.D2L ||
			    ain.getOpcode()==Opcodes.L2D ||
			    ain.getOpcode()==Opcodes.L2F ||
			    ain.getOpcode()==Opcodes.L2I) stack.pop();
			Var result=c.addVar();
			Type resultType;
			switch (ain.getOpcode()) {
			case Opcodes.L2F:
			case Opcodes.I2F:
			case Opcodes.D2F: resultType=Type.FLOAT; break;
			case Opcodes.F2I:
			case Opcodes.L2I:
			case Opcodes.D2I: resultType=Type.INT; break;
			case Opcodes.F2L:
			case Opcodes.I2L:
			case Opcodes.D2L: resultType=Type.LONG; break;
			case Opcodes.L2D:
			case Opcodes.I2D:
			case Opcodes.F2D: resultType=Type.DOUBLE; break;
			case Opcodes.I2B: resultType=Type.BYTE; break;
			case Opcodes.I2C: resultType=Type.CHAR; break;
			case Opcodes.I2S: resultType=Type.SHORT; break;
			default: throw new Error("bad opcode: "+ain.getOpcode());
			}
			h.append(
			    new TypeInst(
				di,OpCode.Cast,
				result,new Arg[]{stack.pop()},
				resultType));
			stack.push(result);
			if (ain.getOpcode()==Opcodes.D2L ||
			    ain.getOpcode()==Opcodes.F2D ||
			    ain.getOpcode()==Opcodes.I2D ||
			    ain.getOpcode()==Opcodes.I2L ||
			    ain.getOpcode()==Opcodes.L2D ||
			    ain.getOpcode()==Opcodes.F2L) stack.push(Arg.NIL);
			break;
		    }
		    case Opcodes.DADD:
		    case Opcodes.DDIV:
		    case Opcodes.DREM:
		    case Opcodes.DMUL: 
		    case Opcodes.DSUB:
		    case Opcodes.LADD:
		    case Opcodes.LDIV:
		    case Opcodes.LREM:
		    case Opcodes.LMUL: 
		    case Opcodes.LSUB:
		    case Opcodes.FADD:
		    case Opcodes.FDIV:
		    case Opcodes.FREM:
		    case Opcodes.FMUL: 
		    case Opcodes.FSUB:
		    case Opcodes.IADD:
		    case Opcodes.IDIV:
		    case Opcodes.IREM:
		    case Opcodes.IMUL: 
		    case Opcodes.ISUB:
		    case Opcodes.IAND:
		    case Opcodes.ISHL:
		    case Opcodes.ISHR:
		    case Opcodes.IUSHR:
		    case Opcodes.IOR:
		    case Opcodes.IXOR:
		    case Opcodes.LAND:
		    case Opcodes.LOR:
		    case Opcodes.LXOR:
		    case Opcodes.LSHL:
		    case Opcodes.LSHR:
		    case Opcodes.LUSHR: {
			OpCode opcode=null;
			switch (ain.getOpcode()) {
			case Opcodes.LADD:
			case Opcodes.IADD:
			case Opcodes.FADD:
			case Opcodes.DADD: opcode=OpCode.Add; break;
			case Opcodes.LDIV:
			case Opcodes.IDIV:
			case Opcodes.FDIV:
			case Opcodes.DDIV: opcode=OpCode.Div; break;
			case Opcodes.LMUL:
			case Opcodes.IMUL:
			case Opcodes.FMUL:
			case Opcodes.DMUL: opcode=OpCode.Mul; break;
			case Opcodes.LREM:
			case Opcodes.IREM:
			case Opcodes.FREM:
			case Opcodes.DREM: opcode=OpCode.Mod; break;
			case Opcodes.LSUB:
			case Opcodes.ISUB:
			case Opcodes.FSUB:
			case Opcodes.DSUB: opcode=OpCode.Sub; break;
			case Opcodes.LAND:
			case Opcodes.IAND: opcode=OpCode.And; break;
			case Opcodes.LSHR:
			case Opcodes.ISHR: opcode=OpCode.Shr; break;
			case Opcodes.LSHL:
			case Opcodes.ISHL: opcode=OpCode.Shl; break;
			case Opcodes.LUSHR:
			case Opcodes.IUSHR: opcode=OpCode.Ushr; break;
			case Opcodes.LOR:
			case Opcodes.IOR: opcode=OpCode.Or; break;
			case Opcodes.LXOR:
			case Opcodes.IXOR: opcode=OpCode.Xor; break;
			default: assert false:ain; break;
			}
			boolean extraCell=
			    ain.getOpcode()==Opcodes.DADD ||
			    ain.getOpcode()==Opcodes.DDIV ||
			    ain.getOpcode()==Opcodes.DMUL ||
			    ain.getOpcode()==Opcodes.DREM ||
			    ain.getOpcode()==Opcodes.DSUB ||
			    ain.getOpcode()==Opcodes.LADD ||
			    ain.getOpcode()==Opcodes.LDIV ||
			    ain.getOpcode()==Opcodes.LMUL ||
			    ain.getOpcode()==Opcodes.LREM ||
			    ain.getOpcode()==Opcodes.LSUB ||
			    ain.getOpcode()==Opcodes.LAND ||
			    ain.getOpcode()==Opcodes.LOR ||
			    ain.getOpcode()==Opcodes.LXOR ||
			    ain.getOpcode()==Opcodes.LSHR ||
			    ain.getOpcode()==Opcodes.LSHL ||
			    ain.getOpcode()==Opcodes.LUSHR;
			if (extraCell &&
			    ain.getOpcode()!=Opcodes.LSHR &&
			    ain.getOpcode()!=Opcodes.LSHL &&
			    ain.getOpcode()!=Opcodes.LUSHR) stack.pop();
			Arg arg2=stack.pop();
			if (extraCell) stack.pop();
			Arg arg1=stack.pop(); 
			Var result=c.addVar();
			h.append(
			    new SimpleInst(
				di,opcode,
				result,new Arg[]{arg1,arg2}));
			stack.push(result);
			if (extraCell) stack.push(Arg.NIL);
			break;
		    }
		    case Opcodes.DCMPG:
		    case Opcodes.DCMPL:
		    case Opcodes.FCMPG:
		    case Opcodes.FCMPL:
		    case Opcodes.LCMP: {
			OpCode opcode=null;
			switch (ain.getOpcode()) {
			case Opcodes.LCMP:
			case Opcodes.DCMPG:
			case Opcodes.FCMPG: opcode=OpCode.CompareG; break;
			case Opcodes.DCMPL:
			case Opcodes.FCMPL: opcode=OpCode.CompareL; break;
			default: assert false:ain; break;
			}
			boolean extraCell=
			    ain.getOpcode()==Opcodes.LCMP ||
			    ain.getOpcode()==Opcodes.DCMPL ||
			    ain.getOpcode()==Opcodes.DCMPG;
			if (extraCell) stack.pop();
			Arg arg2=stack.pop();
			if (extraCell) stack.pop();
			Arg arg1=stack.pop(); 
			Var result=c.addVar();
			h.append(
			    new SimpleInst(
				di,
				opcode,
				result,
				new Arg[]{arg1,arg2}));
			stack.push(result);
			break;
		    }
		    case Opcodes.DCONST_0:
		    case Opcodes.DCONST_1: {
			Var result=c.addVar();
                        h.append(
                            new SimpleInst(
                                di,OpCode.Mov,
                                result,new Arg[]{
                                    DoubleConst.make((double)(ain.getOpcode()-Opcodes.DCONST_0))
                                }));
			stack.push(result);
			stack.push(Arg.NIL);
			break;
		    }
		    case Opcodes.FCONST_0:
		    case Opcodes.FCONST_1:
		    case Opcodes.FCONST_2: {
			Var result=c.addVar();
			h.append(
			    new SimpleInst(
				di,OpCode.Mov,
                                result,new Arg[]{
                                    FloatConst.make((float)(ain.getOpcode()-Opcodes.FCONST_0))
                                }));
			stack.push(result);
			break;
		    }
		    case Opcodes.LCONST_0:
		    case Opcodes.LCONST_1: {
			Var result=c.addVar();
			h.append(
			    new SimpleInst(
				di,OpCode.Mov,
                                result,new Arg[]{
                                    LongConst.make((long)(ain.getOpcode()-Opcodes.LCONST_0))
                                }));
			stack.push(result);
			stack.push(Arg.NIL);
			break;
		    }
		    case Opcodes.LNEG:
		    case Opcodes.DNEG:
		    case Opcodes.FNEG:
		    case Opcodes.INEG: {
			if (ain.getOpcode()==Opcodes.DNEG ||
			    ain.getOpcode()==Opcodes.LNEG) stack.pop();
			Var result=c.addVar();
			h.append(
			    new SimpleInst(
				di,OpCode.Neg,
				result,
				new Arg[]{stack.pop()}));
			stack.push(result);
			if (ain.getOpcode()==Opcodes.DNEG ||
			    ain.getOpcode()==Opcodes.LNEG) stack.push(Arg.NIL);
			break;
		    }
		    case Opcodes.DUP: {
			Arg a=stack.pop();
			stack.push(a);
			stack.push(a);
			break;
		    }
		    case Opcodes.DUP_X1: {
			Arg v1=stack.pop();
			Arg v2=stack.pop();
			stack.push(v1);
			stack.push(v2);
			stack.push(v1);
			break;
		    }
		    case Opcodes.DUP_X2: {
			Arg v1=stack.pop();
			Arg v2=stack.pop();
			Arg v3=stack.pop();
			stack.push(v1);
			stack.push(v3);
			stack.push(v2);
			stack.push(v1);
			break;
		    }
		    case Opcodes.DUP2: {
			Arg v1=stack.pop();
			Arg v2=stack.pop();
			stack.push(v2);
			stack.push(v1);
			stack.push(v2);
			stack.push(v1);
			break;
		    }
		    case Opcodes.DUP2_X1: {
			Arg v1=stack.pop();
			Arg v2=stack.pop();
			Arg v3=stack.pop();
			stack.push(v2);
			stack.push(v1);
			stack.push(v3);
			stack.push(v2);
			stack.push(v1);
			break;
		    }
		    case Opcodes.DUP2_X2: {
			Arg v1=stack.pop();
			Arg v2=stack.pop();
			Arg v3=stack.pop();
			Arg v4=stack.pop();
			stack.push(v2);
			stack.push(v1);
			stack.push(v4);
			stack.push(v3);
			stack.push(v2);
			stack.push(v1);
			break;
		    }
		    case Opcodes.GETFIELD:
		    case Opcodes.GETSTATIC:
		    case Opcodes.PUTFIELD:
		    case Opcodes.PUTSTATIC: {
			FieldInsnNode fin=(FieldInsnNode)ain;
			VisibleClass clazz=c.getContext().getClass(fin.owner);
			FieldSignature sig=FieldSignature.parse(c.getContext(),
								fin.desc,
								fin.name);
			VisibleField field=c.getContext().resolve(c.getOwner(),
								  clazz,
								  sig);
			switch (fin.getOpcode()) {
			case Opcodes.GETFIELD: {
			    Arg ref=stack.pop();
			    Var result=c.addVar();
			    h.append(
				new HeapAccessInst(
				    di,OpCode.GetField,
				    result,new Arg[]{ref},
				    field));
			    stack.push(result);
			    if (field.getType().effectiveBasetype().cells==2) {
				stack.push(Arg.NIL);
			    }
			    break;
			}
			case Opcodes.PUTFIELD: {
			    if (field.getType().effectiveBasetype().cells==2) {
				stack.pop();
			    }
			    Arg value=stack.pop();
			    Arg ref=stack.pop();
			    h.append(
				new HeapAccessInst(
				    di,OpCode.PutField,
				    Var.VOID,new Arg[]{ref,value},
				    field));
			    break;
			}
			case Opcodes.GETSTATIC: {
			    Var result=c.addVar();
			    h.append(
				new HeapAccessInst(
				    di,OpCode.GetStatic,
				    result,Arg.EMPTY,
				    field));
			    stack.push(result);
			    if (field.getType().effectiveBasetype().cells==2) {
				stack.push(Arg.NIL);
			    }
			    break;
			}
			case Opcodes.PUTSTATIC: {
			    if (field.getType().effectiveBasetype().cells==2) {
				stack.pop();
			    }
			    h.append(
				new HeapAccessInst(
				    di,OpCode.PutStatic,
				    Var.VOID,new Arg[]{stack.pop()},
				    field));
			    break;
			}
			default: assert false:fin;
			}
			break;
		    }
		    case Opcodes.GOTO: {
			int targPC=((JumpInsnNode)ain).label.getBCOffset();
			h.setFooter(new Jump(di,headers.get(targPC)));
			mergeWith(targPC,h,stack);
			done=true;
			break;
		    }
		    case Opcodes.ICONST_M1:
		    case Opcodes.ICONST_0:
		    case Opcodes.ICONST_1:
		    case Opcodes.ICONST_2:
		    case Opcodes.ICONST_3:
		    case Opcodes.ICONST_4:
		    case Opcodes.ICONST_5: {
			stack.push(new IntConst(ain.getOpcode()-Opcodes.ICONST_M1-1));
			break;
		    }
		    case Opcodes.IF_ACMPEQ:
		    case Opcodes.IF_ACMPNE:
		    case Opcodes.IF_ICMPEQ:
		    case Opcodes.IF_ICMPNE:
		    case Opcodes.IF_ICMPLT:
		    case Opcodes.IF_ICMPGE:
		    case Opcodes.IF_ICMPGT:
		    case Opcodes.IF_ICMPLE:
		    case Opcodes.IFEQ:
		    case Opcodes.IFNE:
		    case Opcodes.IFLT:
		    case Opcodes.IFGE:
		    case Opcodes.IFGT:
		    case Opcodes.IFLE:
		    case Opcodes.IFNONNULL:
		    case Opcodes.IFNULL: {
			Arg a,b;
			switch (ain.getOpcode()) {
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFLE:
			    b=IntConst.make(0);
			    break;
			case Opcodes.IFNONNULL:
			case Opcodes.IFNULL:
			    b=Arg.NULL;
			    break;
			default:
			    b=stack.pop();
			    break;
			}
			a=stack.pop();
			Arg cmpRes;
			OpCode opcode;
			switch (ain.getOpcode()) {
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFNONNULL:
			case Opcodes.IFNULL: opcode=OpCode.Eq; break;
			default: opcode=OpCode.LessThan; break;
			}
			switch (ain.getOpcode()) {
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
			case Opcodes.IFGT:
			case Opcodes.IFLE: {
			    Arg tmp=a;
			    a=b;
			    b=tmp;
			    break;
			}
			default: break;
			}
			switch (ain.getOpcode()) {
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			    cmpRes=a;
			    break;
			default:
			    cmpRes=c.addVar();
			    h.append(
				new SimpleInst(
				    di,opcode,
				    (Var)cmpRes,new Arg[]{a,b}));
			    break;
			}
			int truePC=((JumpInsnNode)ain).label.getBCOffset();
			int falsePC=((JumpInsnNode)ain).getNext().getBCOffset();
			if (Global.verbosity>=5) {
			    Global.log.println("truePC = "+truePC+", falsePC = "+falsePC);
			}
			OpCode branchOpcode;
			switch (ain.getOpcode()) {
			case Opcodes.IF_ACMPNE:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPLE:
			case Opcodes.IFEQ:
			case Opcodes.IFNONNULL:
			case Opcodes.IFLE:
			case Opcodes.IFGE:
			    branchOpcode=OpCode.BranchZero;
			    break;
			default:
			    branchOpcode=OpCode.BranchNonZero;
			    break;
			}
			if (Global.verbosity>=5) {
			    Global.log.println("truePC = "+truePC+", falsePC = "+falsePC);
			    Global.log.println("h = "+h+", true h = "+headers.get(truePC)+
						", false h = "+headers.get(falsePC));
			}
			h.setFooter(
			    new Branch(
				di,branchOpcode,
				new Arg[]{cmpRes},
				headers.get(falsePC),
				headers.get(truePC)));
			mergeWith(truePC,h,stack);
			mergeWith(falsePC,h,stack);
			done=true;
			break;
		    }
		    case Opcodes.IINC: {
			IincInsnNode iin=(IincInsnNode)ain;
			h.append(
			    new SimpleInst(
				di,OpCode.Add,
				vars[iin.var],
				new Arg[]{
				    vars[iin.var],
				    new IntConst(iin.incr)
				}));
			break;
		    }
		    case Opcodes.INSTANCEOF: {
			TypeInsnNode tin=(TypeInsnNode)ain;
			Arg obj=stack.pop();
			Var result=c.addVar();
			h.append(
			    new TypeInst(
				di,OpCode.Instanceof,
				result,new Arg[]{obj},
				Type.parseRefOnly(c.getContext(),tin.desc).checkResolved()));
			stack.push(result);
			break;
		    }
		    case Opcodes.INVOKEINTERFACE:
		    case Opcodes.INVOKEVIRTUAL:
		    case Opcodes.INVOKESPECIAL:
		    case Opcodes.INVOKESTATIC: {
			MethodInsnNode min=(MethodInsnNode)ain;
			Type t=Type.parseRefOnly(c.getContext(),min.owner).checkResolved();
			VisibleClass owner;
			if (!t.hasEffectiveClass()) {
			    throw new BadBytecode("Bad type for invocation: "+t);
			}
			owner=t.effectiveClass();
			OpCode opcode;
			switch (ain.getOpcode()) {
			case Opcodes.INVOKEINTERFACE:
			    if (!owner.isInterface()) {
				throw new BadBytecode(
				    "INVOKEINTERFACE on something other than "+
				    "interface: "+owner);
			    }
			    opcode=OpCode.InvokeDynamic;
			    break;
			case Opcodes.INVOKEVIRTUAL:
			    if (owner.isInterface()) {
				throw new BadBytecode(
				    "INVOKEVIRTUAL on an interface: "+owner);
			    }
			    opcode=OpCode.InvokeDynamic;
			    break;
			case Opcodes.INVOKESPECIAL:
			    opcode=OpCode.Invoke;
			    break;
			case Opcodes.INVOKESTATIC:
			    opcode=OpCode.InvokeStatic;
			    break;
			default: throw new Error();
			}
			VisibleMethod method;
			MethodSignature sig=
			    MethodSignature.parse(c.getContext(),
						  min.desc,
						  min.name);
			if (ain.getOpcode()==Opcodes.INVOKESPECIAL) {
			    method=c.getContext().resolveSpecial(c.getOwner(),
								 owner,
								 sig);
			} else {
			    method=c.getContext().resolve(c.getOwner(),
							  owner,
							  sig);
			}
			if (ain.getOpcode()==Opcodes.INVOKESTATIC) {
			    if (!method.isStatic()) {
				throw new BadBytecode(
				    "Attempt to use a non-static method with an "+
				    "INVOKESTATIC");
			    }
			} else {
			    if (method.isStatic()) {
				throw new BadBytecode(
				    "Attempt to use a static method with something "+
				    "other than INVOKESTATIC");
			    }
			}
			Type[] params=method.getAllParams();
			Arg[] args=new Arg[params.length];
                        if (Global.verbosity>=3) {
                            Global.log.println("stack = "+stack);
                            Global.log.println("params = "+Util.dump(params));
                        }
			for (int i=params.length;i-->0;) {
			    if (params[i].effectiveBasetype().cells==2) {
				stack.pop();
			    }
			    args[i]=stack.pop();
			}
			Var result;
			if (method.getType()==Type.VOID) {
			    result=Var.VOID;
			} else {
			    result=c.addVar(method.getType().asExectype());
			}
			h.append(
			    new MethodInst(
				di,opcode,
				result,args,
				method));
			if (result!=Var.VOID) {
			    stack.push(result);
			    if (method.getType().effectiveBasetype().cells==2) {
				stack.push(Arg.NIL);
			    }
			}
			break;
		    }
		    case Opcodes.LDC: {
			Object data=((LdcInsnNode)ain).cst;
			if (data instanceof Integer) {
			    stack.push(new IntConst((Integer)data));
			} else {
			    Var v=c.addVar();
			    if (data instanceof Float) {
                                h.append(
                                    new SimpleInst(
                                        di,OpCode.Mov,
                                        v,new Arg[]{
                                            FloatConst.make((Float)data)
                                        }));
			    } else if (data instanceof Long) {
                                h.append(
                                    new SimpleInst(
                                        di,OpCode.Mov,
                                        v,new Arg[]{
                                            LongConst.make((Long)data)
                                        }));
			    } else if (data instanceof Double) {
                                h.append(
                                    new SimpleInst(
                                        di,OpCode.Mov,
                                        v,new Arg[]{
                                            DoubleConst.make((Double)data)
                                        }));
			    } else if (data instanceof String) {
				h.append(new GetStringInst(di,v,(String)data));
			    } else if (data instanceof com.fiji.asm.Type) {
				Type t=Type.parse(
				    c.getContext(),
				    ((com.fiji.asm.Type)data).getDescriptor()).checkResolved();
				h.append(new TypeInst(di,OpCode.GetType,v,Arg.EMPTY,t));
			    } else {
				assert false;
			    }
			    stack.push(v);
			    if (data instanceof Long ||
				data instanceof Double) {
				stack.push(Arg.NIL);
			    }
			}
			break;
		    }
		    case Opcodes.LOOKUPSWITCH: {
			LookupSwitchInsnNode lsin=(LookupSwitchInsnNode)ain;
			assert lsin.keys.size()==lsin.labels.size();
			int[] targetPCs=new int[lsin.keys.size()];
			Header[] targets=new Header[lsin.keys.size()];
			int[] values=new int[lsin.keys.size()];
			for (int i=0;i<lsin.keys.size();++i) {
			    targetPCs[i]=((LabelNode)lsin.labels.get(i)).getBCOffset();
			    targets[i]=headers.get(targetPCs[i]);
			    values[i]=(Integer)lsin.keys.get(i);
			}
			int defPC=lsin.dflt.getBCOffset();
			Arg value=stack.pop();
			h.setFooter(
			    new Switch(
				di,new Arg[]{value},
				headers.get(defPC),targets,values));
			mergeWith(defPC,h,stack);
			for (int nextPC : targetPCs) {
			    mergeWith(nextPC,h,stack);
			}
			done=true;
			break;
		    }
		    case Opcodes.MONITORENTER:
		    case Opcodes.MONITOREXIT: {
			Arg val=stack.pop();
			OpCode opcode;
			if (ain.getOpcode()==Opcodes.MONITORENTER) {
			    opcode=OpCode.MonitorEnter;
			} else {
			    opcode=OpCode.MonitorExit;
			}
			h.append(
			    new SimpleInst(
				di,opcode,
				Var.VOID,new Arg[]{val}));
			break;
		    }
		    case Opcodes.MULTIANEWARRAY: {
			MultiANewArrayInsnNode mnain=
			    (MultiANewArrayInsnNode)ain;
			Type t=Type.parse(c.getContext(),mnain.desc).checkResolved();
			Arg[] args=new Arg[mnain.dims];
			for (int i=mnain.dims;i-->0;) {
			    args[i]=stack.pop();
			}
			Var result=c.addVar();
			h.append(
			    new MultiNewArrayInst(
				di,result,args,t,mnain.dims));
			stack.push(result);
			break;
		    }
		    case Opcodes.NEW: {
			Type t=Type.make(c.getContext().getClass(((TypeInsnNode)ain).desc));
			t.checkResolved();
			Var result=c.addVar();
			h.append(
			    new TypeInst(
				di,OpCode.New,
				result,Arg.EMPTY,t));
			stack.push(result);
			break;
		    }
		    case Opcodes.NEWARRAY: {
			Type t=null;
			switch (((IntInsnNode)ain).operand) {
			case Opcodes.T_BOOLEAN: t=Type.BOOLEAN; break;
			case Opcodes.T_BYTE: t=Type.BYTE; break;
			case Opcodes.T_CHAR: t=Type.CHAR; break;
			case Opcodes.T_DOUBLE: t=Type.DOUBLE; break;
			case Opcodes.T_FLOAT: t=Type.FLOAT; break;
			case Opcodes.T_INT: t=Type.INT; break;
			case Opcodes.T_LONG: t=Type.LONG; break;
			case Opcodes.T_SHORT: t=Type.SHORT; break;
			default: assert false:ain;
			}
			Arg count=stack.pop();
			Var result=c.addVar();
			h.append(
			    new TypeInst(
				di,OpCode.NewArray,
				result,new Arg[]{count},t.makeArray()));
			stack.push(result);
			break;
		    }
		    case Opcodes.NOP: break;
		    case Opcodes.POP: {
			stack.pop();
			break;
		    }
		    case Opcodes.POP2: {
			stack.pop();
			stack.pop();
			break;
		    }
		    case Opcodes.SWAP: {
			Arg a1=stack.pop();
			Arg a2=stack.pop();
			stack.push(a1);
			stack.push(a2);
			break;
		    }
		    case Opcodes.TABLESWITCH: {
			TableSwitchInsnNode tsin=(TableSwitchInsnNode)ain;
			assert tsin.max-tsin.min+1==tsin.labels.size();
			int[] targetPCs=new int[tsin.labels.size()];
			Header[] targets=new Header[tsin.labels.size()];
			int[] values=new int[tsin.labels.size()];
			for (int i=0;i<tsin.labels.size();++i) {
			    targetPCs[i]=
				((LabelNode)tsin.labels.get(i)).getBCOffset();
			    targets[i]=headers.get(targetPCs[i]);
			    values[i]=tsin.min+i;
			}
			int defPC=tsin.dflt.getBCOffset();
			Arg value=stack.pop();
			h.setFooter(
			    new Switch(
				di,new Arg[]{value},
				headers.get(defPC),targets,values));
			mergeWith(defPC,h,stack);
			for (int nextPC : targetPCs) {
			    mergeWith(nextPC,h,stack);
			}
			done=true;
			break;
		    }
		    default: assert false
			    : "Bad opcode with "+ain+" and opcode = "+ain.getOpcode();
		    }
		} catch (ResolutionFailed e) {
		    if (Global.verbosity>=1) {
			Global.log.println(
			    "In "+vm+" resolution failed, inserting patch point: "+e);
		    }
                    vm.getContext().resolutionReport.addUse(
                        e.getResolutionID(),
                        vm.getResolutionID());
                    Arg[] stuff=new Arg[1+vars.length+stack.height()];
                    stuff[0]=receiver;
                    for (int i=0;i<vars.length;++i) {
                        stuff[1+i]=vars[i];
                    }
                    for (int i=0;i<stack.height();++i) {
                        stuff[1+vars.length+i]=stack.get(i);
                    }
                    // FIXME: the way we do it prevents us from handling liveness
                    //    information at patch points in any reasonable way.  the
                    //    best fix is to introduce a *third* patch point form,
                    //    which we use internally here, that includes the RHS
                    //    of the instruction that we failed to resolve.  or, do
                    //    resolution in two steps.  or something.
                    h.setFooter(
			new PatchPointFooter(
			    di,OpCode.PatchPointFooter,stuff,
                            pc,vars.length,stack.height(),
                            e.getClazz(),e.getMessage()));
		    break;
		}
                if (Global.verbosity>=3) {
                    Global.log.println("done with instruction at pc = "+pc+", done = "+done);
                }
	    }
	}
	
	if (Global.verbosity>=3) {
            synchronized (Global.log) {
                Global.log.println("Initial parse dump:");
                CodeDumper.dump(c,Global.log);
            }
	}
	
	// NB we used to assert here that all blocks are reachable, except that
	// sometimes we create junk blocks (like if there are redundant LabelNodes
	// in the instruction stream)
	
	// kill off unreachable code
	new KillDeadCode(c).visitCode();

	// kill dead stores
	// NOTE: later on we are killing code after nullchecks known to fail, which
	// takes care of the non-sane code we would generate if we do
	// for example ArrayLoad(NULL).
	new KillDead(c).visitCode();
	
	if (Global.verbosity>=3) {
            synchronized (Global.log) {
                Global.log.println("After dead var cleanup:");
                CodeDumper.dump(c,Global.log);
            }
	}
	
	// FIXME: allow for UNDEFINED type!  Its Type should be as before but the
	// Exectype should be basetype=UNDEFINED.  any code that uses it should be
	// stubbed.  or - why not just represent those types as NULL?  we could but
	// then the problem is with subsequent debugging...
	// OR: we could just make a Type or Exectype of an undefined type act
	// undefined (as far as subtyping and other behaviors go) without actually
	// having to switch to a different type.
	
	// it seems that the best approach would be to have UNRESOLVED types be
	// their own thing (subtype of Object and of nothing else).  Lubbing on
	// UNRESOLVED should fail.  but that requires making this fixpoint way more
	// complicated.  it seems that if this fixpoint was rewritten to just use
	// SSA everything would be simpler...

	// NOTE: standard JVMs don't have to deal with this.  they seem to force
	// loading of a class whenever its subtyping relationship to the rest of the
	// world needs to be known.
	
	// Here's what other JVMs do, and what we should do, too: if any method in
	// a class cannot be compiled due to a ResolutionFailed, we should make the
	// method bodies of that class trap.  In a closed world setting it would be
	// a NoClassDefFoundError, while in a class loading setting it would trigger
	// class loading and compilation.  The cases where we would get ResolutionFailed
	// are subtle.  Passing an UNRESOLVED variable through without doing anything
	// to it that would require knowing its type does not trigger ResolutionFailed.
	// however, if we have to lub it, or do anything else (field access, method
	// call, etc) then we trigger ResolutionFailed.  interestingly, array instantiation
	// (where the array element is UNRESOLVED) does not trigger ResolutionFailed
	// (it should be compiled to generate a runtime error) but it would trigger
	// ResolutionFailed if lubbing was required.
	
	// figure out liveness.  this will help us a lot.
	SimpleLivenessCalc slc=c.getSimpleLiveness();
        
        if (Global.verbosity>=3) {
            c.recomputeOrder();
            synchronized (Global.log) {
                Global.log.println("Printing slc results for "+c);
                for (Header h : c.headers()) {
                    Global.log.println("Live at head of "+h+": "+slc.liveAtHead(h));
                    Global.log.println("Live at tail of "+h+": "+slc.liveAtTail(h));
                }
            }
        }
	
	// have a worklist, similarly to before.  but this time we don't
	// have a seen set.
	Worklist worklist=new Worklist();
	
	// replace variables according to type
	substGlobal=new HashMap< Var, HashMap< Exectype, Var > >();
	substAtHead=new HashMap< Header, HashMap< Var, Var > >();
	substForHandler=new HashMap< Header, HashMap< Var, Var > >();
	substAtTail=new HashMap< Header, HashMap< Var, Var > >();
	
	for (Var v : c.vars()) {
	    substGlobal.put(v,new HashMap< Exectype, Var >());
	}
	
	for (Header h : c.headers()) {
	    substAtHead.put(h,new HashMap< Var, Var >());
	    substAtTail.put(h,new HashMap< Var, Var >());
	    substForHandler.put(h,new HashMap< Var, Var >());
	}
	
	// create all the variables we need, modulo merging
	rtc=new SubstResultTypeCalc(c);

	// we start with any block that has no live-ins
	for (Header h : c.headers()) {
	    if (slc.liveAtHead(h).isEmpty()) {
		worklist.push(h);
	    }
	}
	
	// propagation.  this does not modify code, only figures out the
	// situation is.
	while (!worklist.empty()) {
	    Header h=worklist.pop();

            try {
                if (Global.verbosity>=6) {
                    Global.log.println("Got "+h+", it has terminal = "+
                                        h.getFooter().isTerminal());
                }

                // build our "working" subst map for this basic block
                HashMap< Var, Var > curSubst=
                    Util.copy(substAtHead.get(h));
                if (Global.verbosity>=6) {
                    Global.log.println(""+h+": "+curSubst);
                }
                
                HashMap< Var, Var > curSubstForHandler=null;
	    
                // make updates to the maps
                for (Operation o : h.operations()) {
                    // what if this throws?  basic idea: the exception handler gets
                    // the LUB of all types used for each variable in all blocks
                    // that flow into it, regardless of whether or not those types
                    // are in use at throw sites.
                    // this goes against the VM spec, but this is what HotSpot 1.6
                    // does.
                    // questions:
                    // 1) how do we implement this?
                    // 2) if there is a catch(A), and a handler that it drops to
                    //    that handles a subtype of A, is the one below it also
                    //    subjected to said lubbing?
                    // answers:
                    // 1) when this situation is detected, create new exception
                    //    handlers along with new blocks that are unique to this
                    //    basic block, have those blocks do the merging, and then
                    //    have them jump to the relevant handlers.
                    //    NB: if a local changes type and there's a handler,
                    //    we'll have to split the block.  or do something.
                    //    I'm not sure what, yet.  one option is to identify the
                    //    LUB type for this block, and duplicate stores to both
                    //    the variable we're using and the LUB.  that may be
                    //    best...  especially since we can increase precision by
                    //    identifying which instructions actually throw.  in that
                    //    case, we want the LUB of types visible at the throw sites.
                    // 2) ?? try it out!  besides ... this isn't so difficult
                    //    to handle.
                    // current solution: we take into account catch(Throwable).
                    // we use liveness to figure out which variables will make it
                    // into a catch block.  we assign to the lubbed versions of the
                    // variables at the appropriate points in time.
                    if (c.getThrows().get(o) &&
                        h.handler()!=null) {
                        if (curSubstForHandler==null) {
                            curSubstForHandler=
                                Util.copy(curSubst,slc.liveForHandler(h));
                        }
                        for (Var origVar : slc.liveForHandler(h)) {
                            Var ourVar=curSubst.get(origVar);
                            Var oldVar=curSubstForHandler.get(origVar);
                            assert ourVar!=null : "for "+origVar+" at "+o+" at "+h;
                            assert oldVar!=null : "for "+origVar+" at "+o+" at "+h;
                            Var newVar=
                                addVarType(
                                    origVar,
                                    Exectype.lub(ourVar.type(),oldVar.type()));
                            curSubstForHandler.put(origVar,newVar);
                            if (Global.verbosity>=6) {
                                assert ourVar!=null;
                                assert oldVar!=null;
                                Global.log.println(
                                    origVar+" will become lub("+ourVar+", "+oldVar+
                                    ") = "+newVar+" at "+o.di()+" in "+h);
                            }
                        }
                    }

                    if (o instanceof Instruction) {
                        handleDef(h,curSubst,(Instruction)o);
                    }
                }
	    
                // handle the footer if there was a change
                if (!h.getFooter().isTerminal() &&
                    !curSubst.equals(substAtTail.get(h))) {
                    Control c=(Control)h.getFooter();
                    for (Header h2 : c.successors()) {
                        HashMap< Var, Var > theirSubst=substAtHead.get(h2);
                        boolean changed=false;
                        // take into account liveness!
                        for (Var origVar : slc.liveAtHead(h2)) {
                            Var ourVar=curSubst.get(origVar);
                            Var theirVar=theirSubst.get(origVar);
                            assert ourVar!=null : "origVar = "+origVar;
                            if (ourVar!=theirVar) {
                                Var newVar;
                                if (theirVar==null) {
                                    newVar=ourVar;
                                } else {
                                    assert theirVar!=null;
                                    newVar=addVarType(origVar,
                                                      Exectype.lub(ourVar.type(),
                                                                   theirVar.type()));
                                }
                                if (newVar!=theirVar) {
                                    if (Global.verbosity>=6) {
                                        Global.log.println(
                                            origVar+" will become lub("+ourVar+
                                            ", "+theirVar+") = "+newVar+" (because of "+
                                            h+" -> "+h2+")");
                                    }
                                    theirSubst.put(origVar,newVar);
                                    changed=true;
                                }
                            }
                        }
                        if (changed) {
                            worklist.push(h2);
                        }
                    }
                    substAtTail.put(h,curSubst);
                }
	    
                // handle exception handlers if there was a change
                if (curSubstForHandler!=null &&
                    !curSubstForHandler.equals(substForHandler.get(h))) {
                    substForHandler.put(h,curSubstForHandler);
                    for (Header h2 : h.exceptionalSuccessors()) {
                        HashMap< Var, Var > theirSubst=substAtHead.get(h2);
                        boolean changed=false;
                        for (Var origVar : slc.liveAtHead(h2)) {
                            Var ourVar=curSubstForHandler.get(origVar);
                            assert ourVar!=null : "for "+origVar+" at "+h2+" coming (exceptionally) from "+h;
                            Var theirVar=theirSubst.get(origVar);
                            if (ourVar!=theirVar) {
                                // no lubbing needed because this handler is unique
                                // to us
                                if (Global.verbosity>=6) {
                                    Global.log.println(
                                        origVar+" will become "+ourVar+" (because of "+
                                        "EH edge "+h+" -> "+h2+")");
                                }
                                changed=true;
                                theirSubst.put(origVar,ourVar);
                            }
                        }
                        if (changed) {
                            worklist.push(h2);
                        }
                    }
                }
            } catch (Throwable e) {
                throw new CompilerException("While handling "+h,e);
            }
	}
	
        if (Global.verbosity>=6) {
            Global.log.println("Modifying code...");
        }
        
	// modify the code
	for (Header h : c.headers()) {
	    HashMap< Var, Var > curSubst=substAtHead.get(h);
            if (Global.verbosity>=6) {
                Global.log.println(""+h+": "+curSubst);
            }
	    HashMap< Var, Var > curSubstForHandler=substForHandler.get(h);
	    for (Operation o : h.operations()) {
		// handle uses
		for (int i=0;i<o.rhs().length;++i) {
		    if (o.rhs()[i] instanceof Var) {
			Var newVar=curSubst.get((Var)o.rhs()[i]);
			if (newVar!=null) {
			    o.rhs()[i]=newVar;
			} // else dead code
		    }
		}
		
		// handle throw sites
		if (c.getThrows().get(o) &&
		    h.handler()!=null) {
		    for (Var origVar : curSubstForHandler.keySet()) {
			Var ourVar=curSubst.get(origVar);
			Var theirVar=curSubstForHandler.get(origVar);
			if (ourVar!=theirVar) {
			    o.prepend(
				new SimpleInst(
				    o.di(),OpCode.Mov,
				    theirVar,new Arg[]{ourVar}));
			}
		    }
		}
		
		// handle defs
		if (o instanceof Instruction) {
		    Instruction i = (Instruction)o;
		    i.setLhs(handleDef(h,curSubst,i));
		}
	    }

	    if (!h.getFooter().isTerminal()) {
		Control c=(Control)h.getFooter();
		for (Header h2 : c.successors()) {
		    HashMap< Var, Var > theirSubst=substAtHead.get(h2);
		    for (Var origVar : theirSubst.keySet()) {
			Var ourVar=curSubst.get(origVar);
			Var theirVar=theirSubst.get(origVar);
			if (ourVar!=null && theirVar!=null && ourVar!=theirVar) {
			    // note that this will have pathologies if the
			    // CFG is not reducible, but those pathologies will
                            // be removed by SSA, so we don't really care.  it's
                            // just part of the pun that we're playing: we convert
                            // to non-SSA IR, do some non-SSA stuff, and then
                            // convert to SSA as soon as possible.  it makes things
                            // simple and easy, sort of.
			    h.append(
				new SimpleInst(
				    c.di(),OpCode.Mov,
				    theirVar,new Arg[]{ourVar}));
			}
		    }
		}
	    }
	}
	
        // clean up patch points
        for (Header h : c.headers()) {
            if (h.getFooter().opcode()==OpCode.PatchPointFooter) {
                PatchPointFooter ppf=(PatchPointFooter)h.getFooter();
                for (int j=0;j<ppf.rhs.length;++j) {
                    if (ppf.rhs[j].type()==Exectype.TOP) {
                        ppf.rhs[j]=Arg.NIL;
                    }
                }
                for (int j=0;j<ppf.rhs.length;++j) {
                    if (ppf.rhs[j].type().effectiveBasetype().cells==2 &&
                        (j+1==ppf.rhs.length ||
                         ppf.rhs[j+1].type()!=Exectype.NIL)) {
                        ppf.rhs[j]=Arg.NIL;
                    }
                }
            }
        }
	
	c.killAllAnalyses();

	if (Global.verbosity>=3) {
            synchronized (Global.log) {
                Global.log.println("Code after type propagation:");
                CodeDumper.dump(c,Global.log);
            }
	}
        
	PhaseFixpoint.simplifyNoCheck(c);
	
	if (Global.verbosity>=3) {
            synchronized (Global.log) {
                Global.log.println("Code after dead var and CFG cleanup:");
                CodeDumper.dump(c,Global.log);
            }
	}
	
	if (Global.verbosity>=4) {
	    Global.log.println("Code with all analyses:");
	    CodeDumper.dump(c,Global.log,CodeDumper.Mode.COMPUTE_ANALYSES);
	}
	
	new SanityCheck(c).doit();
	
	return c;
    }
    
    void mergeWith(int targPC,Header h,MyStack< Arg > stack) {
	if (Global.verbosity>=5) {
	    Global.log.println("Merging with "+headers.get(targPC)+" from "+h+
				" with stack = "+stack);
	}
	MyStack< Arg > targStack=seenPC.get(targPC);
	if (targStack==null) {
	    targStack=new MyStack< Arg >();
	    for (int i=0;i<stack.height();++i) {
		Var v=c.addVar();
		h.append(
		    new SimpleInst(
			h.getFooter().di(),OpCode.Mov,
			v,new Arg[]{stack.get(i)}));
		targStack.push(v);
	    }
	    seenPC.put(targPC,targStack);
	    worklistPC.push(targPC);
	} else {
	    assert stack.height()==targStack.height()
		: "stack = "+stack+", targStack = "+targStack;
	    for (int i=0;i<stack.height();++i) {
		h.append(
		    new SimpleInst(
			h.getFooter().di(),OpCode.Mov,
			(Var)targStack.get(i),
			new Arg[]{stack.get(i)}));
	    }
	}
    }
    
    Var addVarType(Var v,Exectype t) {
	HashMap< Exectype, Var > varsByType=substGlobal.get(v);
	assert varsByType!=null : v;
	Var result=varsByType.get(t);
	if (result==null) {
	    varsByType.put(t,result=c.addVar(t));
	}
	return result;
    }
    
    Var handleDef(Header h,HashMap< Var, Var > curSubst, Instruction i) {
	Var result=i.lhs();
	if (i.lhs()!=Var.VOID) {
	    rtc.curSubst=curSubst;
	    curSubst.put(
		i.lhs(),
		result=addVarType(i.lhs(),rtc.get(i)));
	    rtc.curSubst=null;
	}
	if (Global.verbosity>=8) {
	    Global.log.println("For "+h+": "+i+": "+result);
	}
	return result;
    }
    
    static class HandlerData {
	int targetPC;
	int startPC;
	int endPC;
	VisibleClass handles;
	Header excGetter;
	Header handler;
	
	public HandlerData(int targetPC,
			   int startPC,
			   int endPC,
			   VisibleClass handles,
			   Header excGetter,
			   Header handler) {
	    this.targetPC=targetPC;
	    this.startPC=startPC;
	    this.endPC=endPC;
	    this.handles=handles;
	    this.excGetter=excGetter;
	    this.handler=handler;
	}
	
	public String toString() {
	    return "HandlerData[target = "+targetPC+", start = "+startPC+
		", end = "+endPC+", handles = "+handles+", excGetter = "+
		excGetter+", handler = "+handler+"]";
	}
    }
    
    static class SubstResultTypeCalc extends ResultTypeCalc {
	HashMap< Var, Var > curSubst;
	
	SubstResultTypeCalc(Code c) { super(c); }

	public Exectype typeForArg(Arg a) {
	    assert a!=null;
	    if (a instanceof Var) {
		Var v=(Var)a;
		Var newV=curSubst.get(v);
		if (newV!=null) a=newV;
	    }
	    return a.type();
	}
    }
    
    Code wrapNative(VisibleMethod vm) {
	// FIXME: for conservative GCs, this needs to save all non-volatile
	// registers on the stack.
	
	Code c=new Code(vm,
			PollcheckMode.EXPLICIT_POLLCHECKS_ONLY,
			UnsafeMode.ALLOW_UNSAFE,
			SafetyCheckMode.EXPLICIT_SAFETY_CHECKS_ONLY,
                        ReturnMode.RETURN_OR_THROW,
                        vm.stackOverflowCheckMode());
	
	CLocal nativeFrame=null;
	
	if (!vm.noNativeFrame) {
	    nativeFrame=new CLocal(CTypesystemReferences.NativeFrame_TYPE,"nativeFrame");
	    c.addLocal(nativeFrame);
	}

	DebugInfo di=new DebugInfo(c,0,0);
	c.setRoot(c.addHeader(di));
	
	Header resolve=c.addHeader(di);
	Header makeCall=c.addHeader(di);
	Header excReturn=c.addHeader(di);
	Header okReturn=c.addHeader(di);
	
	Var mrecordPtr=c.addVar(Exectype.POINTER);
	Var methodPtr=c.addVar(Exectype.POINTER);
	Var nativeFramePtr=c.addVar(Exectype.POINTER);
	
	if (!vm.noNativeFrame) {
	    c.root().append(
		new CFieldInst(
		    di,OpCode.GetCVarAddress,
		    nativeFramePtr,Arg.EMPTY,
		    nativeFrame));
	}

	c.root().append(
            new GetMethodInst(
                di,OpCode.GetMethodRec,
                mrecordPtr,Arg.EMPTY,
                vm));
	
	assert vm.getImpl()==MethodImpl.JNI
	    || vm.getImpl()==MethodImpl.IMPORT;
	if (vm.getImpl()==MethodImpl.IMPORT ||
	    Global.staticJNI) {
	    c.root().setFooter(
		new Jump(
		    di,makeCall));
	} else {
	    c.root().append(
		new CFieldInst(
		    di,OpCode.GetCField,
		    methodPtr,new Arg[]{mrecordPtr},
		    CTypesystemReferences.MethodRec_codePtr));
	    c.root().setFooter(
		new Branch(
		    di,OpCode.BranchNonZero,
		    new Arg[]{methodPtr},
		    resolve,makeCall));
	}
	
	// create the call-making block
	Var[] args=new Var[c.params().length];
	for (int i=0;i<c.params().length;++i) {
	    args[i]=c.addVar(c.param(i).asExectype());
	    makeCall.append(
		new ArgInst(di,OpCode.GetArg,args[i],Arg.EMPTY,i));
	}
	
	Var cresult;
	if (c.result()!=Type.VOID) {
	    cresult=c.addVar(Exectype.make(c.cresult()));
	} else {
	    cresult=Var.VOID;
	}
	
	if (vm.noNativeFrame) {
	    makeCall.append(
		new CFieldInst(
		    di,OpCode.GetCField,
		    nativeFramePtr,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_curNF));
	} else {
	    makeCall.append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{
			Arg.THREAD_STATE,
			nativeFramePtr,
			mrecordPtr
		    },
		    CTypesystemReferences.ThreadState_pushAndInitNF));
	}
	
	Var jniEnvPtr=c.addVar(Exectype.POINTER);
	if (vm.isJNI()) {
	    makeCall.append(
		new CFieldInst(
		    di,OpCode.GetCFieldAddress,
		    jniEnvPtr,new Arg[]{nativeFramePtr},
		    CTypesystemReferences.NativeFrame_jni));
	}
	
	Arg[] callArgs=new Arg[
	    c.params().length+(vm.isJNI()?((Global.staticJNI?0:1)+1+(vm.isStatic()?1:0)):0)];
	Basetype[] callParams=new Basetype[
	    c.params().length+(vm.isJNI()?(1+(vm.isStatic()?1:0)):0)];
	
	int curArgIdx=0;
	int curParamIdx=0;
	if (vm.isJNI()) {
	    if (!Global.staticJNI) {
		callArgs[curArgIdx++]=methodPtr;
	    }
	    callArgs[curArgIdx++]=jniEnvPtr;
	    callParams[curParamIdx++]=Basetype.POINTER;
	}
	
	if (vm.isStatic() && vm.isJNI()) {
	    Var classVar=c.addVar(Global.root().classType.asExectype());
	    Var classPtr=c.addVar(Exectype.POINTER);
	    makeCall.append(
		new TypeInst(
		    di,OpCode.GetType,
		    classVar,Arg.EMPTY,
		    c.getOwner().asType()));
	    makeCall.append(
		new TypeInst(
		    di,OpCode.Cast,
		    classPtr,new Arg[]{classVar},
		    Type.POINTER));
	    if (!vm.useObjectsNotHandles) {
		makeCall.append(
		    new CFieldInst(
			di,OpCode.Call,
			classPtr,new Arg[]{
			    nativeFramePtr,
			    Arg.THREAD_STATE,
			    classPtr
			},
			CTypesystemReferences.NativeFrame_addHandle));
	    }
	    callArgs[curArgIdx++]=classPtr;
	    callParams[curParamIdx++]=Basetype.POINTER;
	}
	
	for (int i=0;i<args.length;++i) {
	    if (c.param(i).isObject()) {
		Var carg=c.addVar(Exectype.POINTER);
		makeCall.append(
		    new TypeInst(
			di,OpCode.Cast,
			carg,new Arg[]{args[i]},
			Type.POINTER));
		if (!vm.useObjectsNotHandles) {
		    makeCall.append(
			new CFieldInst(
			    di,OpCode.Call,
			    carg,new Arg[]{
				nativeFramePtr,
				Arg.THREAD_STATE,
				carg
			    },
			    CTypesystemReferences.NativeFrame_addHandle));
		}
		callArgs[curArgIdx++]=carg;
	    } else {
		callArgs[curArgIdx++]=args[i];
	    }
	    callParams[curParamIdx++]=
		c.param(i).effectiveBasetype().pointerifyObject;
	}
	
	if (!vm.noExecStatusTransition) {
	    makeCall.append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_goToNative));
	}
	switch (vm.getImpl()) {
	case JNI:
	    if (Global.staticJNI) {
		makeCall.append(
		    new CFieldInst(
			di,OpCode.Call,
			cresult,callArgs,
			new RemoteFunction(vm.jniFunctionNameLong(),
					   c.cresult(),callParams,vm)));
	    } else {
		makeCall.append(
		    new CallIndirectInst(
			di,OpCode.CallIndirect,
			cresult,callArgs,
			new NativeSignature(c.cresult(),
					    callParams,
					    vm)));
	    }
	    break;
	case IMPORT:
            switch (vm.importMode) {
            case REMOTE_IMPORT:
		makeCall.append(
		    new CFieldInst(
			di,OpCode.Call,
			cresult,callArgs,
			new RemoteFunction(vm.getName(),c.cresult(),callParams,vm)));
                break;
            case GOD_GIVEN_IMPORT:
		makeCall.append(
		    new CFieldInst(
			di,OpCode.Call,
			cresult,callArgs,
			GodGivenFunction.make(vm.getName(),c.cresult(),callParams,vm)));
                break;
            case TRUSTED_GOD_GIVEN_IMPORT:
		makeCall.append(
		    new CFieldInst(
			di,OpCode.Call,
			cresult,callArgs,
			new TrustedGodGivenFunction(vm.getName(),c.cresult(),callParams,vm)));
                break;
            default: throw new Error();
            }
            break;
	default: throw new Error();
	}
	if (!vm.noExecStatusTransition) {
	    makeCall.append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_goToJava));
	}
	
	Var excPtr=c.addVar(Exectype.POINTER);
	Var excVar=c.addVar(Global.root().throwableType.asExectype());

	if (vm.doesNotThrow()) {
	    makeCall.setFooter(
		new Jump(
		    di,okReturn));
	} else {
	    if (vm.useObjectsNotHandles) {
		makeCall.append(
		    new CFieldInst(
			di,OpCode.GetCField,
			excPtr,new Arg[]{Arg.THREAD_STATE},
			CTypesystemReferences.ThreadState_curException));
	    } else {
		makeCall.append(
		    new CFieldInst(
			di,OpCode.GetCField,
			excPtr,new Arg[]{Arg.THREAD_STATE},
			CTypesystemReferences.ThreadState_curExceptionHandle));
	    }
	    makeCall.setFooter(
		new Branch(
		    di,OpCode.BranchNonZero,
		    new Arg[]{excPtr},
		    okReturn,excReturn));
	}
	
	Var result=null;
	if (c.result()!=Type.VOID) {
	    if (c.result().isObject()) {
		result=c.addVar(c.result().asExectype());
		if (!vm.useObjectsNotHandles) {
		    okReturn.append(
			new CFieldInst(
			    di,OpCode.Call,
			    cresult,new Arg[]{cresult},
			    CTypesystemReferences.Handle_get));
		}
		okReturn.append(
		    new TypeInst(
			di,OpCode.Cast,
			result,new Arg[]{cresult},
			c.result()));
	    } else {
		result=cresult;
	    }
	}
	
	if (!vm.noNativeFrame) {
	    okReturn.append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_popNF));
	}

	if (c.result()==Type.VOID) {
	    okReturn.setFooter(
		new Terminal(
		    di,OpCode.Return,
		    Arg.EMPTY));
	} else {
	    okReturn.setFooter(
		new Terminal(
		    di,OpCode.Return,
		    new Arg[]{result}));
	}
	
	if (vm.useObjectsNotHandles) {
	    excReturn.append(
		new CFieldInst(
		    di,OpCode.PutCField,
		    Var.VOID,new Arg[]{
			Arg.THREAD_STATE,
			PointerConst.ZERO
		    },
		    CTypesystemReferences.ThreadState_curException));
	} else {
	    excReturn.append(
		new CFieldInst(
		    di,OpCode.PutCField,
		    Var.VOID,new Arg[]{
			Arg.THREAD_STATE,
			PointerConst.ZERO
		    },
		    CTypesystemReferences.ThreadState_curExceptionHandle));
	    excReturn.append(
		new CFieldInst(
		    di,OpCode.Call,
		    excPtr,new Arg[]{excPtr},
		    CTypesystemReferences.Handle_get));
	}
	excReturn.append(
	    new TypeInst(
		di,OpCode.Cast,
		excVar,new Arg[]{excPtr},
		Global.root().throwableType));
	if (!vm.useObjectsNotHandles) {
	    excReturn.append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_popNF));
	}
	excReturn.setFooter(
	    new Terminal(
		di,OpCode.Throw,
		new Arg[]{excVar}));
	
	resolve.append(
	    new MethodInst(
		di,OpCode.InvokeStatic,
		methodPtr,new Arg[]{
		    mrecordPtr
		},
		Runtime.resolveNative));
	resolve.setFooter(
	    new Jump(
		di,makeCall));
	
	PhaseFixpoint.simplify(c);
	
	return c;
    }
    
    Code makeExport(VisibleMethod vm) {
	Type[] params=vm.getAllParams();
        
	Code c=new Code(new CodeOrigin(vm.getClazz(),null,vm,
				       PollcheckMode.EXPLICIT_POLLCHECKS_ONLY,
				       UnsafeMode.ALLOW_UNSAFE,
				       SafetyCheckMode.IMPLICIT_SAFETY_CHECKS,
                                       ReturnMode.ONLY_RETURN,
                                       StackOverflowCheckMode.DONT_CHECK_STACK_OVERFLOW,
                                       "export"),
                        Type.make(Global.makeResult(vm.getType())),
                        Type.make(Global.makeBareParams(params)),
                        Global.name+"_"+vm.getName());
	c.mayBeCalledFromNative=true;
	
	DebugInfo di=new DebugInfo(c,0,0);
	
	c.setRoot(c.addHeader(di));
	
	Header caller=c.addHeader(di);
	Header returner=c.addHeader(di);
	Header handler=c.addHeader(di);
	
	caller.setHandler(c.addHandler(di,Global.root().throwableClass,null,handler));
	
	Var[] args=new Var[params.length];
	Var result;
	if (vm.getType()!=Type.VOID) {
	    result=c.addVar(vm.getType().asExectype());
	} else {
	    result=Var.VOID;
	}
        
	if (!vm.noExecStatusTransition) {
	    c.root().append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_goToJava));
	}
	
	for (int i=0;i<params.length;++i) {
	    Var p=c.addVar(params[i].asExectype());
	    if (params[i].isObject()) {
		Var np=c.addVar(Exectype.POINTER);
		c.root().append(
		    new ArgInst(di,OpCode.GetArg,np,Arg.EMPTY,i));
		if (!vm.useObjectsNotHandles) {
		    c.root().append(
			new CFieldInst(
			    di,OpCode.Call,
			    np,new Arg[]{np},
			    CTypesystemReferences.Handle_get));
		}
		c.root().append(
		    new TypeInst(
			di,OpCode.Cast,
			p,new Arg[]{np},
			params[i]));
	    } else {
		c.root().append(
		    new ArgInst(di,OpCode.GetArg,p,Arg.EMPTY,i));
	    }
	    args[i]=p;
	}
	
	c.root().setFooter(new Jump(di,caller));
	
	if (vm.isStatic()) {
	    if (vm.getClazz().shouldCallCheckInit()) {
		caller.append(
		    new ClassInst(
			di,OpCode.CheckInit,
			Var.VOID,vm.getClazz()));
	    }
	    caller.append(
		new MethodInst(
		    di,OpCode.InvokeStatic,
		    result,args,
		    vm));
	} else {
	    caller.append(
		new SimpleInst(
		    di,OpCode.NullCheck,
		    Var.VOID,new Arg[]{args[0]}));
	    Var v=c.addVar(vm.getClazz().asExectype());
	    caller.append(
		new TypeCheckInst(
		    di,
		    v,new Arg[]{args[0]},
		    vm.getClazz().asType(),
		    Runtime.incompatibleClassChangeError.asType()));
	    args[0]=v;
	    // assume that since it's an instance method, we've already
	    // static-initialized the class.
	    caller.append(
		new MethodInst(
		    di,OpCode.InvokeDynamic,
		    result,args,
		    vm));
	}
	
	caller.setFooter(new Jump(di,returner));
	
	Arg[] returnArgs;
	
	if (result==Var.VOID) {
	    returnArgs=Arg.EMPTY;
	} else if (result.type().isObject()) {
	    Var nr=c.addVar(Exectype.POINTER);
	    returner.append(
		new TypeInst(
		    di,OpCode.Cast,
		    nr,new Arg[]{result},
		    Type.POINTER));
	    if (!vm.useObjectsNotHandles) {
		returner.append(
		    new CFieldInst(
			di,OpCode.Call,
			nr,new Arg[]{
			    Arg.THREAD_STATE,
			    nr
			},
			CTypesystemReferences.ThreadState_addHandle));
	    }
	    returnArgs=new Arg[]{nr};
	} else {
	    returnArgs=new Arg[]{result};
	}
	
	if (!vm.noExecStatusTransition) {
	    returner.append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_goToNative));
	}
	
	returner.setFooter(
	    new Terminal(
		di,OpCode.Return,
		returnArgs));
	
	Var excVar=c.addVar(Global.root().throwableType.asExectype());
	Var excPtr=c.addVar(Exectype.POINTER);
	
	handler.append(
	    new TypeInst(
		di,OpCode.GetException,
		excVar,Arg.EMPTY,
		Global.root().throwableType));
	handler.append(
	    new SimpleInst(
		di,OpCode.ClearException,
		Var.VOID,Arg.EMPTY));
	handler.append(
	    new TypeInst(
		di,OpCode.Cast,
		excPtr,new Arg[]{excVar},
		Type.POINTER));
	if (vm.useObjectsNotHandles) {
	    handler.append(
		new CFieldInst(
		    di,OpCode.PutCField,
		    Var.VOID,new Arg[]{
			Arg.THREAD_STATE,
			excPtr
		    },
		    CTypesystemReferences.ThreadState_curException));
	} else {
	    handler.append(
		new CFieldInst(
		    di,OpCode.Call,
		    excPtr,new Arg[]{
			Arg.THREAD_STATE,
			excPtr
		    },
		    CTypesystemReferences.ThreadState_addHandle));
	    handler.append(
		new CFieldInst(
		    di,OpCode.PutCField,
		    Var.VOID,new Arg[]{
			Arg.THREAD_STATE,
			excPtr
		    },
		    CTypesystemReferences.ThreadState_curExceptionHandle));
	}
	if (!vm.noExecStatusTransition) {
	    handler.append(
		new CFieldInst(
		    di,OpCode.Call,
		    Var.VOID,new Arg[]{Arg.THREAD_STATE},
		    CTypesystemReferences.ThreadState_goToNative));
	}
	handler.setFooter(
	    new Terminal(
		di,OpCode.Return,
		Util.produceZero(c,c.result(),handler.getFooter())));
	
        Payload.exportMethod(vm,c.asRemoteFunction());
        
	return c;
    }
    
    Code makePatchThunk(VisibleMethod vm,ResolutionFailed e) {
	Code c=new Code(vm,
			PollcheckMode.EXPLICIT_POLLCHECKS_ONLY,
			UnsafeMode.ALLOW_UNSAFE,
			SafetyCheckMode.IMPLICIT_SAFETY_CHECKS,
                        ReturnMode.ONLY_THROW,
                        vm.stackOverflowCheckMode());
	
	DebugInfo di=new DebugInfo(c,0,0);
	
	c.setRoot(c.addHeader(di));
        
        // the locals are exactly the arguments.  load them and pass them on.
        Arg[] stuff=new Arg[1+vm.numAllParams()];
        if (!vm.saveReceiver()) {
            stuff[0]=Arg.NIL;
        } else {
            Var receiver=c.addVar(vm.getAllParams()[0].asExectype());
            c.root().append(
                new ArgInst(
                    di,OpCode.GetArg,receiver,Arg.EMPTY,0));
            stuff[0]=receiver;
        }
        for (int i=1;i<stuff.length;++i) {
            Var arg=c.addVar(vm.getAllParams()[i-1].asExectype());
            c.root().append(
                new ArgInst(di,OpCode.GetArg,arg,Arg.EMPTY,i-1));
            stuff[i]=arg;
        }
        
	c.root().setFooter(
	    new PatchPointFooter(
		di,OpCode.PatchPointFooter,stuff,
                0 /* bytecode pc = 0; i.e. beginning of method */,
                vm.numAllParams() /* the number of arguments */,
                0 /* the stack is empty */,
                e.getClazz(),e.getMessage()));
	
	return c;
    }
    
    Code makeUnsupStub(VisibleMethod vm) {
	Code c=new Code(vm,
			PollcheckMode.EXPLICIT_POLLCHECKS_ONLY,
			UnsafeMode.ALLOW_UNSAFE,
			SafetyCheckMode.IMPLICIT_SAFETY_CHECKS,
                        ReturnMode.ONLY_THROW,
                        vm.stackOverflowCheckMode());
	
	DebugInfo di=new DebugInfo(c,0,0);
	
	c.setRoot(c.addHeader(di));
	
	c.root().append(
	    new MethodInst(
		di,OpCode.InvokeStatic,
		Var.VOID,Arg.EMPTY,
		Runtime.throwUOE));
	
	c.root().setFooter(
	    new Terminal(
		di,OpCode.NotReached,Arg.EMPTY));
	
	return c;
    }
    
    Code handleSynthetic(VisibleMethod vm) {
        if (vm.getName().equals(Constants.SMN_CLONE_HELPER)) {
            return makeCloneHelper(vm);
        } else {
            throw new CompilerException("Unrecognized synthetic method: "+vm);
        }
    }
    
    Code makeCloneHelper(VisibleMethod vm) {
        Code c=new Code(vm,
                        PollcheckMode.EXPLICIT_POLLCHECKS_ONLY,
                        UnsafeMode.ALLOW_UNSAFE,
                        SafetyCheckMode.IMPLICIT_SAFETY_CHECKS,
                        ReturnMode.RETURN_OR_THROW,
                        vm.stackOverflowCheckMode());
        
        DebugInfo di=new DebugInfo(c,0,0);
        
        Header h=c.addHeader(di);
        
        c.setRoot(h);
        
        // NOTE: this potentially disobeys Java encapsulation rules.  when
        // and if the IR is made to enforce these rules we'll need some
        // hack for this.
        
        if (vm.getClazz().isSubclassOf(Global.root().cloneableClass)) {
            Var trg=c.addVar(vm.getClazz().asExectype());
            Var src=c.addVar(vm.getClazz().asExectype());
            
            h.append(
                new ArgInst(
                    di,OpCode.GetArg,src,Arg.EMPTY,0));
            
            h.append(
                new TypeInst(
                    di,OpCode.New,
                    trg,Arg.EMPTY,
                    vm.getClazz().asType()));
            
            for (VisibleField f : vm.getClazz().allFields()) {
                if (f.isInstance()) {
                    Var val=c.addVar(f.getType().asExectype());
                    h.append(
                        new HeapAccessInst(
                            di,OpCode.GetField,
                            val,new Arg[]{src},
                            f));
                    h.append(
                        new HeapAccessInst(
                            di,OpCode.PutField,
                            Var.VOID,new Arg[]{trg,val},
                            f));
                }
            }

            h.setFooter(
                new Terminal(
                    di,OpCode.Return,
                    new Arg[]{trg}));
        } else {
            assert vm.getClazz().isObjectRoot();
            assert vm.getClazz()==Global.root().objectClass;
            
            h.append(
                new MethodInst(
                    di,OpCode.InvokeStatic,
                    Var.VOID,Arg.EMPTY,
                    Runtime.throwCloneNotSupported));
        }
        
        return c;
    }
}


