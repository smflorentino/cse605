package test2;

import common.LOG;

/**
 * Created by scottflo on 4/15/15.
 */
public class Fib implements Runnable {

    public void run() {
        try {
            LOG.info("In fib.");
            long[] numbers = new long[FibExample.ARRAY_SIZE];
            LOG.info("Array Allocated.");
            for(int i =0;i<numbers.length;i++)
            {
                numbers[i] = i* 2;
            }
            LOG.info("Numbers assigned.");
//            System.out.println(numbers);
            LOG.info("Printing ELements...");
            for(int i = 0;i<numbers.length; i++)
            {
                System.out.println(numbers[i]);
            }
        } catch (Throwable e) {
            LOG.info("Exception occurred!");
            LOG.info("Error: "+ e.getMessage());
            e.printStackTrace();
        }
    }
}
