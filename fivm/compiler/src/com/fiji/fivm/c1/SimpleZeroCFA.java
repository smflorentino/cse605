/*
 * SimpleZeroCFA.java
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

import java.util.*;

// "Using Subtyping To Make Inclusion More Like Unification"

/**
 * Implementation of 0CFA using TypeBound sets.  This is meant to be the primary
 * static analysis of fivm, and should be as fast as RTA while being a fair bit
 * more precise.  As well, the core of this analysis is set-agnostic, in the sense
 * that it should not be difficult to hack a version that uses a more precise
 * notion of points-to than just a TypeBound.
 * <p>
 * Some details:
 * <ul>
 * <li>The set representation is TypeBound, though the tie-in to TypeBound is not
 *     so strong that this could not be changed with a modicum of happy hacking.
 *     In particular, each set can be either EXACT, indicating that it contains
 *     instances of a particular type but not of any strict subtypes of that type;
 *     UPPER_BOUND, indicating that it contains instances of subtypes of a particular
 *     type, or BOTTOM, indicating that it is empty (or, rather, it only contains
 *     null).  The main principle behind this set representation is that it only
 *     takes O(1) space.  It could be made to take just one word if we're clever,
 *     but currently we just use TypeBound objects.  This is somewhat nasty since
 *     TypeBounds are immutable, so union and filter operations require allocation.</li>
 * <li>Primitive types are ignored - we can't tell you anything about those.</li>
 * <li>We're monovariant.  Hence the 0 in 0CFA.</li>
 * <li>Constraints are generated from the preprocessed code (see CodeRepo and
 *     CodePreprocessor).  This means that the code is in SSA and has been copy-propagated,
 *     constant-folded, dead-code-eliminated, type-refined (via copy propagation),
 *     and greatly simplified (for example, no more MultiNewArray).  We rely heavily
 *     on this, especially the copy propagation, type refinement, and SSA.</li>
 * <li>We're field-based.  Not to be confused with field-sensitive.  I.e., we
 *     unify all instances of a field into one abstract location.  This could be
 *     changed, and probably wouldn't affect the speed too much, but it would
 *     make the code more complex.</li>
 * <li>We punt on arrays.  Storing to arrays is ignored, while loading from them
 *     results in TOP.  Of course, TOP then immediately gets filtered according to
 *     the array's static type at the point of load, due to copy propagation and
 *     type refinement.  This could be easily improved (for example, we could have
 *     an array load constraint that propagates the arrayElement() of the leastType()
 *     of the source set).  We could be even more precise if we wanted to, especially
 *     if we had a set representation that tracked allocation sites of arrays.</li>
 * <li>This analysis is meant to eventually grow into more than a type-based analysis,
 *     and track allocation sites as well.  When we do this, it will continue to
 *     obey the O(1) set size rule - so as soon as two sets with two different allocation
 *     sites get unioned, we'll turn it into a type set.  Even this dopey treatment
 *     of allocation sites is likely to be a big win for arrays.</li>
 * <li>Native and reflective code is handled using RootsRepo, which isn't necessarily
 *     a sound approach; it's as sound as the information the user gave us.  But,
 *     that's not any worse than the situation for RTA would have been.</li>
 * <li>This analysis is fast.  It converges faster than it takes to compile the
 *     resulting code.</li>
 * <li>This analysis is space-efficient.  It takes a fraction of the amount of memory
 *     that is needed to store the bytecode for the program.</li>
 * <li>This analysis is a big win compared to CHA (25-fold reduction in code size and
 *     compilation time for some programs), and can be a big win over RTA
 *     in places that matter.  Considering how cheap it is, it's probably worth it.</li>
 * </ul>
 */
public class SimpleZeroCFA extends PTA {
    HashMap< VisibleMethod, PTSet[] > paramSets=
	new HashMap< VisibleMethod, PTSet[] >();
    HashMap< VisibleMethod, PTSet > returnSet=
	new HashMap< VisibleMethod, PTSet >();
    HashMap< VisibleField, PTSet > fieldSet=
	new HashMap< VisibleField, PTSet >();
    
    LinkedHashSet< VisibleMethod > called=new LinkedHashSet< VisibleMethod >();
    LinkedHashSet< VisibleMethod > executeMarked=new LinkedHashSet< VisibleMethod >();
    LinkedHashSet< VisibleField > accessed=new LinkedHashSet< VisibleField >();
    LinkedHashSet< Type > used=new LinkedHashSet< Type >();
    LinkedHashSet< Type > inst=new LinkedHashSet< Type >();

    /** Set of instance methods invoked non-dynamically. */
    LinkedHashSet< VisibleMethod > calledDirectly=new LinkedHashSet< VisibleMethod >();

    HashSet< VisibleClass > inflated=new HashSet< VisibleClass >();
    LinkedHashSet< VisibleClass > instClasses=new LinkedHashSet< VisibleClass >();
    LinkedHashSet< VisibleClass > instClassesClosed=new LinkedHashSet< VisibleClass >();
    HashSet< VisibleClass > staticInited=new HashSet< VisibleClass >();
    
    CodeRepo repo;
    RootsRepo rootsRepo;
    
    HashMap< VisibleField, Node > fields=
	new HashMap< VisibleField, Node >();
    
    // NOTE: the handling of MethodInst will have to decide to either link to
    // a:
    // executionAvatar, if we have an Invoke or InvokeStatic;
    // exactAvatar, if we have an InvokeDynamic that is not a dynamicCall(); or
    // dynCall, if we have an InvokeDynamic that is a dynamicCall().
    //
    // The reason for the distinction between non-dynamic InvokeDynamic and
    // Invoke is that InvokeDynamic only goes through if the class of the
    // receiver has been instantiated, while for Invoke only a subclass of
    // the receiver needs to be instantiated.  I'm not sure if the constraint:
    // "Invoke only goes through if a subclass of the receiver is instantiated"
    // is honored precisely, or only conservatively (that the call goes through
    // if the class is just inflated).
    //
    // DynCall may then link to either a:
    // exactAvatar, if we have exact type information about the receiver; or
    // upAvatar, if we have upper bound information about the receiver.
    
    /** 
     * Represents a direct, unambiguous call into the given method.  For static
     * methods, links to these avatars can only be made if the method is added
     * to executeMarked.  For instance methods, executeMarked is handled after
     * we observe that the receiver is not bottom.  All calls to static methods
     * are always linked only to execution avatars (static methods don't have
     * exact or upper bound avatars).  For instance methods, these links only
     * occur for Invoke opcodes, and never for InvokeDynamic opcodes.
     */
    HashMap< VisibleMethod, MethodExecutionAvatar > executionAvatars=
	new HashMap< VisibleMethod, MethodExecutionAvatar >();
    
    /**
     * Represents an InvokeDynamic that we know not to be a dynamic call.  This will
     * occur either if MethodInst tells us that the InvokeDynamic is not a
     * dynamicCall() (either via previous analysis or, more likely, because the
     * method is final), or even if it is a dynamicCall() but we have exact type
     * information about the receiver.  The latter gets handled by the DynCall
     * infrastructure.  Each of these gets linked to exactly one execution avatar
     * automatically when the type is known to be instantiated.
     */
    HashMap< VisibleMethod, MethodAvatar > exactAvatars=
	new HashMap< VisibleMethod, MethodAvatar >();
    
    /**
     * Represents an InvokeDynamic for which we only have upper bound type
     * information.  This occurs when the MethodInst is not a dynamicCall() and
     * DynCall realizes that the receiver is an upper bound.  Each of these gets
     * linked to multiple execution avatars upon instantiation of classes that
     * are subclasses of the given visible class.
     */
    HashMap< VisibleClass, HashMap< MethodSignature, UpperBoundMethodAvatar > > ubAvatars=
	new HashMap< VisibleClass, HashMap< MethodSignature, UpperBoundMethodAvatar > >();
    
    ArrayList< DynCall > dynCalls=
	new ArrayList< DynCall >();
    
    ArrayList< Node > nodes=
	new ArrayList< Node >();
    
    public SimpleZeroCFA(CodeRepo repo,
			 RootsRepo rootsRepo) {
	this.repo=repo;
	this.rootsRepo=rootsRepo;
	
	if (Global.verbosity>=1) {
	    Global.log.println("Running 0CFA.");
	}
	long before=System.currentTimeMillis();
	
	reflect(rootsRepo.rootRoots());
	
	if (Global.verbosity>=1) {
	    Global.log.println(
                "Roots set up, performing fixpoint.  Starting with "+
                (nodes.size()+dynCalls.size())+" constraints.");
	}

	// THE MAIN FIXPOINT LOOP.  this is where the magic happens.
	boolean changed=true;
	for (int cnt=1;changed;++cnt) {
	    if (Global.verbosity>=2) {
		Global.log.println("Starting fixpoint iteration "+cnt);
	    }
	    long innerBefore=System.currentTimeMillis();
	    changed=false;
	    changed|=nodePropagate();
	    changed|=dynCallPropagate();
	    changed|=markToExecuteInstanceMethods();
	    changed|=markToExecuteStaticInit();
	    changed|=processDirectCalls();
	    changed|=executeMarked();
	    long innerAfter=System.currentTimeMillis();
	    if (Global.verbosity>=1) {
		Global.log.println("Fixpoint iteration "+cnt+" with "+(nodes.size()+dynCalls.size())+" constraints completed in "+(innerAfter-innerBefore)+" ms");
	    }
	}
	
	if (Global.verbosity>=1) {
	    Global.log.println("Fixpoint complete, post-processing results.");
	}
	
	for (Map.Entry< VisibleField, Node > e : fields.entrySet()) {
	    VisibleField f=e.getKey();
	    Node n=e.getValue();
	    if (!n.finalSet().isBottom()) {
		accessed.add(f);
	    }
	}
	
	Type.closeSupers(inst);
	
	ArrayList< VisibleMethod > toRemove=new ArrayList< VisibleMethod >();
	for (VisibleMethod m : called) {
	    if (m.hasObjectReceiver() && !inst.contains(m.getClazz().asType())) {
		assert !executeMarked.contains(m) : ""+m+" claims to be executed but its class is never instantiated.";
		toRemove.add(m);
	    }
	}
	called.removeAll(toRemove);
	
	for (VisibleMethod m : called) {
	    used.add(m.getClazz().asType());
	    used.add(m.getType());
	    for (Type t : m.getParams()) {
		used.add(t);
	    }
	}
	
	for (VisibleField f : accessed) {
	    used.add(f.getClazz().asType());
	    used.add(f.getType());
	}
	
	used.addAll(inst);
	Type.closeArrayElements(used);
	Type.closeSupers(used);

	if (Global.verbosity>=3) {
	    Global.log.println("called = "+called);
	    Global.log.println("executeMarked = "+executeMarked);
	    Global.log.println("accessed = "+accessed);
	    Global.log.println("used = "+used);
	    Global.log.println("inst = "+inst);
	}
	
	// FIXME: we can convert the type bounds from UPPER_BOUND to EXACT
	// in cases where we now know that the UPPER_BOUND ones don't have
	// subtypes...  or will that ever happen?  seems like it won't.
	// FIXME: yeah, it will happen.  array loads may create UPPER_BOUND
	// sets and after that we realize that they're actually EXACT.  rare,
	// but it might happen.
	
	for (Map.Entry< VisibleMethod, MethodExecutionAvatar > e
		 : executionAvatars.entrySet()) {
	    VisibleMethod m=e.getKey();
	    MethodExecutionAvatar ma=e.getValue();
	    
	    PTSet[] params=PTSet.bottomArray(ma.params.length);
	    for (int i=0;i<params.length;++i) {
		if (ma.params[i]!=null) {
		    params[i]=ma.params[i].finalSet();
		}
	    }
	    if (m.hasObjectReceiver() && executeMarked.contains(m)) {
		assert !params[0].isBottom()
		    : "receiver is bottom for "+m+", which claims to be executable";
	    }
	    paramSets.put(m,params);
	    
	    if (ma.result!=null) {
		returnSet.put(m,ma.result.finalSet());
	    }
	}
	
	for (Map.Entry< VisibleField, Node > e : fields.entrySet()) {
	    VisibleField f=e.getKey();
	    Node n=e.getValue();
	    fieldSet.put(f,n.finalSet());
	}
	
	long after=System.currentTimeMillis();
	if (Global.verbosity>=1) {
	    Global.log.println("0CFA completed in "+(after-before)+" ms");
	}
	
	// after we are done, clean up to help GC
	exactAvatars=null;
	ubAvatars=null;
	dynCalls=null;
	nodes=null;
	fields=null;
	inflated=null;
	instClasses=null;
	rootsRepo=null;
	repo=null;
	staticInited=null;
	calledDirectly=null;
    }
    
    public Set< Type > usedTypes() {
	return used;
    }
    
    public Set< Type > instantiatedTypes() {
	return inst;
    }
    
    public Set< VisibleMethod > calledMethods() {
	return called;
    }
    
    public Set< VisibleMethod > executedMethods() {
	return executeMarked;
    }
    
    public Set< VisibleField > liveFields() {
	return accessed;
    }
    
    public PTSet top() {
	return TypeBound.OBJECT;
    }
    
    public PTSet setFor(VisibleField f) {
	PTSet result=fieldSet.get(f);
	if (result==null) {
	    return PTSet.bottom();
	} else {
	    return result;
	}
    }
    
    public PTSet[] paramSetForExec(VisibleMethod m) {
	PTSet[] result=paramSets.get(m);
	if (result==null) {
	    return PTSet.bottomArray(m.numAllParams());
	} else {
	    return result;
	}
    }
    
    public PTSet returnSetForExec(VisibleMethod m) {
	PTSet result=returnSet.get(m);
	if (result==null) {
	    return PTSet.bottom();
	} else {
	    return result;
	}
    }
    
    void genConstraints(final Code c,
			final MethodExecutionAvatar ma) {
	// NOTE: c may be a native call wrapper.  that's fine.  but it means
	// that we will see low-level pointer code.  the constraint generator
	// seems to be able to deal with this correctly (cast from pointer to
	// object generates TOP, so native methods will correctly return
	// TOP...)
	
	class VarMap {
	    HashMap< Var, Node > vars=
		new HashMap< Var, Node >();
	    
	    void addVar(Var v) {
		if (v.type().isObject()) {
		    vars.put(v,ma.addNode(v.type().asType()));
		}
	    }
	    
	    Node node(Var v) {
		return vars.get(v);
	    }
	    
	    void assign(Var trg,PTSet other) {
		node(trg).union(other);
	    }
	    
	    void assign(Var trg,Node src) {
		src.addOut(node(trg));
	    }
	    
	    void assign(Node trgNode,Arg src,Type filter) {
		Node srcNode=vars.get(src);
		if (srcNode!=null) {
		    assert trgNode!=null;
		    if (filter==null) {
			srcNode.addOut(trgNode);
		    } else {
			Node filterNode=ma.addNode(filter);
			srcNode.addOut(filterNode);
			filterNode.addOut(trgNode);
		    }
		} else {
		    assert src==Arg.NULL;
		}
	    }
	    
	    void assign(Node trgNode,Arg src) {
		assign(trgNode,src,null);
	    }
	    
	    void assign(Var trg,Arg src,Type filter) {
		assign(node(trg),src,filter);
	    }
		
	    void assign(Var trg,Arg src) {
		assign(trg,src,null);
	    }
	}
	
	VarMap varMap=new VarMap();
	
	LinkedHashSet< Type > alloced=new LinkedHashSet< Type >();
	LinkedHashSet< VisibleMethod > called=new LinkedHashSet< VisibleMethod >();
	LinkedHashSet< VisibleMethod > executeMarked=new LinkedHashSet< VisibleMethod >();
	LinkedHashSet< VisibleField > accessed=new LinkedHashSet< VisibleField >();
	LinkedHashSet< Type > used=new LinkedHashSet< Type >();
	LinkedHashSet< VisibleClass > inited=new LinkedHashSet< VisibleClass >();
	LinkedHashSet< VisibleMethod > calledDirectly=new LinkedHashSet< VisibleMethod >();
	
	for (Var v : c.vars()) {
	    varMap.addVar(v);
            if (v.type().hasEffectiveClass()) {
                repo.willWant(v.type().effectiveClass());
            }
	}
	
	Node top=ma.addNode(Global.root().objectType);
	top.union(TypeBound.OBJECT);
	
	for (Header h : c.headers()) {
	    for (Instruction i : h.instructions()) {
		switch (i.opcode()) {
		case GetArg: {
		    ArgInst ai=(ArgInst)i;
		    if (c.param(ai.getIdx()).isObject()) {
			varMap.assign(ai.lhs(),ma.params[ai.getIdx()]);
		    }
		    break;
		}
		case GetException: {
		    varMap.assign(i.lhs(),TypeBound.OBJECT);
		    break;
		}
		case Mov:
		case Phi:
		case Ipsilon:
                case CastNonZero: {
		    if (i.lhs().type().isObject()) {
			varMap.assign(i.lhs(),i.rhs(0));
		    }
		    break;
		}
		case CheckInit: {
		    ClassInst ci=(ClassInst)i;
                    repo.willWant(ci.value());
		    used.add(ci.value().asType());
		    inited.add(ci.value());
		    break;
		}
		case Instanceof: {
                    Type t=((TypeInst)i).getType();
                    if (t.hasEffectiveClass()) {
                        repo.willWant(t.effectiveClass());
                    }
		    used.add(t);
		    break;
		}
		case Cast: {
		    TypeInst ti=(TypeInst)i;
		    if (i.lhs().type().isObject()) {
			if (i.rhs(0).type().isObject()) {
			    varMap.assign(i.lhs(),i.rhs(0),
					  ti.getType());
			} else {
			    varMap.assign(i.lhs(),TypeBound.OBJECT);
			}
		    }
		    break;
		}
		case TypeCheck: {
		    TypeCheckInst tci=(TypeCheckInst)i;
                    if (tci.typeToCheck().hasEffectiveClass()) {
                        repo.willWant(tci.typeToCheck().effectiveClass());
                    }
                    if (tci.typeToThrow().hasEffectiveClass()) {
                        repo.willWant(tci.typeToThrow().effectiveClass());
                    }
		    used.add(tci.typeToCheck());
		    varMap.assign(i.lhs(),i.rhs(0),tci.typeToCheck());
		    break;
		}
		case InvokeStatic:
		case Invoke:
		case InvokeDynamic: {
		    MethodInst mi=(MethodInst)i;
		    Callable target;
                    repo.willWant(mi.method().getClazz());
		    called.add(mi.method());
		    if (mi.opcode()==OpCode.InvokeDynamic &&
			mi.method().hasObjectReceiver()) {
			if (mi.dynamicCall()) {
			    target=addDynCall(mi.method(),c);
			} else {
			    VisibleMethod vm2=mi.staticTarget();
			    target=exactAvatar(vm2);
			}
		    } else if (mi.opcode()==OpCode.Invoke &&
			       mi.method().hasObjectReceiver()) {
			VisibleMethod vm2=mi.staticTarget();
			target=exactAvatar(vm2);
			calledDirectly.add(vm2);
		    } else {
			if (!mi.method().hasObjectReceiver()) {
			    executeMarked.add(mi.method());
			}
			target=executionAvatar(mi.method());
		    }
		    for (int j=0;j<mi.rhs().length;++j) {
			if (mi.rhs(j).type().isObject()) {
			    varMap.assign(target.params[j],mi.rhs(j));
			}
		    }
		    if (target.result!=null) {
			varMap.assign(mi.lhs(),target.result);
		    }
		    break;
		}
		case GetStatic:
		case GetField: {
		    HeapAccessInst fi=(HeapAccessInst)i;
                    repo.willWant(fi.fieldField().getClazz());
		    if (((VisibleField)fi.field()).getType().isObject()) {
			varMap.assign(i.lhs(),field((VisibleField)fi.field()));
		    } else {
			accessed.add((VisibleField)fi.field());
		    }
		    break;
		}
		case PutStatic:
		case PutField:
		case WeakCASField:
		case WeakCASStatic: {
		    HeapAccessInst fi=(HeapAccessInst)i;
                    repo.willWant(fi.fieldField().getClazz());
		    if (((VisibleField)fi.field()).getType().isObject()) {
			varMap.assign(field((VisibleField)fi.field()),i.rhs(i.rhs().length-1));
		    } else {
			accessed.add((VisibleField)fi.field());
		    }
		    break;
		}
		case OffsetOfField: {
		    FieldInst fi=(FieldInst)i;
                    repo.willWant(fi.field().getClazz());
		    if (((VisibleField)fi.field()).getType().isObject()) {
			// FIXME: should REALLY have a test case for this...
			top.addOut(field((VisibleField)fi.field()));
		    } else {
			accessed.add((VisibleField)fi.field());
		    }
		    break;
		}
		case AddressOfStatic:
		case AddressOfField: {
		    HeapAccessInst fi=(HeapAccessInst)i;
                    repo.willWant(fi.fieldField().getClazz());
		    if (((VisibleField)fi.field()).getType().isObject()) {
			// FIXME: should REALLY have a test case for this...
			top.addOut(field((VisibleField)fi.field()));
		    } else {
			accessed.add((VisibleField)fi.field());
		    }
		    break;
		}
		case GetString: {
		    varMap.assign(i.lhs(),new TypeBound(Global.root().stringType,
							TypeBoundMode.EXACT));
		    break;
		}
		case GetType: {
                    Type t=((TypeInst)i).getType();
                    if (t.hasEffectiveClass()) {
                        repo.willWant(t.effectiveClass());
                    }
		    used.add(t);
		    varMap.assign(i.lhs(),new TypeBound(Global.root().classType,
							TypeBoundMode.EXACT));
		    break;
		}
		case New:
		case NewArray:
                case CastExact: {
		    TypeInst ti=(TypeInst)i;
                    if (ti.getType().hasEffectiveClass()) {
                        repo.willWant(ti.getType().effectiveClass());
                    }
		    alloced.add(ti.getType());
		    varMap.assign(i.lhs(),new TypeBound(ti.getType(),
							TypeBoundMode.EXACT));
		    break;
		}
		case MultiNewArray: {
		    throw new Error("should not see this instruction after post-processing");
		}
		case ArrayLoad: {
		    if (i.lhs().type().isObject()) {
			// FIXME: can do waaaaay better here
			varMap.assign(i.lhs(),
				      new TypeBound(
					  i.rhs(0).type().asType().arrayElement()));
		    }
		    break;
		}
		case Load:
		case StrongLoadCAS: {
		    if (i.lhs().type().isObject()) {
			varMap.assign(i.lhs(),
				      new TypeBound(((MemoryAccessInst)i).getType()));
		    }
		    break;
		}
		default: break;
		}
	    }
	    Footer f=h.getFooter();
	    if (f.opcode()==OpCode.Return && c.result().isObject()) {
		varMap.assign(ma.result,f.rhs(0));
	    }
	}
	
	ma.alloced=new Type[alloced.size()];
	ma.called=new VisibleMethod[called.size()];
	ma.executeMarked=new VisibleMethod[executeMarked.size()];
	ma.accessed=new VisibleField[accessed.size()];
	ma.used=new Type[used.size()];
	ma.inited=new VisibleClass[inited.size()];
	ma.calledDirectly=new VisibleMethod[calledDirectly.size()];
	alloced.toArray(ma.alloced);
	called.toArray(ma.called);
	executeMarked.toArray(ma.executeMarked);
	accessed.toArray(ma.accessed);
	used.toArray(ma.used);
	inited.toArray(ma.inited);
	calledDirectly.toArray(ma.calledDirectly);
    }
    
    void makeExecutionAvatar(VisibleMethod m,Code c) {
	if (c!=null) {
	    genConstraints(c,executionAvatar(m));
	}
    }
    
    boolean inflate(VisibleClass vcBase) {
	if (inflated.contains(vcBase)) {
	    return false;
	}
	boolean changed=false;
	for (VisibleClass vc : vcBase.allSupertypes()) {
	    if (inflated.add(vc)) {
		// generate all contraints for the given class, and wire them to
		// the exact avatars (for static methods) or to the inflated inst
		// avatars (for instance methods)
		ArrayList< Code > codes=repo.codeForClass(vc);
		for (Code c : codes) {
		    VisibleMethod m=c.method();
		    if (m!=null) {
			makeExecutionAvatar(m,c);
		    }
		}
		for (VisibleMethod m : vc.methods()) {
		    if (!m.isAbstract() && !executionAvatars.containsKey(m)) {
			makeExecutionAvatar(m,null);
		    }
		}
		changed=true;
	    }
	}
	return changed;
    }
    
    boolean instantiate(VisibleClass c) {
	if (instClasses.add(c)) {
	    inflate(c);
	    for (VisibleClass c2=c;c2!=null;c2=c2.getSuperclass()) {
		instClassesClosed.add(c2);
	    }
	    for (VisibleMethod m : c.allExactDynableMethods()) {
		MethodExecutionAvatar exec=executionAvatars.get(m);
		assert exec!=null : "inflate() failed to inflate "+m+" for class "+c;
		MethodAvatar call=exactAvatar(m);
		if (!call.initialized) {
		    call.initialized=true;
		    call.reroute(exec);
		}
		for (VisibleClass c2 : c.allSupertypes()) {
		    ubAvatar(c2,m.getSignature()).link(m,exec);
		}
	    }
	    return true;
	} else {
	    return false;
	}
    }
    
    // when do we call this for uses of New, NewArray, and MultiNewArray?
    boolean instantiate(Type t) {
	if (inst.add(t)) {
	    instantiate(t.effectiveClass());
	    return true;
	} else {
	    return false;
	}
    }
    
    boolean reflect(Roots roots) {
	boolean changed=false;
	if (roots!=Roots.EMPTY) {
	    for (VisibleMethod m : roots.called()) {
		if (!m.hasObjectReceiver()) {
		    markToExecute(m);
		}
		for (Node n : executionAvatar(m).params) {
		    if (n!=null) {
			changed|=n.union(TypeBound.OBJECT);
		    }
		}
	    }
	    for (VisibleMethod m : roots.dynCalled()) {
		MethodAvatar ma;
		if (m.runtimeWillCallStatically()) {
		    if (!m.hasObjectReceiver()) {
			markToExecute(m);
			ma=executionAvatar(m);
		    } else {
			ma=exactAvatar(m);
		    }
		} else {
		    ma=ubAvatar(m);
		}
		for (Node n : ma.params) {
		    if (n!=null) {
			changed|=n.union(TypeBound.OBJECT);
		    }
		}
	    }
	    for (VisibleField f : roots.accessed()) {
		if (f.getType().isObject()) {
		    changed|=field(f).union(TypeBound.OBJECT);
		}
		changed|=accessed.add(f);
	    }
	    for (Type t : roots.alloced()) {
		changed|=instantiate(t);
	    }
	    for (Type t : roots.used()) {
		if (t.hasEffectiveClass()) {
		    markToExecuteStaticInitFor(t.effectiveClass());
		}
		changed|=used.add(t);
	    }
	}
	return changed;
    }
    
    boolean execute(VisibleMethod m) {
	inflate(m.getClazz());
	
	MethodExecutionAvatar ma=executionAvatars.get(m);
	
	assert ma!=null : "Don't have an execution avatar for "+m;
	
	if (!ma.initialized) {
	    ma.initialized=true;
	    
	    reflect(rootsRepo.rootsFor(m));
	    
	    for (Type t : ma.alloced) {
		instantiate(t);
	    }
	    
	    for (Type t : ma.used) {
		used.add(t);
	    }
	    
	    for (VisibleMethod m2 : ma.called) {
		called.add(m2);
	    }
	    
	    for (VisibleMethod m2 : ma.executeMarked) {
		assert !m2.hasObjectReceiver();
		markToExecute(m2);
	    }
	    
	    for (VisibleField f : ma.accessed) {
		assert !f.getType().isObject();
		accessed.add(f);
	    }
	    
	    for (VisibleClass c : ma.inited) {
		markToExecuteStaticInitFor(c);
	    }
	    
	    for (VisibleMethod m2 : ma.calledDirectly) {
		calledDirectly.add(m2);
	    }
	    
	    nodePropagate(ma.constraints);
	    // could do something smart here ... like initiate propagation
	    // on instance methods that we call?  or on instance methods that
	    // call us?  eh...  sounds hard.  but I'll probably need it
	    // eventually to make this converge faster.
	    nodes.addAll(ma.constraints);
	    
	    return true;
	} else {
	    return false;
	}
    }
	
    boolean markToExecute(VisibleMethod m) {
        repo.willWant(m.getClazz());
	return executeMarked.add(m) | called.add(m);
    }
    
    boolean markToExecuteInstanceMethods() {
	boolean changed=false;
	for (Map.Entry< VisibleMethod, MethodExecutionAvatar > e
		 : executionAvatars.entrySet()) {
	    VisibleMethod m=e.getKey();
	    MethodAvatar ma=e.getValue();
	    if (m.hasObjectReceiver() &&
		!ma.params[0].isBottom() &&
		markToExecute(m)) {
		changed=true;
	    }
	}
	return changed;
    }
    
    boolean markToExecuteStaticInitFor(VisibleClass c) {
	if (staticInited.add(c)) {
	    if (c.shouldCallCheckInit()) {
		for (VisibleMethod m : c.relevantStaticInits()) {
		    markToExecute(m);
		}
	    }
	    return true;
	} else {
	    return false;
	}
    }
    
    boolean markToExecuteStaticInit() {
	boolean changed=false;
	for (VisibleField f : accessed) {
	    if (f.isStatic()) {
		changed|=markToExecuteStaticInitFor(f.getClazz());
	    }
	}
	for (VisibleMethod m : new ArrayList< VisibleMethod >(called)) {
	    if (m.isStatic()) {
		changed|=markToExecuteStaticInitFor(m.getClazz());
	    }
	}
	for (VisibleClass c : instClasses) {
	    changed|=markToExecuteStaticInitFor(c);
	}
	return changed;
    }
    
    boolean executeMarked() {
	boolean result=false;
	boolean changed=true;
	while (changed) {
	    changed=false;
	    for (VisibleMethod m : new ArrayList< VisibleMethod >(executeMarked)) {
		changed|=execute(m);
	    }
	    if (changed) result=true;
	}
	return result;
    }
    
    boolean processDirectCalls() {
	boolean changed=false;
	for (VisibleMethod m : calledDirectly) {
	    assert m.hasObjectReceiver() : m;
	    assert !m.isInterfaceMethod() : m;
	    if (instClassesClosed.contains(m.getClazz())) {
		// link up the exact avatar to the execution avatar
		// FIXME: is that right?  what if we also have a dyncall that
		// resolves to the exact avatar?  it's a small bug.  basically,
		// we'll flow argument sets from dynamic calls that were
		// exactly resolved into the call, even when those calls
		// could not happen.  oh, well.  we could fix it by creating
		// a directCallAvatar, or something, but that seems like
		// overkill.
		MethodAvatar call=exactAvatar(m);
		if (!call.initialized) {
		    MethodExecutionAvatar exec=executionAvatars.get(m);
		    call.initialized=true;
		    call.reroute(exec);
		    changed=true;
		}
	    }
	}
	return changed;
    }
    
    boolean forward=false;
    boolean nodePropagate(ArrayList< Node > constraints) {
	boolean result=false;
	boolean changed=true;
	while (changed) {
	    changed=false;
	    if (forward) {
		for (int i=0;i<constraints.size();++i) {
		    Node src=constraints.get(i);
		    for (Node trg : src.out) {
			changed|=trg.union(src);
		    }
		}
	    } else {
		for (int i=constraints.size()-1;i>=0;--i) {
		    Node src=constraints.get(i);
		    for (Node trg : src.out) {
			changed|=trg.union(src);
		    }
		}
	    }
	    forward=!forward;
	    if (changed) result=true;
	}
	return result;
    }
    
    boolean nodePropagate() {
	return nodePropagate(nodes);
    }
    
    boolean dynCallPropagate() {
	boolean changed=false;
	for (DynCall dc : dynCalls) {
	    changed|=dc.update(this);
	}
	return changed;
    }
    
    Node field(VisibleField f) {
	Node n=fields.get(f);
	if (n==null) {
	    fields.put(f,n=addNode(f.getType().asExectype().asType()));
	}
	return n;
    }
    
    MethodExecutionAvatar executionAvatar(VisibleMethod m) {
	MethodExecutionAvatar ma=executionAvatars.get(m);
	if (ma==null) {
	    executionAvatars.put(m,ma=new MethodExecutionAvatar(this,m));
	}
	return ma;
    }
    
    MethodAvatar exactAvatar(VisibleMethod m) {
	assert m.hasObjectReceiver();
	MethodAvatar result=exactAvatars.get(m);
	if (result==null) {
	    exactAvatars.put(m,result=new MethodAvatar(this,m));
	}
	assert result!=null;
	return result;
    }
    
    // this method is weird, but I think it makes perfect sense for reflection.
    // it should never be used for anthing else.
    UpperBoundMethodAvatar ubAvatar(VisibleMethod m) {
	assert m.hasObjectReceiver();
	return ubAvatar(m.getClazz(),m.getSignature());
    }
    
    UpperBoundMethodAvatar ubAvatar(VisibleClass c,MethodSignature s) {
	HashMap< MethodSignature, UpperBoundMethodAvatar > forClass=
	    ubAvatars.get(c);
	if (forClass==null) {
	    ubAvatars.put(
		c,
		forClass=new HashMap< MethodSignature, UpperBoundMethodAvatar >());
	}
	UpperBoundMethodAvatar result=forClass.get(s);
	if (result==null) {
	    forClass.put(s,result=new UpperBoundMethodAvatar(this,ActualBinding.INSTANCE,s));
	}
	return result;
    }
    
    Node addNode(Type filter) {
	Node result=new Node(filter);
	nodes.add(result);
	return result;
    }
    
    DynCall addDynCall(VisibleMethod vm,Object context) {
	DynCall result=new DynCall(this,vm,context);
	dynCalls.add(result);
	return result;
    }
    
    static class Node {
	PTSet set;
	Type filter;
	Node[] out;
	
	Node(Type filter) {
	    set=PTSet.bottom();
            assert filter==null || filter.isObject() : filter;
	    this.filter=filter;
	    out=EMPTY;
	}
        
        PTSet currentSet() {
            return set;
        }
        
        PTSet finalSet() {
            return set;
        }
        
        boolean isBottom() {
            return set.isBottom();
        }
	
	boolean union(PTSet other) {
	    PTSet newSet=set.union(other);
	    if (filter!=null) {
		newSet=newSet.filter(filter);
	    }
	    if (newSet.equals(set)) {
		return false;
	    } else {
		set=newSet;
		return true;
	    }
	}
	
	boolean lift(Type t) {
	    return union(new TypeBound(t));
	}
	
	boolean union(Node other) {
	    return union(other.set);
	}
	
	/** add an outgoing edge to this node.  this creates an inclusion
	    constraint. */
	boolean addOut(Node other) {
	    if (other!=this) {
		// FIXME: maybe check if we already have this constraint?
		Node[] newOut=new Node[out.length+1];
		System.arraycopy(out,0,
				 newOut,0,
				 out.length);
		newOut[out.length]=other;
		out=newOut;
		return true;
	    } else {
		return false;
	    }
	}
	
	/** replace all outgoing edges (if there are any) with just this one.
	    this replaces all outgoing inclusion constraints with just one. */
	boolean reroute(Node other) {
	    if (other==this) {
		out=EMPTY;
		return false;
	    } else {
		boolean changed=false;
		if (out.length==1) {
		    if (out[0]!=other) {
			changed=true;
			out[0]=other;
		    }
		} else {
		    out=new Node[]{other};
		    changed=true;
		}
		return changed;
	    }
	}

	static Node[] EMPTY=new Node[0];
    }
    
    static class Callable {
	Node[] params;
	Node result;

	Callable(SimpleZeroCFA parent,VisibleMethod vm) {
	    this(parent,
		 vm.getActualBinding(),
		 vm.getClazz().asType(),
		 vm.getParams(),vm.getType());
	}
	
	Callable(SimpleZeroCFA parent,
		 ActualBinding binding,MethodSignature s) {
	    this(parent,
		 binding,
		 null,
		 s.getParams(),
		 s.getType());
	}
	
	Callable(SimpleZeroCFA parent,
		 ActualBinding binding,
		 Type receiver,
		 Type[] paramTypes,
		 Type result) {
	    int base=0;
            switch (binding) {
            case INSTANCE:
		params=new Node[paramTypes.length+1];
		params[0]=parent.addNode(receiver);
		base=1;
                break;
            case STATIC:
		params=new Node[paramTypes.length];
		base=0;
                break;
            case INSTANCE_UNBOXED:
		params=new Node[paramTypes.length+1];
		base=1;
                break;
            default:
                throw new Error("unknown binding: "+binding);
	    }
	    for (int i=0;i<paramTypes.length;++i) {
		if (paramTypes[i].isObject()) {
		    params[i+base]=parent.addNode(paramTypes[i].asExectype().asType());
		}
	    }
	    if (result.isObject()) {
		this.result=parent.addNode(result.asExectype().asType());
	    }
	}
	
	void addOut(Callable other) {
	    for (int i=0;i<params.length;++i) {
		if (params[i]!=null) {
		    params[i].addOut(other.params[i]);
		}
	    }
	    if (other.result!=null) {
		other.result.addOut(result);
	    }
	}
	
	void reroute(Callable other) {
	    for (int i=0;i<params.length;++i) {
		if (params[i]!=null) {
		    params[i].reroute(other.params[i]);
		}
	    }
	    if (other.result!=null) {
		other.result.addOut(result);
	    }
	}
    }
    
    static class DynCall extends Callable {
	Object context;
	VisibleMethod vm;
	PTSet dispatch;
	MethodAvatar ma;
	
	DynCall(SimpleZeroCFA parent,VisibleMethod vm,Object context) {
	    super(parent,vm);
	    this.vm=vm;
	    if (Global.verbosity>=3) {
		this.context=context;
	    }
	    dispatch=PTSet.bottom();
	}
	
	boolean update(SimpleZeroCFA parent) {
	    PTSet newDispatch=params[0].currentSet();
	    try {
                // FIXME - if the currentSet() is lazily computed then we've
                // got ourselves a bit of a problem here.
		if (newDispatch!=dispatch) {
		    assert !newDispatch.isBottom();
		    dispatch=newDispatch;
		    MethodAvatar newMA;
		    if (newDispatch.isTypeExact() ||
			newDispatch.leastType().isArray()) {
			VisibleMethod target=
			    vm.pruneExactly(newDispatch.leastType().effectiveClass());
			if (target!=null) {
			    newMA=parent.exactAvatar(target);
			} else {
			    // happens if we know that the call cannot succeed based
			    // on the type that flows into it.
			    newMA=null;
			}
		    } else {
			newMA=parent.ubAvatar(newDispatch.leastType().effectiveClass(),
					      vm.getSignature());
		    }
		    if (newMA!=ma) {
			assert newMA!=null;
			ma=newMA;
			reroute(newMA);
		    } /* we could potentially return false on the else branch... */
		    return true;
		} else {
		    return false;
		}
	    } catch (Throwable e) {
		throw new CompilerException(
		    "Failed to update DynCall with newDispatch = "+newDispatch+", "+
		    "vm = "+vm+", and context = "+context,e);
	    }
	}
    }
    
    static class MethodAvatar extends Callable {
	/**
	 * This field has different meaning depending on the kind of avatar:
	 * <ul>
	 * <li>For execution avatars: that this method is
	 *     known to have been invoked (execute() has been called).</li>
	 * <li>For exact avatars of instance methods: that this method's
	 *     receiver class has been instantiated, and so the avatar has
	 *     been linked to the inflated instance method avatar.</li>
	 * <li>For exact avatars of static methods: depends how you look at it.
	 *     I think of it as having no meaning, but really, the exact
	 *     avatar of a static method is aliased with its execution
	 *     avatar.</li>
	 * <li>For upper bound avatars: no meaning.</li>
	 * </ul>
	 */
	boolean initialized;
	
	MethodAvatar(SimpleZeroCFA parent,VisibleMethod vm) {
	    super(parent,vm);
	}
	
	MethodAvatar(SimpleZeroCFA parent,ActualBinding binding,MethodSignature s) {
	    super(parent,binding,s);
	}
    }
    
    static class UpperBoundMethodAvatar extends MethodAvatar {
	HashSet< VisibleMethod > targets=new HashSet< VisibleMethod >();
	
	UpperBoundMethodAvatar(SimpleZeroCFA parent,
			       ActualBinding binding,
			       MethodSignature s) {
	    super(parent,binding,s);
	}
	
	void link(VisibleMethod m,MethodExecutionAvatar ma) {
	    if (targets.add(m)) {
		addOut(ma);
	    }
	}
    }
    
    static class MethodExecutionAvatar extends MethodAvatar {
	ArrayList< Node > constraints;
	
	Type[] alloced;
	Type[] used;
	VisibleMethod[] called;
	VisibleMethod[] executeMarked;
	VisibleField[] accessed;
	VisibleClass[] inited;
	VisibleMethod[] calledDirectly;
	
	MethodExecutionAvatar(SimpleZeroCFA parent,VisibleMethod vm) {
	    super(parent,vm);
	    constraints=new ArrayList< Node >();
	    alloced=Type.EMPTY;
	    used=Type.EMPTY;
	    called=VisibleMethod.EMPTY;
	    executeMarked=VisibleMethod.EMPTY;
	    accessed=VisibleField.EMPTY;
	    inited=VisibleClass.EMPTY;
	    calledDirectly=VisibleMethod.EMPTY;
	}
	
	Node addNode(Type t) {
	    Node result=new Node(t);
	    constraints.add(result);
	    return result;
	}
    }
    
    public void pruneCalledMethods(Set< VisibleMethod > pruneSet) {
        Util.retain(called,pruneSet);
    }
    
    public void pruneExecutedMethods(Set< VisibleMethod > pruneSet) {
        Util.retain(executeMarked,pruneSet);
    }
}

