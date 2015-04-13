package com.fiji.fivm.test;
public class NullObjectArrayLoad {
   public static void main(String[] v) {
      Object[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
