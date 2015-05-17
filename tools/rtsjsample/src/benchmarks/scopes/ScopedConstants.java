package benchmarks.scopes;

/**
 * Created by scottflo on 5/15/15.
 */
public class ScopedConstants
{
	public static final int tinyArraySize = 4;
	public static final int smallArraySize = 10;
	public static final int regularArraySize = 100;
	public static final int bigArraySize = 1000;
	public static final int largeArraySize = 10000;
	public static final int hugeArraySize = 100000;
//	public static final int massiveArraySize = 10000000;

	public static final int[] trialSizes = {tinyArraySize, smallArraySize, regularArraySize, bigArraySize, largeArraySize, hugeArraySize/*, massiveArraySize*/};

	public static final int trialCount = 500;

	public static final int FREE_SPACE = 16384;
}
