package benchmarks;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import common.LOG;


/**
 * Created by scottflo on 4/23/15.
 */
public class MatMultHeap
{
	private static final Utils utils = new Utils();

	private static final int trials = 20;
	final static long[] allocation = new long[trials];
	final static long[] set = new long[trials];
	final static long[] mult = new long[trials];

	public static void main(String[] args)
	{
		for(int i=0; i< trials;i++)
		{
			runTest(i);
		}

		long allocationTotal = 0;
		long setTotal = 0;
		long multTotal = 0;

		for(int i=0; i< trials;i++)
		{
			allocationTotal += allocation[i];
			setTotal += set[i];
			multTotal += mult[i];
		}
		LOG.info("Allocation Time (ns):" + allocationTotal / trials);
		LOG.info("Set Time (ns):" + setTotal / trials);
		LOG.info("Mult Time (ns):" + multTotal / trials);

//		LOG.info("Fragmenting...");

	}

	public static void runTest(int trial)
	{
		assert MemoryAreas.getCurrentArea() == MemoryAreas.getHeapArea();

		final int rows = 100;
		final int cols = 100;
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Fragmenting...");

		int fragmentationCount = 100;
		int arraySize = rows * cols;
		utils.fragmentHeap(fragmentationCount,arraySize);
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

		//Populate the array
		utils.start();
		for(int i = 0; i < rows * cols; i++)
		{
			a[i] = utils.getRandom().nextInt();
			b[i] = utils.getRandom().nextInt();
		}
		utils.finish();
		set[trial] = utils.time();
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Time to set: " + utils.time());
		//Do mat mult
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
}
