package benchmarks;

import com.fiji.fivm.r1.MemoryAreas;
import common.LOG;

/**
 * Created by scottflo on 4/23/15.
 */
public class MatMultHeap
{
	private static final Utils utils = new Utils();
	public static void main(String[] args)
	{
		assert MemoryAreas.getCurrentArea() == MemoryAreas.getHeapArea();

		int trials = 10;

		for(int i=0; i< trials;i++)
		{
			runTest();
		}

//		LOG.info("Fragmenting...");

	}

	public static void runTest()
	{
		final int rows = 100;
		final int cols = 100;
		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
		LOG.info("Fragmenting...");

		int fragmentationCount = 1;
		int arraySize = rows * cols;
		utils.fragmentHeap(fragmentationCount,arraySize);
		//Now do matrix multiplication
		//Generate the array
		utils.start();
		int[] a = new int[rows * cols];
		int[] b = new int[rows * cols];
		utils.finish();
		System.out.println("Time to allocate: " + utils.time());
		//Populate the array
		utils.start();
		for(int i = 0; i < rows * cols; i++)
		{
			a[i] = utils.getRandom().nextInt();
			b[i] = utils.getRandom().nextInt();
		}
		utils.finish();
		System.out.println("Time to set: " + utils.time());
		//Do mat mult
		utils.start();
		multiply1d(rows, cols, a, b);
		utils.finish();
		System.out.println("Time to mult: " + utils.time());
		LOG.info("Current Consumed Heap Space: " + MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
	}

	public static void multiply1d(int rows, int cols, int[] a, int[] b)
	{
		int firstRows, firstCols, secondRows, secondCols, c, d, k;
//		System.out.println("enter the num of rows and columns first matrix");
		firstRows = rows;
		firstCols = cols;

		int first[] = new int[firstRows * firstCols];
//		System.out.println("enter the value of first matrix");
		for (c = 0; c < firstRows * firstCols; c++) {
			first[c] = a[c];
		}

//		System.out.println("enter the num of rows and columns second matrix");
		secondRows = rows;
		secondCols = cols;
		int second[] = new int[secondRows * secondCols];
		int answer[] = new int[firstRows * secondCols];

//		System.out.println("enter the elements of second matrix");
		for (c = 0; c < secondRows * secondCols; c++) {
			second[c] = b[c];
		}

//		if ( firstCols != secondRows ) {
//			throw new IllegalArgumentException("A:Rows: " + firstCols + " did not match B:Columns " + secondRows + ".");
//		}

		for (c = 0; c < firstRows; c++) {
			for (d = 0; d < secondCols; d++) {
				for (k = 0; k < firstCols; k++) {
					answer[(c * secondCols) + d] += first[(c * firstCols) + k] * second[(k * secondCols) + d];
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
