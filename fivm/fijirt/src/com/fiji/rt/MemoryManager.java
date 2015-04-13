package com.fiji.rt;

public abstract class MemoryManager {
    public static class Immortal extends MemoryManager {
    }
    
    public static abstract class GarbageCollector extends MemoryManager {
	private long defMaxMem;
	private long defTrigger;
        private int threadPriority;
	
	public GarbageCollector(long defMaxMem,
				long defTrigger,
                                int threadPriority) {
	    this.defMaxMem=defMaxMem;
	    this.defTrigger=defTrigger;
            this.threadPriority=threadPriority;
	}
	
	public GarbageCollector(long defMaxMem,
				long defTrigger) {
            this(defMaxMem,defTrigger,5);
	}
	
	public long getDefaultMaxMemory() {
	    return defMaxMem;
	}
	
	public long getDefaultTrigger() {
	    return defTrigger;
	}
        
        public int getThreadPriority() {
            return threadPriority;
        }
        
        public void setDefaultMaxMemory(long defMaxMem) {
            this.defMaxMem=defMaxMem;
        }
        
        public void setDefaultTrigger(long defTrigger) {
            this.defTrigger=defTrigger;
        }
        
        public void setThreadPriority(int threadPriority) {
            this.threadPriority=threadPriority;
        }
    }

    public static class ConcurrentMarkRegion extends GarbageCollector {
	public ConcurrentMarkRegion(long defMaxMem,
				    long defTrigger,
                                    int threadPriority) {
	    super(defMaxMem,defTrigger,threadPriority);
	}
	public ConcurrentMarkRegion(long defMaxMem,
				    long defTrigger) {
	    super(defMaxMem,defTrigger);
	}
    }
    
    public static class HybridFragmenting extends GarbageCollector {
	public HybridFragmenting(long defMaxMem,
                                 long defTrigger,
                                 int threadPriority) {
	    super(defMaxMem,defTrigger,threadPriority);
	}
	public HybridFragmenting(long defMaxMem,
                                 long defTrigger) {
	    super(defMaxMem,defTrigger);
	}
    }
    
    public boolean hasGarbageCollector() {
	return this instanceof GarbageCollector;
    }
    
    public static MemoryManager highestThroughputGarbageCollector(long defMaxMem,
								  long defTrigger,
                                                                  int threadPriority) {
	return new ConcurrentMarkRegion(defMaxMem,defTrigger,threadPriority);
    }
    
    public static MemoryManager highestThroughputGarbageCollector(long defMaxMem,
                                                                  long defTrigger) {
	return new ConcurrentMarkRegion(defMaxMem,defTrigger);
    }
    
    public static MemoryManager mostPredictableGarbageCollector(long defMaxMem,
								long defTrigger,
                                                                int threadPriority) {
	return new HybridFragmenting(defMaxMem,defTrigger,threadPriority);
    }
    
    public static MemoryManager mostPredictableGarbageCollector(long defMaxMem,
								long defTrigger) {
	return new HybridFragmenting(defMaxMem,defTrigger);
    }
}


