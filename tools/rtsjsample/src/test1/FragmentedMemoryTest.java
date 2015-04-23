package test1;

import java.lang.System;
import java.util.Random;

public class FragmentedMemoryTest
{
    public static void init(int size)
    {
        //This method is used for fragmenting the heap
        int[] arr=new int[size];
        Random random=new Random();
        for(int i=0;i<arr.length;i++)
        {
            int val = random.nextInt(10);
            arr[i]=val;
        }
    }

    public static void allocate(int size)
    {
        int[] arr=new int[size];
    }

    public static void testTimes(int size)
    {
        int[] arr=new int[size];
        for(int i=0;i<arr.length;i++)
        {
            arr[i]=1;
        }
        //Calculate time to access a random element
        Random random=new Random();
        int index=random.nextInt(arr.length);
        long startTime=System.currentTimeMillis();
        int val=arr[index];
        long endTime=System.currentTimeMillis();
        long accessTime=endTime-startTime;
        System.out.println("Time to access "+index+" element:"+accessTime);
        //Calculate time to access entire array sequentially
        startTime=System.currentTimeMillis();
        for(int i=0;i<arr.length;i++)
        {
            val=arr[i];
        }
        endTime=System.currentTimeMillis();
        long totalAccessTime=endTime-startTime;
        System.out.println("Total Array Access Time:"+totalAccessTime);
    }
    public static void main(String[] args)
    {
        //Fragment the heap
        int n=2*1024*1024;
        int size;
        Random random=new Random();
        for(int i=0;i<100;i++)
        {
            size=random.nextInt(n);
            init(size);
        }
        long startTime,endTime,totalTime;
        int minSize=25*1024; //represents a memory of 100K
        int maxSize=512*1024; //represents a memory of 2M
        //Report 10 results
        for(int i=0;i<10;i++) {
            size=random.nextInt((maxSize-minSize)+1) + minSize; //getting a random size from 100K to 2M
            startTime = System.currentTimeMillis();
            allocate(size);
            endTime = System.currentTimeMillis();
            totalTime = endTime - startTime;
            System.out.println("Allocation Time for array " + i + ":" + totalTime);
        }

        size=random.nextInt((maxSize-minSize)+1) + minSize; //getting a random size from 100K to 2M
        startTime = System.currentTimeMillis();
        testTimes(size);
        endTime=System.currentTimeMillis();
        totalTime=endTime-startTime;
        System.out.println("Allocation/Deallocation Time:"+totalTime);
    }
}