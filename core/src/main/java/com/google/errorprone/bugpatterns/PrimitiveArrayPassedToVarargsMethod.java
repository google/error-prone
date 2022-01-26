/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import java.util.List;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
    summary = "Passing a primitive array to a varargs method is usually wrong",
    severity = WARNING)
public class PrimitiveArrayPassedToVarargsMethod extends BugChecker
    implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    return isVarargs(t, state) ? describeMatch(t) : NO_MATCH;
  }

  /**
   * Assuming the argument in the varargs position is a single one of type int[], here is the truth
   * table: Param type Should return Why int... false Exact type match int[]... false Exact type
   * match for the array element type T... true Will cause boxing Object... true Will cause boxing
   */
  private static boolean isVarargs(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return false;
    }

    // Bail out quickly if the method is not varargs
    if (!symbol.isVarArgs()) {
      return false;
    }

    // Last param must be varags
    List<VarSymbol> params = symbol.getParameters();
    int varargsPosition = params.size() - 1;
    ArrayType varargsParamType = (ArrayType) getLast(params).type;

    // Is the argument at the varargsPosition the only varargs argument and a primitive array?
    JCMethodInvocation methodInvocation = (JCMethodInvocation) tree;
    List<JCExpression> arguments = methodInvocation.getArguments();
    Types types = state.getTypes();
    if (arguments.size() != params.size()) {
      return false;
    }
    Type varargsArgumentType = arguments.get(varargsPosition).type;
    if (!types.isArray(varargsArgumentType) || !types.elemtype(varargsArgumentType).isPrimitive()) {
      return false;
    }

    // Do the param and argument types actually match? i.e. can boxing even happen?
    return !(types.isSameType(varargsParamType, varargsArgumentType)
        || types.isSameType(varargsParamType.getComponentType(), varargsArgumentType));
  }
}
