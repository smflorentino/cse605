package common;

/**
 * Created by scottflo on 4/22/15.
 */
public class Assert
{
	private static final String EMPTY = "";

	public static void assertTrue(boolean condition)
	{
		assertTrue(condition, EMPTY);
	}
	public static void assertTrue(boolean condition, final String message)
	{
		if(!condition)
		{
			throw new Fail(message);
		}
	}
}
