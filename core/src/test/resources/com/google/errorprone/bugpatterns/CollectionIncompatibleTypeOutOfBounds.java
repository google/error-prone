package com.google.errorprone.bugpatterns;

import java.util.Properties;

/**
 * This is a regression test for Issue 222.
 */
public class CollectionIncompatibleTypeOutOfBounds {
  public void test() {
    Properties properties = new Properties();
    properties.get("");
  }
}
