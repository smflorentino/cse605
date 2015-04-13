package com.fiji.fivm.test;
public class MultiANewArray7Dim {
   public static void main(String[] v) {
      byte[][][][][][][] array = new byte[Integer.parseInt(v[0])][Integer.parseInt(v[1])][Integer.parseInt(v[2])][Integer.parseInt(v[3])][Integer.parseInt(v[4])][Integer.parseInt(v[5])][Integer.parseInt(v[6])];
      System.out.println(""+array.getClass()+" "+array[0].getClass()+" "+array[0][0].getClass()+" "+array[0][0][0].getClass()+" "+array[0][0][0][0].getClass()+" "+array[0][0][0][0][0].getClass()+" "+array[0][0][0][0][0][0].getClass());
      System.out.println(""+array.length+" "+array[0].length+" "+array[0][0].length+" "+array[0][0][0].length+" "+array[0][0][0][0].length+" "+array[0][0][0][0][0].length+" "+array[0][0][0][0][0][0].length);
   }
}

