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

package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

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

/** Negative test cases for {@link CollectionIncompatibleType}. */
public class CollectionIncompatibleTypeNegativeCases {

  /* Tests for API coverage */

  public boolean collection(Collection<String> collection1, Collection<String> collection2) {
    boolean result = collection1.contains("ok");
    result &= collection1.contains(null);
    result &= collection1.remove("ok");
    result &= collection1.remove(null);
    result &= collection1.containsAll(collection2);
    result &= collection1.containsAll(null);
    result &= collection1.removeAll(collection2);
    result &= collection1.removeAll(null);
    result &= collection1.retainAll(collection2);
    return result && collection1.retainAll(null);
  }

  public boolean collectionSubtype(ArrayList<String> arrayList1, ArrayList<String> arrayList2) {
    boolean result = arrayList1.contains("ok");
    result &= arrayList1.contains(null);
    result &= arrayList1.remove("ok");
    result &= arrayList1.remove(null);
    result &= arrayList1.containsAll(arrayList2);
    result &= arrayList1.containsAll(null);
    result &= arrayList1.removeAll(arrayList2);
    result &= arrayList1.removeAll(null);
    result &= arrayList1.retainAll(arrayList2);
    return result && arrayList1.retainAll(null);
  }

  public boolean deque(Deque<String> deque) {
    boolean result = deque.removeFirstOccurrence("ok");
    result &= deque.removeFirstOccurrence(null);
    result &= deque.removeLastOccurrence("ok");
    return result && deque.removeLastOccurrence(null);
  }

  public boolean dequeSubtype(LinkedList<String> linkedList) {
    boolean result = linkedList.removeFirstOccurrence("ok");
    result &= linkedList.removeFirstOccurrence(null);
    result &= linkedList.removeLastOccurrence("ok");
    return result && linkedList.removeLastOccurrence(null);
  }

  public int dictionary(Dictionary<String, Integer> dictionary) {
    int result = dictionary.get("ok");
    result += dictionary.get(null);
    result += dictionary.remove("ok");
    return result + dictionary.remove(null);
  }

  public int dictionarySubtype(Hashtable<String, Integer> hashtable) {
    int result = hashtable.get("ok");
    result += hashtable.get(null);
    result += hashtable.remove("ok");
    return result + hashtable.remove(null);
  }

  public int list() {
    List<String> list = new ArrayList<String>();
    int result = list.indexOf("ok");
    result += list.indexOf(null);
    result += list.lastIndexOf("ok");
    return result + list.lastIndexOf(null);
  }

  public int listSubtype() {
    ArrayList<String> arrayList = new ArrayList<>();
    int result = arrayList.indexOf("ok");
    result += arrayList.indexOf(null);
    result += arrayList.lastIndexOf("ok");
    return result + arrayList.lastIndexOf(null);
  }

  public boolean map() {
    Map<Integer, String> map = new HashMap<>();
    String result = map.get(1);
    result = map.getOrDefault(1, "hello");
    boolean result2 = map.containsKey(1);
    result2 = map.containsValue("ok");
    result2 &= map.containsValue(null);
    result = map.remove(1);
    return result2;
  }

  public boolean mapSubtype() {
    ConcurrentNavigableMap<Integer, String> concurrentNavigableMap = new ConcurrentSkipListMap<>();
    String result = concurrentNavigableMap.get(1);
    boolean result2 = concurrentNavigableMap.containsKey(1);
    result2 &= concurrentNavigableMap.containsValue("ok");
    result2 &= concurrentNavigableMap.containsValue(null);
    result = concurrentNavigableMap.remove(1);
    return result2;
  }

  public int stack(Stack<String> stack) {
    int result = stack.search("ok");
    return result + stack.search(null);
  }

  private static class MyStack<E> extends Stack<E> {}

  public int stackSubtype(MyStack<String> myStack) {
    int result = myStack.search("ok");
    return result + myStack.search(null);
  }

  public int vector(Vector<String> vector) {
    int result = vector.indexOf("ok", 0);
    result += vector.indexOf(null, 0);
    result += vector.lastIndexOf("ok", 0);
    return result + vector.lastIndexOf(null, 0);
  }

  public int vectorSubtype(Stack<String> stack) {
    int result = stack.indexOf("ok", 0);
    result += stack.indexOf(null, 0);
    result += stack.lastIndexOf("ok", 0);
    return result + stack.lastIndexOf(null, 0);
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
