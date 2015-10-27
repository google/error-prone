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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.common.base.Optional;
import com.google.common.collect.ClassToInstanceMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Negative test cases for {@link CollectionIncompatibleType}.
 */
public class CollectionIncompatibleTypeNegativeCases {

  /* Tests for API coverage */

  public void collection(Collection<String> collection1, Collection<String> collection2) {
    collection1.contains("ok");
    collection1.remove("ok");
    collection1.containsAll(collection2);
    collection1.removeAll(collection2);
    collection1.retainAll(collection2);
  }

  public void collectionSubtype(ArrayList<String> arrayList1, ArrayList<String> arrayList2) {
    arrayList1.contains("ok");
    arrayList1.remove("ok");
    arrayList1.containsAll(arrayList2);
    arrayList1.removeAll(arrayList2);
    arrayList1.retainAll(arrayList2);
  }

  public boolean deque(Deque<String> deque) {
    boolean result = deque.removeFirstOccurrence("ok");
    return result && deque.removeLastOccurrence("ok");
  }

  public boolean dequeSubtype(LinkedList<String> linkedList) {
    boolean result = linkedList.removeFirstOccurrence("ok");
    return result && linkedList.removeLastOccurrence("ok");
  }

  public int dictionary(Dictionary<String, Integer> dictionary) {
    int result = dictionary.get("ok");
    return result + dictionary.remove("ok");
  }

  public int dictionarySubtype(Hashtable<String, Integer> hashtable) {
    int result = hashtable.get("ok");
    return result + hashtable.remove("ok");
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

  public int stack(Stack<String> stack) {
    return stack.search("ok");
  }

  private static class MyStack<E> extends Stack<E> {}

  public int stackSubtype(MyStack<String> myStack) {
    return myStack.search("ok");
  }

  public int vector(Vector<String> vector) {
    int result = vector.indexOf("ok", 0);
    return result + vector.lastIndexOf("ok", 0);
  }

  public int vectorSubtype(Stack<String> stack) {
    int result = stack.indexOf("ok", 0);
    return result + stack.lastIndexOf("ok", 0);
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

  interface Interface1 {}
  interface Interface2 {}
  private static class NonFinalClass {}

  public boolean bothInterfaces(Collection<Interface1> collection, Interface2 iface2) {
    return collection.contains(iface2);
  }

  public boolean oneInterfaceAndOneNonFinalClass(
      Collection<Interface1> collection, NonFinalClass nonFinalClass) {
    return collection.contains(nonFinalClass);
  }

  public boolean oneNonFinalClassAndOneInterface(
      Collection<NonFinalClass> collection, Interface1 iface) {
    return collection.contains(iface);
  }
  
  public void methodArgHasSubtypeTypeArgument(
      Collection<Number> collection1, Collection<Integer> collection2) {
    collection1.containsAll(collection2);
  }
  
  public void methodArgHasSuperTypeArgument(
      Collection<Integer> collection1, Collection<Number> collection2) {
    collection1.containsAll(collection2);
  }
  
  public void methodArgHasWildcardTypeArgument(
      Collection<? extends Number> collection1, Collection<? extends Integer> collection2) {
    collection1.containsAll(collection2);
  }
  
  public void methodArgCastToCollectionWildcard(
      Collection<Integer> collection1, Collection<String> collection2) {
    collection1.containsAll((Collection<?>) collection2);
  }

}
