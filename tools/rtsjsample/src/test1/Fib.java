//package test1;
//
//import com.fiji.fivm.r1.MemoryAreas;
//import com.fiji.fivm.r1.Pointer;
//import com.fiji.fivm.r1.unmanaged.UMInteger;
//import common.LOG;
//
///**
// * Created by scottflo on 4/15/15.
// */
//public class Fib implements Runnable {
//
//    public void run() {
//        try {
//            LOG.info("In fib.");
//
//			Pointer p = UMInteger.allocate(2);
//			System.out.println(UMInteger.get(p));
//			UMInteger.set(p, 42);
//			System.out.println(UMInteger.get(p));
//
////            long[] numbers = new long[FibMain.ARRAY_SIZE];
//			UMInteger.allocate(42);
//            LOG.info("Memory Area Consumed: " + MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
//            long[] numbers = new long[128];
//            LOG.info("Done.");
//            LOG.info("Memory Area Consumed: " + MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
////            long[] numbers2 = new long[128];
////            LOG.info("Done.");
////            long[] numbers3 = new long[128];
//            LOG.info("Array Allocated.");
//            for(int i =0;i<numbers.length;i++)
//            {
//                numbers[i] = i* 2;
//            }
//            LOG.info("Numbers assigned.");
////            System.out.println(numbers);
//            LOG.info("Printing ELements...");
//            for(int i = 0;i<numbers.length; i++)
//            {
//                System.out.println(numbers[i]);
//            }
//        } catch (Throwable e) {
//            LOG.HEAP("Exception in Runnable...");
//            LOG.HEAP(e.getMessage());
////            MemoryAreas.setCurrentArea(MemoryAreas.getHeapArea());
////            LOG.info("IN HEAP: " + (MemoryAreas.getCurrentArea() == MemoryAreas.getHeapArea()));
////            LOG.info("Error: "+ e.getMessage());
////            e.printStackTrace();
//        }
//    }
//}
