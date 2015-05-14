package benchmarks;

import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMArray;
import java.util.Random;

/**
 * Created by scottflo on 4/23/15.
 */
public class Utils
{
	private final Random RANDOM = new Random();

	private int[] junk;

	private int nextRandomSmallInt(int max)
	{
		return RANDOM.nextInt(max+1);
	}
	private int nextRandomInt(int seed, int max)
	{
		int randomInt = RANDOM.nextInt(max);
		while(randomInt <= 0)
		{
			randomInt = RANDOM.nextInt(max);

		}
		switch(seed % 3)
		{
			case 0:
				randomInt &= Byte.MAX_VALUE;
				break;
			case 1:
				randomInt &= Short.MAX_VALUE;
				break;
			case 2:
				//do nothing
				break;
		}

		while(randomInt <= 0)
		{
			randomInt = RANDOM.nextInt(max);

		}
		return randomInt;
	}

	private void createArray(int size)
	{
		junk = new int[size];
		junk[junk.length-1] = RANDOM.nextInt();
		junk = null;
	}

	private Pointer createScopedMemoryArray(int size)
	{
		return UMArray.allocate(UMArray.UMArrayType.INT, size);
	}

	public void generateRandomFragmentationSizes(int[][] randomSizeMatrix, int maxSize)
	{
		for(int i = 0; i< randomSizeMatrix.length; i++)
		{
			for(int j =0; j< randomSizeMatrix[i].length; j++)
			{

				if(maxSize <= 6)
				{
					randomSizeMatrix[i][j] = (nextRandomSmallInt(maxSize));
				}
				else
				{
					randomSizeMatrix[i][j] = (nextRandomInt(j, maxSize));
				}
			}
		}
	}

	public static int[] calculateScopedMemoryFragmentationOverhead(int[][] randomSizeMatrix)
	{
		int[] overheads = new int[randomSizeMatrix.length];
		for(int i = 0; i< randomSizeMatrix.length; i++)
		{
			for(int j = 0;j<randomSizeMatrix[i].length;j++)
			{
				overheads[i] += UMArray.calcualteScopedMemoryOverhead(8,randomSizeMatrix[i][j],1);
			}
		}
		return overheads;
	}

	/**
	 * Fragment unmanaged memory. This will require overhead to be
	 * pre-calculated, so the "random" sizes will need to be precalculated
	 * as well.
	 * @param sizes
	 */
	public void fragmentUnmanagedMemory(final int[] sizes)
	{
		for (int size : sizes)
		{
			Pointer array = createScopedMemoryArray(size);
			UMArray.setInt(array, size-1, size);
			UMArray.free(array);
		}
	}

	/**
	 * Fragment the Java heap
	 * @param numArrays the number of arrays to create to simulate fragmentation
	 * @param maxArraySize the maximum sized array to create
	 */
	public void fragmentHeap(int numArrays, int maxArraySize)
	{
		for(int i = 0; i<numArrays; i++)
		{
			if(maxArraySize <= 6)
			{
				createArray(nextRandomSmallInt(maxArraySize));
			}
			else
			{
				createArray(nextRandomInt(i, maxArraySize));
			}
//			junk[junk.length-1] = RANDOM.nextInt();
//			junk = null;
		}
	}

	public int sum(int[] array)
	{
		int sum = 0;
		for(int x : array)
		{
			sum += x;
		}
		return sum;
	}

	public Random getRandom()
	{
		return RANDOM;
	}

	private static long start;
	private static long finish;
	public void start()
	{
		start = System.nanoTime();
	}

	public void finish()
	{
		finish = System.nanoTime();
	}
	public long time()
	{
		return finish - start;
	}




}
