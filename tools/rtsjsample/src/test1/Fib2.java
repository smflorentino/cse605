package test1;

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
			Pointer array = UMArray.allocate(UMArray.UMArrayType.INT,300);
			for(int i = 0; i< 300; i++)
			{
				UMArray.setInt(array, i, 3*i);
			}
			for(int i = 0; i< 300; i++)
			{
				int x = UMArray.getInt(array,i);
				assert x ==3*i;
			}
//			assert false;

		}
		catch(Throwable e)
		{
			LOG.HEAP("Exception in Runnable...");
			LOG.info(e.getClass().getName());
			LOG.HEAP(e.getMessage());
		}
	}
}
