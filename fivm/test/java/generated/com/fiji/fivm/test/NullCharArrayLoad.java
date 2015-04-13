package com.fiji.fivm.test;
public class NullCharArrayLoad {
   public static void main(String[] v) {
      char[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
