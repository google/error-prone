/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.beans.Introspector.decapitalize;

import com.google.auto.value.AutoValue;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

/** See summary for details. */
@BugPattern(
    summary =
        "AutoValue instances should not usually contain boxed types that are not Nullable. We"
            + " recommend removing the unnecessary boxing.",
    severity = WARNING)
public class AutoValueBoxedValues extends BugChecker implements ClassTreeMatcher {
  private static final Matcher<MethodTree> ABSTRACT_MATCHER = hasModifier(Modifier.ABSTRACT);
  private static final Matcher<MethodTree> STATIC_MATCHER = hasModifier(Modifier.STATIC);

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!hasAnnotation(tree, AutoValue.class.getName(), state) || isSuppressed(tree, state)) {
      return NO_MATCH;
    }

    Optional<ClassTree> builderClass = findBuilderClass(tree, state);

    // Identify and potentially fix the getters.
    ImmutableList<Getter> getters = handleGetterMethods(tree, state, builderClass);

    // If we haven't modified any getter, it's ok to stop.
    if (getters.stream().allMatch(getter -> getter.fix().isEmpty())) {
      return NO_MATCH;
    }

    // Handle the Builder class, if there is one. Otherwise handle the factory methods.
    if (builderClass.isPresent()) {
      handleSetterMethods(builderClass.get(), state, getters);
    } else {
      handleFactoryMethods(tree, state, getters);
    }

    getters.stream()
        .filter(getter -> !getter.fix().isEmpty())
        .forEach(getter -> state.reportMatch(describeMatch(getter.method(), getter.fix().build())));

    return NO_MATCH;
  }

  /**
   * Returns the {@link List} of {@link Getter} in the {@link AutoValue} class along with the fixes
   * to be applied.
   *
   * @param classTree The {@link AutoValue} class tree.
   * @param state The visitor state.
   * @return The list of {@link Getter} in the class.
   */
  private ImmutableList<Getter> handleGetterMethods(
      ClassTree classTree, VisitorState state, Optional<ClassTree> builderClass) {
    return classTree.getMembers().stream()
        .filter(MethodTree.class::isInstance)
        .map(memberTree -> (MethodTree) memberTree)
        .filter(
            methodTree ->
                ABSTRACT_MATCHER.matches(methodTree, state)
                    && methodTree.getParameters().isEmpty()
                    && !isToBuilderMethod(methodTree, state, builderClass))
        .map(methodTree -> maybeFixGetter(methodTree, state))
        .collect(toImmutableList());
  }

  /**
   * Converts the {@link MethodTree} of a getter to a {@link Getter}. If the getter needs to be
   * fixed, it returns a {@link Getter} with a non-empty {@link SuggestedFix}.
   */
  private Getter maybeFixGetter(MethodTree method, VisitorState state) {
    Getter getter = Getter.of(method);
    Type type = getType(method.getReturnType());
    if (!isSuppressed(method, state)
        && !hasNullableAnnotation(method)
        && isBoxedPrimitive(state, type)) {
      suggestRemoveUnnecessaryBoxing(method.getReturnType(), state, type, getter.fix());
    }
    return getter;
  }

  /**
   * Identifies and fixes the setters in the {@link AutoValue.Builder} class.
   *
   * @param classTree The {@link AutoValue.Builder} class tree.
   * @param state The visitor state.
   * @param getters The {@link List} of {@link Getter} in the {@link AutoValue} class.
   */
  private void handleSetterMethods(ClassTree classTree, VisitorState state, List<Getter> getters) {
    classTree.getMembers().stream()
        .filter(MethodTree.class::isInstance)
        .map(memberTree -> (MethodTree) memberTree)
        .filter(
            methodTree ->
                ABSTRACT_MATCHER.matches(methodTree, state)
                    && methodTree.getParameters().size() == 1
                    && isSameType(getType(methodTree.getReturnType()), getType(classTree), state))
        .forEach(methodTree -> maybeFixSetter(methodTree, state, getters));
  }

  /** Given a setter, it tries to apply a fix if the corresponding getter was also fixed. */
  private void maybeFixSetter(MethodTree methodTree, VisitorState state, List<Getter> getters) {
    if (isSuppressed(methodTree, state)) {
      return;
    }
    boolean allGettersPrefixed = allGettersPrefixed(getters);
    Optional<Getter> fixedGetter =
        getters.stream()
            .filter(
                getter ->
                    !getter.fix().isEmpty()
                        && matchGetterAndSetter(getter.method(), methodTree, allGettersPrefixed))
            .findAny();
    if (fixedGetter.isPresent()) {
      var parameter = methodTree.getParameters().get(0);
      Type type = getType(parameter);
      if (isBoxedPrimitive(state, type) && !hasNullableAnnotation(parameter)) {
        suggestRemoveUnnecessaryBoxing(parameter.getType(), state, type, fixedGetter.get().fix());
      }
    }
  }

  /**
   * Identifies and fixes the factory method in the {@link AutoValue} class.
   *
   * <p>This method only handles the case of "trivial" factory methods, i.e. methods that have one
   * argument for each getter in the class and contains a single return statement passing all the
   * arguments to the constructor in the same order.
   *
   * @param classTree The {@link AutoValue} class tree.
   * @param state The visitor state.
   * @param getters The {@link List} of {@link Getter} in the {@link AutoValue} class.
   */
  private void handleFactoryMethods(ClassTree classTree, VisitorState state, List<Getter> getters) {
    Optional<MethodTree> trivialFactoryMethod =
        classTree.getMembers().stream()
            .filter(MethodTree.class::isInstance)
            .map(memberTree -> (MethodTree) memberTree)
            .filter(
                methodTree ->
                    STATIC_MATCHER.matches(methodTree, state)
                        && isSameType(
                            getType(methodTree.getReturnType()), getType(classTree), state)
                        && isTrivialFactoryMethod(methodTree, getters.size()))
            .findAny();
    if (trivialFactoryMethod.isEmpty()) {
      return;
    }
    for (int idx = 0; idx < getters.size(); idx++) {
      Getter getter = getters.get(idx);
      if (!getter.fix().isEmpty()) {
        var parameter = trivialFactoryMethod.get().getParameters().get(idx);
        Type type = getType(parameter);
        if (isBoxedPrimitive(state, type) && !hasNullableAnnotation(parameter)) {
          suggestRemoveUnnecessaryBoxing(parameter.getType(), state, type, getter.fix());
        }
      }
    }
  }

  /** Returns true if the given tree has a {@code Nullable} annotation. */
  private static boolean hasNullableAnnotation(Tree tree) {
    return NullnessAnnotations.fromAnnotationsOn(getSymbol(tree)).orElse(null) == Nullness.NULLABLE;
  }

  /** Returns the primitive type corresponding to a boxed type. */
  private static Type unbox(VisitorState state, Type type) {
    return state.getTypes().unboxedType(type);
  }

  /** Returns true if the value of {@link Type} is a boxed primitive. */
  private static boolean isBoxedPrimitive(VisitorState state, Type type) {
    if (type.isPrimitive()) {
      return false;
    }
    Type unboxed = unbox(state, type);
    return unboxed != null && unboxed.getTag() != TypeTag.NONE && unboxed.getTag() != TypeTag.VOID;
  }

  private static Optional<ClassTree> findBuilderClass(ClassTree tree, VisitorState state) {
    return tree.getMembers().stream()
        .filter(
            memberTree ->
                memberTree instanceof ClassTree
                    && hasAnnotation(memberTree, AutoValue.Builder.class.getName(), state))
        .map(memberTree -> (ClassTree) memberTree)
        .findAny();
  }

  private static boolean isToBuilderMethod(
      MethodTree methodTree, VisitorState state, Optional<ClassTree> builderClass) {
    return builderClass.isPresent()
        && !STATIC_MATCHER.matches(methodTree, state)
        && isSameType(getType(methodTree.getReturnType()), getType(builderClass.get()), state);
  }

  private static boolean allGettersPrefixed(List<Getter> getters) {
    return getters.stream().allMatch(getter -> !getterPrefix(getter.method()).isEmpty());
  }

  private static String getterPrefix(MethodTree getterMethod) {
    String name = getterMethod.getName().toString();
    if (name.startsWith("get") && !name.equals("get")) {
      return "get";
    } else if (name.startsWith("is")
        && !name.equals("is")
        && getType(getterMethod.getReturnType()).getKind() == TypeKind.BOOLEAN) {
      return "is";
    }
    return "";
  }

  /** Returns true if the getter and the setter are for the same field. */
  private static boolean matchGetterAndSetter(
      MethodTree getter, MethodTree setter, boolean allGettersPrefixed) {
    String getterName = getter.getName().toString();
    if (allGettersPrefixed) {
      String prefix = getterPrefix(getter);
      getterName = decapitalize(getterName.substring(prefix.length()));
    }
    String setterName = setter.getName().toString();
    return getterName.equals(setterName)
        || setterName.equals(
            "set" + Ascii.toUpperCase(getterName.charAt(0)) + getterName.substring(1));
  }

  /**
   * Returns true if the method is a trivial factory method.
   *
   * <p>A trivial factory method is a static method that has one argument for each getter in the
   * class and contains a single return statement passing all the arguments to the constructor in
   * the same order.
   *
   * @param methodTree The method tree to be checked.
   * @param gettersCount The total number of getters in the class.
   * @return True if the method is a trivial factory method, false otherwise.
   */
  private static boolean isTrivialFactoryMethod(MethodTree methodTree, int gettersCount) {
    var params = methodTree.getParameters();
    var statements = methodTree.getBody().getStatements();

    // Trivial factory method must have one argument for each getter and a single return statement.
    if (params.size() != gettersCount
        || statements.size() != 1
        || !(statements.get(0) instanceof ReturnTree)) {
      return false;
    }
    // Trivial factory method must return a new instance.
    ReturnTree returnTree = (ReturnTree) statements.get(0);
    if (!(returnTree.getExpression() instanceof NewClassTree)) {
      return false;
    }
    // Trivial factory method must pass all the arguments to the constructor.
    NewClassTree newClassTree = (NewClassTree) returnTree.getExpression();
    if (newClassTree.getArguments().stream().anyMatch(r -> !(r instanceof IdentifierTree))) {
      return false;
    }
    // Compare the arguments passed to the method to those passed to the constructor.
    var paramsNames = params.stream().map(p -> p.getName().toString()).collect(toImmutableList());
    var constructorArgs =
        newClassTree.getArguments().stream()
            .map(r -> ((IdentifierTree) r).getName().toString())
            .collect(toImmutableList());
    return paramsNames.equals(constructorArgs);
  }

  /**
   * Suggests a fix to replace the boxed type with the unboxed type.
   *
   * <p>For example, if the type is `Integer`, the fix would be to replace `Integer` with `int`.
   *
   * @param tree The tree to be replaced, which can be either a return value or a parameter.
   * @param state The visitor state.
   * @param type The boxed type to be replaced.
   */
  private static void suggestRemoveUnnecessaryBoxing(
      Tree tree, VisitorState state, Type type, SuggestedFix.Builder fix) {
    fix.replace(tree, unbox(state, type).tsym.getSimpleName().toString());
  }

  @AutoValue
  abstract static class Getter {
    abstract MethodTree method();

    abstract SuggestedFix.Builder fix();

    static Getter of(MethodTree method) {
      return new AutoValue_AutoValueBoxedValues_Getter(method, SuggestedFix.builder());
    }
  }
}
