package com.fiji.rt;

public class RTOSConfiguration extends Configuration {
    private int interruptStackSize=32768;
    private int threadStackSize=65536;
    private long nanosPerTick=10*1000*1000;
    private int ticksPerTimeslice=1;
    private int maxOSThreads=10;
    private int maxFileDescriptors=64;
    
    @SuppressWarnings({"unused"})
    private RTOSFilesystem filesystem=null;
    @SuppressWarnings({"unused"})
    private RTOSNetworking networking=null;
    
    RTOSConfiguration(int pageSize,
		      boolean canSetPageSize) {
	super(pageSize,canSetPageSize);
        setMaxThreads(10);
        setMemoryManager(new MemoryManager.ConcurrentMarkRegion(1024*1024,1024*1024/2));
        setStackAllocSize(8*1024);
    }
    
    public int getInterruptStackSize() { return interruptStackSize; }
    public void setInterruptStackSize(int interruptStackSize) {
	if (interruptStackSize<getPageSize() || interruptStackSize>1024*1024) {
	    throw new IllegalArgumentException(
		"Invalid value for interruptStackSize: "+interruptStackSize);
	}
	this.interruptStackSize=interruptStackSize;
    }
    
    public int getThreadStackSize() { return threadStackSize; }
    public void setThreadStackSize(int threadStackSize) {
	if (threadStackSize<getPageSize() || threadStackSize>32*1024*1024) {
	    throw new IllegalArgumentException(
		"Invalid value for threadStackSize: "+threadStackSize);
	}
	this.threadStackSize=threadStackSize;
    }
    
    public long getNanosPerTick() { return nanosPerTick; }
    public void setNanosPerTick(long nanosPerTick) {
	if (nanosPerTick<1000 || nanosPerTick>10*1000*1000*1000) {
	    throw new IllegalArgumentException(
		"Invalid value for nanosPerTick: "+nanosPerTick);
	}
	this.nanosPerTick=nanosPerTick;
    }
    
    public int getTicksPerTimeslice() { return ticksPerTimeslice; }
    public void setTicksPerTimeslice(int ticksPerTimeslice) {
        if (ticksPerTimeslice<1 || ticksPerTimeslice>10000) {
            throw new IllegalArgumentException(
                "Invalid value for ticksPerTimeslice: "+ticksPerTimeslice);
        }
        this.ticksPerTimeslice=ticksPerTimeslice;
    }
    
    public int getMaxOSThreads() { return maxOSThreads; }
    public void setMaxOSThreads(int maxOSThreads) {
        if (maxOSThreads<1 || maxOSThreads>10000) {
            throw new IllegalArgumentException(
                "Invalid value for maxOSThreads: "+maxOSThreads);
        }
        this.maxOSThreads=maxOSThreads;
    }
    
    public int getMaxFileDescriptors() { return maxFileDescriptors; }
    public void setMaxFileDescriptors(int maxFileDescriptors) {
        if (maxFileDescriptors<3 || maxFileDescriptors>10000) {
            throw new IllegalArgumentException(
                "Invalid value for maxFileDescriptors: "+maxFileDescriptors);
        }
        this.maxFileDescriptors=maxFileDescriptors;
    }
}



