package tests;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import common.LOG;

import static common.Assert.assertTrue;

/**
 * Basic tests to verify whether unmanaged primitives work properly
 */
public class UnManagedPrimitivesTest
{
	private static final int SCOPE_SIZE = 10240;
	private static final int TEST_COUNT = 1;
	//Larger here, because arrays are a thing
	private static final int TOTAL_BACKING = SCOPE_SIZE * TEST_COUNT + 1024;
	public static void main(String[] args)
	{
		MemoryAreas.allocScopeBacking(Magic.curThreadState(), TOTAL_BACKING);
		basicIntegerTest();
//		basicInlinedArrayTest();
//		basicArrayTest1();
	}

	public static void basicIntegerTest()
	{
		LOG.info("inlinedArrayTest starting...");
		final int primitiveCount = 256;
		Pointer area = MemoryAreas.alloc(SCOPE_SIZE, false, "scoped", primitiveCount * 8);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					for(int i = 0; i<primitiveCount;i++)
					{

					}
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

}
