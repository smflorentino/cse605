/*
 * LCodePhase.java
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

package com.fiji.fivm.c1.x86;

import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.LArg;
import com.fiji.fivm.c1.x86.arg.Tmp;

public abstract class LCodePhase {
    
    LCode code;
    Object changedCodeReason;
    
    public LCodePhase(LCode code) {
	this.code=code;
    }
    
    // call this if you don't want sanity checking
    public abstract void visitCode();
    
    // call this if you do want sanity checking
    public void doit() {
	int oldVerbosity=Global.verbosity;
	if (Global.noisyMethods.contains(code.origin().origin())) {
	    Global.verbosity=100;
	}
	try {
            try {
                if (Global.verbosity>=3) {
                    Global.log.println("Code before "+this+":");
                    LCodeDumper.dump(code,Global.log);
                }
                visitCode();
                code.verify(toString());
                if (Global.verbosity>=3) {
                    Global.log.println("Code after "+this+":");
                    LCodeDumper.dump(code,Global.log);
                }
            } catch (Throwable e) {
                Global.log.println("In "+this+".doit() caught: "+e);
                e.printStackTrace(Global.log);
                Global.log.println("Code at time of error:");
                LCodeDumper.dump(code,Global.log);
                Util.rethrow(e);
            }
        } catch (Throwable e) {
            throw new CompilerException("Phase "+this+" failed",e);
	} finally {
	    Global.verbosity=oldVerbosity;
	}
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
        LCodePhase lastChangedPhase;
        Object changedCodeReason;
        
        public PhaseResult() {}
        
        public void reset() {
            lastChangedPhase=null;
            changedCodeReason=null;
        }
        
        public void noteChange(LCodePhase phase,
                               Object reason) {
            this.lastChangedPhase=phase;
            this.changedCodeReason=reason;
        }
        
        public LCodePhase lastChangedPhase() {
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
    
    // utilities
    
    protected Tmp tmpify(LOp before,LType type,LArg from) {
        if (from instanceof Tmp) {
            return (Tmp)from;
        } else {
            Tmp tmp=code.addTmp(type.kind());
            before.prepend(
                new LOp(
                    LOpCode.Mov,type,
                    new LArg[]{
                        from,
                        tmp
                    }));
            return tmp;
        }
    }
    
    protected void emitMov(LOp before,LType type,LArg from,LArg to) {
        LUtil.emitMov(code,before,type,from,to);
    }
}

