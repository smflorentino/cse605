package tests;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import common.LOG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
//		basicArrayTest1();
//		basicArrayTest2();
//
////		inlinedArrayTest();
//		smallArrayTest();
		smallArrayTest2();
		smallArrayTest3();
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
						UMArray.setInt(array, j, j * 3);
					}
					for (int j = 0; j < UMArray.length(array); j++)
					{
						assertTrue(UMArray.getInt(array, j) == j * 3);
					}
					UMArray.free(array);
				} catch (Throwable e)
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
						int x = UMArray.getInt(array, i);
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

		Pointer area = MemoryAreas.alloc(18496, false, "scoped", 16448);

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
							assertTrue(UMArray.getInt(array, j) == j * 3 * i);
						}
						UMArray.free(array);
					}
					//All arrays were freed, make sure no memory was leaked
					//We use reportError since our scope is FULLY allocated
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 18496L, 1); //"Scoped Memory was leaked!"
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

	/**
	 * Similar to {@link UnManagedArraysTest#smallArrayTest()}, but with more arrays this time.
	 * We also use calculators for sizes
	 */
	public static void smallArrayTest2()
	{
		LOG.info("smallArrayTest2 starting...");

		final int elemCount = 13337;
		final int activeArrayCount = 6;
		final int lifetimeArrayCount = 48;
		final int scopeSize = UMArray.calculateScopedMemorySize(8, elemCount, lifetimeArrayCount, activeArrayCount);
		final int unManagedSize = UMArray.calculateManagedMemorySize(8, elemCount, activeArrayCount);
		final int overhead = UMArray.calcualteScopedMemoryOverhead(8, elemCount, lifetimeArrayCount);
		Pointer area = MemoryAreas.alloc(scopeSize, false, "scoped", unManagedSize);

		final Pointer[] arrays = new Pointer[activeArrayCount];

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					for(int i =0;i<8;i++)
					{
						for(int j=0; j<activeArrayCount;j++)
						{
							arrays[j] = UMArray.allocate(UMArray.UMArrayType.INT, elemCount);
							Pointer array = arrays[j];
							for (int k = 0; k < UMArray.length(array); k++)
							{
								UMArray.setInt(array, k, k*3*i);
							}
							for (int k = 0; k < UMArray.length(array); k++)
							{
								assertTrue(UMArray.getInt(array,k) == k*3*i);
							}
						}
						for(int j=0; j<activeArrayCount;j++)
						{
							UMArray.free(arrays[j]);
						}

					}
					//All arrays were freed, make sure no memory was leaked
					//We use reportError since our scope is FULLY allocated
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == overhead + unManagedSize , 1); //"Scoped Memory was leaked!"
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
		LOG.info("smallArrayTest2 completed");
	}

	/**
	 * Similar to {@link UnManagedArraysTest#smallArrayTest2()}, but with more arrays this time.
	 * We allocate / deallocate in random order, with random values.
	 */
	public static void smallArrayTest3()
	{
		LOG.info("smallArrayTest3 starting...");

		final int elemCount = 7;
		final int arrayCount = 36;
		final int activeArrayCount = 6;
		final int scopeSize = UMArray.calculateScopedMemorySize(8, elemCount, arrayCount, activeArrayCount);
		final int unManagedSize = UMArray.calculateManagedMemorySize(8, elemCount, activeArrayCount);
		final int overhead = UMArray.calcualteScopedMemoryOverhead(8, elemCount, arrayCount);

		Pointer area = MemoryAreas.alloc(scopeSize, false, "scoped", unManagedSize);

		final Random r = new Random();

		//Populate 6 initial arrays of values
		final int[][] values = new int[activeArrayCount][elemCount];
		for(int i = 0; i< activeArrayCount; i++)
		{
			for(int j = 0; j < elemCount; j++)
			{
				values[i][j] = r.nextInt();
			}
		}

		//Randomize array allocation/deallocation order
//		final int[] activeArrays = new int[activeArrayCount];
//		for(int i = 0; i< activeArrayCount; i++)
//		{
//			activeArrays[i] = i;
//		}
		final List<Integer> activeArrays = new ArrayList<Integer>(activeArrayCount);
		for(int i =0; i< activeArrayCount; i++)
		{
			activeArrays.add(i);
		}
		//Populate array of array pointers
		final Pointer[] arrayPointers = new Pointer[activeArrayCount];

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					for(int i =0;i<6;i++)
					{
						//Perform allocation in random order
						Collections.shuffle(activeArrays, r);
						for(int j =0;j<activeArrayCount;j++)
						{
							int arrayIndex = activeArrays.get(j);
							arrayPointers[arrayIndex] = UMArray.allocate(UMArray.UMArrayType.INT, elemCount);
							Pointer array = arrayPointers[arrayIndex];
							for(int k=0;k<elemCount;k++)
							{
								UMArray.setInt(array, k, values[arrayIndex][k]);
							}
						}
						//Perform verification in random order
						Collections.shuffle(activeArrays, r);
						for(int j =0;j<activeArrayCount;j++)
						{
							int arrayIndex = activeArrays.get(j);

							Pointer array = arrayPointers[arrayIndex];
							for(int k=0;k<elemCount;k++)
							{
								int cur = UMArray.getInt(array, k);
								reportError(cur ==  values[arrayIndex][k], 3); //Value misplaced!
							}
						}

						//Perform deallocation in random order
						Collections.shuffle(activeArrays, r);
						for(int k =0;k<activeArrayCount;k++)
						{
							int arrayIndex = activeArrays.get(k);
							UMArray.free(arrayPointers[arrayIndex]);
						}

					}
					//All arrays were freed, make sure no memory was leaked
					//We use reportError since our scope is FULLY allocated
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == overhead + unManagedSize , 1); //"Scoped Memory was leaked!"
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
		LOG.info("smallArrayTest3 completed");
	}
}
