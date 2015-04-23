package tests;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import common.LOG;

import static common.Assert.assertTrue;
import static common.Report.reportError;

/**
 * Basic Tests to verify that UnmanagedArrays work properly
 */
public class UnManagedArraysTest
{
	private static final int SCOPE_SIZE = 10240;
	private static final int TEST_COUNT = 1;
	//Larger here, because arrays are a thing
	private static final int LARGE_SCOPE_SIZE = 1048756;
	private static final int TOTAL_BACKING = LARGE_SCOPE_SIZE * TEST_COUNT + 1024;

	public static void main(String[] args)
	{
		MemoryAreas.allocScopeBacking(Magic.curThreadState(), TOTAL_BACKING);

//		basicInlinedArrayTest();
		basicArrayTest1();
		basicArrayTest2();

//		inlinedArrayTest();
		smallArrayTest();

//		testCreateSmallUnmanaged();
//		testCreateLargeUnmanaged();
	}

	public static void basicInlinedArrayTest()
	{
		LOG.info("inlinedArrayTest starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", 4096);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					Pointer array = UMArray.allocate(UMArray.UMArrayType.INT, 4);
					for (int j = 0; j < UMArray.length(array); j++)
					{
						UMArray.setInt(array, j, j*3);
					}
					for (int j = 0; j < UMArray.length(array); j++)
					{
						assertTrue(UMArray.getInt(array,j) == j*3);
					}
					UMArray.free(array);
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}
			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("inlinedArrayTest completed");
	}

	//Size divisible by ELEMENTS_PER_BLOCK
	public static void basicArrayTest1()
	{
		LOG.info("basicArrayTest starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", 3072);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					Pointer array = UMArray.allocate(UMArray.UMArrayType.INT,304);
					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 3224L, "Scoped Memory usage incorrect!");
					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 2496L, "Unmanaged Memory usage incorrect!");
					for(int i = 0; i< UMArray.length(array); i++)
					{
						UMArray.setInt(array, i, 3*i);
					}

					for(int i = 0; i< UMArray.length(array); i++)
					{
						int x = UMArray.getInt(array,i);
						assert x ==3*i;
					}
					UMArray.free(array);

					//Verify we leaked the correct amount in scoped memory
					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 3224L, "Scoped Memory usage incorrect!");
					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0L, "Unmanaged Memory was leaked!");
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}

			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("basicArrayTest completed");
	}

	//Size not divisible by ELEMENTS_PER_BLOCK
	public static void basicArrayTest2()
	{
		LOG.info("basicArrayTest starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", 3072);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					Pointer array = UMArray.allocate(UMArray.UMArrayType.INT,300);
					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 3224L, "Scoped Memory usage incorrect!");
					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 2496L, "Unmanaged Memory usage incorrect!");
					for(int i = 0; i< UMArray.length(array); i++)
					{
						UMArray.setInt(array, i, 3*i);
					}

					for(int i = 0; i< UMArray.length(array); i++)
					{
						int x = UMArray.getInt(array,i);
						assert x ==3*i;
					}
					UMArray.free(array);

					//Verify we leaked the correct amount in scoped memory
					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 3224L, "Scoped Memory usage incorrect!");
					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0L, "Unmanaged Memory was leaked!");
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}


			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("basicArrayTest completed");
	}


	public static void inlinedArrayTest()
	{
		LOG.info("inlinedArrayTest starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", 4096);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					for(int i =0;i<128;i++)
					{
						Pointer array = UMArray.allocate(UMArray.UMArrayType.INT, 4);
						for (int j = 0; j < UMArray.length(array); j++)
						{
							UMArray.setInt(array, j, j*3*i);
						}
						for (int j = 0; j < UMArray.length(array); j++)
						{
							assertTrue(UMArray.getInt(array,j) == j*3*i);
						}
						UMArray.free(array);
					}
					//All arrays were freed, make sure no memory was leaked
					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 4096L, "Scoped Memory was leaked!");
					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0L, "Unmanaged Memory was leaked!");
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}
			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("inlinedArrayTest completed");
	}

	/**
	 * 	Create a number of arrays that utilize scoped memory. Verify leakage is correct
	 *
	 * 	Size = 2048
	 * 	Need 2048 / ELEMENTS_PER_BLOCK * sizeof(void) scoped memory per array (1024)
	 * 	Need Math.ceiling(2048 / ELEMENTS_PER_BLOCK) * BLOCK_SIZE of unmanaged memory per array (16384)
	 *
	 * 	Plus two blocks for each header (64+64) = 128
	 *
	 * 	Scoped Memory Size: 40960
	 */

	public static void smallArrayTest()
	{
		LOG.info("smallArrayTest starting...");

		Pointer area = MemoryAreas.alloc(16384*2+1024*2+64*2, false, "scoped", 16384*2+64*2);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					for(int i =0;i<2;i++)
					{
						Pointer array = UMArray.allocate(UMArray.UMArrayType.INT, 2048);
						for (int j = 0; j < UMArray.length(array); j++)
						{
							UMArray.setInt(array, j, j*3*i);
						}
						for (int j = 0; j < UMArray.length(array); j++)
						{
							assertTrue(UMArray.getInt(array,j) == j*3*i);
						}
						UMArray.free(array);
					}
					//All arrays were freed, make sure no memory was leaked
					//We use reportError since our scope is FULLY allocated
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 34944L, 1); //"Scoped Memory was leaked!"
					reportError(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0L, 2);// "Unmanaged Memory was leaked!"
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}
			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("smallArrayTest completed");
	}
}
