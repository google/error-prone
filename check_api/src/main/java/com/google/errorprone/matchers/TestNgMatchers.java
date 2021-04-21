/*
 * Copyright 2020 The Error Prone Authors.
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
package com.google.errorprone.matchers;

import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Matchers for code patterns which appear to be TestNG-based tests.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class TestNgMatchers {

  /**
   * Checks if a method, or any overridden method, is annotated with any annotation from the
   * org.testng package.
   */
  public static boolean hasTestNgAnnotation(MethodTree tree, VisitorState state) {
    MethodSymbol methodSym = getSymbol(tree);
    if (methodSym == null) {
      return false;
    }
    if (hasTestNgAttr(methodSym)) {
      return true;
    }
    return findSuperMethods(methodSym, state.getTypes()).stream()
        .anyMatch(TestNgMatchers::hasTestNgAttr);
  }

  /** Checks if a class is annotated with any annotation from the org.testng package. */
  public static boolean hasTestNgAnnotation(ClassTree tree) {
    ClassSymbol classSym = getSymbol(tree);
    return classSym != null && hasTestNgAttr(classSym);
  }

  /** Checks if a symbol has any attribute from the org.testng package. */
  private static boolean hasTestNgAttr(Symbol methodSym) {
    return methodSym.getRawAttributes().stream()
        .anyMatch(attr -> attr.type.tsym.getQualifiedName().toString().startsWith("org.testng."));
  }

  private TestNgMatchers() {}
}
