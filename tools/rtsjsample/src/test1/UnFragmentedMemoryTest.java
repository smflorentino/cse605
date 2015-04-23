package test1;
import java.lang.System;
import java.util.Random;

public class UnFragmentedMemoryTest
{
    public static void allocate()
    {
        int[] arr1;
        arr1=new int[512*1024];
        for(int i=0;i<arr1.length;i++)
        {
            arr1[i]=1;
        }

        Random random=new Random();
        int index=random.nextInt(arr1.length); //Getting a random index
        long startTime=System.nanoTime();
        int val=arr1[index]; //Accessing the element at the random index
        long endTime=System.nanoTime();
        long accessTime=endTime-startTime;
        System.out.println("Time to access "+index+" element:"+accessTime);

        //Sequentially accessing the array
        startTime=System.nanoTime();
        for(int i=0;i<arr1.length;i++)
        {
            val=arr1[i];
        }
        endTime=System.nanoTime();
        long totalAccessTime=endTime-startTime;
        System.out.println("Total Array Access Time:"+totalAccessTime);
    }
    public static void main (String[] args)
    {
        long startTime=System.nanoTime();
        allocate();
        long endTime= System.nanoTime();
        long totalTime=endTime-startTime;
        System.out.println("Running time:"+totalTime);
    }
}
