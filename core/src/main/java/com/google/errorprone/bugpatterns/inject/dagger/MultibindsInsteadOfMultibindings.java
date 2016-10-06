/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.DAGGER;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.inject.dagger.Util.CAN_HAVE_ABSTRACT_BINDING_METHODS;
import static com.google.errorprone.bugpatterns.inject.dagger.Util.findAnnotation;
import static com.google.errorprone.bugpatterns.inject.dagger.Util.makeConcreteClassAbstract;
import static com.google.errorprone.fixes.SuggestedFixes.addValuesToAnnotationArgument;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasMethod;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Name;

/** Refactors nested {@code @Multibindings} interfaces into {@code @Multibinds} methods. */
@BugPattern(
  name = "MultibindsInsteadOfMultibindings",
  summary = "`@Multibinds` is the new way to declare multibindings.",
  explanation =
      "Nested `@Multibindings` interfaces are being replaced by `@Multibinds` methods in a module.",
  category = DAGGER,
  severity = ERROR
)
public class MultibindsInsteadOfMultibindings extends BugChecker implements ClassTreeMatcher {

  /** Matches {@link dagger.Multibindings @Multibindings}-annotated interfaces. */
  private static final Matcher<Tree> IS_MULTIBINDINGS_INTERFACE =
      hasAnnotation("dagger.Multibindings");

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ImmutableList<ClassTree> multibindingsInterfaces = multibindingsInterfaces(tree, state);
    if (multibindingsInterfaces.isEmpty()) {
      return Description.NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    Optional<Fix> moveMethodsUp = moveMethodsUp(tree, multibindingsInterfaces, state);
    if (moveMethodsUp.isPresent()) {
      description.addFix(moveMethodsUp.get());
    }
    description.addFix(includeNestedModules(tree, multibindingsInterfaces, state));

    return description.build();
  }

  private ImmutableList<ClassTree> multibindingsInterfaces(ClassTree tree, VisitorState state) {
    ImmutableList.Builder<ClassTree> multibindingsInterfaces = ImmutableList.builder();
    for (Tree member : tree.getMembers()) {
      if (IS_MULTIBINDINGS_INTERFACE.matches(member, state)) {
        // @Multibindings can only be applied to interfaces, so this cast is safe.
        multibindingsInterfaces.add((ClassTree) member);
      }
    }
    return multibindingsInterfaces.build();
  }

  /**
   * A fix that replaces {@link dagger.Multibindings @Multibindings} interfaces in a module with
   * individual {@link dagger.multinbindings.Multibinds @Multibinds} methods in the enclosing
   * module.
   *
   * <p>Absent if the enclosing module has instance {@link dagger.Provides @Provides} or {@link
   * dagger.producers.Produces @Produces} methods, or if the new methods would have the same name as
   * other methods in the module.
   */
  // TODO(dpb): No blank line after last method.
  private Optional<Fix> moveMethodsUp(
      ClassTree module, ImmutableList<ClassTree> multibindingsInterfaces, VisitorState state) {
    if (!CAN_HAVE_ABSTRACT_BINDING_METHODS.matches(module, state)) {
      return Optional.absent();
    }
    SuggestedFix.Builder moveMethodsUp =
        SuggestedFix.builder()
            .removeImport("dagger.Multibindings")
            .addImport("dagger.multibindings.Multibinds")
            .merge(makeConcreteClassAbstract(module, state));
    Set<Name> methodNames = new HashSet<>();
    for (ClassTree multibindingsInterface : multibindingsInterfaces) {
      ImmutableList.Builder<String> newMethods = ImmutableList.builder();
      for (MethodTree methodTree : getInterfaceMethods(multibindingsInterface)) {
        if (moduleHasMethodWithSameName(module, state, methodTree)
            || !methodNames.add(methodTree.getName())) {
          return Optional.absent();
        }
        newMethods.add(multibindsMethod(module, methodTree, state));
      }
      moveMethodsUp.replace(multibindingsInterface, Joiner.on('\n').join(newMethods.build()));
    }
    return Optional.of(moveMethodsUp.build());
  }

  private String multibindsMethod(ClassTree module, MethodTree methodTree, VisitorState state) {
    return String.format(
        "%s %s %s();",
        Joiner.on(' ')
            .skipNulls()
            .join(
                "@Multibinds",
                state.getSourceForNode(methodTree.getModifiers()),
                module.getKind().equals(INTERFACE) ? null : ABSTRACT),
        methodTree.getReturnType(),
        methodTree.getName());
  }

  private boolean moduleHasMethodWithSameName(
      ClassTree module, VisitorState state, MethodTree methodTree) {
    return hasMethod(methodIsNamed(methodTree.getName().toString())).matches(module, state);
  }

  /**
   * A fix that transforms {@link dagger.Multibindings @Multibindings} interfaces into {@link
   * dagger.Module @Module}s or {@link dagger.producers.ProducersModule @ProducersModule}s and adds
   * them to the {@code includes} argument of the enclosing {@link dagger.Module @Module} or {@link
   * dagger.producers.ProducersModule @ProducersModule}.
   */
  private Fix includeNestedModules(
      ClassTree module, ImmutableList<ClassTree> multibindingsInterfaces, VisitorState state) {
    SuggestedFix.Builder includeNestedModules =
        SuggestedFix.builder()
            .removeImport("dagger.Multibindings")
            .addImport("dagger.multibindings.Multibinds");

    AnnotationTree moduleAnnotation = moduleAnnotation(module);
    ImmutableList.Builder<String> moduleClassLiteralsBuilder = ImmutableList.builder();
    for (ClassTree multibindingsInterface : multibindingsInterfaces) {
      includeNestedModules.replace(
          findAnnotation("dagger.Multibindings", multibindingsInterface).get(),
          "@" + state.getSourceForNode(moduleAnnotation.getAnnotationType()));

      for (MethodTree methodTree : getInterfaceMethods(multibindingsInterface)) {
        includeNestedModules.prefixWith(methodTree, "@Multibinds ");
      }

      moduleClassLiteralsBuilder.add(
          module.getSimpleName() + "." + multibindingsInterface.getSimpleName() + ".class");
    }
    includeNestedModules.merge(
        addValuesToAnnotationArgument(
            moduleAnnotation, "includes", moduleClassLiteralsBuilder.build(), state));
    return includeNestedModules.build();
  }

  private AnnotationTree moduleAnnotation(ClassTree module) {
    return findAnnotation("dagger.Module", module)
        .or(findAnnotation("dagger.producers.ProducerModule", module))
        .get();
  }

  private ImmutableList<MethodTree> getInterfaceMethods(ClassTree tree) {
    ImmutableList.Builder<MethodTree> methods = ImmutableList.builder();
    for (Tree member : tree.getMembers()) {
      if (member.getKind().equals(METHOD)) {
        methods.add((MethodTree) member);
      }
    }
    return methods.build();
  }
}
