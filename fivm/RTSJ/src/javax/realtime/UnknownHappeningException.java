package javax.realtime;

public class UnknownHappeningException extends RuntimeException{

    public UnknownHappeningException(){}

    public UnknownHappeningException(String description){
	super(description);
    } 

};