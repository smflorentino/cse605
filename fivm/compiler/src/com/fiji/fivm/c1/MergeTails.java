/*
 * MergeTails.java
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

// does one pass of tail merging.  run this as part of a fixpoint that includes
// Simplify.
public class MergeTails extends CodePhase {
    public MergeTails(Code c) { super(c); }
    
    // FIXME: this won't work so well after SSA, but we could make it work
    // after SSA (and work better in general) if we identify block-local
    // variables (block-local = defined in the block, dead at the tail of
    // the block, and dead for exception handling) using the index and contents
    // of the instruction that defines them.  for now we won't do that 'cause
    // it sounds hard-ish.
    
    // FIXME2: handle the case where two basic blocks have operations in
    // common at the end, but not at the beginning.  could that this, I guess,
    // if I aggressively split all basic blocks so that they only contained
    // one operation.  ugh.  the funny thing is that I bet it wouldn't even be
    // slower.
    
    // FIXME3: have a notion of arguments that are meaningless, like the
    // Return(0) that we put when throwing exceptions.

    static class BlockHasher {
	Header h;
	int hashCode;
	
	BlockHasher(Header h) {
	    this.h=h;
	    try {
		hashCode=0;
		if (h.hasHandlers()) {
		    hashCode+=h.handler().hashCode();
		}
		for (Operation o : h.operations()) {
		    hashCode=hashCode*51+OpHashCode.get(o);
		}
	    } catch (Throwable e) {
		throw new CompilerException("Error computing hash code for "+h,e);
	    }
	}
	
	public int hashCode() {
	    return hashCode;
	}
	
	public boolean equals(Object other_) {
	    if (this==other_) return true;
	    if (!(other_ instanceof BlockHasher)) return false;
	    BlockHasher other=(BlockHasher)other_;
	    if (h.handler()!=other.h.handler()) return false;
	    Iterator< Operation > ai=h.operations().iterator();
	    Iterator< Operation > bi=other.h.operations().iterator();
	    for (;;) {
		if (ai.hasNext()!=bi.hasNext()) return false;
		if (!ai.hasNext()) break;
		if (!OpEquals.get(ai.next(),bi.next())) return false;
	    }
	    return true;
	}
    }
    
    public void visitCode() {
	// surprisingly, this is like really fast.  only 4% of
	// exec time.
	
	HashMap< BlockHasher, Header > canonicalizer=
	    new HashMap< BlockHasher, Header >();
	HashMap< Header, Header > repMap=
	    new HashMap< Header, Header >();
	
	for (Header h : code.headers()) {
	    BlockHasher bh=new BlockHasher(h);
	    Header h2=canonicalizer.get(bh);
	    if (h2==null) {
		canonicalizer.put(bh,h);
	    } else {
		repMap.put(h,h2);
	    }
	}
	
	if (!repMap.isEmpty()) {
	    new MultiSuccessorReplacement(repMap).transform(code);
	    setChangedCode();
	    code.killAllAnalyses();
	}
    }
}

