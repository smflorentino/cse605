package com.fiji.fivm.test;
public class NullDoubleArrayLoad {
   public static void main(String[] v) {
      double[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
