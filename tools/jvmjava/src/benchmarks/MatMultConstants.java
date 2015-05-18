package benchmarks;

import common.PlainOldJava;

/**
 * Created by scottflo on 5/13/15.
 *
 * Holds constants for perform matrix multiplication tests.
 */
@PlainOldJava
public class MatMultConstants
{
	static final int trials = 100;
	static final int rows = 400;
	static final int cols = 400;
	static final int fragmentationCount = 1000;
	static final int maxArrayFragmentSize = rows * cols * 2;

	static final int FREE_SPACE = 150 * 1024;
}
