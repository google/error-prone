---
title: RemoveUnusedImports
summary: Unused import
layout: bugpattern
category: JDK
severity: SUGGESTION
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Unused import

## Suppression
Suppress false positives by adding an `@SuppressWarnings("RemoveUnusedImports")` annotation to the enclosing element.

----------

### Positive examples
__RemoveUnusedImportsPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

// BUG: Diagnostic contains: Unused import
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static com.google.common.base.Preconditions.checkNotNull;

// BUG: Diagnostic contains: Unused import
import java.util.ArrayList;
import java.util.Collection;
// BUG: Diagnostic contains: Unused import
import java.util.Collections;
import java.util.HashSet;
// BUG: Diagnostic contains: Unused import
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.Set;
import java.util.UUID;

/**
 * This is a bunch of nonsense that uses stuff in {@link java.util}.  We don't use
 * {@link RandomAccess}.
 *
 * <p>This is a random reference to a member:  {@link Map#get}
 *
 * @see NavigableSet
 */
public class RemoveUnusedImportsPositiveCases {
  private final Object object;

  RemoveUnusedImportsPositiveCases(Object object) {
    this.object = checkNotNull(object);
  }

  Set<UUID> someMethod(Collection<UUID> collection) {
    if (collection.isEmpty()) {
      return emptySet();
    }
    return new HashSet<>(collection);
  }
}
{% endhighlight %}

