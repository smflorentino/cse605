/*
 * LHeader.java
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

import java.util.*;
import com.fiji.fivm.*;
import com.fiji.fivm.c1.*;
import com.fiji.fivm.c1.x86.arg.LArg;

/**
 * The header of a basic block in the Fiji X86 LIR.
 */
public class LHeader extends LNode {
    
    LCode code;
    
    LFooter footer;
    int version;
    
    Object onlist; // used for worklists and such
    
    /**
     * Probability of this header executing as indicated by the user.  This feeds
     * into the BranchPrediction of Branches that lead into this, and is ignored
     * by our own header layout.  Note that this does NOT override the
     * BrandPrediction of branches whose prediction is already not UNKNOWN.
     *
     * This is a bit of a hack at this point.  It's not quite principled.  But that's
     * ok.  It's an optimization.
     */
    HeaderProbability probability;
    
    float frequency=1.f;
    
    LHeader(LCode code) {
        this.code=code;
	next=footer=new LFooter(LOpCode.NotReached,LType.Void,LArg.EMPTY,LHeader.EMPTY);
	footer.prev=this;
        footer.head=this;
	this.probability=HeaderProbability.DEFAULT_PROBABILITY;
    }
    
    public LCode code() {
        return code;
    }
    
    public void setProbability(HeaderProbability probability) {
	this.probability=probability;
    }
    
    public HeaderProbability probability() {
	return probability;
    }
    
    public void setFrequency(float frequency) {
        this.frequency=frequency;
    }
    
    public float frequency() {
        return frequency;
    }
    
    public boolean unlikely() {
	return probability==HeaderProbability.UNLIKELY_TO_EXECUTE;
    }
    
    public boolean likely() {
        return !unlikely();
    }
    
    public float probableFrequency() {
        switch (probability) {
        case DEFAULT_PROBABILITY: return frequency;
        case UNLIKELY_TO_EXECUTE: return frequency/100.f;
        default: throw new Error();
        }
    }
    
    public LOp first() {
	return (LOp)next;
    }
    
    public void setFooter(LFooter footer) {
	LNode prevFooterPrev=this.footer.prev;
	prevFooterPrev.next=footer;
	footer.prev=prevFooterPrev;
	this.footer=footer;
        footer.head=this;
	version++;
    }
    
    public LFooter getFooter() {
	return footer;
    }
    
    public LFooter footer() {
        return footer;
    }
    
    public LOp append(LOp i) {
        assert !i.footer();
	footer.prepend(i);
	return i;
    }
    
    public LOp prepend(LOp i) {
        assert !i.footer();
	i.next=next;
	i.prev=this;
	next.prev=i;
	next=i;
        i.head=this;
	return i;
    }
    
    public void forceOwnership(LOp start) {
        LOp o=start;
        for (;;) {
            o.head=this;
            if (o instanceof LFooter) {
                break;
            }
            o=(LOp)o.next;
        }
    }
    
    public void forceOwnership() {
        forceOwnership((LOp)next);
    }
    
    /**
     * Splits the basic block before the given operation.  The receiver will
     * contain all operations prior to the given one, while the returned
     * block will contain the operation and all that follow it.  A jump
     * will be inserted from the first block to the second.
     * @return The basic block that includes the given operation and all
     *         that follow it.
     */
    public LHeader split(LOp before) {
	LHeader cont=makeSimilar();
	
	LFooter f=footer;
	LNode newTail=before.prev;
	
	cont.next=before;
	before.prev=cont;
	cont.footer=f;
	
	footer=new LFooter(LOpCode.Jump,LType.Void,LArg.EMPTY,new LHeader[]{cont});
        footer.head=this;
	newTail.next=footer;
	footer.prev=newTail;
        
	version++;
        
        cont.forceOwnership();
	
	return cont;
    }
    
    /**
     * Make a basic block that has the same attributes as the given one.
     * The current attributes that are honored are the probability and the
     * exception handler.
     * @return a basic block that has the same attributes as the given one.
     */
    public LHeader makeSimilar() {
	LHeader result=code.addHeader();
	result.setProbability(probability());
        result.setFrequency(frequency());
	return result;
    }
    
    public LHeader makeCopy() {
        LHeader result=code.addHeader();
        result.setProbability(probability());
        result.setFrequency(frequency());
        
        for (LOp i : instructions()) {
            result.append((LOp)i.copy());
        }
        result.setFooter((LFooter)getFooter().copy());
        
        return result;
    }
    
    public void terminate(LOp before) {
	before.prev.next=footer;
	footer.prev=before.prev;
	version++;
    }
    
    public void terminateAfter(LOp after) {
	terminate((LOp)after.next);
    }
    
    public void terminate(LOp before,
			  LFooter newFooter) {
	terminate(before);
	setFooter(newFooter);
    }
    
    public void terminateAfter(LOp after,
			       LFooter newFooter) {
	terminate((LOp)after.next,newFooter);
    }
    
    public boolean notReached(LOp before) {
	if (before.opcode()!=LOpCode.NotReached) {
	    terminate(before,new LFooter(LOpCode.NotReached,LType.Void,LArg.EMPTY,LHeader.EMPTY));
	    return true;
	} else {
	    return false;
	}
    }
    
    public boolean notReachedAfter(LOp after) {
	return notReached((LOp)after.next);
    }
    
    public LHeader[] likelySuccessors() {
        return footer.likelySuccessors();
    }
    
    public LHeader[] unlikelySuccessors() {
        return footer.unlikelySuccessors();
    }
    
    public LHeader[] successors() {
	return footer.successors();
    }
    
    public LHeader successor(int i) {
        return footer.successor(i);
    }
    
    /** forward iterator over instructions.  it is safe to remove an
	instruction after you get it; as well, it is safe to append and
	prepend, in which case the next instruction in the iterator is
	preserved (i.e. you won't iterate over instructions you prepend
	or append) */
    public Iterable< LOp > instructions() {
	return new Iterable< LOp >() {
	    public Iterator< LOp > iterator() {
		return new Iterator< LOp >() {
		    LNode cur=next;
		    public boolean hasNext() {
			return cur!=footer;
		    }
		    public LOp next() {
			try {
			    LOp result=(LOp)cur;
			    cur=cur.next;
			    return result;
			} catch (ClassCastException e) {
			    throw new NoSuchElementException(
				"While prcessing "+LHeader.this+", we got a class "+
				"cast on "+cur);
			}
		    }
		    public void remove() {
			try {
			    ((LOp)cur.prev).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    /** forward iterator over operations.  it is safe to remove an
	instruction after you get it; as well, it is safe to append and
	prepend, in which case the next operation in the iterator is
	preserved (i.e. you won't iterate over operations you prepend
	or append) */
    public Iterable< LOp > operations() {
	return new Iterable< LOp >(){
	    public Iterator< LOp > iterator() {
		return new Iterator< LOp >(){
		    LNode cur=next;
		    public boolean hasNext() {
			return cur!=null && !(cur instanceof LHeader);
		    }
		    public LOp next() {
			try {
			    LOp result=(LOp)cur;
			    cur=cur.next;
			    return result;
			} catch (ClassCastException e) {
			    throw new NoSuchElementException();
			}
		    }
		    public void remove() {
			try {
			    ((LOp)cur.prev).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    /** forward iterator over instructions.  it is safe to remove an
	instruction after you get it; as well, it is safe to append and
	prepend, in which case the next instruction in the iterator is
	preserved (i.e. you won't iterate over instructions you prepend
	or append).  this iterator has additional intelligence for
	when you split or terminate blocks; in that case it will not
	iterate into the new block. */
    public Iterable< LOp > instructions2() {
	return new Iterable< LOp >() {
	    public Iterator< LOp > iterator() {
		return new Iterator< LOp >() {
		    LNode cur=next;
		    int expectedVersion=version;
		    public boolean hasNext() {
			return cur!=footer && expectedVersion==version;
		    }
		    public LOp next() {
			if (expectedVersion!=version || cur==footer) {
			    throw new NoSuchElementException();
			}
			LOp result=(LOp)cur;
			cur=cur.next;
			return result;
		    }
		    public void remove() {
			if (expectedVersion!=version) {
			    throw new NoSuchElementException();
			}
			try {
			    ((LOp)cur.prev).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    /** forward iterator over operations.  it is safe to remove an
	instruction after you get it; as well, it is safe to append and
	prepend, in which case the next operation in the iterator is
	preserved (i.e. you won't iterate over operations you prepend
	or append).  this iterator has additional intelligence for
	when you split or terminate blocks; in that case it will not
	iterate into the new block.  */
    public Iterable< LOp > operations2() {
	return new Iterable< LOp >(){
	    public Iterator< LOp > iterator() {
		return new Iterator< LOp >(){
		    LNode cur=next;
		    int expectedVersion=version;
		    public boolean hasNext() {
			return cur!=null
			    && !(cur instanceof LHeader)
			    && expectedVersion==version;
		    }
		    public LOp next() {
			if (expectedVersion!=version) {
			    throw new NoSuchElementException();
			}
			try {
			    LOp result=(LOp)cur;
			    cur=cur.next;
			    return result;
			} catch (ClassCastException e) {
			    throw new NoSuchElementException();
			}
		    }
		    public void remove() {
			if (expectedVersion!=version) {
			    throw new NoSuchElementException();
			}
			try {
			    ((LOp)cur.prev).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    public Iterable< LOp > reverseInstructions() {
	return new Iterable< LOp >() {
	    public Iterator< LOp > iterator() {
		return new Iterator< LOp >() {
		    LNode cur=footer.prev;
		    public boolean hasNext() {
			return cur!=LHeader.this;
		    }
		    public LOp next() {
			try {
			    LOp result=(LOp)cur;
			    cur=cur.prev;
			    return result;
			} catch (ClassCastException e) {
			    if (Global.verbosity>=1) {
				Global.log.println("Got exception for "+cur+" while iterating over "+LHeader.this+" (note that ("+cur+"=="+LHeader.this+")=="+(cur==LHeader.this)+")");
				e.printStackTrace(Global.log);
			    }
			    throw new NoSuchElementException();
			}
		    }
		    public void remove() {
			try {
			    ((LOp)cur.next).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    public Iterable< LOp > reverseOperations() {
	return new Iterable< LOp >() {
	    public Iterator< LOp > iterator() {
		return new Iterator< LOp >() {
		    LNode cur=footer;
		    public boolean hasNext() {
			return cur!=LHeader.this;
		    }
		    public LOp next() {
			try {
			    LOp result=(LOp)cur;
			    cur=cur.prev;
			    return result;
			} catch (ClassCastException e) {
			    if (Global.verbosity>=1) {
				Global.log.println("Got exception for "+cur+" while iterating over "+LHeader.this+" (note that ("+cur+"=="+LHeader.this+")=="+(cur==LHeader.this)+")");
				e.printStackTrace(Global.log);
			    }
			    throw new NoSuchElementException();
			}
		    }
		    public void remove() {
			try {
			    ((LOp)cur.next).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    public String toString() {
	return "B"+order;
    }
    
    public String labelName() {
        return code.cname()+"_B"+order+"B";
    }
    
    public static Iterable< LHeader > emptyHeaderIterable() {
	return new Iterable< LHeader >() {
	    public Iterator< LHeader > iterator() {
		return new Iterator< LHeader >() {
		    public LHeader next() {
			throw new NoSuchElementException();
		    }
		    public boolean hasNext() {
			return false;
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public static LHeader[] EMPTY=new LHeader[0];
    public static CompactArrayList< LHeader > EMPTY_CAL=new CompactArrayList< LHeader >();
    public static ArrayList< LHeader > EMPTY_AL=new ArrayList< LHeader >();
    
    public static Comparator< LHeader > FREQUENCY_COMPARATOR=
        new Comparator< LHeader >() {
        
        public int compare(LHeader a,LHeader b) {
            if (a.frequency<b.frequency) {
                return -1;
            } else if (a.frequency>b.frequency) {
                return 1;
            } else {
                return 0;
            }
        }
    };
}


