package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_2;
import static javax.safetycritical.annotate.Level.SUPPORT;
import static javax.safetycritical.annotate.Phase.CLEANUP;
import static javax.safetycritical.annotate.Phase.INITIALIZATION;

import javax.realtime.MemoryArea;
import javax.realtime.NoHeapRealtimeThread;
import javax.realtime.PriorityParameters;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;

@SCJAllowed(LEVEL_2)
public class ManagedThread extends NoHeapRealtimeThread
        implements ManagedSchedulable {
    
    
	/**
	 * Does not allow this to escape local variables. Creates a link from the
	 * constructed object to the priority parameter. Thus, priority must reside
	 * in a scope that encloses "this".
	 * <p>
	 * The priority represented by priority parameter is consulted only once, at
	 * construction time. If priority.getPriority() returns different values at
	 * different times, only the initial value is honored.
	 * <p>
	 * TBD: what is the "default" ThreadConfigurationParameters? Or should re
	 * remove this constructor?
	 */
	@SCJAllowed(LEVEL_2)
	@SCJRestricted(INITIALIZATION)
	public ManagedThread(PriorityParameters priority, StorageParameters storage) {
		super(priority, (MemoryArea) null);
	}

	/**
	 * Does not allow this to escape local variables. Creates a link from the
	 * constructed object to the priority, memory, and logic parameters . Thus,
	 * all of these parameters must reside in a scope that enclose "this".
	 * <p>
	 * The priority represented by priority parameter is consulted only once, at
	 * construction time. If priority.getPriority() returns different values at
	 * different times, only the initial value is honored.
	 * <p>
	 * TBD: I though we did not want this constructor. -jjh
	 */
	@SCJAllowed(LEVEL_2)
	@SCJRestricted(INITIALIZATION)
	public ManagedThread(PriorityParameters priority,
			StorageParameters mem_info, Runnable logic) {
		super(priority, (MemoryArea) null); // super(schedule, null, null, area,
											// null, null);
	}

	/**
	 * @see javax.safetycritical.ManagedSchedulable#register()
	 */
	@SCJAllowed(LEVEL_2)
	@SCJRestricted(INITIALIZATION)
	public final void register() {
	}

	@SCJAllowed(SUPPORT)
	@SCJRestricted(CLEANUP)
	public void cleanUp() {
	}

	public StorageParameters getThreadConfigurationParameters() {
		return null;
	}
	
	//@SCJAllowed(SUPPORT)
    //public void run() { }
}
