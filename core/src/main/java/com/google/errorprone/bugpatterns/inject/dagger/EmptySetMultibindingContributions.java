/*
 * Copyright 2016 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Flags.Flag;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.Name;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** @author gak@google.com (Gregory Kick) */
@BugPattern(
    name = "EmptySetMultibindingContributions",
    summary =
        "@Multibinds is a more efficient and declarative mechanism for ensuring that a set"
            + " multibinding is present in the graph.",
    severity = WARNING)
public final class EmptySetMultibindingContributions extends BugChecker
    implements MethodTreeMatcher {
  private static final Matcher<AnnotationTree> HAS_DAGGER_ONE_MODULE_ARGUMENT =
      anyOf(
          hasArgumentWithValue("injects", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("staticInjections", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("overrides", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("addsTo", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("complete", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("library", Matchers.<ExpressionTree>anything()));

  /** We're just not going to worry about Collections.EMPTY_SET. */
  private static final Matcher<ExpressionTree> COLLECTIONS_EMPTY_SET =
      MethodMatchers.staticMethod().onClass(Collections.class.getCanonicalName()).named("emptySet");

  private static final Matcher<ExpressionTree> IMMUTABLE_SETS_OF =
      MethodMatchers.staticMethod()
          .onClassAny(
              ImmutableSet.class.getCanonicalName(), ImmutableSortedSet.class.getCanonicalName())
          .named("of")
          .withParameters();

  private static final Matcher<ExpressionTree> SET_CONSTRUCTORS =
      anyOf(
          noArgSetConstructor(HashSet.class),
          noArgSetConstructor(LinkedHashSet.class),
          noArgSetConstructor(TreeSet.class));

  @SuppressWarnings("rawtypes")
  private static Matcher<ExpressionTree> noArgSetConstructor(Class<? extends Set> setClass) {
    return MethodMatchers.constructor().forClass(setClass.getCanonicalName()).withParameters();
  }

  private static final Matcher<ExpressionTree> SET_FACTORY_METHODS =
      anyOf(
          setFactory("newHashSet"),
          setFactory("newLinkedHashSet"),
          setFactory("newConcurrentHashSet"));

  private static Matcher<ExpressionTree> setFactory(String factoryName) {
    return MethodMatchers.staticMethod()
        .onClass(Sets.class.getCanonicalName())
        .named(factoryName)
        .withParameters();
  }

  private static final Matcher<ExpressionTree> ENUM_SET_NONE_OF =
      MethodMatchers.staticMethod().onClass(EnumSet.class.getCanonicalName()).named("noneOf");

  private static final Matcher<ExpressionTree> EMPTY_SET =
      anyOf(
          COLLECTIONS_EMPTY_SET,
          IMMUTABLE_SETS_OF,
          SET_CONSTRUCTORS,
          SET_FACTORY_METHODS,
          ENUM_SET_NONE_OF);

  private static final Matcher<MethodTree> DIRECTLY_RETURNS_EMPTY_SET =
      Matchers.singleStatementReturnMatcher(EMPTY_SET);

  private static final Matcher<MethodTree> RETURNS_EMPTY_SET =
      new Matcher<MethodTree>() {
        @Override
        public boolean matches(MethodTree method, VisitorState state) {
          List<? extends VariableTree> parameters = method.getParameters();
          if (!parameters.isEmpty()) {
            return false;
          }
          return DIRECTLY_RETURNS_EMPTY_SET.matches(method, state);
        }
      };

  private static final Matcher<Tree> ANNOTATED_WITH_PRODUCES_OR_PROVIDES =
      anyOf(hasAnnotation("dagger.Provides"), hasAnnotation("dagger.producers.Produces"));

  private static final Matcher<MethodTree> CAN_BE_A_MULTIBINDS_METHOD =
      allOf(
          ANNOTATED_WITH_PRODUCES_OR_PROVIDES,
          hasAnnotation("dagger.multibindings.ElementsIntoSet"),
          RETURNS_EMPTY_SET);

  @Override
  public Description matchMethod(MethodTree method, VisitorState state) {
    if (!CAN_BE_A_MULTIBINDS_METHOD.matches(method, state)) {
      return NO_MATCH;
    }

    JCClassDecl enclosingClass = ASTHelpers.findEnclosingNode(state.getPath(), JCClassDecl.class);

    // Check to see if this is in a Dagger 1 module b/c it doesn't support @Multibinds
    for (JCAnnotation annotation : enclosingClass.getModifiers().getAnnotations()) {
      if (ASTHelpers.getSymbol(annotation.getAnnotationType())
              .getQualifiedName()
              .contentEquals("dagger.Module")
          && HAS_DAGGER_ONE_MODULE_ARGUMENT.matches(annotation, state)) {
        return NO_MATCH;
      }
    }

    return fixByModifyingMethod(state, enclosingClass, method);
  }

  private Description fixByModifyingMethod(
      VisitorState state, JCClassDecl enclosingClass, MethodTree method) {
    JCModifiers methodModifiers = ((JCMethodDecl) method).getModifiers();
    String replacementModifiersString = createReplacementMethodModifiers(state, methodModifiers);

    JCModifiers enclosingClassModifiers = enclosingClass.getModifiers();
    String enclosingClassReplacementModifiersString =
        createReplacementClassModifiers(state, enclosingClassModifiers);

    SuggestedFix.Builder fixBuilder =
        SuggestedFix.builder()
            .addImport("dagger.multibindings.Multibinds")
            .replace(methodModifiers, replacementModifiersString)
            .replace(method.getBody(), ";");
    fixBuilder =
        (enclosingClassModifiers.pos == -1)
            ? fixBuilder.prefixWith(enclosingClass, enclosingClassReplacementModifiersString)
            : fixBuilder.replace(enclosingClassModifiers, enclosingClassReplacementModifiersString);
    return describeMatch(method, fixBuilder.build());
  }

  private String createReplacementMethodModifiers(VisitorState state, JCModifiers modifiers) {
    ImmutableList.Builder<String> modifierStringsBuilder =
        ImmutableList.<String>builder().add("@Multibinds");

    for (JCAnnotation annotation : modifiers.annotations) {
      Name annotationQualifiedName = ASTHelpers.getSymbol(annotation).getQualifiedName();
      if (!(annotationQualifiedName.contentEquals("dagger.Provides")
          || annotationQualifiedName.contentEquals("dagger.producers.Produces")
          || annotationQualifiedName.contentEquals("dagger.multibindings.ElementsIntoSet"))) {
        modifierStringsBuilder.add(state.getSourceForNode(annotation));
      }
    }

    EnumSet<Flag> methodFlags = Flags.asFlagSet(modifiers.flags);
    methodFlags.remove(Flags.Flag.STATIC);
    methodFlags.remove(Flags.Flag.FINAL);
    methodFlags.add(Flags.Flag.ABSTRACT);

    for (Flag flag : methodFlags) {
      modifierStringsBuilder.add(flag.toString());
    }

    return Joiner.on(' ').join(modifierStringsBuilder.build());
  }

  private String createReplacementClassModifiers(
      VisitorState state, JCModifiers enclosingClassModifiers) {
    ImmutableList.Builder<String> classModifierStringsBuilder = ImmutableList.builder();

    for (JCAnnotation annotation : enclosingClassModifiers.annotations) {
      classModifierStringsBuilder.add(state.getSourceForNode(annotation));
    }

    EnumSet<Flag> classFlags = Flags.asFlagSet(enclosingClassModifiers.flags);
    classFlags.remove(Flags.Flag.FINAL);
    classFlags.add(Flags.Flag.ABSTRACT);
    for (Flag flag : classFlags) {
      classModifierStringsBuilder.add(flag.toString());
    }

    return Joiner.on(' ').join(classModifierStringsBuilder.build());
  }
}
