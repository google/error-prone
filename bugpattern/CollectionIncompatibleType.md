---
title: CollectionIncompatibleType
summary: Incompatible type as argument to Object-accepting Java collections method
layout: bugpattern
category: JDK
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Various Collections APIs have methods that take `Object` rather than
the proper type parameter. If an argument is given whose type is incompatible
with the appropriate type argument, it's unlikely that the method call is
going to do what you intended.

To learn why Collections APIs have these non-generic methods, see Kevin
Bourillion's blog post, "[Why does Set.contains() take an Object, not an E?]
(http://smallwig.blogspot.com/2007/12/why-does-setcontains-take-object-not-e.html)".

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

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Positive test cases for {@link CollectionIncompatibleType}.
 */
public class CollectionIncompatibleTypePositiveCases {
  
  public boolean collection() {
    Collection<Integer> collection = new ArrayList<>();
    // BUG: Diagnostic contains:
    boolean result = collection.contains("bad");
    // BUG: Diagnostic contains:
    return result && collection.remove("bad");
  }

  public boolean collectionSubtype() {
    ArrayList<Integer> arrayList = new ArrayList<>();
    // BUG: Diagnostic contains:
    boolean result = arrayList.contains("bad");
    // BUG: Diagnostic contains:
    return result && arrayList.remove("bad");
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

  public boolean boundedWildcard() {
    Collection<? extends Date> collection = new ArrayList<>();
    // BUG: Diagnostic contains:
    return collection.contains("bad");
  }
  
  private static class MyHashMap<K extends Integer, V extends String> extends HashMap<K, V> {}
  
  public boolean boundedTypeParameters(MyHashMap<?, ?> myHashMap) {
    // BUG: Diagnostic contains:
    return myHashMap.containsKey("bad");
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

package com.google.errorprone.bugpatterns;

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
  
  public boolean extendsContainedType() {
    Collection<Date> collection = new ArrayList<>();
    return collection.contains(new B());
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
{% endhighlight %}

