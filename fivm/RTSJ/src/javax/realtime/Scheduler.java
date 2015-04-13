package javax.realtime;


public abstract class Scheduler{

    protected static Scheduler defaultScheduler;

    protected Scheduler(){
    }
    
    protected abstract boolean addToFeasibility(Schedulable schedulable);
    
    public abstract void fireSchedulable(Schedulable schedulable);

    public static Scheduler getDefaultScheduler(){
	return defaultScheduler;
    }

    public abstract String getPolicyName();

    public abstract boolean isFeasible();

    protected abstract boolean removeFromFeasibility(Schedulable schedulable);

    public static void setDefaultScheduler(Scheduler scheduler){
	defaultScheduler = scheduler;
    }

    public abstract boolean setIfFeasible(Schedulable schedulable,
					  ReleaseParameters release,
					  MemoryParameters memory);

    public abstract boolean setIfFeasible(Schedulable schedulable,
					  ReleaseParameters release,
					  MemoryParameters memory,
					  ProcessingGroupParameters group);

    public abstract boolean setIfFeasible(Schedulable schedulable,
					  SchedulingParameters scheduling,
					  ReleaseParameters release,
					  MemoryParameters memory,
					  ProcessingGroupParameters group);

}