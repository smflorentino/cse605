package benchmarks;

import com.fiji.fivm.r1.Magic;
import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import common.LOG;

import java.io.FileNotFoundException;

/**
 * Created by scottflo on 4/23/15.
 *
 * Performs Matrix multiplication on unmanaged memory after "fragmenting" it. Results are printed and written
 * to a file; a file name argument must be provided.
 */
public class MatMultScoped
{
	private static final Utils utils = new Utils();

	final static int arraySize = MatMultConstants.rows * MatMultConstants.cols;

	final static long[] allocation = new long[MatMultConstants.trials];
	final static long[] sequentialSet = new long[MatMultConstants.trials];
	final static long[] sequentialGet = new long[MatMultConstants.trials];
	final static long[] mult = new long[MatMultConstants.trials];
	final static long[] fragment = new long[MatMultConstants.trials];
	final static long[] totalTime = new long[MatMultConstants.trials];

	public static PrintLogWriter logger;

	public static void main(String[] args) throws FileNotFoundException
	{
		if(args.length == 0)
		{
			throw new IllegalArgumentException("Please provide a file name.");
		}
		logger = new PrintLogWriter(args[0]);
		//Generate sizes for fragmentation
		final int[][] randomSizes = new int[MatMultConstants.trials][MatMultConstants.fragmentationCount];
		utils.generateRandomFragmentationSizes(randomSizes, (int) MatMultConstants.maxArrayFragmentSize);
		//Calculate the overhead needed
		int[] fragmentationOverheads = Utils.calculateScopedMemoryFragmentationOverhead(randomSizes);
		//Calculate scope size
		int scopeSize = 0;
		//Hold 3 arrays for matrix multiplication
		scopeSize += UMArray.calculateScopedMemorySize(8, arraySize, 3, 3);
		//And calculate the overhead needed for fragmentation...
		scopeSize += utils.sum(fragmentationOverheads);
		//Add overhead for actual calculation for each trial... (each trial creates 3 matrices...a,b,c)
		scopeSize += UMArray.calcualteScopedMemoryOverhead(8, arraySize, 3 * MatMultConstants.trials);
		//Add ~75K for printing and stack traces...
		scopeSize += MatMultConstants.FREE_SPACE / 2;

		//Calculate Unmanaged Size:
		int unManagedSize = UMArray.calculateManagedMemorySize(8, arraySize, 3) + MatMultConstants.FREE_SPACE / 2;


		MemoryAreas.allocScopeBacking(Magic.curThreadState(), scopeSize + MatMultConstants.FREE_SPACE);

		Pointer memoryArea = MemoryAreas.alloc(scopeSize, false, "scoped", unManagedSize);

//		LOG.info("Fragmentation overhead: " + fragmentationOverhead);
		LOG.info("Sccoped Memory Size: " + MemoryAreas.size(memoryArea));
		MemoryAreas.enter(memoryArea, new Runnable()
		{
			public void run()
			{
				try
				{
					for (int i = 0; i < MatMultConstants.trials; i++)
					{
						long start = System.currentTimeMillis();
						runTest(i, randomSizes[i]);
						long end = System.currentTimeMillis();
						totalTime[i] = end - start;
					}
				}
				catch(Throwable e)
				{
					LOG.info(e.getClass().getName());
					LOG.info(e.getMessage());

				}

			}
		});

		//Subtract off fragmentation time
		for(int i = 0;i<MatMultConstants.trials; i++)
		{
			totalTime[i] -= (fragment[i] / 1000000);
		}

		long allocationTotal = 0;
		long setTotal = 0;
		long getTotal = 0;
		long multTotal = 0;
		long fragmentTotal = 0;
		long totalTotal = 0;

		for(int i=0; i< MatMultConstants.trials;i++)
		{
			allocationTotal += allocation[i];
			setTotal += sequentialSet[i];
			getTotal += sequentialGet[i];
			multTotal += mult[i];
			fragmentTotal += fragment[i];
			totalTotal += totalTime[i];
		}
//		logger.println("Avg Time to fragment: (ms): " + fragmentTotal / trials / 1000000);
		logger.println(allocationTotal / MatMultConstants.trials + " ns - Avg Allocation Time");
		logger.println(setTotal / MatMultConstants.trials + " ns - Avg Set Time");
		logger.println(getTotal / MatMultConstants.trials + " ns - Avg Get time");
		logger.println(multTotal / MatMultConstants.trials + " ns -Avg Mult Time");
		logger.println(totalTotal / MatMultConstants.trials + " ms - Avg Execution Time");
//		logger.println("Avg Total Execution Time (ms): " + totalTotal);


		//Log Results
		printArray("Allocation", allocation);
		printArray("Sequential Set", sequentialSet);
		printArray("Sequential Get", sequentialGet);
		printArray("Mat Mult", mult);
		printArray("Total Time", totalTime);
		logger.close();

	}

	public static void runTest(final int trial, final int[] fragmentationSizes)
	{
		System.out.println("Starting Trial " + trial + "...");

		int rows = MatMultConstants.rows;
		int cols = MatMultConstants.cols;
//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Fragmenting...");
		utils.start();
		utils.fragmentUnmanagedMemory(fragmentationSizes);
		utils.finish();
		fragment[trial] = utils.time();
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

//		LOG.info("Populating...");
		//Populate the array
		utils.start();
		for(int i = 0; i < rows * cols; i++)
		{
			UMArray.setInt(a,i,utils.getRandom().nextInt());
			UMArray.setInt(b,i,utils.getRandom().nextInt());
		}
		utils.finish();
		sequentialSet[trial] = utils.time();

//		LOG.info("Accessing...");
		//Access all elements in array
		utils.start();
		int z = 0;
		for(int i = 0; i < rows * cols; i++)
		{
			int x = UMArray.getInt(a, i);
			int y = UMArray.getInt(b, i);
			z|= x | y;
		}
		UMArray.setInt(a, 0, z);
		utils.finish();
		sequentialGet[trial] = utils.time();

//		LOG.info("Current Consumed Stack Space: " + MemoryAreas.consumed(MemoryAreas.getStackArea()));
//		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getHeapArea()));
//		LOG.info("Time to set: " + utils.time());
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

	public static void multiply1d(int rows, int cols, Pointer a, Pointer b)
	{
		int firstRows, firstCols, /*secondRows,*/ secondCols, c, d, k;
//		System.out.println("enter the num of rows and columns first matrix");
		firstRows = rows;
		firstCols = cols;

//		int first[] = new int[firstRows * firstCols];
////		System.out.println("enter the value of first matrix");
//		for (c = 0; c < firstRows * firstCols; c++) {
//			first[c] = a[c];
//		}

//		System.out.println("enter the num of rows and columns second matrix");
//		secondRows = rows;
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
		UMArray.free(a);
		UMArray.free(b);
		UMArray.free(answer);
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
		for(int i = 0;i<MatMultConstants.trials; i++)
		{
			logger.print(array[i]);
			logger.print(' ');
		}
		logger.print('\n');
	}
}
