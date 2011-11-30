// Copyright 2011 Google Inc. All Rights Reserved.

/**
 * Positive test cases for the empty if statement check.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PositiveCases {

  public static void positiveCase1() {
    int i = 10;
    if (i == 10); {
      i++;
    }
  }

  public static void positiveCase2() {
    int i = 10;
    if (i == 10);
    i++;
    System.out.println("foo");
  }

  public static void positiveCase3() {
    int i = 10;
    if (i == 10)
      ;
    i++;
    System.out.println("foo");
  }

  public static void positiveCase4() {
    int i = 10;
    if (i == 10)            ;
  }
}
