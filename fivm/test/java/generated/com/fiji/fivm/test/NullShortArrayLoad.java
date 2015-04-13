package com.fiji.fivm.test;
public class NullShortArrayLoad {
   public static void main(String[] v) {
      short[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
