package javax.realtime;


public final class RealtimeSystem{

    static final byte BIG_ENDIAN = Byte.parseByte("BIG_ENDIAN");
    static final byte LITTLE_ENDIAN = Byte.parseByte("LITTLE_ENDIAN");
    static final byte BYTE_ORDER = Byte.parseByte("BYTE_ORDER");

    private static RealtimeSecurity _manager = new RealtimeSecurity();
    private static GarbageCollector _GC;

    public static GarbageCollector currentGC(){
	return _GC;
    }
    
    public static int getConcurrentLocksUser(){
	return -1;
    }

    public static int getMaximumconcurrentLocks(){
	return Integer.MAX_VALUE;
    }

    public static RealtimeSecurity getSecurityManager(){
	return _manager;
    }

    public static void setMaximumConcurrentLocks(int number,
						 boolean hard){
    }

    public static void setSecurityManager(RealtimeSecurity manager){
	_manager = manager;
    }

    public static MonitorControl getInitialMonitorControl(){
	return PriorityInheritance.instance();
    }

}