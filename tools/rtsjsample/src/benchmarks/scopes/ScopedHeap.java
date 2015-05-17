package benchmarks.scopes;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import common.LOG;

import java.util.Random;

/**
 * Created by scottflo on 5/15/15.
 */
public class ScopedHeap
{
	private static final Random RANDOM = new Random();

	private static final long[] results = new long[ScopedConstants.trialSizes.length];

	public static void main(String[] args)
	{
		long totalTrialCount = ScopedConstants.trialSizes.length * ScopedConstants.trialCount;
		long maxArraySize = ScopedConstants.trialSizes[ScopedConstants.trialSizes.length-1];
		long sizePerScope = maxArraySize * 4;
		long sizeNeeded = totalTrialCount * (sizePerScope + ScopedConstants.FREE_SPACE * 2);
		LOG.info("Size of backing store: " + sizeNeeded);
		MemoryAreas.allocScopeBacking(Magic.curThreadState(),sizeNeeded);

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

		for(int i = 0; i< trialCount; i++)
		{
			long start = System.nanoTime();
			Pointer memoryArea = MemoryAreas.alloc((arraySize * 4) + ScopedConstants.FREE_SPACE, false, "scoped");
			MemoryAreas.enter(memoryArea, new Runnable()
			{
				public void run()
				{
					int[] array = null;
					try
					{
						array = new int[arraySize];
						for (int i = 0; i < arraySize; i++)
						{
							int index = RANDOM.nextInt(arraySize);
							array[index] = RANDOM.nextInt(arraySize);
						}
					} catch (Throwable e)
					{
						LOG.info(e.getClass().getName());
						LOG.info(e.getMessage());
						if(array != null)
						{
							array[0] = 0;
						}
					}
				}
			});
			MemoryAreas.pop(memoryArea);
			MemoryAreas.free(memoryArea);
			long finish = System.nanoTime();
			times[i] = finish - start;
		}

		long totalTime = 0;
		for(int i = 0; i < trialCount; i++)
		{
			totalTime += times[arraySizeIndex];
		}
		totalTime = totalTime / trialCount;
		results[arraySizeIndex] = totalTime;
	}
}
