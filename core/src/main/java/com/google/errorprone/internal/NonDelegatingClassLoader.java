/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.internal;

import com.google.common.collect.ImmutableSet;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * A non-delegating {@link java.net.URLClassLoader} that searches its own resource path
 * <i>before</i> the runtime classpath, reversing the usual classloader delegation model. This makes
 * it possible to override runtime classes.
 *
 * <p>The classloader is also given the classloader from the environment it is created in, and a
 * whitelist of classes that it should load from that environment. This list must include anything
 * that crosses classloader boundaries (for example, the input or return types of the callback).
 *
 * @author cushon@google.com
 */
public class NonDelegatingClassLoader extends URLClassLoader {
  private final ClassLoader original;
  private final ImmutableSet<String> whiteList;

  public static NonDelegatingClassLoader create(Set<String> whiteList, URLClassLoader original) {
    return create(whiteList, original.getURLs(), original);
  }

  public static NonDelegatingClassLoader create(
      Set<String> whiteList, URL[] urls, ClassLoader original) {
    return new NonDelegatingClassLoader(original, urls, whiteList);
  }

  private NonDelegatingClassLoader(ClassLoader original, URL[] urls, Set<String> whiteList) {
    super(urls, null);
    this.original = original;
    this.whiteList = ImmutableSet.copyOf(whiteList);
  }

  @Override
  public Class<?> loadClass(String name, boolean complete) throws ClassNotFoundException {
    if (whiteList.contains(name)) {
      return original.loadClass(name);
    }

    try {
      synchronized (getClassLoadingLock(name)) {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
          return c;
        }
        return findClass(name);
      }
    } catch (ClassNotFoundException e) {
      return super.loadClass(name, complete);
    }
  }
}
