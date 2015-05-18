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
	private static final int TEST_COUNT = 10;
	//Larger here, because arrays are a thing
	private static final int LARGE_SCOPE_SIZE = 15 * 1048756;
	private static final int TOTAL_BACKING = LARGE_SCOPE_SIZE * TEST_COUNT + 1024;

	public static void main(String[] args)
	{
		MemoryAreas.allocScopeBacking(Magic.curThreadState(), TOTAL_BACKING);

		basicInlinedArrayTest();
		basicArrayTest1();
		basicArrayTest2();
		basicLargeArrayTest1();

		inlinedArrayTest();
		smallArrayTest();
		smallArrayTest2();
		smallArrayTest3();
		interleavedAllocations();
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
		LOG.info("basicArrayTest1 starting...");

		final int elemCount = 256;
		final int managedMemorySize = UMArray.calculateManagedMemorySize(8, elemCount, 1);
		final int consumedScoped = UMArray.calcualteScopedMemoryOverhead(8, elemCount, 1) + managedMemorySize;
		Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", managedMemorySize);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					Pointer array = UMArray.allocate(UMArray.UMArrayType.INT,elemCount);
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == consumedScoped, 1); //"Scoped Memory usage incorrect!"
					reportError(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == managedMemorySize, 2); //"Unmanaged Memory usage incorrect!"
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
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == consumedScoped, 3); //"Scoped Memory usage incorrect!"
					reportError(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0L, 4); //"Unmanaged Memory usage incorrect!"
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}

			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("basicArrayTest1 completed");
	}

	//Size not divisible by ELEMENTS_PER_BLOCK
	public static void basicArrayTest2()
	{
		LOG.info("basicArrayTest2 starting...");

		final int elemCount = 257;
		final int managedMemorySize = UMArray.calculateManagedMemorySize(8, elemCount, 1);
		final int consumedScoped = UMArray.calcualteScopedMemoryOverhead(8, elemCount, 1) + managedMemorySize;
		Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", managedMemorySize);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					Pointer array = UMArray.allocate(UMArray.UMArrayType.INT,elemCount);
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == consumedScoped, 1); //"Scoped Memory usage incorrect!"
					reportError(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == managedMemorySize, 2); //"Unmanaged Memory usage incorrect!"
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
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == consumedScoped, 3); //"Scoped Memory usage incorrect!"
					reportError(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0L, 4); //"Unmanaged Memory usage incorrect!"
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}

			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("basicArrayTest2 completed");
	}

	//Size not divisible by ELEMENTS_PER_BLOCK
	public static void basicLargeArrayTest1()
	{
		LOG.info("basicLargeArrayTest1 starting...");

		final int elemCount = 750*750;
		int scopedSize = UMArray.calculateScopedMemorySize(8, elemCount, 1, 1);
		int unManagedSize = UMArray.calculateManagedMemorySize(8, elemCount, 1);
		Pointer area = MemoryAreas.alloc(scopedSize, false, "scoped", unManagedSize);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					Pointer array = UMArray.allocate(UMArray.UMArrayType.INT, elemCount);
//					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 3224L, "Scoped Memory usage incorrect!");
//					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 2496L, "Unmanaged Memory usage incorrect!");
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
//					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 3224L, "Scoped Memory usage incorrect!");
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
		LOG.info("basicLargeArrayTest1 completed");
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
		final int elemCount =  2048;
		final int totalArrayCount = 3;
		final int scopedSize = UMArray.calculateScopedMemorySize(8, elemCount, 3, 1);
		final int unManagedSize = UMArray.calculateManagedMemorySize(8, elemCount, 1);
		final int scopedUsage = UMArray.calcualteScopedMemoryOverhead(8, elemCount, 3);
		Pointer area = MemoryAreas.alloc(scopedSize, false, "scoped", unManagedSize);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					for(int i =0;i<totalArrayCount;i++)
					{
						Pointer array = UMArray.allocate(UMArray.UMArrayType.INT, elemCount);
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
					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == scopedUsage + unManagedSize, 1); //"Scoped Memory was leaked!"
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

	/**
	 * Similar to {@link UnManagedArraysTest#smallArrayTest3()}, but we interleave regular scoped allocation with unmanaged allocations.
	 */
	public static void interleavedAllocations()
	{
		LOG.info("interleavedAllocations starting...");

		final int elemCount = 7;
		final int arrayCount = 36;
		final int activeArrayCount = 6;
		final int scopeSize = UMArray.calculateScopedMemorySize(8, elemCount, arrayCount, activeArrayCount) + 10240;
		final int unManagedSize = UMArray.calculateManagedMemorySize(8, elemCount, activeArrayCount);

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
						int[] regularScopedAllocation = new int[127];
						for(int j=0;j<regularScopedAllocation.length;j++)
						{
							regularScopedAllocation[j] = i;
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
//					reportError(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == overhead + unManagedSize , 1); //"Scoped Memory was leaked!"
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
		LOG.info("interleavedAllocations completed");
	}
}
