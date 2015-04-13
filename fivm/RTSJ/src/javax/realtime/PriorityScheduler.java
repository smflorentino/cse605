package javax.realtime;
import java.lang.Thread;
import com.fiji.fivm.ThreadPriority;

public class PriorityScheduler extends Scheduler{

    public static int MIN_PRIORITY = 10;
    public static int MAX_PRIORITY = 255;
    public static String policy = "Priority";
    private static PriorityScheduler singleton = new PriorityScheduler();

    protected PriorityScheduler(){
    }

    public static PriorityScheduler instance(){
	return singleton; 
    }

    protected boolean addToFeasibility(Schedulable schedulable){
	return true;
    }
    
    public void fireSchedulable(Schedulable schedulable){
	new RealtimeThread(schedulable).start();
    }

    public static int getMaxPriority(Thread t){
	return ThreadPriority.priority(ThreadPriority.NORMAL_MAX); 
    }

    public static int getMaxPriority(RealtimeThread t){
	return MAX_PRIORITY; 
    }

    public int getMaxPriority(){
	return MAX_PRIORITY;
    }

    public static int getMinPriority(Thread t) {
	return ThreadPriority.priority(ThreadPriority.NORMAL_MIN);
    }

    public static int getMinPriority(RealtimeThread t) {
	return MIN_PRIORITY;
    } 
    
    public int getMinPriority() {
	return MIN_PRIORITY;
    } 
    
    public String getPolicyName(){
	return policy;
    }

    public boolean isFeasible(){
	return true;
    }

    protected boolean removeFromFeasibility(Schedulable schedulable){
	return true;
    }

    public boolean setIfFeasible(Schedulable schedulable,
					  ReleaseParameters release,
					  MemoryParameters memory){
	return true;
    }

    public boolean setIfFeasible(Schedulable schedulable,
					  ReleaseParameters release,
					  MemoryParameters memory,
					  ProcessingGroupParameters group){
	return true;
    }

    public boolean setIfFeasible(Schedulable schedulable,
					  SchedulingParameters scheduling,
					  ReleaseParameters release,
					  MemoryParameters memory,
					  ProcessingGroupParameters group){
	return true;
    }
};
