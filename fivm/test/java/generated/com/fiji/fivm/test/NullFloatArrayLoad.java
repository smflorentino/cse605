package com.fiji.fivm.test;
public class NullFloatArrayLoad {
   public static void main(String[] v) {
      float[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
