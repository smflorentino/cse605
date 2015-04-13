package javax.realtime;

import java.lang.VMThread;

/**
 * a placeholder for the realtime thread from SCJ
 * 
 * @author plsek
 *
 */
public class RealtimeThread extends Thread implements Schedulable  {
	public RealtimeThread() {
		
	}
	
	public RealtimeThread(VMThread vmThread, int priority, boolean daemon) {
		System.out.println("[DBG] : DUMMY Realtime VMThread created!!! ERROR!");
	}
	
	public RealtimeThread(VMThread vmThread,String name, int priority, boolean daemon) {
		System.out.println("[DBG] : DUMMY Realtime VMThread created!!! ERROR!");
	}
	
	
	public static RealtimeThread currentRealtimeThread() {
		return null;
	}
		
}
