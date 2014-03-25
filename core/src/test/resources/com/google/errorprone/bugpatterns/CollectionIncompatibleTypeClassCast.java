package com.google.errorprone.bugpatterns;

import java.util.HashMap;

/**
 * This is a regression test for Issue 222.
 */
public class CollectionIncompatibleTypeClassCast<K, V> extends HashMap<K, V> {
  public void test(K k) {
    get(k);
  }
}
