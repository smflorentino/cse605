/*
 * FullObjLocAnalysis.java
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

public class FullObjLocAnalysis extends ObjLocAnalysis {

    static class Node {
	ObjectLocation location=ObjectLocation.bottom();
	
	boolean union(ObjectLocation newLoc) {
	    if (location!=newLoc) {
		location=newLoc;
		return true;
	    } else {
		return false;
	    }
	}
    }
    
    static abstract class Constraint {
	abstract boolean propagate();
    }
    
    static class SimpleConstraint extends Constraint {
	Node source;
	Node target;
	
	SimpleConstraint(Node source,
			 Node target) {
	    this.source=source;
	    this.target=target;
	}
	
	boolean propagate() {
	    return target.union(target.location.lub(source.location));
	}
    }

    /** Constraints for <b>instance</b> fields.  Static fields are ignored
	by the constraint system. */
    static abstract class FieldConstraint extends Constraint {
	Node field;
	Node source;
	Node target;
	
	FieldConstraint(Node field,
			Node source,
			Node target) {
	    this.field=field;
	    this.source=source;
	    this.target=target;
	}
    }
    
    static class FieldLoadConstraint extends FieldConstraint {
	FieldLoadConstraint(Node field,
			    Node source,
			    Node target) {
	    super(field,source,target);
	}
	
	boolean propagate() {
	    if (source.location!=ObjectLocation.IN_HEAP) {
		return target.union(target.location.lub(field.location));
	    } else {
		return false;
	    }
	}
    }
    
    static class FieldStoreConstraint extends FieldConstraint {
	FieldStoreConstraint(Node field,
			     Node source,
			     Node target) {
	    super(field,source,target);
	}
	
	boolean propagate() {
	    if (target.location!=ObjectLocation.IN_HEAP) {
		return field.union(field.location.lub(source.location));
	    } else {
		return false;
	    }
	}
    }
    
    // these are execution nodes, not call nodes
    HashMap< VisibleMethod, Node[] > paramSets=new HashMap< VisibleMethod, Node[] >();
    HashMap< VisibleMethod, Node > returnSets=new HashMap< VisibleMethod, Node >();

    HashMap< FieldLikeThing, Node > fieldSets=new HashMap< FieldLikeThing, Node >();
    
    public FullObjLocAnalysis() {
	if (Global.verbosity>=1) {
	    Global.log.println("Initializing nodes...");
	}
	
	for (VisibleMethod m : Global.analysis().executedMethods()) {
	    Node[] array=new Node[m.numAllParams()];
	    for (int i=0;i<array.length;++i) {
		array[i]=new Node();
	    }
	    paramSets.put(m,array);
	    returnSets.put(m,new Node());
	}
	
	for (VisibleField f : Global.analysis().liveFields()) {
	    if (f.isInstance()) {
		fieldSets.put(f,new Node());
	    }
	}
	
	final Node arrayElement=new Node();
	fieldSets.put(ArrayElementField.INSTANCE,arrayElement);
	
	if (Global.verbosity>=1) {
	    Global.log.println("Setting up roots...");
	}
	
	Roots roots=Global.analysis().combinedRoots(Global.rootsRepo);
	
	for (VisibleMethod m : Util.composeIterables(roots.called(),
						     roots.dynCalled())) {
            if (m.isExecuted()) {
                for (Node n : paramSets.get(m)) {
                    n.union(ObjectLocation.top());
                }
                returnSets.get(m).union(ObjectLocation.top());
            }
	}
	
	for (VisibleField f : roots.accessed()) {
	    if (f.isInstance() && f.shouldExist()) {
		fieldSets.get(f).union(ObjectLocation.top());
	    }
	}
	
	if (Global.verbosity>=1) {
	    Global.log.println("Building constraints...");
	}
	
	final ArrayList< Constraint > constraints=new ArrayList< Constraint >();
	
	final Node top=new Node();
	top.union(ObjectLocation.top());
	
	Global.forAllCode(new Code.Visitor() {
		public void visit(Code c) {
		    if (c.method()==null) {
			return; // ignore synthetic methods
		    }
		    
		    Node[] params=paramSets.get(c.method());
		    Node result=returnSets.get(c.method());
		    
		    HashMap< Var, Node > vars=new HashMap< Var, Node >();
		    for (Var v : c.vars()) {
			vars.put(v,new Node());
		    }
		    
		    for (Header h : c.headers()) {
			for (Instruction i : h.instructions()) {
			    switch (i.opcode()) {
			    case GetArg: {
				ArgInst ai=(ArgInst)i;
				if (c.param(ai.getIdx()).isObject()) {
				    constraints.add(new SimpleConstraint(
							params[ai.getIdx()],
							vars.get(i.lhs())));
				}
				break;
			    }
			    case Mov:
			    case Phi:
			    case Ipsilon:
                            case Cast:
                            case TypeCheck:
                            case CastExact:
                            case CastNonZero: {
				if (i.rhs(0).type().isObject() &&
				    i.rhs(0) instanceof Var) {
				    constraints.add(new SimpleConstraint(
							vars.get(i.rhs(0)),
							vars.get(i.lhs())));
				}
				break;
			    }
			    case InvokeStatic:
			    case Invoke:
			    case InvokeDynamic: {
				MethodInst mi=(MethodInst)i;
				for (VisibleMethod m : mi.possibleTargets()) {
				    // I feel like I shouldn't have to do this check.
				    if (m.isExecuted()) {
					Node[] myParams=paramSets.get(m);
					assert myParams!=null : m;
					for (int j=0;j<m.numAllParams();++j) {
					    if (i.rhs(j) instanceof Var &&
						i.rhs(j).type().isObject()) {
						constraints.add(new SimpleConstraint(
								    vars.get(i.rhs(j)),
								    myParams[j]));
					    }
					}
					if (m.getType().isObject()) {
					    constraints.add(new SimpleConstraint(
								returnSets.get(m),
								vars.get(i.lhs())));
					}
				    }
				}
				break;
			    }
			    case GetField: {
				HeapAccessInst fi=(HeapAccessInst)i;
				if (fi.shouldExist() &&
				    fi.fieldType().isObject() &&
				    i.rhs(0) instanceof Var) {
				    constraints.add(new FieldLoadConstraint(
							fieldSets.get(fi.field()),
							vars.get(i.rhs(0)),
							vars.get(i.lhs())));
				}
				break;
			    }
			    case PutField:
			    case WeakCASField: {
				HeapAccessInst fi=(HeapAccessInst)i;
				if (fi.shouldExist() &&
				    fi.fieldType().isObject() &&
				    i.rhs(0) instanceof Var &&
				    StoreSourceCalc.get(i) instanceof Var) {
				    constraints.add(new FieldStoreConstraint(
							fieldSets.get(fi.field()),
							vars.get(StoreSourceCalc.get(i)),
							vars.get(i.rhs(0))));
				}
				break;
			    }
			    case OffsetOfField: {
				FieldInst fi=(FieldInst)i;
				if (fi.field().shouldExist() &&
				    fi.field().getType().isObject()) {
				    fieldSets.get(fi.field()).union(ObjectLocation.top());
				}
				break;
			    }
			    case AddressOfField: {
				HeapAccessInst fi=(HeapAccessInst)i;
				if (((VisibleField)fi.field()).shouldExist() &&
				    ((VisibleField)fi.field()).getType().isObject() &&
				    fi.rhs(0) instanceof Var) {
				    constraints.add(new FieldStoreConstraint(
							fieldSets.get(fi.field()),
							top,
							vars.get(i.rhs(0))));
				}
				break;
			    }
			    case ArrayLoad: {
				// FIXME: this could be merged with GetField
				if (i.lhs().type().isObject() &&
				    i.rhs(0) instanceof Var) {
				    constraints.add(new FieldLoadConstraint(
							arrayElement,
							vars.get(i.rhs(0)),
							vars.get(i.lhs())));
				}
				break;
			    }
			    case ArrayStore:
			    case WeakCASElement: {
				if (i.rhs(0) instanceof Var &&
				    StoreSourceCalc.isReference(i) &&
				    StoreSourceCalc.get(i) instanceof Var) {
				    constraints.add(new FieldStoreConstraint(
							arrayElement,
							vars.get(StoreSourceCalc.get(i)),
							vars.get(i.rhs(0))));
				}
				break;
			    }
			    case New:
			    case NewArray: {
				vars.get(i.lhs()).union(allocLocation(c));
				break;
			    }
			    case Load:
			    case StrongLoadCAS: {
				vars.get(i.lhs()).union(ObjectLocation.top());
				break;
			    }
			    default: break;
			    }
			}
			Footer f=h.getFooter();
			if (f.opcode()==OpCode.Return &&
			    c.result().isObject() &&
			    f.rhs(0) instanceof Var) {
			    constraints.add(new SimpleConstraint(vars.get(f.rhs(0)),
								 result));
			}
		    }
		}
	    });
	
	if (Global.verbosity>=1) {
	    Global.log.println("Solving "+constraints.size()+" constraints...");
	}
	
	boolean changed=true;
	for (int i=1;changed;++i) {
	    if (Global.verbosity>=1) {
		Global.log.println("  Iteration #"+i+"...");
	    }
	    changed=false;
	    for (Constraint c : constraints) {
		changed|=c.propagate();
	    }
	}
    }
    
    protected ObjectLocation setForInstField(VisibleField f) {
	Node node=fieldSets.get(f);
	if (node==null) {
	    return ObjectLocation.bottom();
	} else {
	    return node.location;
	}
    }
    
    public ObjectLocation[] paramSetForExec(VisibleMethod m) {
	Node[] nodes=paramSets.get(m);
	if (nodes==null) {
	    return ObjectLocation.bottomArray(m.numAllParams());
	} else {
	    ObjectLocation[] result=new ObjectLocation[nodes.length];
	    for (int i=0;i<result.length;++i) {
		result[i]=nodes[i].location;
	    }
	    return result;
	}
    }
    
    public ObjectLocation returnSetForExec(VisibleMethod m) {
	Node node=returnSets.get(m);
	if (node==null) {
	    return ObjectLocation.bottom();
	} else {
	    return node.location; 
	}
    }
}

