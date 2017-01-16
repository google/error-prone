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
