/*
 * CodePhase.java
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

public abstract class CodePhase implements Contextable {
    
    protected Code code;
    Object changedCodeReason;
    
    public CodePhase(Code code) {
	this.code=code;
    }
    
    public Context getContext() { return code.getContext(); }
    
    // call this if you don't want sanity checking
    public abstract void visitCode();
    
    // call this if you do want sanity checking
    public void doit() {
        long before=CodePhaseTimings.tic();
	int oldVerbosity=Global.verbosity;
	if (Global.noisyMethods.contains(code.origin().origin())) {
	    Global.verbosity=100;
	}
	try {
	    if (Global.verbosity>=2) {
		code.recomputeOrder();
		Code origCode=code.copy();
		try {
		    if (Global.verbosity>=3) {
			Global.log.println("Code before "+this+":");
			CodeDumper.dump(code,Global.log);
		    }
		    visitCode();
                    code.verify(toString());
		    if (Global.verbosity>=3) {
			Global.log.println("Code after "+this+":");
			CodeDumper.dump(code,Global.log);
		    }
		} catch (Throwable e) {
		    Global.log.println("In "+this+".doit() caught: "+e);
		    e.printStackTrace(Global.log);
		    Global.log.println("Code before phase:");
		    CodeDumper.dump(origCode,Global.log,CodeDumper.Mode.COMPUTE_ANALYSES);
		    Global.log.println("Code at time of error:");
		    //code.killAllAnalyses();
		    CodeDumper.dump(code,Global.log,CodeDumper.Mode.COMPUTE_ANALYSES);
		    Util.rethrow(e);
		}
	    } else {
		try {
		    visitCode();
                    code.verify(toString());
		} catch (Throwable e) {
		    Global.log.println("In "+this+".doit() caught: "+e);
		    e.printStackTrace(Global.log);
		    Global.log.println("Code at time of error:");
		    CodeDumper.dump(code,Global.log,CodeDumper.Mode.COMPUTE_ANALYSES);
		    Util.rethrow(e);
		}
	    }
        } catch (Throwable e) {
            throw new CompilerException("Phase "+this+" failed",e);
	} finally {
	    Global.verbosity=oldVerbosity;
	}
        CodePhaseTimings.toc(this,before);
    }
    
    public void setChangedCode() { setChangedCode(this); }
    public void setChangedCode(Object why) {
	if (Global.verbosity>=5) {
	    Global.log.println("Changed code because "+why);
	}
	changedCodeReason=why;
    }
    public boolean changedCode() {
	return changedCodeReason!=null;
    }
    public Object changedCodeReason() {
	return changedCodeReason;
    }
    public void reset() {
	changedCodeReason=null;
    }
    
    public static class PhaseResult {
        CodePhase lastChangedPhase;
        Object changedCodeReason;
        
        public PhaseResult() {}
        
        public void reset() {
            lastChangedPhase=null;
            changedCodeReason=null;
        }
        
        public void noteChange(CodePhase phase,
                               Object reason) {
            this.lastChangedPhase=phase;
            this.changedCodeReason=reason;
        }
        
        public CodePhase lastChangedPhase() {
            return lastChangedPhase;
        }
        
        public Object changeCodeReason() {
            return changedCodeReason;
        }
        
        public String toString() {
            return ""+changedCodeReason+" in "+lastChangedPhase;
        }
    }
    
    public boolean doitAndCheckChanged(PhaseResult pr) {
        reset();
        doit();
        boolean result=changedCode();
        if (result && pr!=null) {
            pr.noteChange(this,changedCodeReason());
        }
        reset();
        return result;
    }
    
    public boolean doitAndCheckChanged() {
        return doitAndCheckChanged(null);
    }
    
    public String toString() {
        return getClass().getName();
    }
    
    // utilities
    
    protected Arg loadTypeData(Arg objVar,Operation before) {
	Var objTypeData=code.addVar(Exectype.POINTER);
	before.prepend(
	    new SimpleInst(
		before.di(),OpCode.GetTypeDataForObject,
		objTypeData,new Arg[]{objVar}));
	return objTypeData;
    }
    
    protected void loadAtOffset(Var lhs,
				Operation before,
				Arg ptr,
				int offset,
				Type t,
                                Mutability mutability,
                                Volatility volatility) {
	Var offPtr=code.addVar(Exectype.POINTER);
	before.prepend(
	    new SimpleInst(
		before.di(),OpCode.Add,
		offPtr,
		new Arg[]{
		    ptr,
		    new PointerConst(offset)
		}));
	before.prepend(
	    new MemoryAccessInst(
		before.di(),OpCode.Load,
		lhs,new Arg[]{offPtr},
		t,mutability,volatility));
    }
    
    protected void loadAtOffset(Var lhs,
                                Operation before,
                                Arg ptr,
                                int offset,
                                Type t) {
        loadAtOffset(lhs,before,ptr,offset,t,
                     Mutability.MUTABLE,
                     Volatility.NON_VOLATILE);
    }
    
    protected Arg[] produceZero(Type type,
				Operation before) {
	return Util.produceZero(code,type,before);
    }
    
}

