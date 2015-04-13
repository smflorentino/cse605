package javax.realtime;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

public abstract class MemoryArea {

    Pointer area;
    long size;
    int occupancy;
    private Runnable logic;
    private Pointer[] typeDisplay;

    protected MemoryArea(){
    }

    protected MemoryArea(long size){
	if(size < 0)
	    throw new IllegalArgumentException();
	this.area = MemoryAreas.alloc(size, true, null);
	MemoryAreas.setJavaArea(this.area, this);
	this.size = size;
	this.typeDisplay = new Pointer[1];
	this.typeDisplay[0] = area;
    }

    protected MemoryArea(long size, Runnable logic){
	if(size < 0)
	    throw new IllegalArgumentException();
	this.area = MemoryAreas.alloc(size, true, null);
	MemoryAreas.setJavaArea(this.area, this);
	this.size = size;
	this.size = size;
	this.typeDisplay = new Pointer[1];
	this.typeDisplay[0] = area;
    }
    
    private void enterImpl(Runnable logic) {
	synchronized (this) {
	    if (occupancy++ == 0) {
		MemoryAreas.push(area);
	    }
	}
	try {
	    MemoryAreas.enter(area, logic);
	} finally {
	    synchronized (this) {
		if (--occupancy == 0) {
		    MemoryAreas.pop(area);
		}
	    }
	}
    }

    public void enter(){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	if(this.logic == null)
	    throw new IllegalArgumentException();
	enterImpl(logic);
    }

    public void enter(Runnable logic) {
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	if(logic == null)
	    throw new IllegalArgumentException();
	enterImpl(logic);
    }

    public void executeInArea(Runnable logic){
	if(logic == null)
	    throw new IllegalArgumentException();
	MemoryAreas.enter(area, logic);	
    }

    public static MemoryArea getMemoryArea(Object object){
	return ((MemoryArea) MemoryAreas.getJavaArea(MemoryAreas.areaOf(object)));
    }

    public long memoryConsumed(){
	return MemoryAreas.consumed(area);
    }

    public long memoryRemaining(){
	return MemoryAreas.available(area);
    }
   
    public Object newArray(Class<Object> type, int number){
	return new Object();
    }

    public Object newInstance(Class<Object> type) {
	return new Object();
    }
    
    // not supported with FijiCore
    //  public Object newInstance(Constructor c, Object[] args){
    //	return new Object();
    //}

    public long size(){
	return size;
    }

}
