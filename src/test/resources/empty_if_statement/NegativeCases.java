// Copyright 2011 Google Inc. All Rights Reserved.

/**
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class NegativeCases {

  // just a normal use of if
  public static void negativeCase1() {
    int i = 10;
    if (i == 10) {
      System.out.println("foo");
    }
    i++;
  }

  // empty then part but nonempty else
  public static void negativeCase2() {
    int i = 0;
    if (i == 10)
      ;
    else
      System.out.println("not 10");
  }

  // multipart if with non-empty else
  public static void negativeCase3() {
    int i = 0;
    if (i == 10)
      ;
    else if (i == 11)
      ;
    else if (i == 12)
      ;
    else
      System.out.println("not 10, 11, or 12");
  }


}
