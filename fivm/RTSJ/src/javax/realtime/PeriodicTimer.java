package javax.realtime;

public class PeriodicTimer extends RealtimeThread { 

    protected Runnable _logic;
    protected volatile boolean go=false;
    private long period_;

    public PeriodicTimer(HighResolutionTime from, RelativeTime period, AsyncEventHandler logic) {
        // TODO Does not consider the from time as the application does not really use it for now.
        // you may wish to implement it better.
        super();
        period_ = period.millis * 1000000 + period.nanos;
        System.out.println("Created timer with period "+period_);
    }
    
    public void run() {
        System.out.println("Timer started.");
        go=true;
        while (go) {
            System.out.println("Firing timer");
            waitForNextPeriod();
            _logic.run();
        }
        go=true; 
        System.out.println("Timer stopped.");
    }

}