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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.util.ASTHelpers.getAnnotationsWithSimpleName;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ImmutableRefactoring",
    summary = "Refactors uses of the JSR 305 @Immutable to Error Prone's annotation",
    severity = SUGGESTION)
public class ImmutableRefactoring extends BugChecker implements CompilationUnitTreeMatcher {
  private final WellKnownMutability wellKnownMutability;

  @Inject
  ImmutableRefactoring(WellKnownMutability wellKnownMutability) {
    this.wellKnownMutability = wellKnownMutability;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableChecker immutableChecker =
        new ImmutableChecker(
            wellKnownMutability,
            ImmutableSet.of(
                javax.annotation.concurrent.Immutable.class.getName(),
                com.google.errorprone.annotations.Immutable.class.getName()));
    Optional<? extends ImportTree> immutableImport =
        tree.getImports().stream()
            .filter(
                i -> {
                  Symbol s = ASTHelpers.getSymbol(i.getQualifiedIdentifier());
                  return s != null
                      && s.getQualifiedName()
                          .contentEquals(javax.annotation.concurrent.Immutable.class.getName());
                })
            .findFirst();
    if (!immutableImport.isPresent()) {
      return Description.NO_MATCH;
    }
    Set<ClassTree> notOk = new HashSet<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree node, Void unused) {
        if (!ASTHelpers.hasAnnotation(
            node, javax.annotation.concurrent.Immutable.class.getName(), state)) {
          return super.visitClass(node, null);
        }
        boolean violator =
            immutableChecker.matchClass(
                    node,
                    VisitorState.createConfiguredForCompilation(
                            state.context,
                            description -> notOk.add(node),
                            ImmutableMap.of(),
                            state.errorProneOptions())
                        .withPath(getCurrentPath()))
                != Description.NO_MATCH;
        if (violator) {
          notOk.add(node);
        }
        return super.visitClass(node, null);
      }
    }.scan(state.getPath(), null);

    SuggestedFix.Builder fixBuilder =
        SuggestedFix.builder()
            .removeImport(javax.annotation.concurrent.Immutable.class.getName())
            .addImport(com.google.errorprone.annotations.Immutable.class.getName());
    for (ClassTree classTree : notOk) {
      getAnnotationsWithSimpleName(classTree.getModifiers().getAnnotations(), "Immutable")
          .forEach(fixBuilder::delete);
      fixBuilder.prefixWith(
          classTree,
          "// This class was annotated with javax.annotation.concurrent.Immutable, but didn't seem"
              + " to be provably immutable."
              + "\n");
    }
    return describeMatch(immutableImport.get(), fixBuilder.build());
  }
}
