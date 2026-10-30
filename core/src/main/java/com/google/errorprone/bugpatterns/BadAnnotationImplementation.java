/*
 * Copyright 2014 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.suppliers.Suppliers.ANNOTATION_TYPE;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.overridesObjectEquals;
import static com.google.errorprone.util.ASTHelpers.overridesObjectHashCode;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.ENUM;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.lang.annotation.Annotation;

/**
 * Checker that ensures implementations of {@link Annotation} override equals and hashCode.
 * Otherwise, the implementation inherits equals and hashCode from {@link Object}, and those do not
 * meet the contract specified by the {@link Annotation} interface.
 */
@BugPattern(
    summary =
        "Classes that implement Annotation must override equals and hashCode. Consider "
            + "using AutoAnnotation instead of implementing Annotation by hand.",
    severity = ERROR)
public class BadAnnotationImplementation extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ClassTree> CLASS_TREE_MATCHER =
      allOf(anyOf(kindIs(CLASS), kindIs(ENUM)), isSubtypeOf(ANNOTATION_TYPE));

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!CLASS_TREE_MATCHER.matches(classTree, state)) {
      return Description.NO_MATCH;
    }

    // If this is an enum that is trying to implement Annotation, give a special error message.
    if (classTree.getKind() == Kind.ENUM) {
      return buildDescription(classTree)
          .setMessage(
              "Enums cannot correctly implement Annotation because their equals and hashCode "
                  + "methods are final. Consider using AutoAnnotation instead of implementing "
                  + "Annotation by hand.")
          .build();
    }

    ClassSymbol symbol = getSymbol(classTree);
    if (!overridesObjectEquals(symbol, state) || !overridesObjectHashCode(symbol, state)) {
      return describeMatch(classTree);
    }

    return Description.NO_MATCH;
  }
}
