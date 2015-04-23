package test1;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import common.LOG;


/**
 * Created by scottflo on 4/20/15.
 */
public class Fib2 implements Runnable
{
	public void run()
	{
		try
		{
			LOG.HEAP(MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
			LOG.HEAP(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()));
			Pointer array = UMArray.allocate(UMArray.UMArrayType.INT,300);
			for(int i = 0; i< 300; i++)
			{
				UMArray.setInt(array, i, 3*i);
			}
			LOG.HEAP(MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
			LOG.HEAP(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()));

			for(int i = 0; i< 300; i++)
			{
				int x = UMArray.getInt(array,i);
				assert x ==3*i;
			}
			UMArray.free(array);
			LOG.HEAP(MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
			LOG.HEAP(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()));





			LOG.HEAP(MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
			LOG.HEAP(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()));
			array = UMArray.allocate(UMArray.UMArrayType.INT,300);
			for(int i = 0; i< 300; i++)
			{
				UMArray.setInt(array, i, 3*i);
			}
			LOG.HEAP(MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
			LOG.HEAP(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()));

			for(int i = 0; i< 300; i++)
			{
				int x = UMArray.getInt(array,i);
				assert x ==3*i;
			}
			UMArray.free(array);
			LOG.HEAP(MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
			LOG.HEAP(MemoryAreas.consumedUnmanaged(MemoryAreas.getCurrentArea()));

//			LOG.HEAP(Long.toString(MemoryAreas.consumed(MemoryAreas.getCurrentArea())));
//			assert false;

		}
		catch(Throwable e)
		{
			LOG.HEAP("Exception in Runnable...");
			final String className = e.getClass().getName();
			LOG.HEAP(className);
			final String message = e.getMessage();
			LOG.HEAP(message);
		}
	}
}
