package java.lang;
import javax.realtime.*;

import edu.purdue.scj.utils.Utils;

public class VMThread extends VMThreadBase {
	static ThreadGroup chooseGroup(Thread child,
			ThreadGroup group,
			Thread parent) {
		// Need to check where we are allocated to see if the allocation
		// is allowed (eg. plain threads must be in heap or immortal) and
		// whether we will have a thread group (real-time threads allocated
		// in scoped memory do not have thread groups)
		MemoryArea ma = MemoryArea.getMemoryArea(child);
		if (ma != ImmortalMemory.instance() ) {
			if (child instanceof RealtimeThread) {
				return null;
			}
			else {
				// we're creating a plain Thread in scoped memory
				throw new IllegalAssignmentError("Can't create Java thread in scoped memory");
			}
		}

		if (parent.getThreadGroup() == null) {
			// FIXME: should this happen when a group is explicitly
			// provided, or just when group == null?

			// For RTSJ a plain thread created by an RTT with no group is placed
			// in the application root threadgroup. This could still be rejected
			// by the access check later.
			assert parent instanceof RealtimeThread;
			return ThreadGroup.root;
		} else {
			return VMThreadBase.chooseGroup(child, group, parent);
		}
	}

	static int choosePriority(Thread child, Thread parent) {
		if (child instanceof RealtimeThread)
			return Thread.MAX_PRIORITY; // ignored
		else if (parent instanceof RealtimeThread)
			return child.getThreadGroup().getMaxPriority();
		else
			return VMThreadBase.choosePriority(child, parent);
	}

	static void create(Thread thread, long stacksize) {
		//VMThread vmThread =  (thread instanceof RealtimeThread
		//	      ? (VMThread) new VMRealtimeThread(thread)
		//	      : new VMThread(thread));
		//Utils.debugPrint("[OVM] VMThread.create");
		VMThread vmThread = null;
		if (thread instanceof RealtimeThread) {
			//Utils.debugPrint("[OVM] VMThread.create... RealtimeThread");
			vmThread =  (VMThread) new VMRealtimeThread(thread);
		}
		else {
			//Utils.debugPrint("[OVM] VMThread.create... regular thread");
			vmThread = new VMThread(thread);
		}
		//Utils.debugPrint("[OVM] VMThread.create... start()");
		vmThread.start(stacksize);
	}

	VMThread(Thread t) { super(t); }
	// constructor for primordial VMThread
	VMThread(int priority, boolean daemon) { super(priority, daemon); }

	// This hooks is called by the Realtime dispatcher just after a
	// thread has been destroyed.  
	void finalizeThread() { }

	
	
	// FIXME: should this be an Opaque or a MemoryArea?  Probably the
	// latter.
	MemoryArea getInitialArea() {
		//Utils.debugPrint("[OVM] VMThread.getInitialArea");
		return MemoryArea.getMemoryArea(this);
	}
}
