// Copyright 2011 Google Inc. All Rights Reserved.



/**
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class NegativeCases {

  public static void negativeCase1() {
    int i = 10;
    if (i == 10) {
      System.out.println("foo");
    }
    i++;
  }

  public static void negativeCase2() {
    int i = 0;
    for (;;) {
      if (i > 10) {
        break;
      }
      i++;
    }
  }


}
