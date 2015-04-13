package javax.realtime;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;

public class HeapMemory extends MemoryArea {

    private static HeapMemory singleton;
    private static Pointer area;
    private static Pointer[] typeDisplay;
    

    static{
	singleton = new HeapMemory();
	area = MemoryAreas.getHeapArea();
	MemoryAreas.setJavaArea(area, singleton);
	typeDisplay = new Pointer[1];
	typeDisplay[0] = area;
    }

    private HeapMemory(){
    }

    public static HeapMemory instance() {
	return singleton;
    }

    public void executeInArea(Runnable logic){
	try{
	    RealtimeThread current = RealtimeThread.currentRealtimeThread();
	    Pointer[] temporaryScopeStack = current.scopeStack;
	    current.scopeStack = typeDisplay;
	    MemoryAreas.enter(area, logic);
	    current.scopeStack = temporaryScopeStack;
	}
	catch(ClassCastException e){
	    MemoryAreas.enter(area, logic);
	}
    }
    
}
