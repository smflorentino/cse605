package javax.safetycritical;

import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.SCJAllowed;


import javax.safetycritical.annotate.SCJRestricted;
	


@SCJAllowed(LEVEL_1)
public class PriorityScheduler extends javax.realtime.PriorityScheduler {

     @SCJAllowed(LEVEL_1) 
	 public static PriorityScheduler instance() { 
		 //TODO:
		 return null; 
     }

	
	/**
	 * 
	 * @return Returns the maximum hardware real-time priority supported by this virtual machine. @SCJAllowed
	 */
	@SCJAllowed(LEVEL_1) 
	@SCJRestricted()
	public int getMaxHardwarePriority() {
		//TODO:
		return 2000;
	}
	
	
	/**
	 * Returns the minimum hardware real-time priority supported by this virtual machine.
	 */
	@SCJAllowed(LEVEL_1)
	@SCJRestricted()
	public int getMinHardwarePriority() {
		//TODO:
		return 1000;
	} 
}
