package javax.realtime;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

public abstract class ScopedMemory extends MemoryArea {

    private Object portal;
    private Pointer area;
    private Runnable logic;
    private long size;

    protected Object lock;
    protected int refCount;
    protected Pointer[] typeDisplay;
    protected int bucket;

    public ScopedMemory(long size){
	if(size < 0)
	    throw new IllegalArgumentException();
	this.area = MemoryAreas.alloc(size, true, null);
	MemoryAreas.setJavaArea(this.area, this);
	this.size = size;
	this.refCount = 0;
	this.lock = new Object();
    }

    public ScopedMemory(long size, Runnable logic){
	if(size < 0)
	    throw new IllegalArgumentException();
	this.area = MemoryAreas.alloc(size, true, null);
	MemoryAreas.setJavaArea(this.area, this);
	this.size = size;
	this.logic = logic;
	this.refCount = 0;
	this.lock = new Object();
    }

    private void enter2(Runnable logic){
	RealtimeThread current;
	try{
	    current = RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	if(logic == null)
	    throw new IllegalArgumentException();
	synchronized(lock){
	    if(refCount == 0){
		if(current.scopeStack != null){
		    this.bucket = current.scopeStack.length;
		    this.typeDisplay = new Pointer[this.bucket];
		    for(int i = 0; i < current.scopeStack.length; i++)
			this.typeDisplay[i] = current.scopeStack[i];
		    this.typeDisplay[bucket] = area;
		}
	        else{
		    this.bucket = 0;
		    this.typeDisplay = new Pointer[1];
		    this.typeDisplay[0] = area;
		}
		MemoryAreas.push(area);
	    }
	    else{
		try{
		    if(typeDisplay[bucket-1] != current.scopeStack[bucket-1])
			throw new ScopedCycleException();
		}
		catch(Exception e){
		    throw new ScopedCycleException();
		}
	    }
	    refCount++;
	}
	MemoryAreas.enter(area, logic);
	synchronized(lock){
	    if(refCount == 1){
		MemoryAreas.pop(area);
		this.typeDisplay = null;
		this.bucket = -1;
		this.portal = null;
	    }
	    refCount--;
	}
    }

    public void enter(){
	enter2(this.logic);
    }

    public void enter(Runnable logic){
	enter2(logic);
    }

    public void executeInArea(Runnable logic){
	RealtimeThread current;
	Pointer[] temporaryScopeStack;
	try{
	    current = RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	try{
	    if(current.scopeStack[bucket] != area)
		throw new InaccessibleAreaException();
	}
	catch(Exception e){
	    throw new InaccessibleAreaException();
	}
	if(logic == null)
	    throw new IllegalArgumentException();
	temporaryScopeStack = current.scopeStack;
	current.scopeStack = typeDisplay;
	MemoryAreas.enter(area, logic);
	current.scopeStack = temporaryScopeStack;
    }

    public long getMaximumSize(){
	return super.size();
    }

    public Object getPortal(){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	return portal;
    }

    public int getReferenceCount(){
	return refCount;
    }

    public void join(){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
    }

    public void join(HighResolutionTime time){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
    }

    public void joinAndEnter(){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
    }

    public void joinAndEnter(HighResolutionTime time){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
    }

    public void joinAndEnter(Runnable logic){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
    }

    public void joinAndEnter(Runnable logic,
			     HighResolutionTime time){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
    }

    public Object newArray(Class<Object> type, int number){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	return super.newArray(type, number);
    }

    public Object newInstance(Class<Object> type){
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	return super.newInstance(type);
    }

    public void setPortal(Object newPortal){
	//fixme check if this is in callers scope stack
	if(newPortal == null)
	    return;
	try{
	    RealtimeThread.currentRealtimeThread();
	}
	catch(ClassCastException e){
	    throw new IllegalThreadStateException();
	}
	if(MemoryArea.getMemoryArea(newPortal) == this){
	    this.portal = newPortal;
	    return;
	}
	throw new IllegalAssignmentError();
    }

}
