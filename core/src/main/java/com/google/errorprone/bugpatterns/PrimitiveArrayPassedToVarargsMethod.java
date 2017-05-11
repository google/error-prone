/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.List;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
  name = "PrimitiveArrayPassedToVarargsMethod",
  summary = "Passing a primitive array to a varargs method is usually wrong",
  explanation =
      "When you pass a primitive array as the only argument to a varargs method, the "
          + "primitive array is autoboxed into a single-element Object array. This is usually "
          + "not what was intended.",
  category = JDK,
  severity = WARNING
)
public class PrimitiveArrayPassedToVarargsMethod extends BugChecker
    implements MethodInvocationTreeMatcher {

  /**
   * Assuming the argument in the varargs position is a single one of type int[], here is the truth
   * table: Param type Should return Why int... false Exact type match int[]... false Exact type
   * match for the array element type T... true Will cause boxing Object... true Will cause boxing
   */
  private static final Matcher<MethodInvocationTree> isVarargs =
      new Matcher<MethodInvocationTree>() {
        @Override
        public boolean matches(MethodInvocationTree t, VisitorState state) {
          Symbol symbol = ASTHelpers.getSymbol(t);
          if (!(symbol instanceof MethodSymbol)) {
            return false;
          }
          MethodSymbol methodSymbol = (MethodSymbol) symbol;

          // Bail out quickly if the method is not varargs
          if (!methodSymbol.isVarArgs()) {
            return false;
          }

          // Last param must be varags
          List<VarSymbol> params = methodSymbol.getParameters();
          int varargsPosition = params.length() - 1;
          ArrayType varargsParamType = (ArrayType) params.last().type;

          // Is the argument at the varargsPosition the only varargs argument and a primitive array?
          JCMethodInvocation methodInvocation = (JCMethodInvocation) t;
          List<JCExpression> arguments = methodInvocation.getArguments();
          Types types = state.getTypes();
          if (arguments.size() != params.length()) {
            return false;
          }
          Type varargsArgumentType = arguments.get(varargsPosition).type;
          if (!types.isArray(varargsArgumentType)
              || !types.elemtype(varargsArgumentType).isPrimitive()) {
            return false;
          }

          // Do the param and argument types actually match? i.e. can boxing even happen?
          return !(types.isSameType(varargsParamType, varargsArgumentType)
              || types.isSameType(varargsParamType.getComponentType(), varargsArgumentType));
        }
      };

  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    return isVarargs.matches(t, state) ? describeMatch(t) : NO_MATCH;
  }
}
