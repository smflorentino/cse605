
package javax.realtime;

public class PriorityInheritance extends MonitorControl{

    private static PriorityInheritance singleton;

    private PriorityInheritance(){
	super();
    }

    public static PriorityInheritance instance(){
	if(singleton == null)
	    singleton = new PriorityInheritance();
	return singleton;
    }

}