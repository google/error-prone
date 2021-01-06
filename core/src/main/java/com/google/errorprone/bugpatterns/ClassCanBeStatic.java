/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * @author alexloh@google.com (Alex Loh)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    name = "ClassCanBeStatic",
    summary = "Inner class is non-static but does not reference enclosing class",
    severity = WARNING,
    tags = {StandardTags.STYLE, StandardTags.PERFORMANCE})
public class ClassCanBeStatic extends BugChecker implements ClassTreeMatcher {

  private static final String REFASTER_ANNOTATION =
      "com.google.errorprone.refaster.annotation.BeforeTemplate";

  @Override
  public Description matchClass(final ClassTree tree, final VisitorState state) {
    final ClassSymbol currentClass = ASTHelpers.getSymbol(tree);
    if (currentClass == null || !currentClass.hasOuterInstance()) {
      return NO_MATCH;
    }
    if (currentClass.getNestingKind() != NestingKind.MEMBER) {
      // local or anonymous classes can't be static
      return NO_MATCH;
    }
    switch (currentClass.owner.enclClass().getNestingKind()) {
      case TOP_LEVEL:
        break;
      case MEMBER:
        // class is nested inside an inner class, so it can't be static
        if (currentClass.owner.enclClass().hasOuterInstance()) {
          return NO_MATCH;
        }
        break;
      case LOCAL:
      case ANONYMOUS:
        // members of local and anonymous classes can't be static
        return NO_MATCH;
    }
    if (tree.getExtendsClause() != null
        && ASTHelpers.getType(tree.getExtendsClause()).tsym.hasOuterInstance()) {
      return NO_MATCH;
    }
    if (CanBeStaticAnalyzer.referencesOuter(tree, currentClass, state)) {
      return NO_MATCH;
    }
    if (tree.getMembers().stream().anyMatch(m -> hasAnnotation(m, REFASTER_ANNOTATION, state))) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFixes.addModifiers(tree, state, Modifier.STATIC));
  }
}
