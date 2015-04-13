package com.fiji.fivm.test;

import com.fiji.fivm.r1.Pointer;

/**
 * Created by scottflo on 4/13/15.
 */
public class IntPointerTest {
    public static void main(String[] args)
    {
        Pointer p = Pointer.parsePointer("asd");
        int x = (int) p.zero();

    }

}
