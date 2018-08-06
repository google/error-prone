/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.util.Context;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "BoxedPrimitiveConstructor",
    category = Category.JDK,
    summary = "valueOf or autoboxing provides better time and space performance",
    severity = SeverityLevel.WARNING,
    tags = StandardTags.PERFORMANCE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class BoxedPrimitiveConstructor extends BugChecker implements NewClassTreeMatcher {

  private static final Matcher<Tree> TO_STRING =
      toType(ExpressionTree.class, instanceMethod().anyClass().named("toString").withParameters());

  private static final Matcher<Tree> HASH_CODE =
      toType(ExpressionTree.class, instanceMethod().anyClass().named("hashCode").withParameters());

  private static final Matcher<Tree> COMPARE_TO =
      toType(
          ExpressionTree.class,
          instanceMethod().onDescendantOf("java.lang.Comparable").named("compareTo"));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree.getIdentifier());
    if (sym == null) {
      return NO_MATCH;
    }
    Types types = state.getTypes();
    Symtab symtab = state.getSymtab();
    // TODO(cushon): consider handling String also
    if (sym.equals(types.boxedClass(symtab.byteType))
        || sym.equals(types.boxedClass(symtab.charType))
        || sym.equals(types.boxedClass(symtab.shortType))
        || sym.equals(types.boxedClass(symtab.intType))
        || sym.equals(types.boxedClass(symtab.longType))
        || sym.equals(types.boxedClass(symtab.doubleType))
        || sym.equals(types.boxedClass(symtab.floatType))
        || sym.equals(types.boxedClass(symtab.booleanType))) {
      return describeMatch(tree, buildFix(tree, state));
    }
    return NO_MATCH;
  }

  private Fix buildFix(NewClassTree tree, VisitorState state) {
    boolean autoboxFix = shouldAutoboxFix(state);
    Types types = state.getTypes();
    Type type = types.unboxedTypeOrType(getType(tree));
    if (types.isSameType(type, state.getSymtab().booleanType)) {
      Object value = literalValue(tree.getArguments().iterator().next());
      if (value instanceof Boolean) {
        return SuggestedFix.replace(tree, literalFix((boolean) value, autoboxFix));
      } else if (value instanceof String) {
        return SuggestedFix.replace(
            tree, literalFix(Boolean.parseBoolean((String) value), autoboxFix));
      }
    }

    // Primitive constructors are all unary
    JCTree.JCExpression arg = (JCTree.JCExpression) getOnlyElement(tree.getArguments());
    Type argType = getType(arg);
    if (autoboxFix && argType.isPrimitive()) {
      return SuggestedFix.builder()
          .replace(
              ((JCTree) tree).getStartPosition(),
              arg.getStartPosition(),
              maybeCast(state, type, argType))
          .replace(state.getEndPosition(arg), state.getEndPosition(tree), "")
          .build();
    }

    JCTree parent = (JCTree) state.getPath().getParentPath().getParentPath().getLeaf();
    if (TO_STRING.matches(parent, state)) {
      // e.g. new Integer($A).toString() -> String.valueOf($A)
      return SuggestedFix.builder()
          .replace(parent.getStartPosition(), arg.getStartPosition(), "String.valueOf(")
          .replace(state.getEndPosition(arg), state.getEndPosition(parent), ")")
          .build();
    }

    String typeName = state.getSourceForNode(tree.getIdentifier());
    DoubleAndFloatStatus doubleAndFloatStatus = doubleAndFloatStatus(state, type, argType);
    if (HASH_CODE.matches(parent, state)) {
      // e.g. new Integer($A).hashCode() -> Integer.hashCode($A)
      SuggestedFix.Builder fix = SuggestedFix.builder();
      String replacement;

      String optionalCast = "";
      String optionalSuffix = "";
      switch (doubleAndFloatStatus) {
        case PRIMITIVE_DOUBLE_INTO_FLOAT:
          // new Float(double).compareTo($foo) => Float.compare((float) double, foo)
          optionalCast = "(float) ";
          break;
        case BOXED_DOUBLE_INTO_FLOAT:
          // new Float(Double).compareTo($foo) => Float.compare(Double.floatValue(), foo)
          optionalSuffix = ".floatValue()";
          break;
        default:
          break;
      }

      // In JDK8, there are primitive static methods on each type (Long.hashCode($long)). However,
      // in Java 7, those don't exist, so we suggest Guava
      if (shouldUseGuavaHashCode(state.context)) {
        fix.addImport("com.google.common.primitives." + typeName + "s");
        replacement = String.format("%ss.hashCode(", typeName);
      } else {
        replacement = String.format("%s.hashCode(", typeName);
      }
      return fix.replace(
              parent.getStartPosition(), arg.getStartPosition(), replacement + optionalCast)
          .replace(state.getEndPosition(arg), state.getEndPosition(parent), optionalSuffix + ")")
          .build();
    }

    if (COMPARE_TO.matches(parent, state)
        && ASTHelpers.getReceiver((ExpressionTree) parent).equals(tree)) {
      JCMethodInvocation compareTo = (JCMethodInvocation) parent;
      // e.g. new Integer($A).compareTo($B) -> Integer.compare($A, $B)
      JCTree.JCExpression rhs = getOnlyElement(compareTo.getArguments());

      String optionalCast = "";
      String optionalSuffix = "";
      switch (doubleAndFloatStatus) {
        case PRIMITIVE_DOUBLE_INTO_FLOAT:
          // new Float(double).compareTo($foo) => Float.compare((float) double, foo)
          optionalCast = "(float) ";
          break;
        case BOXED_DOUBLE_INTO_FLOAT:
          // new Float(Double).compareTo($foo) => Float.compare(Double.floatValue(), foo)
          optionalSuffix = ".floatValue()";
          break;
        default:
          break;
      }

      return SuggestedFix.builder()
          .replace(
              compareTo.getStartPosition(),
              arg.getStartPosition(),
              String.format("%s.compare(%s", typeName, optionalCast))
          .replace(
              /* startPos= */ state.getEndPosition(arg),
              /* endPos= */ rhs.getStartPosition(),
              String.format("%s, ", optionalSuffix))
          .replace(state.getEndPosition(rhs), state.getEndPosition(compareTo), ")")
          .build();
    }

    // Patch new Float(Double) => Float.valueOf(float) by downcasting the double, since
    // neither valueOf(float) nor valueOf(String) match.
    String prefixToArg;
    String suffix = "";
    switch (doubleAndFloatStatus) {
      case PRIMITIVE_DOUBLE_INTO_FLOAT:
        // new Float(double) => Float.valueOf((float) double)
        prefixToArg = String.format("%s.valueOf(%s", typeName, "(float) ");
        break;
      case BOXED_DOUBLE_INTO_FLOAT:
        // new Float(Double) => Double.floatValue()
        prefixToArg = "";
        suffix = ".floatValue(";
        break;
      default:
        prefixToArg = String.format("%s.valueOf(", typeName);
        break;
    }

    return SuggestedFix.builder()
        .replace(((JCTree) tree).getStartPosition(), arg.getStartPosition(), prefixToArg)
        .postfixWith(arg, suffix)
        .build();
  }

  private static boolean shouldUseGuavaHashCode(Context context) {
    return Source.instance(context).compareTo(Source.lookup("1.7")) <= 0; // 7 or below
  }

  private String maybeCast(VisitorState state, Type type, Type argType) {
    if (doubleAndFloatStatus(state, type, argType)
        == DoubleAndFloatStatus.PRIMITIVE_DOUBLE_INTO_FLOAT) {
      // e.g.: new Float(3.0d) => (float) 3.0d
      return "(float) ";
    }
    // primitive widening conversions can't be combined with autoboxing, so add a
    // explicit widening cast unless we're sure the expression doesn't get autoboxed
    ASTHelpers.TargetType targetType = ASTHelpers.targetType(state);
    if (targetType != null
        && !isSameType(type, argType, state)
        && !isSameType(targetType.type(), type, state)) {
      return String.format("(%s) ", type);
    }
    return "";
  }

  private enum DoubleAndFloatStatus {
    NONE,
    PRIMITIVE_DOUBLE_INTO_FLOAT,
    BOXED_DOUBLE_INTO_FLOAT
  }

  private DoubleAndFloatStatus doubleAndFloatStatus(
      VisitorState state, Type recieverType, Type argType) {
    Types types = state.getTypes();
    if (!types.isSameType(recieverType, state.getSymtab().floatType)) {
      return DoubleAndFloatStatus.NONE;
    }
    if (types.isSameType(argType, types.boxedClass(state.getSymtab().doubleType).type)) {
      return DoubleAndFloatStatus.BOXED_DOUBLE_INTO_FLOAT;
    }
    if (types.isSameType(argType, state.getSymtab().doubleType)) {
      return DoubleAndFloatStatus.PRIMITIVE_DOUBLE_INTO_FLOAT;
    }
    return DoubleAndFloatStatus.NONE;
  }

  private boolean shouldAutoboxFix(VisitorState state) {
    switch (state.getPath().getParentPath().getLeaf().getKind()) {
      case METHOD_INVOCATION:
        // autoboxing a method argument affects overload resolution
        return false;
      case MEMBER_SELECT:
        // can't select members on primitives (e.g. `theInteger.toString()`)
        return false;
      case TYPE_CAST:
        // can't combine autoboxing and casts to reference types
        return false;
      default:
        return true;
    }
  }

  private String literalFix(boolean value, boolean autoboxFix) {
    if (autoboxFix) {
      return value ? "true" : "false";
    }
    return value ? "Boolean.TRUE" : "Boolean.FALSE";
  }

  private Object literalValue(Tree arg) {
    if (!(arg instanceof LiteralTree)) {
      return null;
    }
    return ((LiteralTree) arg).getValue();
  }
}
