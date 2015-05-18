package benchmarks;

import java.lang.Runnable;
import java.lang.String;
import java.lang.System;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import common.LOG;
import com.fiji.fivm.r1.unmanaged.UMInteger;

public class UnmanagedPrimitivesTest
{
    public static final int SCOPE_SIZE = 2000000 * 4;
    public static final int totalBacking = SCOPE_SIZE + 1024;

    public static void main(String[] args)
    {
        MemoryAreas.allocScopeBacking(Magic.curThreadState(),totalBacking);
        Pointer scoped = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped", 1000000 * 4);
        MemoryAreas.enter(scoped,new Runnable()
        {
            public void run()
            {
                long startTime=System.nanoTime();
                testInteger();
                long endTime=System.nanoTime();
                long totalTime=endTime-startTime;
                System.out.println("Integer Allocation/Deallocation Time:"+totalTime);
            }
        });
    }

    public static void testInteger()
    {
        int max=1000000;
        int val=0;
        Pointer p[]=null;
        long startTime=System.nanoTime();
        for(int i=0;i<max;i++)
        {
            p[i]=UMInteger.allocate(i);
        }
        long endTime=System.nanoTime();
        long totalTime=endTime-startTime;
        System.out.println("Integer Allocation Time:" + totalTime);
        startTime=System.nanoTime();
        for(int i=0;i<max;i++)
        {
            val=UMInteger.get(p[i]);
        }
        endTime=System.nanoTime();
        totalTime=endTime-startTime;
        System.out.println(val);
        System.out.println("Integer access time:" + totalTime);

        startTime=System.nanoTime();
        for(int i=0;i<max;i++)
        {
            UMInteger.free(p[i]);
        }
        endTime=System.nanoTime();
        totalTime=endTime-startTime;
        LOG.HEAP("Integer Deallocation Time:"+totalTime);
    }
}