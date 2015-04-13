package javax.realtime;

public abstract class MonitorControl{

    protected MonitorControl(){}
    
    public static MonitorControl getMonitorControl(){
	return PriorityInheritance.instance();
    }

    public static MonitorControl getMonitorControl(Object o){
	if(o == null)
	    throw new IllegalArgumentException();
	return PriorityInheritance.instance();
    }

    public static MonitorControl setMonitorControl(MonitorControl policy){
	if(policy == null)
	    return PriorityInheritance.instance();
	if(!(policy instanceof PriorityInheritance))
	    throw new UnsupportedOperationException();
	return PriorityInheritance.instance();
    }

    public static MonitorControl setMonitorControl(Object o, 
						   MonitorControl policy){
	if(policy == null)
	    return PriorityInheritance.instance();
	if(!(policy instanceof PriorityInheritance))
	    throw new UnsupportedOperationException();
	if(o == null)
	    throw new IllegalArgumentException();
	return PriorityInheritance.instance();
    }

}