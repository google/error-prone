/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import javax.lang.model.element.Name;

/** @author irogers@google.com (Ian Rogers) */
@BugPattern(
    name = "SwigMemoryLeak",
    summary = "SWIG generated code that can't call a C++ destructor will leak memory",
    category = JDK,
    severity = WARNING)
public class SwigMemoryLeak extends BugChecker implements LiteralTreeMatcher {
  private static final Matcher<MethodTree> ENCLOSING_CLASS_HAS_FINALIZER =
      Matchers.enclosingClass(Matchers.hasMethod(Matchers.methodIsNamed("finalize")));

  @Override
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    // Is there a literal matching the message SWIG uses to indicate a
    // destructor problem?
    if (tree.getValue() == null
        || !tree.getValue().equals("C++ destructor does not have public access")) {
      return NO_MATCH;
    }
    // Is it within a delete method?
    MethodTree enclosingMethodTree =
        ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    Name name = enclosingMethodTree.getName();
    if (!name.contentEquals("delete")) {
      return NO_MATCH;
    }
    // Does the enclosing class lack a finalizer?
    if (ENCLOSING_CLASS_HAS_FINALIZER.matches(enclosingMethodTree, state)) {
      return NO_MATCH;
    }
    return buildDescription(tree).build();
  }
}
