---
title: URLEqualsHashCode
summary: Creation of a Set/HashSet/HashMap of java.net.URL. equals() and hashCode() of java.net.URL class make blocking internet connections.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Equals and HashCode method of java.net.URL make blocking network calls. Either
use java.net.URI or if that isn't possible, use Collection<URL> or List<URL>.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("URLEqualsHashCode")` annotation to the enclosing element.

----------

### Positive examples
__URLEqualsHashCodePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.testdata;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Positive test cases for URLEqualsHashCode check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class URLEqualsHashCodePositiveCases {

  public void setOfURL() {
    // BUG: Diagnostic contains: java.net.URL
    Set<URL> urlSet = new HashSet<URL>();
  }

  public void setOfCompleteURL() {
    // BUG: Diagnostic contains: java.net.URL
    Set<java.net.URL> urlSet = new HashSet<java.net.URL>();
  }

  public void hashmapOfURL() {
    // BUG: Diagnostic contains: java.net.URL
    HashMap<URL, String> urlMap = new HashMap<URL, String>();
  }

  public void hashmapOfCompleteURL() {
    // BUG: Diagnostic contains: java.net.URL
    HashMap<java.net.URL, String> urlMap = new HashMap<java.net.URL, String>();
  }

  public void hashsetOfURL() {
    // BUG: Diagnostic contains: java.net.URL
    HashSet<URL> urlSet = new HashSet<URL>();
  }

  public void hashsetOfCompleteURL() {
    // BUG: Diagnostic contains: java.net.URL
    HashSet<java.net.URL> urlSet = new HashSet<java.net.URL>();
  }

  private static class ExtendedSet extends HashSet<java.net.URL> {
    // no impl.
  }

  public void hashSetExtendedClass() {
    // BUG: Diagnostic contains: java.net.URL
    HashSet extendedSet = new ExtendedSet();

    // BUG: Diagnostic contains: java.net.URL
    Set urlSet = new ExtendedSet();
  }

  private static class ExtendedMap extends HashMap<java.net.URL, String> {
    // no impl.
  }

  public void hashMapExtendedClass() {
    // BUG: Diagnostic contains: java.net.URL
    HashMap extendedMap = new ExtendedMap();

    // BUG: Diagnostic contains: java.net.URL
    Map urlMap = new ExtendedMap();
  }
}
{% endhighlight %}

### Negative examples
__URLEqualsHashCodeNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.testdata;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Negative test cases for URLEqualsHashCode check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class URLEqualsHashCodeNegativeCases {

  private static class Url {
    private Url() {
      // no impl
    }
  }

  // Set and HashSet of non-URL class.
  public void setOfUrl() {
    Set<Url> urlSet;
  }

  public void hashsetOfUrl() {
    HashSet<Url> urlSet;
  }

  // Collection(s) of type URL
  public void collectionOfURL() {
    Collection<URL> urlSet;
  }

  public void listOfURL() {
    List<URL> urlSet;
  }

  public void arraylistOfURL() {
    ArrayList<URL> urlSet;
  }

  public void hashmapWithURLAsValue() {
    HashMap<String, java.net.URL> stringToUrlMap;
  }

  private static class ExtendedMap extends HashMap<String, java.net.URL> {
    // no impl.
  }

  public void hashMapExtendedClass() {
    ExtendedMap urlMap;
  }
}
{% endhighlight %}

