package benchmarks;

import java.lang.Integer;

public class IntegerPointer
{
    int num;
    public void set(int n)
    {
        num=new Integer(n);
    }
    public int get()
    {
        return num;
    }
}
