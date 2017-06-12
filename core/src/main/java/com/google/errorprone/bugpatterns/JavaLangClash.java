/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.Iterables.getFirst;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "JavaLangClash",
  category = JDK,
  summary = "Never reuse class names from java.lang",
  severity = WARNING,
  tags = StandardTags.STYLE
)
public class JavaLangClash extends BugChecker
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

  private Description check(Tree tree, Name simpleName, VisitorState state) {
    Symtab symtab = state.getSymtab();
    PackageSymbol javaLang =
        symtab.enterPackage(symtab.java_base, Names.instance(state.context).java_lang);
    Symbol other =
        getFirst(
            javaLang.members().getSymbolsByName(simpleName, s -> s.getModifiers().contains(PUBLIC)),
            null);
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (other == null || other.equals(symbol)) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(String.format("%s clashes with %s\n", symbol, other))
        .build();
  }
}
