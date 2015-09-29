/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.common.base.Optional;
import com.google.common.collect.ClassToInstanceMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Negative test cases for {@link CollectionIncompatibleType}.
 */
public class CollectionIncompatibleTypeNegativeCases {
  
  /* Tests for API coverage */
  
  public boolean collection() {
    Collection<String> collection = new ArrayList<>();
    boolean result = collection.contains("ok");
    return result && collection.remove("ok");
  }

  public boolean collectionSubtype() {
    ArrayList<String> arrayList = new ArrayList<>();
    boolean result = arrayList.contains("ok");
    return result && arrayList.remove("ok");
  }

  public int list() {
    List<String> list = new ArrayList<String>();
    int result = list.indexOf("ok");
    return result + list.lastIndexOf("ok");
  }

  public void listSubtype() {
    ArrayList<String> arrayList = new ArrayList<>();
    int result = arrayList.indexOf("ok");
    result = arrayList.lastIndexOf("ok");
  }

  public boolean map() {
    Map<Integer, String> map = new HashMap<>();
    String result = map.get(1);
    boolean result2 = map.containsKey(1);
    result2 = map.containsValue("ok");
    result = map.remove(1);
    return false;
  }

  public boolean mapSubtype() {
    ConcurrentNavigableMap<Integer, String> concurrentNavigableMap = new ConcurrentSkipListMap<>();
    String result = concurrentNavigableMap.get(1);
    boolean result2 = concurrentNavigableMap.containsKey(1);
    result2 = concurrentNavigableMap.containsValue("ok");
    result = concurrentNavigableMap.remove(1);
    return false;
  }

  /* Tests for behavior */
  
  private class B extends Date {}
  
  public boolean argTypeExtendsContainedType() {
    Collection<Date> collection = new ArrayList<>();
    return collection.contains(new B());
  }
  
  public boolean containedTypeExtendsArgType() {
    Collection<String> collection = new ArrayList<>();
    Object actuallyAString = "ok";
    return collection.contains(actuallyAString);
  }
  
  public boolean boundedWildcard() {
    Collection<? extends Date> collection = new ArrayList<>();
    return collection.contains(new Date()) || collection.contains(new B());
  }

  public boolean unboundedWildcard() {
    Collection<?> collection = new ArrayList<>();
    return collection.contains("ok") || collection.contains(new Object());
  }

  public boolean rawType() {
    Collection collection = new ArrayList();
    return collection.contains("ok");
  }

  private class DoesntExtendCollection<E> {
    public boolean contains(Object o) {
      return true;
    }
  }
  public boolean doesntExtendCollection() {
    DoesntExtendCollection<String> collection = new DoesntExtendCollection<>();
    return collection.contains(new Date());
  }
  
  private static class Pair<A, B> {
    public A first;
    public B second;
  }
  
  public boolean declaredTypeVsExpressionType(Pair<Integer, String> pair, List<Integer> list) {
    return list.contains(pair.first);
  }

  public boolean containsParameterizedType(
      Collection<Class<? extends String>> collection, Class<?> clazz) {
    return collection.contains(clazz);
  }

  public boolean containsWildcard(Collection<String> collection, Optional<?> optional) {
    return collection.contains(optional.get());
  }

  public <T extends String> T subclassHasDifferentTypeParameters(
      ClassToInstanceMap<String> map, Class<T> klass) {
    return klass.cast(map.get(klass));
  }
  
  // Ensure we don't match Hashtable.contains and ConcurrentHashtable.contains because there is a
  // separate check, HashtableContains, specifically for them.
  public boolean hashtableContains() {
    Hashtable<Integer, String> hashtable = new Hashtable<>();
    ConcurrentHashMap<Integer, String> concurrentHashMap = new ConcurrentHashMap<>();
    return hashtable.contains(1) || concurrentHashMap.contains(1);
  }
  
  private static class MyHashMap<K extends Integer, V extends String> extends HashMap<K, V> {}
  
  public boolean boundedTypeParameters(MyHashMap<?, ?> myHashMap) {
    return myHashMap.containsKey(1);
  }
}
