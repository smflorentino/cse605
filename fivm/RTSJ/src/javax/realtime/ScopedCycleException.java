package javax.realtime;

public class ScopedCycleException extends RuntimeException{

    public ScopedCycleException(){}

    public ScopedCycleException(String description){
	super(description);
    } 

};