package benchmarks;

import common.PlainOldJava;

import java.io.FileNotFoundException;


/**
 * Created by scottflo on 4/23/15.
 * Performs a number of matrix multiplication operations after fragmenting the Java heap.
 *
 * Writes results to a file; takes a file name as an argument.
 */
@PlainOldJava
public class MatMultHeap
{
	private static final Utils utils = new Utils();

	private static final int trials = MatMultConstants.trials;
	private static final int rows = MatMultConstants.rows;
	private static final int cols = MatMultConstants.cols;
	private static final int fragmentationCount = MatMultConstants.fragmentationCount;

	final static long[] allocation = new long[trials];
	final static long[] sequentialSet = new long[trials];
	final static long[] sequentialGet = new long[trials];
	final static long[] mult = new long[trials];
	final static long[] fragment = new long[trials];
	final static long[] totalTime = new long[trials];

	public static PrintLogWriter logger;

	public static void main(String[] args) throws FileNotFoundException
	{
		if(args.length == 0)
		{
			throw new IllegalArgumentException("Please provide a file name.");
		}
		logger = new PrintLogWriter(args[0]);
		for(int i=0; i< trials;i++)
		{
			long start = System.currentTimeMillis();
			runTest(i);
			long end = System.currentTimeMillis();
			totalTime[i] = end - start;
		}
		//Subtract off fragmentation time
		for(int i = 0;i<trials; i++)
		{
			totalTime[i] -= (fragment[i] / 1000000);
		}

		long allocationTotal = 0;
		long setTotal = 0;
		long getTotal = 0;
		long multTotal = 0;
		long fragmentTotal = 0;
		long totalTotal = 0;

		for(int i=0; i< trials;i++)
		{
			allocationTotal += allocation[i];
			setTotal += sequentialSet[i];
			getTotal += sequentialGet[i];
			multTotal += mult[i];
			fragmentTotal += fragment[i];
			totalTotal += totalTime[i];
		}
//		logger.println("Avg Time to fragment: (ms): " + fragmentTotal / trials / 1000000);
		logger.println(allocationTotal / trials + " ns - Avg Allocation Time");
		logger.println(setTotal / trials + " ns - Avg Set Time");
		logger.println(getTotal / trials + " ns - Avg Get time");
		logger.println(multTotal / trials + " ns -Avg Mult Time");
		logger.println(totalTotal / trials + " ms - Avg Execution Time");
//		logger.println("Avg Total Execution Time (ms): " + totalTotal);


		//Log Results
		printArray("Allocation", allocation);
		printArray("Sequential Set", sequentialSet);
		printArray("Sequential Get", sequentialGet);
		printArray("Mat Mult", mult);
		printArray("Total Time", totalTime);
		logger.close();

	}

	public static void runTest(int trial)
	{
		System.out.println("Starting Trial " + trial + "...");

//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Fragmenting...");

		int arraySize = rows * cols;
		utils.start();
		utils.fragmentHeap(fragmentationCount,(int) MatMultConstants.maxArrayFragmentSize);
		utils.finish();
		fragment[trial] = utils.time();
		//Now do matrix multiplication
		//Generate the array
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Allocating...");
		utils.start();
		int[] a = new int[rows * cols];
		int[] b = new int[rows * cols];
		utils.finish();
		allocation[trial] = utils.time();
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Time to allocate: " + utils.time());
//		LOG.info("Populating...");
		//Populate the array
		utils.start();
		for(int i = 0; i < rows * cols; i++)
		{
			a[i] = utils.getRandom().nextInt();
			b[i] = utils.getRandom().nextInt();
		}
		utils.finish();
		sequentialSet[trial] = utils.time();

//		LOG.info("Accessing...");
		//Access all elements in array
		utils.start();
		int z = 0;
		for(int i = 0; i < rows * cols; i++)
		{
			int x = a[i];
			int y = b[i];
			z|= x | y;
		}
		a[0] = z;
		utils.finish();
		sequentialGet[trial] = utils.time();
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Time to sequentialSet: " + utils.time());
		//Do mat mult
//		LOG.info("Multiplying...");
		utils.start();
		multiply1d(rows, cols, a, b);
		utils.finish();
		mult[trial] = utils.time();
//		LOG.info("Time to mult: " + utils.time());
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
	}

	public static void multiply1d(int rows, int cols, int[] a, int[] b)
	{
		int firstRows, firstCols, secondRows, secondCols, c, d, k;
//		System.out.println("enter the num of rows and columns first matrix");
		firstRows = rows;
		firstCols = cols;

//		int first[] = new int[firstRows * firstCols];
////		System.out.println("enter the value of first matrix");
//		for (c = 0; c < firstRows * firstCols; c++) {
//			first[c] = a[c];
//		}

//		System.out.println("enter the num of rows and columns second matrix");
		secondRows = rows;
		secondCols = cols;
//		int second[] = new int[secondRows * secondCols];
		int answer[] = new int[firstRows * secondCols];

////		System.out.println("enter the elements of second matrix");
//		for (c = 0; c < secondRows * secondCols; c++) {
//			second[c] = b[c];
//		}

//		if ( firstCols != secondRows ) {
//			throw new IllegalArgumentException("A:Rows: " + firstCols + " did not match B:Columns " + secondRows + ".");
//		}

		for (c = 0; c < firstRows; c++) {
			for (d = 0; d < secondCols; d++) {
				for (k = 0; k < firstCols; k++) {
					answer[(c * secondCols) + d] += a[(c * firstCols) + k] * b[(k * secondCols) + d];
					//Equivalent to  answer[c][d] += first[c][k] * second[k][d];
				}
			}
		}

//		System.out.println("The product is ");
//		for (c = 0; c < firstRows; c++) {
//			for (d = 0; d < secondCols; d++) {
//				System.out.print(answer[(c * secondCols) + d] + "\t");
//			}
//			System.out.println();
//		}
	}

	private static void printArray(String msg, long[] array)
	{
		System.out.println(msg);
		for(int i = 0;i< trials; i++)
		{
			logger.print(array[i]);
			logger.print(' ');
		}
		logger.print('\n');
	}
}
