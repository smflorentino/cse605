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

import edu.purdue.scj.BackingStoreID;
import edu.purdue.scj.VMSupport;
//import edu.purdue.scj.utils.Utils;

/**
 * ScopeStack is allocated in each thread's ScopeStack space. Each thread can
 * have several ScopeStack due to executeInArea(), but only one of them is
 * active. Since the ScopeStack space is also a stack (each element of it would
 * be a ScopeStack), the top is always the active one.
 * 
 * Stack copying occurs at:
 * 
 * 1) Current thread invokes executeInArea(). 2)
 * 
 * > ScopeStacks owned by a thread have shorter life time than the thread.
 * 
 * > The array referenced by ScopeStack._stack should be allocated in the stack
 * space of its owner thread.
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
	BackingStoreID[] _stack;

	/** Create an empty scope stack with the default capacity. */
	ScopeStack(Thread owner) {
		_size = 0;
		_activePointer = 0;
		_capacity = _INCREASE;
		_memSize = constructionSizeOf(_INCREASE);
		// _owner = owner;
		allocBackingStoreArray();
		
		//System.out.println("Scope stack created!");
		// pushing the ImmortalMemory on stack
		MemoryArea immortalMem = ImmortalMemory.instance();
		push(immortalMem);
		//System.out.println("Immortal Pushed!");
		
		//System.out.println("[SCJ-DBG] ScopeStack: Scope stack created, immortal memory pushed!");
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
		// _owner = owner;
		allocBackingStoreArray();

		for (int i = 0; i < _size; i++)
			setAt(other.getAt(i), i);
	}

	/**
	 * Return the memory area at the given index in this scope stack, or null if
	 * the index is out of range.
	 */
	MemoryArea areaAt(int index, boolean active) {
		int limit = active ? _activePointer : _size;
		if (index >= 0 && index < limit)
			return MemoryArea.getMemoryAreaObject(getAt(index));
		else
			return null;
	}

	/**
	 * Return the 'current' memory area in this scope stack. Always the top of
	 * the active part of the stack.
	 */
	MemoryArea getCurrentArea() {
		return MemoryArea.getMemoryAreaObject(getAt(_activePointer - 1));
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
			if (getAt(i) == area.get_scopeID())
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
			if (getAt(i) != ImmortalMemory.instance().get_scopeID())
				return (ScopedMemory) MemoryArea.getMemoryAreaObject(getAt(i));
		return null;
	}

	/** Allow to pop only when the whole stack is active */
	void pop() {
		if (_size == 0)
			throw new Error("attempt to pop empty stack");
		if (_size != _activePointer)
			throw new Error("attempt to pop a scope when it is inactive");
		setAt(null, _size--);
		_activePointer--;
	}

	/** Allow to push only when the whole stack is active */
	void push(MemoryArea area) {
		if (_size == _capacity)
			grow();
		if (_size != _activePointer) {
			////Utils.panic("attempt to push a scope when the current top is inactive");
		}
		try {
			setAt(area.get_scopeID(), _size++);
		} catch (Throwable t) {
			////Utils.debugPrint("Get exception .....................................");
			////Utils.debugPrint(t.toString());
		}
		_activePointer++;
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

	void dump() {
		//Utils.debugPrint(this + " dump...");
		//Utils.debugPrint("\t - size: " + _size);
		//Utils.debugPrint("\t - activePointer: " + _activePointer);
		//Utils.debugPrint("\t - scopes");
		for (int i = _size - 1; i >= 0; i--) {
			//Utils.debugPrint("\t\t " + VMSupport.getNote(getAt(i)));
		}
	}

	/**
	 * TODO: Release the BackingStoreID array. Need to implement when we are
	 * addressing the owner issue. Don't function now.
	 */
	void free() {
		// TODO: don't support reclamation so far
		// VMSupport.popScopeInStackSpace(_owner);
	}

	@com.fiji.fivm.r1.NoScopeChecks
	BackingStoreID[] get_stack() {
		return _stack;
	}

	@com.fiji.fivm.r1.NoScopeChecks
	void set_stack(BackingStoreID[] stack) {
		_stack = stack;
	}

	@com.fiji.fivm.r1.NoScopeChecks
	void setAt(BackingStoreID id, int index) {
		_stack[index] = id;
	}

	@com.fiji.fivm.r1.NoScopeChecks
	BackingStoreID getAt(int index) {
		return _stack[index];
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

		// BackingStoreID oldScope = VMSupport.getCurrentScope();
		// BackingStoreID newScope = VMSupport.pushScopeInStackSpace(_owner,
		// _memSize);
		// VMSupport.setCurrentScope(newScope);
		// now in owner's stack space
		try {
			set_stack(new BackingStoreID[_capacity]);
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
