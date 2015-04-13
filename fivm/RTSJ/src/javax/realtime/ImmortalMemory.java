package javax.realtime;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

public class ImmortalMemory extends MemoryArea {

    private static ImmortalMemory singleton;
    private static Pointer[] typeDisplay;
    

    static{
	singleton = new ImmortalMemory();
	MemoryAreas.setJavaArea(singleton.area, singleton);
	typeDisplay = new Pointer[1];
	typeDisplay[0] = singleton.area;
    }
	   
    private ImmortalMemory(){
	super();
	area = MemoryAreas.getImmortalArea();
	size = MemoryAreas.size(area);
    }

    public static ImmortalMemory instance() {
	return singleton;
    }

    public void executeInArea(Runnable logic){
	try{
	    RealtimeThread current = RealtimeThread.currentRealtimeThread();
	    Pointer[] temporaryScopeStack = current.scopeStack;
	    current.scopeStack = typeDisplay;
	    MemoryAreas.enter(singleton.area, logic);
	    current.scopeStack = temporaryScopeStack;
	}
	catch(ClassCastException e){
	    MemoryAreas.enter(singleton.area, logic);
	}
    }

 
}
