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

import java.lang.reflect.Array;
import java.util.Set;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;

/**
 * 
 * 
 * 
 * TODO: revise the implementation, make sure the set grows its size predictively.
 * 
 * TODO: No java.util in SCJ. Change the implementation to an internal data structure.
 * 
 * .
 * 
 * An array-backed implementation of the Set interface, that is based on object
 * identity not equivalence. Additionally for the javax.realtime package this
 * allows for resizing in the correct memory area, allows for allocation free
 * iteration and generally reduces unnecessary allocation. This implementation
 * does not permit null elements.
 * <p>
 * Iteration is allowed by exposing the internal array within our package.
 * 
 * This was based on the GNU Classpath ArrayList implementation.
 * 
 * @author David Holmes
 *
 */
class IdentityArraySet extends AbstractSet implements Set, Cloneable {

    /**
     * The default capacity for new ArraySets.
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The number of elements in this set. Package access for direct iteration
     */
    int size;

    /**
     * Where the data is stored. Package access for direct iteration
     */
    Object[] data;

    /** mod-count for fail-fast proper iterator */
    private int modCount = 0;

    /** our memory area - for resizing (updated by clone() ) */
    private MemoryArea thisArea = MemoryArea.getMemoryArea(this);

    /**
     * Construct a new ArraySet with the supplied initial capacity.
     * 
     * @param capacity
     *            initial capacity of this ArraySet
     * @throws IllegalArgumentException
     *             if capacity is negative
     */
    public IdentityArraySet(int capacity) {
        if (capacity < 0)
            throw new IllegalArgumentException("negative capacity");
        data = new Object[capacity];
    }

    /**
     * Construct a new ArraySet with the default capcity (16).
     */
    public IdentityArraySet() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Construct a new ArraySet, and initialize it with the elements in the
     * supplied Collection. The initial capacity is 110% of the Collection's
     * size.
     * 
     * @param c
     *            the collection whose elements will initialize this set
     * @throws NullPointerException
     *             if c is null
     */
    public IdentityArraySet(Collection c) {
        this((int) (c.size() * 1.1f));
        addAll(c);
    }

    /**
     * Guarantees that this set will have at least enough capacity to hold
     * minCapacity elements. This implementation will grow the set to
     * max(current * 2, minCapacity) if (minCapacity &gt; current).
     * 
     * @param minCapacity
     *            the minimum guaranteed capacity
     */
    public void ensureCapacity(int minCapacity) {
        int current = data.length;

        if (minCapacity > current) {
            try {
                Object[] newData = (Object[]) thisArea.newArray(Object.class,
                        Math.max(current * 2, minCapacity));
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            } catch (IllegalAccessException iae) {
                throw new InternalError(iae.toString());
            }
        }
    }

    /**
     * Returns the number of elements in this set.
     * 
     * @return the set size
     */
    public int size() {
        return size;
    }

    /**
     * Checks if the set is empty.
     * 
     * @return true if there are no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns true iff element is in this ArraySet.
     * 
     * @param e
     *            the element whose inclusion in the Set is being tested
     * @return true if the set contains e
     */
    public boolean contains(Object e) {
        for (int i = 0; i < size; i++)
            if (e == data[i])
                return true;
        return false;
    }

    /**
     * Creates a shallow copy of this ArraySet (elements are not cloned).
     * 
     * @return the cloned object
     */
    public Object clone() {
        IdentityArraySet clone = null;
        try {
            clone = (IdentityArraySet) super.clone();
            clone.data = (Object[]) data.clone();
            clone.thisArea = MemoryArea.getMemoryArea(clone);
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString()); // Impossible to get here.
        }
        return clone;
    }

    /**
     * Returns an Object array containing all of the elements in this ArraySet.
     * The array is independent of this set.
     * 
     * @return an array representation of this set
     */
    public Object[] toArray() {
        Object[] array = new Object[size];
        System.arraycopy(data, 0, array, 0, size);
        return array;
    }

    /**
     * Returns an Array whose component type is the runtime component type of
     * the passed-in Array. The returned Array is populated with all of the
     * elements in this ArraySet. If the passed-in Array is not large enough to
     * store all of the elements in this Set, a new Array will be created and
     * returned; if the passed-in Array is <i>larger</i> than the size of this
     * Set, then size() index will be set to null.
     * 
     * @param a
     *            the passed-in Array
     * @return an array representation of this set
     * @throws ArrayStoreException
     *             if the runtime type of a does not allow an element in this
     *             list
     * @throws NullPointerException
     *             if a is null
     */
    public Object[] toArray(Object[] a) {
        if (a.length < size)
            a = (Object[]) Array.newInstance(a.getClass().getComponentType(),
                    size);
        else if (a.length > size)
            a[size] = null;
        System.arraycopy(data, 0, a, 0, size);
        return a;
    }

    /**
     * Appends the supplied element to the end of this list.
     * 
     * @param e
     *            the element to be appended to this list
     * @return true, the add will always succeed
     */
    public boolean add(Object e) {
        if (e == null)
            throw new NullPointerException("null elements not allowed");
        if (contains(e))
            return false;
        modCount++;
        if (size == data.length)
            ensureCapacity(size + 1);
        data[size++] = e;
        return true;
    }

    // more efficient implementation for array

    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < size; i++) {
            hash += data[i].hashCode();
        }
        return hash;
    }

    public boolean remove(Object o) {
        for (int i = 0; i < size; i++) {
            if (data[i] == o) {
                modCount++;
                // swap the removed element with the end element
                data[i] = data[size - 1];
                data[--size] = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all elements from this Set
     */
    public void clear() {
        if (size > 0) {
            modCount++;
            // Allow for garbage collection.
            Arrays.fill(data, 0, size, null);
            size = 0;
        }
    }

    public Iterator iterator() {
        return new Iterator() {
            private int pos = 0;
            private int size = size();
            private int knownMod = modCount;

            private void checkMod() {
                if (knownMod != modCount)
                    throw new ConcurrentModificationException();
            }

            public boolean hasNext() {
                checkMod();
                return pos < size;
            }

            public Object next() {
                checkMod();
                if (pos == size)
                    throw new NoSuchElementException();
                return data[pos++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
