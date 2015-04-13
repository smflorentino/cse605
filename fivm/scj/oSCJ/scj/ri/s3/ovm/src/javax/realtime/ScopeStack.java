/**
 *  This file is part of oSCJ.
 *
 *   oSCJ is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   oSCJ is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with oSCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *   Copyright 2009, 2010 
 *   @authors  Lei Zhao, Ales Plsek
 */

package javax.realtime;

import org.ovmj.java.Opaque;

import edu.purdue.scj.VMSupport;
import edu.purdue.scj.utils.Utils;

/**
 * The scope stack is a conceptual data structure defined by the memory usage
 * rules of the Real-time Specification for Java. Each real-time thread (and
 * effectively async event handler) has a current scope stack. The initial scope
 * stack is determined by the scope stack of the creating thread/handler and by
 * the initial memory area passed to the thread/handler constructor.
 * <p>
 * The scope stack then grows/shrinks as different memory areas are entered and
 * left. This would be a simple structure except for the fact that the
 * <tt>executeInArea</tt> method requires the formation of a new scope stack
 * which temporarily replaces the current one for the duration of that method.
 * This means that the entire stack has to be "pushed" on entry to the method
 * and "popped" on exit - which can be done by copying the data structure.
 * 
 * <p>
 * The scope stack does not control or influence the "single parent rule" It's
 * main role is to answer the question: "is area X on the current scope stack?",
 * and to maintain the scope-stack indexing mechanism that the RTSJ defines.
 * 
 * <p>
 * Rules and definitions:
 * <ul>
 * <li>The current allocation context is the <b>top</b> of the scope stack.
 * <li>The bottom of the scope stack has index zero, and is the outermost scope
 * (even if it is actually heap or immortal).
 * <li>Outer scopes have a longer lifetime than inner ones, and so an outer
 * scope can not hold a reference to an object from an inner scope (that is the
 * RTSJ programming model - though the VM can violate this when safe).
 * <li>The initial memory area of the thread/handler has a constant index in the
 * initial scope stack. If the initial scope stack is pushed, due to
 * executeInArea, it may not be valid to ask for the index of the initial area
 * (this is a somewhat vague area of the spec and the information is completely
 * useless in any case).
 * </ul>
 * 
 * <p>
 * The scope stack naturally contains references to memory areas that are
 * "newer" than the area in which the current thread/handler was constructed.
 * These references need to be managed with scope checks disabled.
 * 
 * <h3>Memory Management</h3>
 * 
 * <p>
 * Memory management for a scope stack is done using an executive domain
 * transient area. These areas allow for dynamic memory allocation without
 * consuming any memory from the current allocation context. This means that we
 * can grow a scope stack as needed by making a new area of the desired size,
 * copying over the contents, and then freeing an area when it is no longer
 * needed.
 * <p>
 * This memory management approach has to be applied at two levels. Internally,
 * the data structures of a scope stack are kept in a special transient area
 * that grows as needed. Additionally, when a temporary scope stack is needed,
 * that will also be allocated in its own transient area.
 * 
 * <h3>Thread-Safety</h3>
 * <p>
 * Scope stacks are not thread-safe. Only the current thread should ever access
 * or modify its own scope stack.
 * 
 * @author David Holmes
 * 
 *         ScopeStack is allocated in each thread's ScopeStack space. Each
 *         thread can have several ScopeStack due to executeInArea(), but only
 *         one of them is active. Since the ScopeStack space is also a stack
 *         (each element of it would be a ScopeStack), the top is always the
 *         active one.
 * 
 *         Stack copying occurs at:
 * 
 *         1) Current thread invokes executeInArea(). 2)
 * 
 *         > ScopeStacks owned by a thread have shorter life time than the
 *         thread.
 * 
 *         > The array referenced by ScopeStack._stack should be allocated in
 *         the stack space of its owner thread.
 */
class ScopeStack {

	/**
	 * Default initial scope stack capacity - and the increment by which we
	 * expand.
	 */
	static final int _INCREASE = 8;

	/**
	 * Size of a raw ScopeStack object. Note that this doesn't get allocated in
	 * the region used by this ScopeStack but rather has to be accounted for in
	 * the allocation context when a ScopeStack is created - usually when the
	 * thread is constructed, but also for executeInArea. Note that the creating
	 * context needs to allow for debugging/assertions in its space
	 * considerations.
	 */
	static final long _BASE_SIZE = VMSupport.sizeOf(ScopeStack.class);

	/** Current scope stack size - also acts as next index into the stack */
	int _size;

	/** Current scope stack capacity */
	int _capacity;

	/** The memory space in bytes that this ScopeStack uses */
	long _memSize;

	/**
	 * An index below which all scopes are active. This is used in
	 * executeInArea, where we have the whole stack but just lower part of it is
	 * active.
	 */
	int _activePointer;

	/**
	 * The owner thread of this stack. We need to know the owner because the
	 * scope stack is created in creater's memory space, but we may want to put
	 * it in the owner's space as it can grow.
	 * 
	 * TODO: as far we don't address this issue. The scope stack is simply put
	 * in creater's space and not allowed to grow. In other word, you can ignore
	 * this field now.
	 */
	Thread _owner;

	/**
	 * The actual scope stack, again storing BackingStoreID, and including
	 * scopes as well as immortal.
	 * 
	 * The array object should reside in
	 */
	Opaque[] _stack;

	/** Create an empty scope stack with the default capacity. */
	ScopeStack(Thread owner) {
		_size = 0;
		_activePointer = 0;
		_capacity = _INCREASE;
		_memSize = constructionSizeOf(_INCREASE);
		_owner = owner;
		allocBackingStoreArray();
	}

	/**
	 * Create a scope stack that is a copy of the given scope stack's active
	 * part. The memory used by this constructor is determined by the memory
	 * currently being used by the other ScopeStack, as we make an identical
	 * copy.
	 */
	ScopeStack(Thread owner, ScopeStack other) {
		_size = other._activePointer;
		_activePointer = other._activePointer;
		_capacity = other._capacity;
		_memSize = other._memSize;
		_owner = owner;
		allocBackingStoreArray();

		for (int i = 0; i < _size; i++)
			VMSupport.storeInOpaqueArray(_stack, i, other._stack[i]);
	}

	/**
	 * Return the memory area at the given index in this scope stack, or null if
	 * the index is out of range.
	 */
	MemoryArea areaAt(int index, boolean active) {
		int limit = active ? _activePointer : _size;
		if (index >= 0 && index < limit)
			return MemoryArea.getMemoryAreaObject(_stack[index]);
		else
			return null;
	}

	void dump() {
		Utils.debugPrintln(this + " dump...");
		Utils.debugPrintln("\t - size: " + _size);
		Utils.debugPrintln("\t - activePtr: " + _activePointer);
		Utils.debugPrintln("\t - capacity: " + _capacity);
		Utils.debugPrintln("\t - scopes:");
		for (int i = _size - 1; i >= 0; i--) {
			Utils.debugPrintln("\t\t " + MemoryArea.getMemoryAreaObject(_stack[i]));
		}
	}

	/**
	 * Return the 'current' memory area in this scope stack. Always the top of
	 * the active part of the stack.
	 */
	MemoryArea getCurrentArea() {
		return MemoryArea.getMemoryAreaObject(_stack[_activePointer - 1]);
	}

	/** Return the size of this scope stack */
	int getDepth(boolean active) {
		return active ? _activePointer : _size;
	}

	/**
	 * Returns the index of the given scoped memory area in the current scope
	 * stack, or -1 if the area is not in the current scope stack
	 * 
	 * @param area
	 *            the scoped memory area to look for
	 */
	int getIndex(MemoryArea area, boolean active) {
		int limit = active ? _activePointer : _size;
		for (int i = limit - 1; i >= 0; i--)
			if (_stack[i] == area.area)
				return i;
		return -1;
	}

	/**
	 * Get the top most ScopedMemory. Since there are only two kinds of memory
	 * area in SCJ, we can be sure a memory area is ScopedMemmory as long as it
	 * is not ImmortalMemory. Return null if no ScopedMemory found.
	 */
	ScopedMemory getTopScopedMemory(boolean active) {
		int limit = active ? _activePointer : _size;
		for (int i = limit - 1; i >= 0; i--)
			if (_stack[i] != ImmortalMemory.instance().area)
				return (ScopedMemory) MemoryArea.getMemoryAreaObject(_stack[i]);
		return null;
	}

	/** Allow to pop only when the whole stack is active */
	Opaque pop() {
		if (_size == 0)
			throw new Error("attempt to pop empty stack");
		if (_size != _activePointer)
			throw new Error("attempt to pop a scope when it is inactive");

		Opaque top = _stack[--_size];
		_stack[_size] = null;
		_activePointer--;
		return top;
	}

	/** Allow to push only when the whole stack is active */
	void push(MemoryArea area) {
		// Utils.debugPrint("[SCJ] ScopeStack.push, area: " + area);

		// dump();
		// Utils.debugPrint("[SCJ] \t\t dump OK " );

		if (_size != _activePointer)
			throw new Error(
					"attempt to push a scope when the current top is inactive");
		if (_size == _capacity)
			grow();
		LibraryImports.storeInOpaqueArray(_stack, _size++, area.area);
		_activePointer++;

		// Utils.debugPrint("[SCJ] \t\t dump-test after push:" );
		// dump();
		// Utils.debugPrint("[SCJ] \t\t dump-test after push: Ok" );
	}

	/**
	 * used in moving the *current* allocation scope along side the stack up and
	 * down
	 */
	void setActivePointer(int pointer) {
		if (pointer < 0 || pointer > _size)
			throw new Error("Invalid active pointer: " + pointer);
		_activePointer = pointer;
	}

	/**
	 * TODO: Release the Opaque array. Need to implement when we are addressing
	 * the owner issue. Don't function now.
	 */
	void free() {
		// TODO: don't support reclamation so far
		// VMSupport.popScopeInStackSpace(_owner);
	}

	/**
	 * Returns the amount of memory required during construction of a ScopeStack
	 * with the given capacity. Not all constructors have the same allocation
	 * pattern however, and some have their memory usage determined by the
	 * constructor argument - e.g. for copy constructors. See the constructor
	 * docs for details.
	 */
	private static long constructionSizeOf(int capacity) {
		return VMSupport.sizeOfReferenceArray(capacity);
	}

	private void allocBackingStoreArray() {
		// TODO: currently, we create the stack in the current memory space

		// Opaque oldScope = VMSupport.getCurrentScope();
		// Opaque newScope = VMSupport.pushScopeInStackSpace(_owner,
		// _memSize);
		// VMSupport.setCurrentScope(newScope);
		// now in owner's stack space
		try {
			_stack = new Opaque[_capacity];
			// TODO: due to memory allocation, we may get OOME here?
		} finally {
			// VMSupport.setCurrentScope(oldScope);
		}
	}

	/** Grows the scope stack by CAPACITY. */
	private void grow() {
		throw new UnsupportedOperationException(
				"Scope stack grow unimplemented");
		// TODO: implement this
	}
}
