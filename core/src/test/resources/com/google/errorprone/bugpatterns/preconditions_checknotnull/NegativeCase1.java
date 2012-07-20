package com.google.errorprone.bugpatterns.preconditions_checknotnull;

public class NegativeCase1 {
  public void go() {
    Preconditions.checkNotNull("this is ok");
  }
  
  private static class Preconditions {
    static void checkNotNull(String string) {
      System.out.println(string);
    }
  }
}