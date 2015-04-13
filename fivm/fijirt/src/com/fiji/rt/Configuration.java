package com.fiji.rt;

import com.fiji.fivm.IntUtil;

public class Configuration {
    private int maxThreads = 1024;

    private int stackAllocSize = 65536;

    private int pageSize;
    private boolean canSetPageSize;

    private boolean gcScopedMemory = false;

    private MemoryManager mm=
	new MemoryManager.ConcurrentMarkRegion(200*1024*1024,100*1024*1024);
    
    Configuration(int pageSize,
		  boolean canSetPageSize) {
	this.pageSize = pageSize;
	this.canSetPageSize = canSetPageSize;
    }
    
    public int getMaxThreads() { return maxThreads; }
    public void setMaxThreads(int maxThreads) {
	if (maxThreads<1 || maxThreads>65536) {
	    throw new IllegalArgumentException(
		"Illegal value for maxThreads: "+maxThreads);
	}
	this.maxThreads=maxThreads;
    }
    
    public int getStackAllocSize() { return stackAllocSize; }
    public void setStackAllocSize(int stackAllocSize) {
	if (stackAllocSize<pageSize || stackAllocSize>32*1024*1024) {
	    throw new IllegalArgumentException(
		"Illegal value for stackAllocSize: "+stackAllocSize);
	}
	this.stackAllocSize = stackAllocSize;
    }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) {
	if (!canSetPageSize) {
	    throw new UnsupportedOperationException(
		"Cannot set page size on this platform.");
	}
	if (pageSize<4 || IntUtil.countOneBits(pageSize)!=1) {
	    throw new IllegalArgumentException(
		"Bad value for page size: "+pageSize);
	}
	this.pageSize=pageSize;
    }
    
    public MemoryManager getMemoryManager() { return mm; }
    public void setMemoryManager(MemoryManager mm) {
	if (mm==null) throw new NullPointerException();
	this.mm=mm;
    }

    public boolean getGCScopedMemory() { return gcScopedMemory; }
    public void setGCScopedMemory(boolean gcScopedMemory) {
	this.gcScopedMemory = gcScopedMemory;
    }
}


