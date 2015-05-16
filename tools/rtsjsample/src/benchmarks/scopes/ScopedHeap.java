package benchmarks.scopes;

import benchmarks.MatMultConstants;
import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import common.LOG;

/**
 * Created by scottflo on 5/15/15.
 */
public class ScopedHeap
{

	public static void main(String[] args)
	{
		int scopeSize =
		MemoryAreas.allocScopeBacking(Magic.curThreadState(), scopeSize + MatMultConstants.FREE_SPACE);

		Pointer memoryArea = MemoryAreas.alloc(scopeSize, false, "scoped", unManagedSize);

//		LOG.info("Fragmentation overhead: " + fragmentationOverhead);
		LOG.info("Sccoped Memory Size: " + MemoryAreas.size(memoryArea));
		MemoryAreas.enter(memoryArea, new Runnable()
		{
			public void run()
			{
				try
				{
					for (int i = 0; i < MatMultConstants.trials; i++)
					{
						long start = System.currentTimeMillis();
						runTest(i, randomSizes[i]);
						long end = System.currentTimeMillis();
						totalTime[i] = end - start;
					}
				}
				catch(Throwable e)
				{
					LOG.info(e.getClass().getName());
					LOG.info(e.getMessage());

				}

			}
		});
	}
}
