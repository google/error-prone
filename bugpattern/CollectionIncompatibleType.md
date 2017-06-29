---
title: CollectionIncompatibleType
summary: Incompatible type as argument to Object-accepting Java collections method
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Querying a collection for an element it cannot possibly contain is almost
certainly a bug.

In a generic collection type, query methods such as `Map.get(Object)` and
`Collection.remove(Object)` accept a parameter that identifies a potential
element to *look for* in that collection. This check reports cases where this
element *cannot* be present because its type and the collection's generic
element type are "incompatible." A typical example:

```java
Set<Long> values = ...
if (values.contains(1)) { ... }
```

This code looks reasonable, but there's a problem: The `Set` contains `Long`
instances, but the argument to `contains` is an `Integer`. Because no instance
can be of type `Integer` and of type `Long` at the same time, the `contains`
check always fails. This is clearly not what the developer intended.

Why does the collection API permit this kind of mistake? Why not declare the
method as `contains(E)`? After all, that is what the collections API does for
methods that *store* an element in the collection: They require that passed type
be strictly *assignable to* the collection's element type. For example:

```java
void addIntegerOne(Set<? extends Number> numbers) {
  numbers.add(1); // won't compile
}
```

The code above rightly won't compile, because `numbers` *might* be a (for
example) `Set<Long>`, and adding an `Integer` value would corrupt it.

But this restriction is necessary only for methods that insert elements. Methods
that only query or remove elements cannot corrupt the collection:

```java
void removeIntegerOne(Set<? extends Number> numbers) {
  numbers.remove(1); // should compile (and does)
}
```

In this case, the `Integer` `1` might be contained in `numbers`, and should be
removed if it is, but if `numbers` is a `Set<Long>`, no harm is done.

We'd like to define `contains` in a way that rejects the bad call but permits
the good one. But Java's type system is not powerful enough. Our solution is
static analysis.

The specific restriction we would like to express for the two types is not
assignability, but "compatibility". Informally, we mean that it must at least be
*possible* for some instance to be of both types. Formally, we require that a
"casting conversion" exist between the types as defined by [JLS 5.5.1]
(https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.5.1).

The result is that the method can be defined as `contains(Object)`, permitting
the "good" call above, but that Error Prone will give errors for incompatible
arguments, preventing the "bad."

### Footnote: Would requiring `E` have been better?

We might say: Sure, a buggy `remove` call can't corrupt a collection. And sure,
someone might want to pass an `Object` reference that happens to contain an `E`.
But isn't that a low standard for an API? We don't normally write code that way:

```java
void throwIfUnchecked(Object throwable) { // no "need" to require Throwable
  if (throwable instanceof RuntimeException) {
    throw (RuntimeException) throwable;
  }
  if (throwable instanceof Error) {
    throw (Error) throwable;
  }
}
```

Such code would invite bugs. To avoid that, we require a `Throwable`. Users who
have an `Object` reference that might be a `Throwable` can test `instanceof` and
cast. So why not require the same thing in the collections API?

Of course, we can't really change the API of `Collection`. But if we were
designing a similar API, what would we do -- require `E` or accept any `Object`?

The burden of proof falls on accepting `Object`, since doing so permits buggy
code. And we're not going to settle for "it occasionally saves users a cast."

*The main reason to accept `Object` is to permit a fast, type-safe `Set.equals`
implementation.*

(`equals` is actually just one example of the general problem, which arises with
many uses of wildcards wildcards. Once you've read the following, consider the
problem of implementing `Collection.removeAll(Collection<?>)` without
`contains(Object)`. Then consider how the problem would exist even if the
signature were `removeAll(Collection<? extends E>)`. The `removeAll` problem is
at least "solvable" by changing the signature to `removeAll(Collection<E>)`, but
that signature may reject useful calls.)

Here's how: `equals` necessarily accepts a plain `Object`. It can test whether
that `Object` is a `Set`, but it can't know the element type it was originally
declared with. In short, `equals` has to operate on a `Set<?>`.

If `contains` were to require an `E`, `equals` would be in trouble because it
doesn't know what `E` is. In particular, it wouldn't be able to call
`otherSet.contains(myElement)` for any of its elements.

It would have only two options: It could copy the entire other `Set` into a
`Set<Object>`, or it could perform an unchecked cast. Copying is wasteful, so in
practice, `equals` would need an unchecked cast. This is probably acceptable,
but we might feel strange for defining an API that can be implemented only by
performing unchecked casts.

Does a cleaner implementation (and occasional convenience to callers) outweigh
the bugs that accepting `Object` enables? That's a tough question. The good news
is that this Error Prone check gives you some of the best of both worlds.

### Footnote: Collection containing an incompatible type

It is technically possible for a `Set<Integer>` to contain a `String` element,
but only if an `unchecked` warning was earlier ignored or improperly suppressed.
Such practice should never be treated as acceptable, so it makes no practical
difference to our arguments above.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CollectionIncompatibleType")` annotation to the enclosing element.

----------

### Positive examples
__CollectionIncompatibleTypePositiveCases.java__

{% highlight java %}
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
{% endhighlight %}

### Negative examples
__CollectionIncompatibleTypeNegativeCases.java__

{% highlight java %}
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
{% endhighlight %}

