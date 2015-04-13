/*
 * Header.java
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
import com.fiji.fivm.*;
import java.nio.*;

/**
 * The header of a basic block.  This class directly conveys almost all of the
 * information necessary for basic-block-level analysis and transformation.
 * It holds the exception handler information for the block, some branch
 * prediction information, and identifying information (via Node.order).  It
 * also holds references to the basic block's first Operation as well as its
 * Footer.  The Footer contains successor information, though convenience
 * methods are provided by this class to retrieve this information directly.
 * The first operation is represented by Node.next.  Node.prev should be null.
 * Node.next might be equal to Header.footer, if the block has zero Instructions
 * and only serves as a control flow point.
 * <p>
 * The Fiji IR has two properties that may at first be surprising:
 * <ol>
 * <li>Predecessor information is implicit.  Modifying the control flow graph
 *     is performed by ignoring predecessors and then invalidating them by
 *     using Code.killAllAnalyses().  When predecessor information is needed,
 *     it is recomputed on-the-fly using Code.getPreds().
 * <li>Header is a Basic Block and vice versa.  I.e. if you see references
 *     to Header in the code, these are really references to the block.
 *     This is because there is one Header per Basic Block and one Basic Block
 *     per Header.  Thus, we do not have any objects to separately identify
 *     blocks other than just the Header.  Interestingly, the Footer also
 *     has this property (one-to-one mapping between block and Footer) but
 *     we never use the Footer to identify basic blocks.
 * </ol>
 * Headers contain a linked list of Operations, starting with Header.next
 * (inherited from Node.next).  This is a doubly-linked list using prev/next
 * pointers in Node.  Per basic block, this list "logically" terminates with
 * a Footer.  I.e. you have a Header serving as the head of the list, followed
 * by zero or more Instructions, and finally terminated with a Footer.  However,
 * Footer.next may be used to designate the Footer's default successor - so
 * it is not adviseable to follow the next pointers until null is reached as
 * this has almost no meaning.
 */
public class Header extends Node {
    
    Code code;
    
    Footer footer;
    ExceptionHandler handler;
    int version;
    
    float frequency=1.f;
    
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
    
    Header(DebugInfo di,
	   Code code) {
	super(di);
	next=footer=new Terminal(di,OpCode.NotReached,Arg.EMPTY);
	footer.prev=this;
        footer.head=this;
	this.code=code;
	this.probability=HeaderProbability.DEFAULT_PROBABILITY;
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
    
    public ExceptionHandler handler() { return handler; }
    public boolean hasHandlers() { return handler!=null; }
    
    public void setHandler(ExceptionHandler eh) { this.handler=eh; }
    
    public Operation first() {
	return (Operation)next;
    }
    
    public void setFooter(Footer footer) {
	Node prevFooterPrev=this.footer.prev;
	prevFooterPrev.next=footer;
	footer.prev=prevFooterPrev;
	this.footer=footer;
        footer.head=this;
	version++;
    }
    
    public Footer getFooter() {
	return footer;
    }
    
    public Footer footer() {
        return footer;
    }
    
    public Code code() { return code; }
    
    public Instruction append(Instruction i) {
	footer.prepend(i);
	return i;
    }
    
    public Instruction prepend(Instruction i) {
	i.next=next;
	i.prev=this;
	next.prev=i;
	next=i;
        i.head=this;
	return i;
    }
    
    public void forceOwnership(Operation start) {
        Operation o=start;
        for (;;) {
            o.head=this;
            if (o instanceof Footer) {
                break;
            }
            o=(Operation)o.next;
        }
    }
    
    public void forceOwnership() {
        forceOwnership((Operation)next);
    }
    
    /**
     * Splits the basic block before the given operation.  The receiver will
     * contain all operations prior to the given one, while the returned
     * block will contain the operation and all that follow it.  A jump
     * will be inserted from the first block to the second.
     * @return The basic block that includes the given operation and all
     *         that follow it.
     */
    public Header split(Operation before) {
	Header cont=makeSimilar(before.di());
	
	Footer f=footer;
	Node newTail=before.prev;
	
	cont.next=before;
	before.prev=cont;
	cont.footer=f;
	
	footer=new Jump(before.di(),cont);
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
    public Header makeSimilar(DebugInfo di) {
	Header result=code.addHeader(di);
	result.setProbability(probability());
	result.setHandler(handler);
        result.setFrequency(frequency());
	return result;
    }
    
    public Header makeCopy() {
        Header result=code.addHeader(di);
        result.setProbability(probability());
        result.setHandler(handler);
        result.setFrequency(frequency());
        
        for (Instruction i : instructions()) {
            result.append(i.copyAndMultiAssign());
        }
        result.setFooter((Footer)getFooter().copyAndMultiAssign());
        
        return result;
    }
    
    public void terminate(Operation before) {
	before.prev.next=footer;
	footer.prev=before.prev;
	version++;
    }
    
    public void terminateAfter(Instruction after) {
	terminate((Operation)after.next);
    }
    
    public void terminate(Operation before,
			  Footer newFooter) {
	terminate(before);
	setFooter(newFooter);
    }
    
    public void terminateAfter(Instruction after,
			       Footer newFooter) {
	terminate((Operation)after.next,newFooter);
    }
    
    public boolean notReached(Operation before) {
	if (before.opcode()!=OpCode.NotReached) {
	    terminate(before,new Terminal(di,OpCode.NotReached,Arg.EMPTY));
	    return true;
	} else {
	    return false;
	}
    }
    
    public boolean notReachedAfter(Instruction after) {
	return notReached((Operation)after.next);
    }
    
    public Iterable< Header > likelySuccessors() {
        return footer.likelySuccessors();
    }
    
    public Iterable< Header > normalSuccessors() {
	return footer.successors();
    }
    
    public Header defaultSuccessor() {
        return footer.defaultSuccessor();
    }
    
    public Iterable< ExceptionHandler > handlers() {
	return new Iterable< ExceptionHandler >() {
	    public Iterator< ExceptionHandler > iterator() {
		return new Iterator< ExceptionHandler >() {
		    ExceptionHandler curHandler=handler;
		    public boolean hasNext() {
			return curHandler!=null;
		    }
		    public ExceptionHandler next() {
			if (curHandler==null) {
			    throw new NoSuchElementException();
			}
			ExceptionHandler result=curHandler;
			curHandler=curHandler.dropsTo;
			return result;
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public Iterable< Header > exceptionalSuccessors() {
	return new Iterable< Header >() {
	    public Iterator< Header > iterator() {
		return new Iterator< Header >() {
		    ExceptionHandler curHandler=handler;
		    public boolean hasNext() {
			return curHandler!=null;
		    }
		    public Header next() {
			if (curHandler==null) {
			    throw new NoSuchElementException();
			}
			Header result=(Header)curHandler.next;
			curHandler=curHandler.dropsTo;
			return result;
		    }
		    public void remove() {
			throw new UnsupportedOperationException();
		    }
		};
	    }
	};
    }
    
    public Iterable< Header > allSuccessors() {
	return Util.composeIterables(normalSuccessors(),exceptionalSuccessors());
    }
    
    /** forward iterator over instructions.  it is safe to remove an
	instruction after you get it; as well, it is safe to append and
	prepend, in which case the next instruction in the iterator is
	preserved (i.e. you won't iterate over instructions you prepend
	or append) */
    public Iterable< Instruction > instructions() {
	return new Iterable< Instruction >() {
	    public Iterator< Instruction > iterator() {
		return new Iterator< Instruction >() {
		    Node cur=next;
		    public boolean hasNext() {
			return cur!=footer;
		    }
		    public Instruction next() {
			try {
			    Instruction result=(Instruction)cur;
			    cur=cur.next;
			    return result;
			} catch (ClassCastException e) {
			    throw new NoSuchElementException(
				"While prcessing "+Header.this+", we got a class "+
				"cast on "+cur);
			}
		    }
		    public void remove() {
			try {
			    ((Instruction)cur.prev).remove();
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
    public Iterable< Operation > operations() {
	return new Iterable< Operation >(){
	    public Iterator< Operation > iterator() {
		return new Iterator< Operation >(){
		    Node cur=next;
		    public boolean hasNext() {
			return cur!=null && !(cur instanceof Header);
		    }
		    public Operation next() {
			try {
			    Operation result=(Operation)cur;
			    cur=cur.next;
			    return result;
			} catch (ClassCastException e) {
			    throw new NoSuchElementException();
			}
		    }
		    public void remove() {
			try {
			    ((Instruction)cur.prev).remove();
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
    public Iterable< Instruction > instructions2() {
	return new Iterable< Instruction >() {
	    public Iterator< Instruction > iterator() {
		return new Iterator< Instruction >() {
		    Node cur=next;
		    int expectedVersion=version;
		    public boolean hasNext() {
			return cur!=footer && expectedVersion==version;
		    }
		    public Instruction next() {
			if (expectedVersion!=version || cur==footer) {
			    throw new NoSuchElementException();
			}
			Instruction result=(Instruction)cur;
			cur=cur.next;
			return result;
		    }
		    public void remove() {
			if (expectedVersion!=version) {
			    throw new NoSuchElementException();
			}
			try {
			    ((Instruction)cur.prev).remove();
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
    public Iterable< Operation > operations2() {
	return new Iterable< Operation >(){
	    public Iterator< Operation > iterator() {
		return new Iterator< Operation >(){
		    Node cur=next;
		    int expectedVersion=version;
		    public boolean hasNext() {
			return cur!=null
			    && !(cur instanceof Header)
			    && expectedVersion==version;
		    }
		    public Operation next() {
			if (expectedVersion!=version) {
			    throw new NoSuchElementException();
			}
			try {
			    Operation result=(Operation)cur;
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
			    ((Instruction)cur.prev).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    public Iterable< Instruction > reverseInstructions() {
	return new Iterable< Instruction >() {
	    public Iterator< Instruction > iterator() {
		return new Iterator< Instruction >() {
		    Node cur=footer.prev;
		    public boolean hasNext() {
			return cur!=Header.this;
		    }
		    public Instruction next() {
			try {
			    Instruction result=(Instruction)cur;
			    cur=cur.prev;
			    return result;
			} catch (ClassCastException e) {
			    if (Global.verbosity>=1) {
				Global.log.println("Got exception for "+cur+" while iterating over "+Header.this+" (note that ("+cur+"=="+Header.this+")=="+(cur==Header.this)+")");
				e.printStackTrace(Global.log);
			    }
			    throw new NoSuchElementException();
			}
		    }
		    public void remove() {
			try {
			    ((Instruction)cur.next).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    public Iterable< Operation > reverseOperations() {
	return new Iterable< Operation >() {
	    public Iterator< Operation > iterator() {
		return new Iterator< Operation >() {
		    Node cur=footer;
		    public boolean hasNext() {
			return cur!=Header.this;
		    }
		    public Operation next() {
			try {
			    Operation result=(Operation)cur;
			    cur=cur.prev;
			    return result;
			} catch (ClassCastException e) {
			    if (Global.verbosity>=1) {
				Global.log.println("Got exception for "+cur+" while iterating over "+Header.this+" (note that ("+cur+"=="+Header.this+")=="+(cur==Header.this)+")");
				e.printStackTrace(Global.log);
			    }
			    throw new NoSuchElementException();
			}
		    }
		    public void remove() {
			try {
			    ((Instruction)cur.next).remove();
			} catch (ClassCastException e) {
			    throw new IllegalStateException(e);
			}
		    }
		};
	    }
	};
    }
    
    public <T> T accept(Visitor<T> v) {
	return v.visit(this);
    }
    
    public String toString() {
	return "B"+order;
    }
    
    public String labelName() {
        return code.cname()+"_B"+order+"B";
    }
    
    int getNioSize() {
        int result=4+4+4+4;
        for (Operation o : operations()) {
            result+=o.getNioSize();
        }
        return result;
    }
    
    void writeTo(NioContext ctx,
                 ByteBuffer buffer) {
        buffer.putInt(order);
        buffer.putInt(ctx.diCodes.codeFor(di));
        buffer.putInt(ctx.nodeCodes.codeFor(handler));
        buffer.putInt(probability.ordinal());
        buffer.putFloat(frequency);
        for (Operation o : operations()) {
            o.writeTo(ctx,buffer);
        }
    }
    
    Header(NioRead r,Code c) {
        this((DebugInfo)null,c);
    }
    
    void readFrom(NioContext ctx,
                  ByteBuffer buffer) {
        order=buffer.getInt();
        di=ctx.diCodes.forCode(buffer.getInt());
        handler=(ExceptionHandler)ctx.nodeCodes.forCode(buffer.getInt());
        probability=HeaderProbability.values()[buffer.getInt()];
        frequency=buffer.getFloat();
        for (;;) {
            Operation o=Operation.readFrom(ctx,buffer);
            if (o instanceof Instruction) {
                append((Instruction)o);
            } else {
                setFooter((Footer)o);
                break;
            }
        }
    }
    
    public static Iterable< Header > emptyHeaderIterable() {
	return new Iterable< Header >() {
	    public Iterator< Header > iterator() {
		return new Iterator< Header >() {
		    public Header next() {
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
    
    public static Header[] EMPTY=new Header[0];
    public static CompactArrayList< Header > EMPTY_CAL=new CompactArrayList< Header >();
    public static ArrayList< Header > EMPTY_AL=new ArrayList< Header >();
}


