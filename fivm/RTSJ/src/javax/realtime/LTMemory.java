package javax.realtime;

public class LTMemory extends ScopedMemory {

   

    public LTMemory(long size){
	super(size);
    }

    public LTMemory(long initial, 
		    long maximum) {
	super(maximum);
    }

    public LTMemory(long initial, 
		    long maximum,
		    Runnable logic){
	super(maximum, logic);
    }
    
    public LTMemory(long size,
		    Runnable logic){
	super(size, logic);
    }

    

}
