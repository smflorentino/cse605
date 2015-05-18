package tests;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import com.fiji.fivm.r1.unmanaged.UMInteger;
import common.LOG;

import static common.Assert.assertTrue;

/**
 * Basic tests to verify that unmanaged memory is working correctly.
 */
public class UnManagedMemoryTest
{
	private static final int SCOPE_SIZE = 40960;
	private static final int TEST_COUNT = 1;
	private static final int TOTAL_BACKING = SCOPE_SIZE * TEST_COUNT + 1024;

	public static void main(String[] args)
	{
		MemoryAreas.allocScopeBacking(Magic.curThreadState(), TOTAL_BACKING);

		testCreateEmptyUnmanaged();
		testCreateSmallUnmanaged();
		testCreateLargeUnmanaged();
		testCreateNegativeUnmanaged();
		testCreateNon64ModuloUnmanaged();
		testOutofMemoryError();
	}

	public static void testCreateEmptyUnmanaged()
	{
		LOG.info("testCreateUnmanagedScoped starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped", 0);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0, "Unmanaged consumed should be 0");
				assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 0, "Consumed should be 0");
			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("testCreateUnmanagedScoped completed");
	}

	public static void testCreateSmallUnmanaged()
	{
		LOG.info("testCreateSmallUnmanaged starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped",128);

		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0);
				assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 128);
			}
		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("testCreateSmallUnmanaged completed");
	}

	public static void testCreateLargeUnmanaged()
	{
		LOG.info("testCreateLargeUnmanaged starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped",32768);
		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					assertTrue(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()) == 0);
					assertTrue(MemoryAreas.consumed(MemoryAreas.getCurrentArea()) == 32768);
				}
				catch(Throwable e)
				{
					LOG.FAIL(e.getMessage());
				}
			}

		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);
		LOG.info("testCreateLargeUnmanaged completed");
	}

	public static void testCreateNegativeUnmanaged()
	{
		boolean thrown = false;
		LOG.info("testCreateNegativeUnmanaged starting...");

		try
		{
			Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped", -1);
		}
		catch(IllegalArgumentException e)
		{
			//Pass..
			thrown = true;
		}
		assertTrue(thrown, "Unmanaged memory size cannot be negative");

		LOG.info("testCreateNegativeUnmanaged completed");
	}

	public static void testCreateNon64ModuloUnmanaged()
	{
		LOG.info("testCreateNon64ModuloUnmanaged starting...");
		boolean thrown = false;

		try
		{
			Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped", 65);
		}
		catch(IllegalArgumentException e)
		{
			//Pass..
			thrown = true;
		}
		assertTrue(thrown, "Unmanaged memory size must be a multiple of 64");

		LOG.info("testCreateNon64ModuloUnmanaged completed");
	}

	public static void testOutofMemoryError()
	{
		LOG.info("testOutofMemoryError starting...");

		Pointer area = MemoryAreas.alloc(SCOPE_SIZE,false,"scoped",64);
		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				boolean thrown = false;
				try
				{
					UMArray.allocate(UMArray.UMArrayType.INT, 128);
				}
				catch(OutOfMemoryError e)
				{
					thrown = true;
				}
				assertTrue(thrown, "OOME should be thrown!");
			}

		});
		MemoryAreas.pop(area);
		MemoryAreas.free(area);

		LOG.info("testOutofMemoryError completed");
	}

}
