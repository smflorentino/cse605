
package test1;

import com.fiji.fivm.r1.MemoryAreas;
import com.fiji.fivm.r1.Pointer;
import com.fiji.fivm.r1.unmanaged.UMByte;
import com.fiji.fivm.r1.unmanaged.UMShort;
import com.fiji.fivm.r1.unmanaged.UMInteger;
import com.fiji.fivm.r1.unmanaged.UMLong;
import com.fiji.fivm.r1.unmanaged.UMChar;
import com.fiji.fivm.r1.unmanaged.UMBoolean;
import com.fiji.fivm.r1.unmanaged.UMFloat;
import com.fiji.fivm.r1.unmanaged.UMDouble;
import common.LOG;

/*
 * Created by mihirlibran on 04/20/15
 */
public class AllocatePrimitivesTest implements Runnable
{
    public void run()
    {
        try
        {
            //LOG.HEAP("In AllocatePrimitivesTest");

            //Allocating an integer

            int i=8;
            Pointer p=UMInteger.allocate(i);
            //System.out.println(UMInteger.get(p));
            UMInteger.set(p, 21);
            //System.out.println(UMInteger.get(p));
            LOG.PRINT(UMInteger.get(p));


            //Allocating a character
            char m='m';
            p=UMChar.allocate(m);
            //System.out.println(UMChar.get(p));
            UMChar.set(p, 's');
            LOG.PRINT(UMChar.get(p));

            //Allocating a long
            long num=12936L;
            p=UMLong.allocate(num);
            //System.out.println(UMLong.get(p));
            UMLong.set(p,1278612876L);
           LOG.PRINT(UMLong.get(p));


            //Allocating a byte
            byte b=0;
            p=UMByte.allocate((byte)b);
            //System.out.println(UMByte.get(p));
            UMByte.set(p,(byte)127);
            LOG.PRINT(UMByte.get(p));

            //Allocating a short
            short s=-5;
            p=UMShort.allocate(s);
            //System.out.println(UMShort.get(p));
            UMShort.set(p, (short)32767);
            LOG.PRINT(UMShort.get(p));

            //Allocating a boolean
            boolean var=false;
            p=UMBoolean.allocate(var);
            assert !UMBoolean.get(p);
            UMBoolean.set(p, true);
            assert UMBoolean.get(p);

            //Allocating a float
            float f=12.98f;
            p=UMFloat.allocate(f);
            assert UMFloat.get(p)==12.98f;
            UMFloat.set(p, 1237.56f);
            //System.out.println(UMDouble.get(p));
            assert UMFloat.get(p)==1237.56f;

            //Allocating a double
            double d=12312.98;
            p=UMDouble.allocate(d);
            assert UMDouble.get(p)==12312.98;
            UMDouble.set(p, 123712.12123);
            //System.out.println(UMDouble.get(p));
            assert UMDouble.get(p)==123712.12123;

            //LOG.HEAP("Memory Area Consumed:");
            //LOG.HEAP(MemoryAreas.consumed(MemoryAreas.getCurrentArea()));
        }
        catch(Throwable e)
        {
            LOG.HEAP("Exception in Runnable...");
            LOG.info(e.getClass().toString());
        }
    }
}