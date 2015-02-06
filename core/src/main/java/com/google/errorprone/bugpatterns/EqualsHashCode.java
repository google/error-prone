/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.matchers.MethodVisibility.Visibility.PUBLIC;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * Classes that override equals should also override hashCode.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "EqualsHashCode",
    summary = "Classes that override equals should also override hashCode.",
    explanation = "The contact for Object.hashCode states that if two objects are equal, then"
    + " calling the hashCode() method on each of the two objects must produce the same result."
    + " Implementing equals() but not hashCode() causes broken behaviour when trying to store the"
    + " object in a collection. See Effective Java 3.9 for more information and a discussion of"
    + " how to correctly implement hashCode().",
    category = JDK, severity = WARNING, maturity = MATURE)
public class EqualsHashCode extends BugChecker
    implements ClassTreeMatcher{

  public static Matcher<MethodTree> methodHasArity(final int arity) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        return methodTree.getParameters().size() == arity;
      }
    };
  }

  private static final Matcher<MethodTree> EQUALS_MATCHER = allOf(
      methodIsNamed("equals"),
      methodHasVisibility(PUBLIC),
      methodReturns(BOOLEAN_TYPE),
      methodHasParameters(variableType(isSameType(OBJECT_TYPE))));

  private static final Matcher<MethodTree> HASHCODE_MATCHER = allOf(
      methodIsNamed("hashCode"),
      methodHasVisibility(PUBLIC),
      methodReturns(INT_TYPE),
      methodHasArity(0));

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {

    MethodTree equals = null;
    MethodTree hashCode = null;

    for (Tree member : classTree.getMembers()) {
      if (member instanceof MethodTree) {
        MethodTree methodTree = (MethodTree) member;
        if (EQUALS_MATCHER.matches(methodTree, state)) {
          equals = methodTree;
        } else if (HASHCODE_MATCHER.matches(methodTree, state)) {
          hashCode = methodTree;
        }
      }
    }

    if (equals != null && hashCode == null) {
      return describeMatch(equals);
    }

    return Description.NO_MATCH;
  }
}
