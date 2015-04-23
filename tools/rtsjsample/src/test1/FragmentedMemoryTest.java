package test1;

import java.lang.System;
import java.util.Random;

public class FragmentedMemoryTest
{
    public static void allocate(int size)
    {
        int[] arr=new int[size];
    }
    public static void main(String[] args)
    {
        //Fragment the heap
        int n=2*1024*1024;
        int size;
        Random random=new Random();
        for(int i=0;i<5000;i++)
        {
            size=random.nextInt(n);
            allocate(size);
        }
        long startTime=System.currentTimeMillis();
        size=2*1024*1024;
        allocate(size);
        long endTime=System.currentTimeMillis();
        long totalTime=endTime-startTime;
        System.out.println("Allocation Time:"+totalTime);
    }
}