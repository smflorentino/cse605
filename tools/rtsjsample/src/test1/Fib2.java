package test1;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMFloat;
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
			int[] myArray = new int[256];
			myArray[126] = 20;
			myArray[127] = 21;
//			myArray[2	024] = 10;
		}
		catch(Throwable e)
		{
			LOG.FATAL("Exception in Runnable...");
			LOG.info(e.getClass().getName());
			LOG.FATAL(e.getMessage());
		}
	}
}
