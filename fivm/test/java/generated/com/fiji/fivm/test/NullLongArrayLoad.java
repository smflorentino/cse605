package com.fiji.fivm.test;
public class NullLongArrayLoad {
   public static void main(String[] v) {
      long[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
