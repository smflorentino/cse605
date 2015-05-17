package benchmarks.scopes;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import common.LOG;

import java.util.Random;

/**
 * Created by scottflo on 5/17/15.
 */
public class ScopedUnmanaged
{
	private static final Random RANDOM = new Random();

	private static final long[] results = new long[ScopedConstants.trialSizes.length];

	public static void main(String[] args)
	{
//		long totalTrialCount = ScopedConstants.trialSizes.length * ScopedConstants.trialCount;
		long maxArraySize = ScopedConstants.trialSizes[ScopedConstants.trialSizes.length-1];
		long sizePerScope = maxArraySize * 4 * ScopedConstants.trialCount;
		long sizeNeeded = (sizePerScope + ScopedConstants.FREE_SPACE * 2);
		LOG.info("Size of backing store: " + sizeNeeded);
		MemoryAreas.allocScopeBacking(Magic.curThreadState(), sizeNeeded);

		for(int i = 0; i< ScopedConstants.trialSizes.length; i++)
		{
			LOG.info("Starting size " + ScopedConstants.trialSizes[i]);
			runTest(i, ScopedConstants.trialCount);
		}
		for(int i = 0; i< ScopedConstants.trialSizes.length; i++)
		{
			LOG.info("Array Size: " + ScopedConstants.trialSizes[i] + " Average Time (ns): " + results[i]);
		}
	}

	public static void runTest(final int arraySizeIndex, final int trialCount)
	{
		final int arraySize = ScopedConstants.trialSizes[arraySizeIndex];
		final long[] times = new long[trialCount];
		int unmanagedSize = UMArray.calculateManagedMemorySize(8, arraySize, 1);
		int scopedSize = UMArray.calculateScopedMemorySize(8, arraySize, trialCount, 1);
		Pointer memoryArea = MemoryAreas.alloc(scopedSize + 2 * ScopedConstants.FREE_SPACE, false, "scoped", unmanagedSize + ScopedConstants.FREE_SPACE);

			MemoryAreas.enter(memoryArea, new Runnable()
			{
				public void run()
				{
					for (int i = 0; i < trialCount; i++)
					{
						Pointer array = Pointer.fromInt(0);
						long start = System.nanoTime();
						try
						{
							array = UMArray.allocate(UMArray.UMArrayType.INT, arraySize);
							for (int j = 0; j < arraySize; j++)
							{
								int index = RANDOM.nextInt(arraySize);
								UMArray.setInt(array, index, RANDOM.nextInt(arraySize));
							}
							UMArray.free(array);
						} catch (Throwable e)
						{
							LOG.info(e.getClass().getName());
							LOG.info(e.getMessage());
							LOG.info(Integer.toString(UMArray.getInt(array,0)));
	//						if (array != null)
	//						{
	//							UMArray.setInt(array, 0, 0);
	//						}
						}

						long finish = System.nanoTime();
						times[i] = finish - start;
					}
				}

			});

		MemoryAreas.pop(memoryArea);
		MemoryAreas.free(memoryArea);

		long totalTime = 0;
		for (int i = 0; i < trialCount; i++)
		{
			totalTime += times[arraySizeIndex];
		}
		totalTime = totalTime / trialCount;
		results[arraySizeIndex] = totalTime;
	}
}
