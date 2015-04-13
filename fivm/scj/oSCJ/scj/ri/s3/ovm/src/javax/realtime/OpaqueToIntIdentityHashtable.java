package javax.realtime;
import org.ovmj.java.Opaque;


/**
 * Fixed capacity hashtable mapping Opaque references to int values.
 * No allocation is performed after construction, except asking for 
 * an iterator.
 * <p>If a key is not found the sentinel value <tt>Integer.MIN_VALUE</tt> 
 * is returned.
 *
 * @author David Holmes (September 2005)
 * @author Adapted from Doug Lea's IdentityHashMap
 */
class OpaqueToIntIdentityHashtable {

    // the implementation is a linear-probe table. Entries are stored in the
    // first empty slot where the starting index of given by the hash(k) 
    // function. Removals cause a rehash of the table to ensure null entries
    // appear where they should.

    /** sentinel value for a non-existent entry */
    static final int NOTFOUND = Integer.MIN_VALUE; 

    /** default capacity */
    static final int DEFAULT_CAPACITY = 16;

    // we use a linear probe table

    /** the keys in the map */
    private final Opaque[] keys;

    /** the values in the map */
    private final int[] values;

    /** Current size of map */
    private int size; 

    /** the maximum valid index into this map. This is capacity-1 */
    private final int maxIndex;

    /** The raw size of an instance as allocated by new */
    static final long baseSize = 
        LibraryImports.sizeOf(OpaqueToIntIdentityHashtable.class);

    /** Size needed for the iterator used by one of the copy constructors */
    static final long iteratorSize =
        LibraryImports.sizeOf(Iterator.class);

    /** Return how much memory is consumed by constructing an instance of this
        class with the given capacity. This count does not allow for creation
        of any monitors for any of the objects created during construction.
    */
    static long sizeOf(int capacity) {
        capacity = minCapacity(capacity);
        long size = baseSize;
        // key array
        size += LibraryImports.sizeOfReferenceArray(capacity);
        // values array
        size += LibraryImports.sizeOfPrimitiveArray(capacity, int.class);
        return size;
    }

    /** Construct a hashtable with the default capacity */
    OpaqueToIntIdentityHashtable() {
        this(DEFAULT_CAPACITY);
    }


    /** Construct a hashtable with the given capacity (rounded up to a multiple
        of two)
     */
    public OpaqueToIntIdentityHashtable(int capacity) {    
        int cap = minCapacity(capacity);
        this.maxIndex =  cap - 1;
        this.keys = new Opaque[cap];
        this.values = new int[cap];

        /* --- DEBUG CODE  --- */
        if (false) {
            //Opaque current 
            //    = LibraryImports.setCurrentArea(HeapMemory.instance.are);
            try {
                System.out.println("Created hashtable of capacity " + cap);
            }
            finally {
           //     LibraryImports.setCurrentArea(current);
            }
        }
        /* -- end DEBUG --- */

    }

    /**
     * Create a hashtable with the same contents, size and capacity as the
     * given hashtable.
     */
    public OpaqueToIntIdentityHashtable(OpaqueToIntIdentityHashtable other) {
        this.size = other.size;
        this.maxIndex = other.maxIndex;
        this.keys = (Opaque[]) other.keys.clone();
        this.values = (int[]) other.values.clone();
        /* --- DEBUG CODE  --- */
        if (false) {
            //Opaque current 
            //    = LibraryImports.setCurrentArea(HeapMemory.instance._);
            try {
                System.out.println("Created copied hashtable of capacity " + 
                                   (maxIndex+1));
            }
            finally {
             //   LibraryImports.setCurrentArea(current);
            }
        }
        /* -- end DEBUG --- */

    }

    /**
     * Create a hashtable of the given capacity (rounded up to a power of 
     * two) that initially contains all the entries from the given other
     * hashtable
     */
    public OpaqueToIntIdentityHashtable(int capacity,
                                         OpaqueToIntIdentityHashtable other){
        this(capacity);

        // now copy over all the entries in other. We can't just copy the
        // array contents because we need to re-hash. Allocating an iterator
        // wastes memory but as long as we account for it in our sizing that
        // is okay.
        Iterator iter = other.getIterator();
        while(iter.hasNext()) {
            Opaque key = iter.nextKey();
            this.put(key, other.get(key));
            if (Assert.ENABLED)
                Assert.check(this.get(key) != NOTFOUND ? Assert.OK :
                             "copying other yielded invalid entry");
        }
    }

    /**
     * Actual capacity must always be a power of two
     */
    static int minCapacity(int capacity) {
        int cap = 2;
        while (cap < capacity)
            cap <<= 1;
        return cap;
    }

    /** hash based on identity hashcode with suitable spreader */
    int hash(Opaque key) {
        int hash = System.identityHashCode(key);
        hash += ~(hash << 9);
        hash ^=  (hash >>> 14);
        hash +=  (hash << 4);
        hash ^=  (hash >>> 10);
        return hash & maxIndex;
    }

    /** Get next index with wrapping */
    int nextIndex(int i) {
        return (i == maxIndex ? 0 : i+1);
    }

    /**
     * gets something from hashtable or returns NOTFOUND.
     * Does _not_ insert anything into the hashtable.
     **/
    public final int get(Opaque key) {
        if (key == null)
            throw new IllegalArgumentException("null key not permitted");
        Opaque[] table = keys;  // store in local to avoid read barriers
        Opaque k;
        int i = hash(key);
        while ( (k = table[i]) != null) {
            if (k == key)
                return values[i];
            i = nextIndex(i);
        }
        return NOTFOUND;
    }


    /** keeps track of collisions for profiling purposes */
    int collisions = 0;  // hash(a) == hash(b)
    int displacements = 0; // someone's in my slot!

    static final boolean TRACK_COLLISIONS = false;

    /**
     * Put something into the hashtable. Checks to see if
     * the key is already in the hashtable, and if so, updates
     * the value associated with the key.
     **/
    public final void put(Opaque key, int value) {
        if (key == null)
            throw new IllegalArgumentException("null key not permitted");
        Opaque[] table = keys;  // store in local to avoid read barriers
        Opaque k;
        int i = hash(key);

        /* --- DEBUG CODE for examing hash spread --- */
        if (false) {
           // Opaque current 
            //    = LibraryImports.setCurrentArea(HeapMemory.instance._scopeID);
            try {
                System.out.println("HASHED on put(): " + i + " (id-hash = " + 
                                   Integer.
                                   toHexString(System.identityHashCode(key)) + 
                                   ")");
            }
            finally {
              //  LibraryImports.setCurrentArea(current);
            }
        }
        /* -- end DEBUG --- */

        if (TRACK_COLLISIONS) {
            if (table[i] != null && table[i] != key) {
                if (hash(key) == hash(table[i]))
                    collisions++;
                else
                    displacements++;
            }
        }

        while ( (k = table[i]) != null) {
            if (k == key) { // replace
                values[i] = value;
                return;
            }
            i = nextIndex(i);
        }
        // insert in empty slot
        if (size == maxIndex)
            throw new Error("Capacity exceeded!");
        LibraryImports.storeInOpaqueArray(table, i, key);
        values[i] = value;
        size++;
    }


    public int getCollisions() {
        return collisions;
    }

    public void printCollisionInfo() {
        //Opaque current 
        //    = LibraryImports.setCurrentArea(HeapMemory.instance._scopeID);
        try {
            System.out.println("Collision rate: " + collisions + "/" + size +
                               " (" + 
                               ((float)((int)(1000*((float)collisions)/size)))/10 +
                               "%)");
            System.out.println("Displacement rate: " + displacements + "/" + size +
                               " (" + 
                               ((float)((int)(1000*((float)displacements)/size)))/10 +
                               "%)");

        }
        finally {
          //  LibraryImports.setCurrentArea(current);
        }

    }

    /** remove the entry for the given key, returning the value it
        mapped to, or NOTFOUND if the key wasn't present.
    */
    public int remove(Opaque key) {
        if (key == null)
            throw new IllegalArgumentException("null key not permitted");
        Opaque[] table = keys;  // store in local to avoid read barriers
        Opaque k;
        int i = hash(key);
        while ( (k = table[i]) != null) {
            if (k == key) {
                int ret = values[i];
                size--;
                table[i] = null;
                closeDeletion(i);
                return ret;
            }
            i = nextIndex(i);
        }
        return NOTFOUND;
    }


    /**
     * Rehash all possibly-colliding entries following a
     * deletion. This preserves the linear-probe
     * collision properties required by get, put, etc.
     *
     * @param d the index of a newly empty deleted slot
     */
    private void closeDeletion(int d) {
        // Adapted from Knuth Section 6.4 Algorithm R
        Opaque[] table = keys; // store in locals to avoid read barriers
        int[] vals = values;

        // Look for items to swap into newly vacated slot
        // starting at index immediately following deletion,
        // and continuing until a null slot is seen, indicating
        // the end of a run of possibly-colliding keys.
        Opaque k;
        for (int i = nextIndex(d); (k = table[i]) != null;
             i = nextIndex(i) ) {
            // The following test triggers if the item at slot i (which
            // hashes to be at slot r) should take the spot vacated by d.
            // If so, we swap it in, and then continue with d now at the
            // newly vacated i.  This process will terminate when we hit
            // the null slot at the end of this run.
            // The test is messy because we are using a circular table.
            int r = hash(k);
            if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
                LibraryImports.storeInOpaqueArray(table, d, k);
                vals[d] = vals[i];
                table[i] = null;
                d = i;
            }
        }
    }



    
    /**
     * Return the size (number of elements currently present) of the hashtable.
     **/
    public int size() {
        return size;
    }

    /**
     * Return the capacity of this map
     */
    int capacity() {
        return maxIndex+1;
    }

    /** debugging method to print the contents - beware this requires a lot
        of memory
    */
    void dump() {
        System.out.println(this);
        if (size == 0) {
            System.out.println("\t<empty>");
            return;
        }
        Iterator iter = getIterator();
        for (int i = 0; iter.hasNext(); i++) {
            Opaque key = iter.nextKey();
            int index = get(key);
            System.out.print("\tentry[");
            System.out.print(i);
            System.out.print("] = @");
            System.out.print(Integer.toHexString(LibraryImports.
                                                 toAddress(key)));
            System.out.print(", ");
            System.out.println(index);
        }
    }

    // the (strange looking) iterator is only used for debugging

    public Iterator getIterator() {
        return new Iterator();
    }

    public final class Iterator {
        int index = (size != 0 ? 0 : keys.length); // current slot.
        boolean indexValid; // To avoid unnecessary next computation

        public boolean hasNext() {
            Opaque[] table = keys; // local to avoid read barriers
            for (int i = index; i < table.length ; i++) {
                Opaque key = table[i];
                if (key != null) {
                    index = i;
                    return indexValid = true;
                }
            }
            index = table.length;
            return false;
        }

        protected int nextIndex() {
            if (!indexValid && !hasNext())
                throw new java.util.NoSuchElementException();

            indexValid = false;
            int lastReturnedIndex = index;
            index++;
            return lastReturnedIndex;
        }

        public Opaque nextKey() {
            return keys[nextIndex()];
        }

        public int nextValue() {
            return values[nextIndex()];
        }
    } // End Iterator

}
