// Copyright 2011 Google Inc. All Rights Reserved.

import com.google.common.collect.Ordering;
import java.util.Comparator;

/**
 * Positive test cases for theOrdering.from(new Comparator<T>() { ... }) check
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PositiveCases {

  public static void positiveCase1() {
    Ordering<String> ord = Ordering.from(new Comparator<String>() {
      @Override
      public int compare(String first, String second) {
        int compare = first.length() - second.length();
        return (compare != 0) ? compare : first.compareTo(second);
      }
    });
  }

}
