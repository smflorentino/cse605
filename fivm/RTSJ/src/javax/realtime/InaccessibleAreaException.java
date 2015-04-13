package javax.realtime;

public class InaccessibleAreaException extends RuntimeException{

    public InaccessibleAreaException(){}

    public InaccessibleAreaException(String description){
	super(description);
    }

};