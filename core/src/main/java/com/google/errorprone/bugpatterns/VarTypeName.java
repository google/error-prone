/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import javax.lang.model.element.Name;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "VarTypeName",
  summary = "`var` should not be used as a type name.",
  severity = ERROR
)
public class VarTypeName extends BugChecker implements ClassTreeMatcher, TypeParameterTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return check(tree, tree.getSimpleName());
  }

  @Override
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    return check(tree, tree.getName());
  }

  private Description check(Tree tree, Name name) {
    return name.contentEquals("var") ? describeMatch(tree) : NO_MATCH;
  }
}
