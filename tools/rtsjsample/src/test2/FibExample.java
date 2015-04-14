package test2;

import javax.realtime.LTMemory;

/**
 * Created by scottflo on 4/14/15.
 */
public class FibExample {

    public static void main(String[] args)
    {
        LTMemory scoped1 = new LTMemory(8*1024 + 64);
        scoped1.enter(new Fib());
    }
    public static class Fib implements Runnable {

        long[] numbers;
        @Override
        public void run() {
            numbers = new long[1024];
            numbers[0] = 1;
            numbers[1] = 1;
            for(int i =2;i<numbers.length;i++)
            {
                numbers[i] = numbers[i-1] + numbers[i-2];
            }
            for(int i = 0;i<numbers.length; i++)
            {
                System.out.println(numbers[i]);
            }
        }
    }
}
