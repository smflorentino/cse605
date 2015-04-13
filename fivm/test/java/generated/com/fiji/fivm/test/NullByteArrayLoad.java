package com.fiji.fivm.test;
public class NullByteArrayLoad {
   public static void main(String[] v) {
      byte[] array=null;
      System.out.println(array[Integer.parseInt(v[0])]);
      System.out.println("got to here.");
   }
}
