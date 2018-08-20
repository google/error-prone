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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.Optional;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ImmutableRefactoring",
    summary = "Refactors uses of the JSR 305 @Immutable to Error Prone's annotation",
    severity = SUGGESTION,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ImmutableRefactoring extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableChecker immutableChecker =
        new ImmutableChecker(
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
    boolean[] ok = {true};
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree node, Void unused) {
        if (immutableChecker.matchClass(node, createVisitorState().withPath(getCurrentPath()))
            != Description.NO_MATCH) {
          ok[0] = false;
        }
        return super.visitClass(node, null);
      }

      @Override
      public Void visitNewClass(NewClassTree node, Void unused) {
        if (immutableChecker.matchNewClass(node, createVisitorState().withPath(getCurrentPath()))
            != Description.NO_MATCH) {
          ok[0] = false;
        }
        return super.visitNewClass(node, null);
      }

      private VisitorState createVisitorState() {
        return new VisitorState(
            state.context,
            (Description description) -> {
              ok[0] = false;
            },
            ImmutableMap.of(),
            state.errorProneOptions());
      }
    }.scan(state.getPath(), null);
    if (!ok[0]) {
      // TODO(cushon): replace non-compliant @Immutable annotations with javadoc
      return Description.NO_MATCH;
    }
    return describeMatch(
        immutableImport.get(),
        SuggestedFix.builder()
            .removeImport(javax.annotation.concurrent.Immutable.class.getName())
            .addImport(com.google.errorprone.annotations.Immutable.class.getName())
            .build());
  }
}
