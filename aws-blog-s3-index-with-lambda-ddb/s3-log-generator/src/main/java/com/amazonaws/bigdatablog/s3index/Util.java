package com.amazonaws.bigdatablog.s3index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Util {
   public static final BufferedReader SYSIN = new BufferedReader(new InputStreamReader(System.in));

   public static String prompt(String label) throws IOException {
      System.out.print(label + ": ");
      return SYSIN.readLine();
   }
}
