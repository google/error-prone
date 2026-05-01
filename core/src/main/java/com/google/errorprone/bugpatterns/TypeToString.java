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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.ProcessingEnvUtils.TYPE_MIRROR_TYPE;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.lang.model.type.TypeKind;

/**
 * Flags {@code javax.lang.model.type.TypeMirror#toString} usage in {@link BugChecker}s.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary =
        "TypeMirror#toString shouldn't be used for comparison as it is expensive and fragile.",
    severity = SUGGESTION)
public class TypeToString extends AbstractToString {

  private static final TypePredicate IS_TYPE = isDescendantOf("javax.lang.model.type.TypeMirror");

  private static final Matcher<Tree> STRING_EQUALS =
      toType(
          MemberSelectTree.class,
          instanceMethod().onExactClass("java.lang.String").named("equals"));

  private static boolean isAutoValueGenerated(VisitorState state) {
    return ASTHelpers.getGeneratedBy(state).stream()
        .anyMatch(s -> s.startsWith("com.google.auto.value.processor."));
  }

  private static boolean typeToString(Type type, VisitorState state) {
    if (isAutoValueGenerated(state)) {
      return false;
    }
    Tree equalsMethodSelect = state.getPath().getParentPath().getLeaf();
    return IS_TYPE.apply(type, state) && STRING_EQUALS.matches(equalsMethodSelect, state);
  }

  @Inject
  TypeToString(ErrorProneFlags flags) {
    super(flags);
  }

  @Override
  protected TypePredicate typePredicate() {
    return TypeToString::typeToString;
  }

  @Override
  protected Optional<String> descriptionMessageForDefaultMatch(Type type, VisitorState state) {
    return Optional.of("TypeMirror#toString shouldn't be used as it is expensive and fragile.");
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree stringifiedExpr, VisitorState state) {
    return Optional.empty();
  }

  @Override
  protected Optional<Fix> toStringFix(
      Tree toStringCall, ExpressionTree stringifiedExpr, VisitorState state) {
    Tree equalsMethodSelect = state.getPath().getParentPath().getLeaf();
    if (!STRING_EQUALS.matches(equalsMethodSelect, state)) {
      return Optional.empty();
    }
    Tree equalsCallTree = state.getPath().getParentPath().getParentPath().getLeaf();
    if (!(equalsCallTree instanceof MethodInvocationTree equalsCall)) {
      return Optional.empty();
    }
    ExpressionTree argument = equalsCall.getArguments().get(0);
    // Look for type.toString().equals(<class-name-constant-string>)
    SuggestedFix.Builder fix = SuggestedFix.builder();
    Optional<String> optimizedFix =
        constantTypeNameFix(
            fix, state.getSourceForNode(stringifiedExpr), argument, /* negate= */ false, state);
    if (optimizedFix.isPresent()) {
      return Optional.of(fix.replace(equalsCall, optimizedFix.get()).build());
    }

    // type1.toString().equals(type2.toString()) -> types.isSameType(type1, type2)
    return ProcessingEnvUtils.getTypesExpr(state)
        .flatMap(
            typesExpr ->
                typeMirrorToCompare(argument, state)
                    .map(
                        typeMirrorToCompare ->
                            SuggestedFix.replace(
                                equalsCall,
                                String.format(
                                    "%s.isSameType(%s, %s)",
                                    typesExpr,
                                    state.getSourceForNode(stringifiedExpr),
                                    typeMirrorToCompare))));
  }

  private static Optional<String> typeMirrorToCompare(ExpressionTree argument, VisitorState state) {
    if (argument instanceof MethodInvocationTree argInv
        && getSymbol(argInv).getSimpleName().contentEquals("toString")) {
      ExpressionTree argReceiver = ASTHelpers.getReceiver(argInv);
      if (isSubtype(getType(argReceiver), TYPE_MIRROR_TYPE.get(state), state)) {
        return Optional.of(state.getSourceForNode(argReceiver));
      }
    }
    return Optional.empty();
  }

  // Regex pattern for fully-qualified Java class or package names.
  private static final String JAVA_CLASS_NAME_REGEX =
      "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*"
          + "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
  private static final Pattern JAVA_CLASS_NAME_PATTERN = Pattern.compile(JAVA_CLASS_NAME_REGEX);

  static Optional<String> constantTypeNameFix(
      SuggestedFix.Builder fix, String lhs, Tree rhsTree, boolean negate, VisitorState state) {
    String prefix = negate ? "!" : "";
    String rhs = ASTHelpers.constValue(rhsTree, String.class);
    if (rhs == null || !JAVA_CLASS_NAME_PATTERN.matcher(rhs).matches()) {
      // Only fix compile-time constant strings that look like class or primitive types, to avoid
      // incorrect fixes for generics
      return Optional.empty();
    }
    return primitiveOrVoidKind(rhs)
        .map(
            kind ->
                // type.toString().equals("int") -> type.getKind() == TypeKind.INT
                Optional.of(
                    String.format(
                        "%s%s.getKind() == %s.%s",
                        prefix,
                        lhs,
                        SuggestedFixes.qualifyType(state, fix, "javax.lang.model.type.TypeKind"),
                        kind)))
        .orElseGet(
            () -> {
              // type.toString().equals("java.lang.Object") ->
              //    java.util.Objects.equals(type, elements.getTypeElement("java.lang.Object")
              var typesExprOpt = ProcessingEnvUtils.getTypesExpr(state);
              var elementsExprOpt = ProcessingEnvUtils.getElementsExpr(state);
              if (typesExprOpt.isEmpty() || elementsExprOpt.isEmpty()) {
                return Optional.empty();
              }
              return Optional.of(
                  String.format(
                      "%s%s.equals(%s.asElement(%s), %s.getTypeElement(%s))",
                      prefix,
                      SuggestedFixes.qualifyType(state, fix, "java.util.Objects"),
                      typesExprOpt.get(),
                      lhs,
                      elementsExprOpt.get(),
                      state.getSourceForNode(rhsTree)));
            });
  }

  private static Optional<TypeKind> primitiveOrVoidKind(String name) {
    return Optional.ofNullable(
        switch (name) {
          case "boolean" -> TypeKind.BOOLEAN;
          case "byte" -> TypeKind.BYTE;
          case "short" -> TypeKind.SHORT;
          case "int" -> TypeKind.INT;
          case "long" -> TypeKind.LONG;
          case "char" -> TypeKind.CHAR;
          case "float" -> TypeKind.FLOAT;
          case "double" -> TypeKind.DOUBLE;
          case "void" -> TypeKind.VOID;
          default -> null;
        });
  }
}
