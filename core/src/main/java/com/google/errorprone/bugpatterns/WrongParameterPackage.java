/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;

import javax.lang.model.element.ElementKind;


/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
@BugPattern(name = "ParameterPackage", summary = "Method parameter has wrong package", explanation =
    "Method does not override method in superclass due to wrong package for "
    + "parameter. For example, defining a method void foo(alpha.Foo x) when the superclass "
    + "contains a method void foo(beta.Foo x). The defined method was probably meant to "
    + "override the superclass method.", 
    category = JDK, 
    severity = ERROR, 
    maturity = EXPERIMENTAL)
public class WrongParameterPackage extends DescribingMatcher<MethodTree> {

  private MethodSymbol supermethod;
  
  @Override
  public boolean matches(MethodTree tree, VisitorState state) {
    MethodSymbol method = (MethodSymbol) ASTHelpers.getSymbol(tree);
    ClassSymbol classSym = method.enclClass();
    TypeSymbol superClass = classSym.getSuperclass().tsym;

    for (Symbol s : superClass.members().getElementsByName(method.name)) {
      if (s.getKind() == ElementKind.METHOD) {
        MethodSymbol supermethod = (MethodSymbol) s;

        // if this method actually overrides the supermethod, then it's correct and not a match.
        if (method.overrides(supermethod, superClass, state.getTypes(), true)) {
          return false;
        }

        // if this doesn't have the right number of parameters, look at other ones.
        if (supermethod.params.size() != method.params.size()) {
          continue;
        }

        for (int x = 0; x < method.params.size(); x++) {
          Type methodParamType = method.params.get(x).type;
          Type supermethodParamType = supermethod.params.get(x).type;
          if (methodParamType.tsym.name.contentEquals(supermethodParamType.tsym.name)
              && !state.getTypes().isSameType(methodParamType, supermethodParamType)) {
            this.supermethod = supermethod;
            return true;
          }
        }

      }
    }
    return false;
  }

  @Override
  public Description describe(MethodTree tree, VisitorState state) {
    SuggestedFix fix = new SuggestedFix();

    MethodSymbol method = (MethodSymbol) ASTHelpers.getSymbol(tree);
    
    if (supermethod == null){
      throw new IllegalStateException("Matching supermethod was not found");
    }

    for (int x = 0; x < method.params.size(); x++) {
      Type methodParamType = method.params.get(x).type;
      Type supermethodParamType = supermethod.params.get(x).type;
      if (methodParamType.tsym.name.contentEquals(supermethodParamType.tsym.name)
          && !state.getTypes().isSameType(methodParamType, supermethodParamType)) {
        VariableTree param = tree.getParameters().get(x);

        // TODO(scottjohnson): Name is most likely more qualified than necessary.
        Name replacement = supermethodParamType.tsym.getQualifiedName();
        fix.replace(param, replacement.toString() + " " + param.getName().toString());
      }
    }

    return new Description(tree, getDiagnosticMessage(), fix);
  }

  /**
   * Scanner for WrongParameterPackage
   * 
   * @author scottjohnson@google.com (Scott Johnson)
   */
  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<MethodTree> wrongParameterPackage = new WrongParameterPackage();

    @Override
    public Void visitMethod(MethodTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, wrongParameterPackage);
      return super.visitMethod(node, visitorState);
    }
  }

}
