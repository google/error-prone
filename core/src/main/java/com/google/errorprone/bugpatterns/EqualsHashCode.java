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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.hashCodeMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.instanceHashCodeInvocation;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.singleStatementReturnMatcher;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.overridesObjectEquals;
import static com.google.errorprone.util.ASTHelpers.overridesObjectHashCode;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.google.errorprone.util.AnnotationNames.AUTO_ONE_OF_ANNOTATION;
import static com.google.errorprone.util.AnnotationNames.AUTO_VALUE_ANNOTATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/**
 * Classes that override {@link Object#equals} should also override {@link Object#hashCode}, and
 * vice versa.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    summary = "Classes that override equals should also override hashCode.",
    severity = ERROR,
    tags = StandardTags.FRAGILE_CODE)
public class EqualsHashCode extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<MethodTree> NON_TRIVIAL_EQUALS =
      allOf(
          equalsMethodDeclaration(), not(singleStatementReturnMatcher(instanceEqualsInvocation())));

  private static final Matcher<ExpressionTree> SUPER_HASH_CODE =
      allOf(
          instanceHashCodeInvocation(),
          (tree, state) ->
              getReceiver(tree) instanceof IdentifierTree id
                  && id.getName().equals(state.getNames()._super));

  private static final Matcher<ExpressionTree> IDENTITY_HASH_CODE_THIS =
      allOf(
          staticMethod()
              .onClass("java.lang.System")
              .named("identityHashCode")
              .withParameters("java.lang.Object"),
          (tree, state) ->
              tree instanceof MethodInvocationTree invocation
                  && stripParentheses(getOnlyElement(invocation.getArguments()))
                      instanceof IdentifierTree id
                  && id.getName().equals(state.getNames()._this));

  private static final Matcher<MethodTree> NON_TRIVIAL_HASH_CODE =
      allOf(
          hashCodeMethodDeclaration(),
          not(singleStatementReturnMatcher(anyOf(SUPER_HASH_CODE, IDENTITY_HASH_CODE_THIS))),
          not(EqualsHashCode::singleStatementThrow));

  private final boolean checkHashCodeWithoutEquals;

  @Inject
  EqualsHashCode(ErrorProneFlags flags) {
    this.checkHashCodeWithoutEquals =
        flags.getBoolean("EqualsHashCode:CheckHashCodeWithoutEquals").orElse(true);
  }

  public EqualsHashCode() {
    this(ErrorProneFlags.empty());
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    TypeSymbol symbol = getSymbol(classTree);
    if (symbol.getKind() != ElementKind.CLASS) {
      return NO_MATCH;
    }
    // don't flag java.lang.Object
    if (symbol == state.getSymtab().objectType.tsym) {
      return NO_MATCH;
    }
    MethodTree equals = null;
    MethodTree hashCode = null;
    for (Tree member : classTree.getMembers()) {
      if (!(member instanceof MethodTree methodTree)) {
        continue;
      }
      if (NON_TRIVIAL_EQUALS.matches(methodTree, state)) {
        equals = methodTree;
      } else if (checkHashCodeWithoutEquals && NON_TRIVIAL_HASH_CODE.matches(methodTree, state)) {
        hashCode = methodTree;
      }
    }
    if (equals != null && !overridesObjectHashCode(symbol, state)) {
      return isSuppressed(equals, state) ? NO_MATCH : describeMatch(equals);
    }
    if (hashCode != null && !overridesObjectEquals(symbol, state)) {
      // Exempt abstract hashCode() declarations on @AutoValue/@AutoOneOf classes (such as
      // @Memoized hashCode()): AutoValue still generates both equals and hashCode over all
      // properties in the implementation subclass. Concrete hashCode() overrides suppress
      // AutoValue's hashCode generation while leaving its generated all-property equals in place,
      // so we still flag them unless equals is also overridden or the warning is suppressed.
      if (getSymbol(hashCode).getModifiers().contains(Modifier.ABSTRACT)
          && (hasAnnotation(symbol, AUTO_VALUE_ANNOTATION, state)
              || hasAnnotation(symbol, AUTO_ONE_OF_ANNOTATION, state))) {
        return NO_MATCH;
      }
      return isSuppressed(hashCode, state)
          ? NO_MATCH
          : buildDescription(hashCode)
              .setMessage("Classes that override hashCode should also override equals.")
              .build();
    }
    return NO_MATCH;
  }

  private static boolean singleStatementThrow(MethodTree methodTree, VisitorState state) {
    BlockTree body = methodTree.getBody();
    return body != null
        && body.getStatements().size() == 1
        && getOnlyElement(body.getStatements()) instanceof ThrowTree;
  }
}
