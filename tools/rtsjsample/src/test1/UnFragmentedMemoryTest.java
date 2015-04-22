import java.lang.System;

public class UnFragmentedMemoryTest
{
    public static void allocate()
    {
        //An integer is 4 bytes. Initial heap size is 2*1024*1024
        //So to allocate 1M we will have to allocate an integer array with 1*1024*1024/4=256*1024
        int[] arr1;
        arr1=new int[256*1024];
    }
    public static void main (String[] args)
    {
        long startTime=System.currentTimeMillis();
        allocate();
        allocate();
        allocate();
        allocate();
        allocate();
        allocate();
        long endTime= System.currentTimeMillis();
        long totalTime=endTime-startTime;
        System.out.println("Running time:"+totalTime);
    }
}
