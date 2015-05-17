package benchmarks;

import java.lang.Integer;
import java.lang.String;
import java.lang.System;

/*
 * Basic test to simulate an int pointer using a wrapper class
 */
public class HeapPrimitivesTest
{
    public static void testInteger()
    {
        long startTime,endTime,totalTime;
        int max=1000000;
        int val=0;
        IntegerPointer[] intPointer=new IntegerPointer[max];
        startTime= System.nanoTime();
        for(int i=0;i<max;i++)
        {
            intPointer[i]=new IntegerPointer();
            intPointer[i].set(i);
        }
        endTime=System.nanoTime();
        totalTime=endTime-startTime;
        System.out.println("Integer creation time:"+totalTime);

        startTime=System.nanoTime();
        for(int i=0;i<max;i++)
        {
            val=intPointer[i].get();
        }
        endTime=System.nanoTime();
        totalTime=endTime-startTime;
        System.out.println(val);
        System.out.println("Integer access time:"+totalTime);
    }
    public static void main(String[] args)
    {
        long startTime=System.nanoTime();
        testInteger();
        long endTime=System.nanoTime();
        long totalTime=endTime-startTime;
        System.out.println("Integer Allocation/Deallocation time:"+totalTime);
    }
}
