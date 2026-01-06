/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.emptyFix;

import com.google.common.cache.CacheLoader;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ExpressionTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "The result of CacheLoader#load must be non-null.", severity = WARNING)
public final class CacheLoaderNull extends AbstractAsyncTypeReturnsNull {
  public CacheLoaderNull() {
    super(CacheLoader.class);
  }

  @Override
  protected SuggestedFix provideFix(ExpressionTree tree) {
    // The default suggestion is immediateFuture(null), which doesn't make sense for CacheLoader.
    return emptyFix();
  }
}
