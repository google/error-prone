/*
 * Copyright 2023 The Error Prone Authors.
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

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.Tree;

/** An abstract class that detects use of the unsafe APIs. */
public abstract class AbstractBanUnsafeAPIChecker extends BugChecker {

  protected <T extends Tree> Description matchHelper(
      T tree, VisitorState state, Matcher<T> matcher) {
    if (state.errorProneOptions().isTestOnlyTarget() || !matcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Description.Builder description = buildDescription(tree);

    return description.build();
  }
}
