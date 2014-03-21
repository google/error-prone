package com.google.errorprone.bugpatterns;

import java.util.HashMap;

public class CollectionIncompatibleTypeClassCast<K, V> extends HashMap<K, V> {
  public void test(K k) {
    get(k);
  }
}
