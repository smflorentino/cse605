package benchmarks;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import common.LOG;

/**
 * Scope Size:30240192
 Managed Size:240192
 Overhead:30000000

 */

/**
 * Created by scottflo on 4/23/15.
 */
public class MatMultScoped
{
	private static final Utils utils = new Utils();

	static final int trials = 1;
	static final int fragmentationCount = 100;
	final static int rows = 100;
	final static int cols = 100;
	final static int arraySize = rows * cols;


	final static long[] allocation = new long[trials];
	final static long[] set = new long[trials];
	final static long[] mult = new long[trials];

	public static void main(String[] args)
	{
		MemoryAreas.allocScopeBacking(Magic.curThreadState(), 30240192 + 1024 * 4);

		//Generate sizes for fragmentation
		final int[] randomSizes = new int[fragmentationCount];
		utils.generateRandomSizes(randomSizes, arraySize);
		int fragmentationOverhead = utils.calculateScoepdMemoryOverhead(randomSizes);

		Pointer area = MemoryAreas.alloc(trials * (255192 + fragmentationOverhead) + 10240, false, "scoped", 240192 + 10240);

		LOG.info("Fragmentation overhead: " + fragmentationOverhead);
		LOG.info("Sccoped Memory Size: " + MemoryAreas.size(area));
		MemoryAreas.enter(area, new Runnable()
		{
			public void run()
			{
				try
				{
					for (int i = 0; i < trials; i++)
					{
						runTest(i, randomSizes);
					}
				}
				catch(Throwable e)
				{
					LOG.info(e.getClass().getName());
					LOG.info(e.getMessage());

				}

			}
		});

		//Calculated the scoped memory overhead we need for THESE:

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
	}

	public static void runTest(int trial, int[] fragmentationSizes)
	{
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Fragmenting...");

		utils.fragmentUnmanagedMemory(fragmentationSizes);
		//Now do matrix multiplication
		//Generate the array
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Allocating...");
		utils.start();
		Pointer a = UMArray.allocate(UMArray.UMArrayType.INT,rows*cols);
		Pointer b = UMArray.allocate(UMArray.UMArrayType.INT,rows*cols);
		utils.finish();
		allocation[trial] = utils.time();
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Time to allocate: " + utils.time());

		//Populate the array
		utils.start();
		for(int i = 0; i < rows * cols; i++)
		{
			UMArray.setInt(a,i,utils.getRandom().nextInt());
			UMArray.setInt(b,i,utils.getRandom().nextInt());
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

	public static void multiply1d(int rows, int cols, Pointer a, Pointer b)
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
		Pointer answer = UMArray.allocate(UMArray.UMArrayType.INT, firstRows * secondCols);
//		int answer[] = new int[firstRows * secondCols];

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
//					int val = UMArray.getInt(answer,(c * secondCols) + d);
					UMArray.setInt(answer, (c * secondCols) + d, UMArray.getInt(answer,(c * secondCols) + d) + UMArray.getInt(a,(c * firstCols) + k) + UMArray.getInt(b,(k * secondCols) + d));
//					answer[(c * secondCols) + d] += a[(c * firstCols) + k] * b[(k * secondCols) + d];
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
