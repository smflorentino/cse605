//package tests;
//
//import com.fiji.fivm.r1.Magic;
//import com.fiji.fivm.r1.MemoryAreas;
//import com.fiji.fivm.r1.Pointer;
//import common.LOG;
//
//import static common.Assert.assertTrue;
//
///**
// * Basic tests to verify that unmanaged memory is working correctly.
// */
//public class UnManagedMemoryTest
//{
//	private static final int SCOPE_SIZE = 40960;
//	private static final int TEST_COUNT = 1;
//	private static final int TOTAL_BACKING = SCOPE_SIZE * TEST_COUNT + 1024;
//
//	public static void main(String[] args)
//	{
//		MemoryAreas.allocScopeBacking(Magic.curThreadState(), TOTAL_BACKING);
//
//		testCreateEmptyUnmanaged();
//		testCreateSmallUnmanaged();
//		testCreateLargeUnmanaged();
//
//	}
//
//	public static void testCreateEmptyUnmanaged()
//	{
//		LOG.info("testCreateUnmanagedScoped starting...");
//
//		Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped", 0);
//
//		MemoryAreas.enter(area, new Runnable()
//		{
//			public void run()
//			{
//				assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0, "Unmanaged consumed should be 0");
//				assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 0, "Consumed should be 0");
//			}
//		});
//		MemoryAreas.pop(area);
//		MemoryAreas.free(area);
//		LOG.info("testCreateUnmanagedScoped completed");
//	}
//
//	public static void testCreateSmallUnmanaged()
//	{
//		LOG.info("testCreateSmallUnmanaged starting...");
//
//		Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped",128);
//
//		MemoryAreas.enter(area, new Runnable()
//		{
//			public void run()
//			{
//				assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0);
//				assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 128);
//			}
//		});
//		MemoryAreas.pop(area);
//		MemoryAreas.free(area);
//		LOG.info("testCreateSmallUnmanaged completed");
//	}
//
//	public static void testCreateLargeUnmanaged()
//	{
//		LOG.info("testCreateLargeUnmanaged starting...");
//
//		Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped",32768);
//		MemoryAreas.enter(area, new Runnable()
//		{
//			public void run()
//			{
//				try
//				{
//					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0);
//					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 32768);
//				}
//				catch(Throwable e)
//				{
//					LOG.FAIL(e.getMessage());
//				}
//			}
//
//		});
//		MemoryAreas.pop(area);
//		MemoryAreas.free(area);
//		LOG.info("testCreateLargeUnmanaged completed");
//	}
//}
