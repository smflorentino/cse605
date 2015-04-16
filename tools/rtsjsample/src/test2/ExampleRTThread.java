package test2;

import common.LOG;

import javax.realtime.LTMemory;

/**
 * Created by scottflo on 4/15/15.
 */
public class ExampleRTThread implements Runnable{

    public void run() {
        LOG.info("Allocating Scoped Memory...");
        LTMemory scoped1 = new LTMemory(FibExample.ARRAY_SIZE * FibExample.ARRAY_ELEMENT_SIZE * 64);
        LOG.info("Entering Fib...");
        scoped1.enter(new Fib());
        LOG.info("Exiting Fib");
    }
}
