/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.checkers;

import com.google.errorprone.RefactoringVisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.Tree;

/**
 * In addition to matching an AST node, also gives a message and/or suggested fix.
 * @author alexeagle@google.com (Alex Eagle)
 */
public abstract class RefactoringMatcher<T extends Tree> implements Matcher<T> {

  /**
   * Additional description of a matched AST node, useful in reporting the error or performing an
   * automated refactoring.
   * @param t an AST node which matched this matcher
   * @param state the shared state
   * @return the description
   */
  public abstract Refactor refactor(T t, RefactoringVisitorState state);

  public static class Refactor {

    /**
     * The AST node which matched
     */
    public Tree node;

    /**
     * Printed by the compiler when a match is found in interactive use.
     */
    public String message;

    /**
     * Replacements to suggest in an error message or use in automated refactoring
     */
    public SuggestedFix suggestedFix;

    public Refactor(Tree node, String message, SuggestedFix suggestedFix) {
      this.message = message;
      this.suggestedFix = suggestedFix;
      this.node = node;
    }
  }
}
