/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.util.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    altNames = "JavaLangClash",
    summary = "Never reuse class names from java.lang",
    severity = WARNING,
    tags = StandardTags.STYLE)
public class AvoidCommonTypeNames extends BugChecker
    implements ClassTreeMatcher, TypeParameterTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    Name simpleName = ((JCClassDecl) tree).getSimpleName();
    return check(tree, simpleName, state);
  }

  @Override
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    return check(tree, ((JCTypeParameter) tree).getName(), state);
  }

  private static final ImmutableSet<String> IGNORED =
      ImmutableSet.of(
          // java.lang.Compiler is deprecated for removal in 9 and should not be used, so we don't
          // care if other types named 'Compiler' are declared
          "Compiler",
          // References to java.lang.Module are rare, and it is a commonly used simple name
          "Module");

  private static final Supplier<ImmutableMap<String, ClassSymbol>> NAMES_TO_AVOID =
      memoize(
          state -> {
            Symtab symtab = state.getSymtab();
            return stream(
                    symtab
                        .enterPackage(symtab.java_base, state.getNames().java_lang)
                        .members()
                        .getSymbols(
                            s ->
                                s instanceof ClassSymbol
                                    && s.getModifiers().contains(PUBLIC)
                                    && !IGNORED.contains(s.getSimpleName().toString())))
                .collect(toImmutableMap(s -> s.getSimpleName().toString(), s -> (ClassSymbol) s));
          });

  private Description check(Tree tree, Name simpleName, VisitorState state) {
    ClassSymbol clashingSymbol = NAMES_TO_AVOID.get(state).get(simpleName.toString());
    if (clashingSymbol != null) {
      Symbol symbol = getSymbol(tree);
      // If the symbol isn't the java.lang class, there's a clash.
      if (symbol != null && !clashingSymbol.equals(symbol)) {
        return buildDescription(tree)
            .setMessage("%s clashes with %s", symbol, clashingSymbol)
            .build();
      }
    }
    return NO_MATCH;
  }
}
