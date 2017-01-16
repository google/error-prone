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
