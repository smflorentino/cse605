package javax.realtime;

public class MemoryInUseException extends RuntimeException{

    public MemoryInUseException(){}

    public MemoryInUseException(String description){
	super(description);
    } 

};