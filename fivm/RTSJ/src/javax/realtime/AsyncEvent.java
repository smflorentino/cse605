package javax.realtime;

import java.util.TreeSet;
import java.util.Comparator;
import java.util.Iterator;

public class AsyncEvent {

    protected TreeSet<AsyncEventHandler> slots;
    public AsyncEvent() {
	slots=new TreeSet<AsyncEventHandler>((Comparator)new AEHComparator());
    }
    
    public synchronized void addHandler(AsyncEventHandler handler) {
	handler.attach();
        slots.add(handler);
    }

    public void bindTo(java.lang.String happening){
	if(happening == null)
	    throw new IllegalArgumentException();
	throw new UnknownHappeningException("Unsupported Happening: "+happening);
    }

    public ReleaseParameters createReleaseParameters(){
	return new ReleaseParameters();
    }

    public synchronized void fire() {
	Iterator<AsyncEventHandler> iter=slots.iterator();
	while (iter.hasNext()) {
	    AsyncEventHandler current = iter.next();
	    synchronized(current){		
		int fireCount = current.getAndIncrementPendingFireCount();
		if(fireCount == 0){
		    current.notify();
		}
	    }
        }
    }

    public boolean handleBy(AsyncEventHandler handler){
	return slots.contains(handler);
    }

    public synchronized void removeHandler(AsyncEventHandler handler){
	slots.remove(handler);
    }

    public synchronized void setHandler(AsyncEventHandler handler){
	slots.clear();
	handler.attach();
	slots.add(handler);
    }

    public void unbindTo(java.lang.String happening){
	if(happening == null)
	    throw new IllegalArgumentException();
	throw new UnknownHappeningException("Unsupported Happening: "+happening);
    }
}
