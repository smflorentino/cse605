package common;

/**
 * Created by scottflo on 4/22/15.
 */
public class Report
{
	public static void reportError(boolean condition, final int errorCode)
	{
		if(!condition)
		{
			LOG.HEAP_ERROR_CODE(errorCode);
		}
	}
}
