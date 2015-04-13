/*
 * PhaseFixpoint.java
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


public class PhaseFixpoint extends TimedCodePhase {
    CodePhase[] phases;
    int limit;
    boolean doit;
    TimeoutPoller poller;
    
    public PhaseFixpoint(Code c,CodePhase[] phases,int limit,boolean doit,TimeoutPoller poller) {
	super(c);
	this.phases=phases;
	this.limit=limit;
        this.doit=doit;
        this.poller=poller;
	for (CodePhase cp : phases) {
	    assert cp.code==code;
	}
    }
    
    public void visitCode() {
	boolean changed=true;
	for (int cnt=1;changed && limit--!=0;++cnt) {
	    changed=false;
	    for (CodePhase cp : phases) {
                poll();
                if (cp instanceof TimedCodePhase) {
                    ((TimedCodePhase)cp).setPoller(poller());
                }
                if (doit) {
                    cp.doit();
                } else {
                    cp.visitCode();
                }
		if (Global.verbosity>=4) {
		    Global.log.println("In PhaseFixpoint, Code after "+cp+":");
		    CodeDumper.dump(code,
				    Global.log,
				    CodeDumper.Mode.NO_ANALYSES);
		}
		if (cp.changedCode()) {
		    if ((cnt%100)==0 && Global.verbosity>=2) {
			Global.log.println("Fixpoint continues because "+cp+", "+cp.changedCodeReason());
		    }
		    changed=true;
		}
		cp.reset();
	    }
	    if (changed) setChangedCode();
	}
    }
    
    public String toString() {
	return "PhaseFixpoint["+Util.dump(phases)+"]";
    }
    
    public static void fixpoint(CodePhase[] phases) {
	fixpoint(phases,-1);
    }
    
    public static void fixpoint(CodePhase[] phases,int limit) {
	new PhaseFixpoint(phases[0].code,phases,limit,false,null).doit();
    }
    
    public static void fixpointSafe(CodePhase[] phases,int limit) {
	new PhaseFixpoint(phases[0].code,phases,limit,true,null).doit();
    }
    
    // common fixpoints
    
    public static CodePhase[] simplifyPhases(Code c) {
	return new CodePhase[]{new KillDead(c),new LocalCopyProp(c),new Simplify(c)};
    }
    
    public static CodePhase[] fullSimplifyPhases(Code c) {
        if (true) {
            return new CodePhase[]{new KillDead(c),new LocalCopyProp(c),new OptPeephole(c),new Simplify(c)};
        } else if (true) {
            return simplifyPhases(c);
        } else {
            return new CodePhase[]{new KillDead(c),new LocalCopyProp(c),new MergeTails(c),new Simplify(c)};
        }
    }
    
    public static void simplifyNoCheck(Code c) {
	new PhaseFixpoint(c,simplifyPhases(c),-1,false,null).visitCode();
    }
    
    public static void simplify(Code c) {
	fixpoint(simplifyPhases(c));
    }
    
    public static void fullSimplify(Code c) {
	fixpoint(fullSimplifyPhases(c));
    }
    
    public static void rce(Code c) {
        if (false) {
            PhaseFixpoint.fixpoint(
                new CodePhase[]{new OptRCE(c),
                                new SimpleProp(c)},
                -1);
        } else {
            PhaseFixpoint.fixpointSafe(
                new CodePhase[]{new SimpleGVN(c),
                                new OptReload(c),
                                new SimpleProp(c)},
                -1);
        }
    }
}

