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
    public static final int SCOPE_SIZE = 4096 * 8;
    public static final int totalBacking = SCOPE_SIZE + 1024;

    public static void main(String[] args)
    {
        MemoryAreas.allocScopeBacking(Magic.curThreadState(),totalBacking);
        Pointer scoped = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped", 16384);
        MemoryAreas.enter(scoped,new Runnable()
        {
            public void run()
            {
                try
                {
                    long startTime = System.nanoTime();
                    testInteger();
                    long endTime = System.nanoTime();
                    long totalTime = endTime - startTime;
                    LOG.HEAP(totalTime);
                }
                catch(Throwable e)
                {
                    LOG.info(e.getMessage());
                }
            }
        });
    }

    public static void testInteger()
    {
        int max=1000;
        int val=0;
        Pointer p[]=new Pointer[max];
        long startTime=System.nanoTime();
        for(int i=0;i<max;i++)
        {
            p[i]=UMInteger.allocate(i);
        }
        long endTime=System.nanoTime();
        long totalTime=endTime-startTime;
        LOG.HEAP(totalTime);
        startTime=System.nanoTime();
        for(int i=0;i<max;i++)
        {
            val=UMInteger.get(p[i]);
        }
        endTime=System.nanoTime();
        totalTime=endTime-startTime;
        LOG.HEAP(val);
        LOG.HEAP(totalTime);

        startTime=System.nanoTime();
        for(int i=0;i<max;i++)
        {
            UMInteger.free(p[i]);
        }
        endTime=System.nanoTime();
        totalTime=endTime-startTime;
        LOG.HEAP(totalTime);
    }
}