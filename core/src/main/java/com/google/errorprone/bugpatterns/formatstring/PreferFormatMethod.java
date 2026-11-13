/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isExactType;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE_ARRAY;
import static com.google.errorprone.suppliers.Suppliers.STRING_TYPE;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.findMatchingMethods;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.google.errorprone.util.AnnotationNames.FORMAT_METHOD_ANNOTATION;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.ParameterPredicates;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Types;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer calling a @FormatMethod overload instead of wrapping arguments in"
            + " String.format or String.formatted.",
    severity = WARNING)
public final class PreferFormatMethod extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> STRING_FORMAT =
      staticMethod()
          .onClass("java.lang.String")
          .named("format")
          .withParametersMatching(
              ParameterPredicates.of(isExactType(STRING_TYPE)),
              ParameterPredicates.varargsOf(ParameterPredicates.of(isExactType(OBJECT_TYPE))));

  private static final Matcher<ExpressionTree> STRING_FORMATTED =
      instanceMethod().onExactClass("java.lang.String").named("formatted");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(tree, tree.getArguments(), getSymbol(tree), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (tree.getClassBody() != null) {
      return NO_MATCH;
    }
    return match(tree, tree.getArguments(), getSymbol(tree), state);
  }

  private Description match(
      ExpressionTree tree,
      List<? extends ExpressionTree> arguments,
      @Nullable MethodSymbol methodSymbol,
      VisitorState state) {
    if (methodSymbol == null || arguments.isEmpty() || methodSymbol.isVarArgs()) {
      return NO_MATCH;
    }
    List<VarSymbol> methodParams = methodSymbol.getParameters();
    if (arguments.size() != methodParams.size()) {
      return NO_MATCH;
    }
    if (hasAnnotation(methodSymbol, FORMAT_METHOD_ANNOTATION, state)) {
      return NO_MATCH;
    }

    Type stringType = state.getSymtab().stringType;
    int lastIdx = methodParams.size() - 1;

    // Pattern 1: (..., String) -> (..., @FormatString String, Object...)
    if (isSameType(methodParams.get(lastIdx).type, stringType, state)) {
      UnwrappedFormatCall unwrapped = extractUnwrappedFormatCall(arguments.get(lastIdx), state);
      if (unwrapped != null) {
        SuggestedFix fix =
            tryDirectPositionalUnwrap(tree, arguments, methodSymbol, unwrapped, state);
        if (fix != null) {
          return describeMatch(tree, fix);
        }
      }
    }

    // Pattern 2: (String, Throwable) -> (Throwable, @FormatString String, Object...)
    if (methodParams.size() == 2
        && isSameType(methodParams.get(0).type, stringType, state)
        && isSubtype(methodParams.get(1).type, state.getSymtab().throwableType, state)
        && arguments.get(1).getKind() != Kind.NULL_LITERAL) {
      UnwrappedFormatCall unwrapped = extractUnwrappedFormatCall(arguments.get(0), state);
      if (unwrapped != null) {
        SuggestedFix fix = tryCauseSwapUnwrap(tree, arguments, methodSymbol, unwrapped, state);
        if (fix != null) {
          return describeMatch(tree, fix);
        }
      }
    }

    return NO_MATCH;
  }

  private static @Nullable SuggestedFix tryDirectPositionalUnwrap(
      ExpressionTree tree,
      List<? extends ExpressionTree> arguments,
      MethodSymbol methodSymbol,
      UnwrappedFormatCall unwrapped,
      VisitorState state) {
    List<VarSymbol> methodParams = methodSymbol.getParameters();
    int lastIdx = methodParams.size() - 1;
    Types types = state.getTypes();
    ImmutableSet<MethodSymbol> siblings = findSiblingMethods(tree, methodSymbol, state);

    boolean hasMatchingFormatOverload =
        siblings.stream()
            .anyMatch(
                candidate -> {
                  if (!isValidFormatOverloadCandidate(candidate, methodSymbol, state)) {
                    return false;
                  }
                  List<VarSymbol> candidateParams = candidate.getParameters();
                  if (candidateParams.size() != methodParams.size() + 1
                      || FormatStringUtils.formatStringIndex(candidate, state) != lastIdx) {
                    return false;
                  }
                  for (int k = 0; k < lastIdx; k++) {
                    if (!isSubtype(
                        types.erasure(methodParams.get(k).type),
                        types.erasure(candidateParams.get(k).type),
                        state)) {
                      return false;
                    }
                  }
                  return true;
                });
    if (!hasMatchingFormatOverload) {
      return null;
    }

    ImmutableList<ExpressionTree> rewrittenArgs =
        ImmutableList.<ExpressionTree>builder()
            .addAll(arguments.subList(0, lastIdx))
            .add(unwrapped.formatString())
            .addAll(unwrapped.formatArgs())
            .build();
    if (hasShadowingNonFormatOverload(siblings, methodSymbol, lastIdx, rewrittenArgs, state)) {
      return null;
    }

    String replacement =
        state.getSourceForNode(unwrapped.formatString())
            + ", "
            + getArgumentsSource(unwrapped.formatArgs(), state);
    SuggestedFix fix = SuggestedFix.replace(arguments.get(lastIdx), replacement);
    return SuggestedFixes.compilesWithFix(
            fix, state, ImmutableList.of(), /* onlyInSameCompilationUnit= */ true)
        ? fix
        : null;
  }

  private static @Nullable SuggestedFix tryCauseSwapUnwrap(
      ExpressionTree tree,
      List<? extends ExpressionTree> arguments,
      MethodSymbol methodSymbol,
      UnwrappedFormatCall unwrapped,
      VisitorState state) {
    Type causeArgType = getType(arguments.get(1));
    if (causeArgType == null) {
      return null;
    }
    Types types = state.getTypes();
    ImmutableSet<MethodSymbol> siblings = findSiblingMethods(tree, methodSymbol, state);

    boolean hasMatchingCauseSwapOverload =
        siblings.stream()
            .anyMatch(
                candidate -> {
                  if (!isValidFormatOverloadCandidate(candidate, methodSymbol, state)) {
                    return false;
                  }
                  List<VarSymbol> candidateParams = candidate.getParameters();
                  if (candidateParams.size() != 3
                      || FormatStringUtils.formatStringIndex(candidate, state) != 1) {
                    return false;
                  }
                  Type candidateCauseType = types.erasure(candidateParams.get(0).type);
                  return isSubtype(candidateCauseType, state.getSymtab().throwableType, state)
                      && isSubtype(types.erasure(causeArgType), candidateCauseType, state);
                });
    if (!hasMatchingCauseSwapOverload) {
      return null;
    }

    ImmutableList<ExpressionTree> rewrittenArgs =
        ImmutableList.<ExpressionTree>builder()
            .add(arguments.get(1))
            .add(unwrapped.formatString())
            .addAll(unwrapped.formatArgs())
            .build();
    if (hasShadowingNonFormatOverload(
        siblings, methodSymbol, /* expectedFormatStringIndex= */ 1, rewrittenArgs, state)) {
      return null;
    }

    String replacement =
        state.getSourceForNode(arguments.get(1))
            + ", "
            + state.getSourceForNode(unwrapped.formatString())
            + ", "
            + getArgumentsSource(unwrapped.formatArgs(), state);
    SuggestedFix fix =
        SuggestedFix.replace(
            getStartPosition(arguments.get(0)),
            state.getEndPosition(arguments.get(1)),
            replacement);
    return SuggestedFixes.compilesWithFix(
            fix, state, ImmutableList.of(), /* onlyInSameCompilationUnit= */ true)
        ? fix
        : null;
  }

  private static ImmutableSet<MethodSymbol> findSiblingMethods(
      ExpressionTree tree, MethodSymbol methodSymbol, VisitorState state) {
    Type searchType = getSearchType(tree, methodSymbol, state);
    return findMatchingMethods(
        methodSymbol.name,
        candidate ->
            candidate.isConstructor() == methodSymbol.isConstructor()
                && (!methodSymbol.isConstructor() || candidate.owner.equals(methodSymbol.owner)),
        searchType,
        state.getTypes());
  }

  private static Type getSearchType(
      ExpressionTree tree, MethodSymbol methodSymbol, VisitorState state) {
    if (methodSymbol.isConstructor()) {
      return enclosingClass(methodSymbol).asType();
    }
    if (!methodSymbol.isStatic() && getReceiver(tree) == null) {
      Type ownerType = enclosingClass(methodSymbol).asType();
      for (Tree ancestor : state.getPath()) {
        if (ancestor instanceof ClassTree classTree) {
          ClassSymbol classSym = getSymbol(classTree);
          if (classSym != null && isSubtype(classSym.type, ownerType, state)) {
            return classSym.type;
          }
        }
      }
    }
    return getReceiverType(tree);
  }

  private static boolean isValidFormatOverloadCandidate(
      MethodSymbol candidate, MethodSymbol methodSymbol, VisitorState state) {
    if (candidate.equals(methodSymbol)) {
      return false;
    }
    MethodTree enclosingMethodTree = state.findEnclosing(MethodTree.class);
    MethodSymbol enclosingMethod =
        enclosingMethodTree == null ? null : getSymbol(enclosingMethodTree);
    if (candidate.equals(enclosingMethod)) {
      return false;
    }
    if (enclosingMethod != null
        && !candidate.isConstructor()
        && !candidate.isStatic()
        && enclosingMethod.overrides(
            candidate, (TypeSymbol) candidate.owner, state.getTypes(), true)) {
      return false;
    }
    if (candidate.isStatic() != methodSymbol.isStatic()
        || !candidate.isVarArgs()
        || !hasAnnotation(candidate, FORMAT_METHOD_ANNOTATION, state)
        || !isLikelyAccessible(candidate, state)) {
      return false;
    }
    List<VarSymbol> candidateParams = candidate.getParameters();
    if (candidateParams.size() < 2
        || !isSameType(candidateParams.getLast().type, OBJECT_TYPE_ARRAY.get(state), state)) {
      return false;
    }
    if (!methodSymbol.isConstructor()) {
      Types types = state.getTypes();
      Type methodRet = types.erasure(methodSymbol.getReturnType());
      Type candidateRet = types.erasure(candidate.getReturnType());
      if (isSameType(methodRet, state.getSymtab().voidType, state)) {
        if (!isSameType(candidateRet, state.getSymtab().voidType, state)) {
          return false;
        }
      } else if (!isSubtype(candidateRet, methodRet, state)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isLikelyAccessible(MethodSymbol candidate, VisitorState state) {
    if (candidate.getModifiers().contains(Modifier.PUBLIC)) {
      return true;
    }
    ClassTree enclosingClassTree = state.findEnclosing(ClassTree.class);
    if (enclosingClassTree == null) {
      return true;
    }
    ClassSymbol callSiteClass = getSymbol(enclosingClassTree);
    if (callSiteClass == null) {
      return true;
    }
    if (callSiteClass.outermostClass().equals(candidate.owner.outermostClass())) {
      return true;
    }
    if (candidate.isPrivate()) {
      return false;
    }
    if (enclosingPackage(callSiteClass).equals(enclosingPackage(candidate))) {
      return true;
    }
    return candidate.getModifiers().contains(Modifier.PROTECTED);
  }

  private static boolean hasShadowingNonFormatOverload(
      ImmutableSet<MethodSymbol> siblings,
      MethodSymbol originalMethod,
      int expectedFormatStringIndex,
      List<ExpressionTree> rewrittenArgs,
      VisitorState state) {
    for (MethodSymbol other : siblings) {
      if (other.isConstructor() != originalMethod.isConstructor()) {
        continue;
      }
      if (hasAnnotation(other, FORMAT_METHOD_ANNOTATION, state)
          && other.isVarArgs()
          && FormatStringUtils.formatStringIndex(other, state) == expectedFormatStringIndex
          && isSameType(
              other.getParameters().getLast().type, OBJECT_TYPE_ARRAY.get(state), state)) {
        continue;
      }
      if (canAcceptArguments(other, rewrittenArgs, state)) {
        return true;
      }
    }
    return false;
  }

  private static boolean canAcceptArguments(
      MethodSymbol method, List<ExpressionTree> args, VisitorState state) {
    List<VarSymbol> params = method.getParameters();
    int argCount = args.size();
    if (!method.isVarArgs()) {
      if (params.size() != argCount) {
        return false;
      }
      for (int i = 0; i < argCount; i++) {
        if (!isArgConvertible(args.get(i), params.get(i).type, state)) {
          return false;
        }
      }
      return true;
    }

    int fixedCount = params.size() - 1;
    if (argCount < fixedCount) {
      return false;
    }
    for (int i = 0; i < fixedCount; i++) {
      if (!isArgConvertible(args.get(i), params.get(i).type, state)) {
        return false;
      }
    }
    Type varargsArrayType = params.get(fixedCount).type;
    if (argCount == fixedCount + 1
        && isArgConvertible(args.get(fixedCount), varargsArrayType, state)) {
      return true;
    }
    if (!(varargsArrayType instanceof ArrayType arrayType)) {
      return false;
    }
    for (int i = fixedCount; i < argCount; i++) {
      if (!isArgConvertible(args.get(i), arrayType.elemtype, state)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isArgConvertible(ExpressionTree arg, Type targetType, VisitorState state) {
    Types types = state.getTypes();
    Type erasedTarget = types.erasure(targetType);
    if (arg.getKind() == Kind.NULL_LITERAL) {
      return !erasedTarget.isPrimitive();
    }
    Type argType = getType(arg);
    if (argType == null) {
      return true;
    }
    return types.isConvertible(argType, erasedTarget);
  }

  private static @Nullable UnwrappedFormatCall extractUnwrappedFormatCall(
      ExpressionTree arg, VisitorState state) {
    ExpressionTree stripped = stripParentheses(arg);
    if (!(stripped instanceof MethodInvocationTree innerCall)) {
      return null;
    }
    if (STRING_FORMAT.matches(innerCall, state)) {
      List<? extends ExpressionTree> innerArgs = innerCall.getArguments();
      if (innerArgs.size() < 2) {
        return null;
      }
      ExpressionTree formatString = stripParentheses(innerArgs.get(0));
      ImmutableList<ExpressionTree> formatArgs =
          ImmutableList.copyOf(innerArgs.subList(1, innerArgs.size()));
      if (StrictFormatStringValidation.validate(formatString, formatArgs, state) != null) {
        return null;
      }
      return new UnwrappedFormatCall(formatString, formatArgs);
    }
    if (STRING_FORMATTED.matches(innerCall, state)) {
      ExpressionTree receiver = getReceiver(innerCall);
      List<? extends ExpressionTree> innerArgs = innerCall.getArguments();
      if (receiver == null || innerArgs.isEmpty()) {
        return null;
      }
      ExpressionTree formatString = stripParentheses(receiver);
      ImmutableList<ExpressionTree> formatArgs = ImmutableList.copyOf(innerArgs);
      if (StrictFormatStringValidation.validate(formatString, formatArgs, state) != null) {
        return null;
      }
      return new UnwrappedFormatCall(formatString, formatArgs);
    }
    return null;
  }

  private static String getArgumentsSource(ImmutableList<ExpressionTree> args, VisitorState state) {
    int start = getStartPosition(args.getFirst());
    int end = state.getEndPosition(args.getLast());
    return state.getSourceCode().subSequence(start, end).toString();
  }

  private record UnwrappedFormatCall(
      ExpressionTree formatString, ImmutableList<ExpressionTree> formatArgs) {}
}
