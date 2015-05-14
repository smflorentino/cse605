package benchmarks;

import common.PlainOldJava;

/**
 * Created by scottflo on 5/13/15.
 */
@PlainOldJava
public class MatMultConstants
{
	static final int trials = 25;
	static final int rows = 200;
	static final int cols = 200;
	static final int fragmentationCount = 100;
	static final int maxArrayFragmentSize = rows * cols * 2;

	static final int FREE_SPACE = 150 * 1024;
}
