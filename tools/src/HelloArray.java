package test;

public class HelloArray
{
	public static void main(String[] args)
	{
		System.out.println("HelloWorld");
		int[] array = new int[1028];
		for(int i=0;i<array.length;i++)
		{
			array[i] = i*2;
		}
		System.out.println(array[1]);
		Kaymar elem = new Kaymar(5);
		System.out.println(elem.getElement());
		System.out.println(array[10512]);
	}
}
