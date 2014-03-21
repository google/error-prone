package com.google.errorprone.bugpatterns;

import java.util.Properties;

public class CollectionIncompatibleTypeOutOfBounds {
  public void test() {
    Properties properties = new Properties();
    properties.get("");
  }
}
