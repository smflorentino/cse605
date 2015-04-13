package com.fiji.fivm.test;
public class NullBooleanArrayStore {
   public static void main(String[] v) {
      boolean[] array=null;
      array[Integer.parseInt(v[0])] = false;
      System.out.println("got to here.");
   }
}
