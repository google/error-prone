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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Positive test cases for {@link CollectionIncompatibleType}. */
public class CollectionIncompatibleTypePositiveCases {

  /* Tests for API coverage */

  public void collection(Collection<Integer> collection1, Collection<String> collection2) {
    // BUG: Diagnostic contains: Argument '"bad"' should not be passed to this method
    // its type String is not compatible with its collection's type argument Integer
    collection1.contains("bad");
    // BUG: Diagnostic contains:
    collection1.remove("bad");
    // BUG: Diagnostic contains: Argument 'collection2' should not be passed to this method
    // its type Collection<String> has a type argument String that is not compatible with its
    // collection's type argument Integer
    collection1.containsAll(collection2);
    // BUG: Diagnostic contains:
    collection1.removeAll(collection2);
    // BUG: Diagnostic contains:
    collection1.retainAll(collection2);
  }

  public void collectionSubtype(ArrayList<Integer> arrayList1, ArrayList<String> arrayList2) {
    // BUG: Diagnostic contains: Argument '"bad"' should not be passed to this method
    // its type String is not compatible with its collection's type argument Integer
    arrayList1.contains("bad");
    // BUG: Diagnostic contains:
    arrayList1.remove("bad");
    // BUG: Diagnostic contains: Argument 'arrayList2' should not be passed to this method
    // its type ArrayList<String> has a type argument String that is not compatible with its
    // collection's type argument Integer
    arrayList1.containsAll(arrayList2);
    // BUG: Diagnostic contains:
    arrayList1.removeAll(arrayList2);
    // BUG: Diagnostic contains:
    arrayList1.retainAll(arrayList2);
  }

  public boolean deque(Deque<Integer> deque) {
    // BUG: Diagnostic contains:
    boolean result = deque.removeFirstOccurrence("bad");
    // BUG: Diagnostic contains:
    return result && deque.removeLastOccurrence("bad");
  }

  public boolean dequeSubtype(LinkedList<Integer> linkedList) {
    // BUG: Diagnostic contains:
    boolean result = linkedList.removeFirstOccurrence("bad");
    // BUG: Diagnostic contains:
    return result && linkedList.removeLastOccurrence("bad");
  }

  public String dictionary(Dictionary<Integer, String> dictionary) {
    // BUG: Diagnostic contains:
    String result = dictionary.get("bad");
    // BUG: Diagnostic contains:
    return result + dictionary.remove("bad");
  }

  public String dictionarySubtype(Hashtable<Integer, String> hashtable) {
    // BUG: Diagnostic contains:
    String result = hashtable.get("bad");
    // BUG: Diagnostic contains:
    return result + hashtable.remove("bad");
  }

  public int list() {
    List<String> list = new ArrayList<String>();
    // BUG: Diagnostic contains:
    int result = list.indexOf(1);
    // BUG: Diagnostic contains:
    return result + list.lastIndexOf(1);
  }

  public void listSubtype() {
    ArrayList<String> arrayList = new ArrayList<>();
    // BUG: Diagnostic contains:
    int result = arrayList.indexOf(1);
    // BUG: Diagnostic contains:
    result = arrayList.lastIndexOf(1);
  }

  public boolean map() {
    Map<Integer, String> map = new HashMap<>();
    // BUG: Diagnostic contains:
    String result = map.get("bad");
    // BUG: Diagnostic contains:
    result = map.getOrDefault("bad", "soBad");
    // BUG: Diagnostic contains:
    boolean result2 = map.containsKey("bad");
    // BUG: Diagnostic contains:
    result2 = map.containsValue(1);
    // BUG: Diagnostic contains:
    result = map.remove("bad");
    return false;
  }

  public boolean mapSubtype() {
    ConcurrentNavigableMap<Integer, String> concurrentNavigableMap = new ConcurrentSkipListMap<>();
    // BUG: Diagnostic contains:
    String result = concurrentNavigableMap.get("bad");
    // BUG: Diagnostic contains:
    boolean result2 = concurrentNavigableMap.containsKey("bad");
    // BUG: Diagnostic contains:
    result2 = concurrentNavigableMap.containsValue(1);
    // BUG: Diagnostic contains:
    result = concurrentNavigableMap.remove("bad");
    return false;
  }

  public int stack(Stack<Integer> stack) {
    // BUG: Diagnostic contains:
    return stack.search("bad");
  }

  private static class MyStack<E> extends Stack<E> {}

  public int stackSubtype(MyStack<Integer> myStack) {
    // BUG: Diagnostic contains:
    return myStack.search("bad");
  }

  public int vector(Vector<Integer> vector) {
    // BUG: Diagnostic contains:
    int result = vector.indexOf("bad", 0);
    // BUG: Diagnostic contains:
    return result + vector.lastIndexOf("bad", 0);
  }

  public int vectorSubtype(Stack<Integer> stack) {
    // BUG: Diagnostic contains:
    int result = stack.indexOf("bad", 0);
    // BUG: Diagnostic contains:
    return result + stack.lastIndexOf("bad", 0);
  }

  /* Tests for behavior */

  public boolean errorMessageUsesSimpleNames(Collection<Integer> collection) {
    // BUG: Diagnostic contains: Argument '"bad"' should not be passed to this method
    // its type String is not compatible with its collection's type argument Integer
    return collection.contains("bad");
  }

  private static class Date {}

  public boolean errorMessageUsesFullyQualifedNamesWhenSimpleNamesAreTheSame(
      Collection<java.util.Date> collection1, Collection<Date> collection2) {
    // BUG: Diagnostic contains: Argument 'new Date()' should not be passed to this method
    // its type
    // com.google.errorprone.bugpatterns.collectionincompatibletype.testdata.CollectionIncompatibleTypePositiveCases.Date is not compatible with its collection's type argument java.util.Date
    return collection1.contains(new Date());
  }

  public boolean boundedWildcard() {
    Collection<? extends Date> collection = new ArrayList<>();
    // BUG: Diagnostic contains:
    return collection.contains("bad");
  }

  private static class Pair<A, B> {
    public A first;
    public B second;
  }

  public boolean declaredTypeVsExpressionType(Pair<Integer, String> pair, List<Integer> list) {
    // BUG: Diagnostic contains:
    return list.contains(pair.second);
  }

  public String subclassHasDifferentTypeParameters(ClassToInstanceMap<String> map, String s) {
    // BUG: Diagnostic contains:
    return map.get(s);
  }

  private static class MyArrayList extends ArrayList<Integer> {}

  public void methodArgumentIsSubclassWithDifferentTypeParameters(
      Collection<String> collection, MyArrayList myArrayList) {
    // BUG: Diagnostic contains:
    collection.containsAll(myArrayList);
  }

  private static class IncompatibleBounds<K extends String, V extends Number> {
    private boolean function(Map<K, V> map, K key) {
      // BUG: Diagnostic contains:
      return map.containsValue(key);
    }
  }

  interface Interface {}

  private static final class FinalClass1 {}

  private static final class FinalClass2 {}

  private static class NonFinalClass1 {}

  private static class NonFinalClass2 {}

  public boolean oneInterfaceAndOneFinalClass(
      Collection<Interface> collection, FinalClass1 finalClass1) {
    // BUG: Diagnostic contains:
    return collection.contains(finalClass1);
  }

  public boolean oneFinalClassAndOneInterface(Collection<FinalClass1> collection, Interface iface) {
    // BUG: Diagnostic contains:
    return collection.contains(iface);
  }

  public boolean bothNonFinalClasses(
      Collection<NonFinalClass1> collection, NonFinalClass2 nonFinalClass2) {
    // BUG: Diagnostic contains:
    return collection.contains(nonFinalClass2);
  }

  public boolean bothFinalClasses(Collection<FinalClass1> collection, FinalClass2 finalClass2) {
    // BUG: Diagnostic contains:
    return collection.contains(finalClass2);
  }

  public boolean oneNonFinalClassAndOneFinalClass(
      Collection<NonFinalClass1> collection, FinalClass1 finalClass1) {
    // BUG: Diagnostic contains:
    return collection.contains(finalClass1);
  }

  public boolean oneFinalClassAndOneNonFinalClass(
      Collection<FinalClass1> collection, NonFinalClass1 nonFinalClass1) {
    // BUG: Diagnostic contains:
    return collection.contains(nonFinalClass1);
  }
}
