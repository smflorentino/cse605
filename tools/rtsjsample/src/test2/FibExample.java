package test2;

import common.LOG;

import javax.realtime.RealtimeThread;

/**
 * Created by scottflo on 4/14/15.
 */
public class FibExample {

    public static final int ARRAY_SIZE = 1024;
    public static final int ARRAY_ELEMENT_SIZE = 8;
    public static void main(String[] args)
    {
        LOG.info("Starting main");
        RealtimeThread rt = new RealtimeThread(new ExampleRTThread());
        rt.start();
    }

}
