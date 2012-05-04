package com.google.errorprone;

public class Main {
  public static void main(String[] args) {
    int a = Integer.parseInt(args[0]);
    if (a == 3); // BUG!
      throw new IllegalArgumentException("First arg must not be 3");
  }
}