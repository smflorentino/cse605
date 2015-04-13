/*
 * Simplify.java
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

public class Simplify extends KillDeadCode {
    public Simplify(Code code) { super(code); }
    
    public void visitCode() {
        long before=CodePhaseTimings.tic();
        
	boolean changed=true;
	PredsCalc preds=new PredsCalc(code);
        int cnt=0;
        String lastChange=null;
	while (changed) {
            cnt++;
            if ((cnt%100)==0) {
                Global.log.println("Simplify taking very long time on "+code.shortName()+
                                    "; last change: "+lastChange);
            }
            
            lastChange=null;
	    changed=super.runOnce();
            if (changed) {
                lastChange="KillDeadCode";
            }
	    
	    verifySanity();
	
            boolean curChanged=false;
            
            ThrowingHeadersCalc thc=code.getThrowingHeaders();
            for (Header h : code.headers()) {
                if (h.handler()!=null && thc.doesNotThrow(h)) {
                    if (Global.verbosity>=5) {
                        Global.log.println("Labeling "+h+" as non-throwing.");
                    }
                    h.setHandler(null);
                    curChanged=changed=true;
                    lastChange="dropped handlers from non-throwing header";
                }
            }
            if (curChanged) preds=new PredsCalc(code);

            verifySanity();
	    
	    // figure out which jumps are redundant, and combine the jumper
	    // and jumpee
	    // a jump is redundant if:
	    // - only successor of the jumper is the jumpee
	    // - only predecessor of the jumpee is the jumper
	    // - jumper and jumpee have same exception handler -or- one of them doesn't throw
	    HashSet< Header > killed=new HashSet< Header >();
	    curChanged=false;
	    for (Header h : code.headers()) {
		if (!killed.contains(h) &&
		    h.getFooter().opcode()==OpCode.Jump &&
		    !killed.contains(h.getFooter().next()) /* redundant? */ &&
		    (h.handler()==((Header)h.getFooter().next()).handler() ||
		     thc.doesNotThrow(h.defaultSuccessor())) &&
		    h.getFooter().next()!=code.root()) {
		    Header h2=(Header)h.getFooter().next();
		    if (Global.verbosity>=6) {
			Global.log.println("Considering killing "+h2+", which has:");
			Global.log.println("  Normal preds: "+preds.normalPredecessors(h2));
			Global.log.println("  Exceptional preds: "+preds.exceptionalPredecessors(h2));
		    }
		    Iterator< Header > targPredsI=preds.allPredecessors(h2).iterator();
		    if (targPredsI.hasNext() &&
			targPredsI.next()==h &&
			!targPredsI.hasNext()) {
			if (Global.verbosity>=6) {
			    Global.log.println(
				"Killing "+h2+" because of a redundant "+
				"jump from "+h);
			    if (Global.verbosity>=101) {
				Global.log.println("dumping code before kill:");
				CodeDumper.dump(code,
						Global.log,
						CodeDumper.Mode.NO_ANALYSES);
				Global.log.println("h2's next: "+h2.next);
				Global.log.println("h2's footer: "+h2.footer);
			    }
			}
			Node preFooterNode=h.footer.prev;
			if (Global.verbosity>=10) {
			    Global.log.println("h = "+h);
			    Global.log.println("pre footer node: "+preFooterNode);
			    Global.log.println("pre footer node equals h: "+(preFooterNode==h));
			}
			preFooterNode.next=h2.next;
			h2.next.prev=preFooterNode;
			h.footer=h2.footer;
                        h.forceOwnership((Operation)h2.next);
			killed.add(h2);
			curChanged=changed=true;
                        lastChange="dropped unnecessary Jump";
			if (Global.verbosity>=101) {
			    Global.log.println("dumping code right after kill:");
			    CodeDumper.dump(code,
					    Global.log,
					    CodeDumper.Mode.NO_ANALYSES);
			}
		    }
		}
	    }
	    if (curChanged) {
		// remove them now since they are kinda corrupt
		for (Iterator< Header > i=code.headers().iterator();i.hasNext();) {
		    Header h=i.next();
		    if (killed.contains(h)) {
			if (Global.verbosity>=6) {
			    Global.log.println("Removing "+h+" (jump case)");
			}
			i.remove();
		    }
		}
		preds=new PredsCalc(code);
	    }
	    
	    verifySanity();

	    if (Global.verbosity>=101) {
		Global.log.println("dumping code after kill:");
		CodeDumper.dump(code,
				Global.log,
				CodeDumper.Mode.NO_ANALYSES);
	    }
	    
	    
	    if (Global.verbosity>=101) {
		Global.log.println(
		    "Dumping code before redundant block elimination:");
		CodeDumper.dump(code,Global.log);
	    }
	    
            code.recomputeOrder();

	    // figure out which blocks are redundant
	    // a block is redundant if it is empty and ends in a jump.  the
	    // predecessors might as well jump to the redundant block's jumpee.
	    // special case: a block that is just a jump, but that jump
	    // jumps to itself, is not redundant!
	    // NOTE: this pass is not redundant to the one just before, since
	    // the one just before will only remove redundant blocks that
	    // are jumped to, rather than ones that are branched to or are
	    // gotten to via exception handlers.
	    killed=new HashSet< Header >();
	    HashSet< Header > offLimits=new HashSet< Header >();
	    curChanged=false;
	    for (Header h : code.headers()) {
		if (!offLimits.contains(h) &&
		    h.next() == h.getFooter() &&
		    h.getFooter().opcode()==OpCode.Jump &&
		    h.getFooter().defaultSuccessor()!=h) {
		    assert h.handler()==null;
		    for (Header ph : preds.normalPredecessors(h)) {
			ph.getFooter().accept(new SuccessorReplacement(
						  h,(Header)h.getFooter().next()));
		    }
		    for (ExceptionHandler eh : preds.exceptionHandlerPredecessors(h)) {
			if (Global.verbosity>=4) {
			    Global.log.println("Retargetting "+eh+" from "+eh.next+" to "+h.getFooter().next());
			}
			eh.next=h.getFooter().next();
		    }
		    if (code.root()==h) {
			code.setRoot((Header)h.getFooter().next());
		    }
		    if (Global.verbosity>=6) {
			Global.log.println(
			    "Killing "+h+" because of a redundant "+
			    "block; preds = "+Util.dump(preds.allPredecessors(h))+
			    "; succs = "+Util.dump(h.allSuccessors()));
		    }
		    killed.add(h);
		    offLimits.add((Header)h.getFooter().next());
		    changed=curChanged=true;
                    lastChange="killed redundant block";
		}
	    }
	    if (curChanged) {
		// remove them now since they are kinda corrupt
		for (Iterator< Header > i=code.headers().iterator();i.hasNext();) {
		    Header h=i.next();
		    if (killed.contains(h)) {
			if (Global.verbosity>=6) {
			    Global.log.println("Removing "+h+" (block case)");
			}
			i.remove();
		    }
		}
		preds=new PredsCalc(code);
	    }
	    
	    verifySanity();

	    // figure out which exception handlers are identical, and combine
	    // them
	    // two exception handlers are identical if:
	    // - they drop to the same exception handlers
	    // - they have the same successors
	    // - they handle the same classes exceptions
	    HashMap< EHKey, ExceptionHandler > canonicalHandlers=
		new HashMap< EHKey, ExceptionHandler >();
	    HashMap< ExceptionHandler, ExceptionHandler > ehRepMap=
		new HashMap< ExceptionHandler, ExceptionHandler >();
	    curChanged=false;
	    for (ExceptionHandler eh : code.handlers()) {
		EHKey hasher=new EHKey(eh);
		ExceptionHandler eh2=canonicalHandlers.get(hasher);
		if (eh2==null) {
		    canonicalHandlers.put(hasher,eh);
		} else {
		    if (Global.verbosity>=5) {
			Global.log.println("in "+code+" replacing "+eh+" with "+eh2);
		    }
		    ehRepMap.put(eh,eh2);
		}
	    }
	    if (!ehRepMap.isEmpty()) {
		if (Global.verbosity>=5) {
		    Global.log.println("running replacement...");
		}
		new MultiSuccessorReplacement(ehRepMap).transform(code);
		changed=curChanged=true;
                lastChange="combined identical exception handlers";
		preds=new PredsCalc(code);
	    }
	    
	    verifySanity();

	    if (changed) {
		code.killAllAnalyses();
		setChangedCode();
	    }
	}
        
        CodePhaseTimings.toc(this,before);
    }
    
    static class EHKey {
	ExceptionHandler dropsTo;
	VisibleClass handles;
	Header next;
	
	EHKey(ExceptionHandler dropsTo,
	      VisibleClass handles,
	      Header next) {
	    this.dropsTo=dropsTo;
	    this.handles=handles;
	    this.next=next;
	}
	
	EHKey(ExceptionHandler eh) {
	    this(eh.dropsTo,eh.handles,(Header)eh.next);
	}
	
	public int hashCode() {
	    return (dropsTo!=null?dropsTo.hashCode():5)
		+(handles==null?0:handles.hashCode())+next.hashCode();
	}
	
	public boolean equals(Object other_) {
	    if (this==other_) return true;
	    if (!(other_ instanceof EHKey)) return false;
	    EHKey other=(EHKey)other_;
	    return dropsTo==other.dropsTo
		&& handles==other.handles
		&& next==other.next;
	}
    }
}


