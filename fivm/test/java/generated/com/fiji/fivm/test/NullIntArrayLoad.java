package com.fiji.fivm.test;
public class NullIntArrayLoad {
   public static void main(String[] v) {
      int[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
