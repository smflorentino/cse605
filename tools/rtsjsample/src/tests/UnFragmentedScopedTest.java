//package tests;
//
//import java.lang.String;
//import java.lang.System;
//import java.lang.Throwable;
//import java.util.Random;
//
//import com.fiji.fivm.r1.Magic;
//import com.fiji.fivm.r1.MemoryAreas;
//import com.fiji.fivm.r1.Pointer;
//import com.fiji.fivm.r1.unmanaged.UMArray;
//import common.LOG;
//import static common.Assert.assertTrue;
//
//public class UnFragmentedScopedTest
//{
//    private static final int SCOPE_SIZE = 10240;
//    private static final int TEST_COUNT = 1;
//    private static final int LARGE_SCOPE_SIZE = 1048756;
//    private static final int TOTAL_BACKING = LARGE_SCOPE_SIZE * TEST_COUNT + 1024;
//
//    public static testTimes()
//    {
//        //Allocating an array of 512 elements
//        Pointer array = UMArray.allocate(UMArray.UMArrayType.INT, 512);
//        for (int j = 0; j < UMArray.length(array); j++)
//        {
//            UMArray.setInt(array, j, j * 3);
//        }
//
//        //Getting a random index
//        Random random=new Random();
//        int index=random.nextInt(512);
//        long startTime=System.nanoTime();
//        assertTrue(UMArray.getInt(array, index) == index * 3); //accessing the element at 'index'
//        long endTime=System.nanoTime();
//        long accessTime=endTime-startTime;
//        System.out.println("Time taken to access element "+index+":"+accessTime);
//
//        //Getting the time taken to access the entire array
//        startTime=System.nanoTime();
//        for (int j = 0; j < UMArray.length(array); j++)
//        {
//            assertTrue(UMArray.getInt(array, j) == j * 3);
//        }
//        endTime=System.nanoTime();
//        accessTime=endTime-startTime;
//        System.out.println("Time taken to access array sequentially:"+accessTime);
//        UMArray.free(array);
//    }
//
//    public static allocate(int size)
//    {
//        Pointer array = UMArray.allocate(UMArray.UMArrayType.INT, size);
//        for (int j = 0; j < UMArray.length(array); j++)
//        {
//            UMArray.setInt(array, j, j * 3);
//        }
//        UMArray.free(array);
//    }
//
//    public static void main(String[] args)
//    {
//        MemoryAreas.allocScopeBacking(Magic.curThreadState(), TOTAL_BACKING);
//        Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", 4096);
//
//        MemoryAreas.enter(area, new Runnable()
//        {
//            public void run()
//            {
//                try
//                {
//                    //Allocate an array of random size
//                    long startTime,endTime,accessTime;
//                    Random random=new Random();
//                    int size=random.nextInt(4096)
//                    startTime=System.nanoTime();
//                    allocate(size);
//                    endTime=System.nanoTime();
//                    accessTime=endTime-startTime;
//                    System.out.println("Allocation/Deallocation Time for a randomly sized array:"+accessTime);
//
//                    startTime = System.nanoTime();
//                    testTimes();
//                    endTime = System.nanoTime();
//                    accessTime=endTime-startTime;
//                    System.out.println("Allocation/Deallocation Times:"+accessTime);
//                }
//                catch(Throwable e)
//                {
//                    LOG.FAIL(e.getMessage());
//                }
//            }
//        }
//    }
//}