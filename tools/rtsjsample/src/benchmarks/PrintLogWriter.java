package benchmarks;

import common.PlainOldJava;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Created by scottflo on 5/13/15.
 */
@PlainOldJava
public class PrintLogWriter extends PrintWriter
{
	public PrintLogWriter(String file) throws FileNotFoundException
	{
		super(file);
	}

	@Override
	public void print(char c)
	{
		super.print(c);
		System.out.print(c);
	}

	@Override
	public void print(long l)
	{
		super.print(l);
		System.out.print(l);
	}

	@Override
	public void println(int i)
	{
		super.print(i);
		System.out.print(i);
	}

	@Override
	public void println(String s)
	{
		super.println(s);
		System.out.println(s);
	}

}
