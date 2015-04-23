package test1;

import java.lang.System;
import java.util.Random;

public class UnFragmentedMemoryTest
{
    public static void allocate()
    {
        //An integer is 4 bytes. Initial heap size is 2*1024*1024
        //So to allocate 1M we will have to allocate an integer array with 1*1024*1024/4=256*1024 elements
        int[] arr1;
        arr1=new int[512*1024];
        for(int i=0;i<arr1.length;i++)
        {
            arr1[i]=1;
        }
        Random random=new Random();
        int index=random.nextInt(arr1.length);
        long startTime=System.currentTimeMillis();
        int val=arr1[index];
        long endTime=System.currentTimeMillis();
        long accessTime=endTime-startTime;
        System.out.println("Time to access "+index+" element:"+accessTime);
        startTime=System.currentTimeMillis();
        for(int i=0;i<arr1.length;i++)
        {
            val=arr1[i];
        }
        endTime=System.currentTimeMillis();
        long totalAccessTime=endTime-startTime;
        System.out.println("Total Array Access Time:"+totalAccessTime);
    }
    public static void main (String[] args)
    {
        long startTime=System.currentTimeMillis();
        allocate();
        long endTime= System.currentTimeMillis();
        long totalTime=endTime-startTime;
        System.out.println("Running time:"+totalTime);
    }
}
