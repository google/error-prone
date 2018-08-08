/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.PROTOBUF;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/** Matches comparison of proto fields to {@code null}. */
@BugPattern(
    name = "ProtoFieldNullComparison",
    summary = "Protobuf fields cannot be null.",
    category = PROTOBUF,
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ProtoFieldNullComparison extends BugChecker implements CompilationUnitTreeMatcher {

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessage";

  private static final Matcher<ExpressionTree> PROTO_RECEIVER =
      instanceMethod().onDescendantOf(PROTO_SUPER_CLASS);

  private static final Matcher<Tree> RETURNS_LIST = Matchers.isSubtypeOf("java.util.List");

  private static final Set<Kind> COMPARISON_OPERATORS =
      EnumSet.of(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO);

  private static final Matcher<ExpressionTree> EXTENSION_METHODS_WITH_FIX =
      Matchers.instanceMethod()
          .onDescendantOf("com.google.protobuf.GeneratedMessage.ExtendableMessage")
          .named("getExtension")
          .withParameters("com.google.protobuf.ExtensionLite");

  private static final Matcher<ExpressionTree> EXTENSION_METHODS_WITH_NO_FIX =
      anyOf(
          Matchers.instanceMethod()
              .onDescendantOf("com.google.protobuf.MessageOrBuilder")
              .named("getRepeatedField")
              .withParameters("com.google.protobuf.Descriptors.FieldDescriptor", "int"),
          Matchers.instanceMethod()
              .onDescendantOf("com.google.protobuf.GeneratedMessage.ExtendableMessage")
              .named("getExtension")
              .withParameters("com.google.protobuf.ExtensionLite", "int"),
          Matchers.instanceMethod()
              .onDescendantOf("com.google.protobuf.MessageOrBuilder")
              .named("getField")
              .withParameters("com.google.protobuf.Descriptors.FieldDescriptor"));

  private static boolean isNull(ExpressionTree tree) {
    return tree.getKind() == Kind.NULL_LITERAL;
  }

  private final boolean trackAssignments;

  public ProtoFieldNullComparison(ErrorProneFlags flags) {
    trackAssignments = flags.getBoolean("ProtoFieldNullComparison:TrackAssignments").orElse(false);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ProtoNullComparisonScanner scanner = new ProtoNullComparisonScanner(state);
    scanner.scan(state.getPath(), null);
    return Description.NO_MATCH;
  }

  private class ProtoNullComparisonScanner extends TreePathScanner<Void, Void> {
    private final Map<Symbol, ExpressionTree> effectivelyFinalValues = new HashMap<>();
    private final VisitorState state;

    private ProtoNullComparisonScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitMethod(MethodTree method, Void unused) {
      return isSuppressed(method) ? null : super.visitMethod(method, unused);
    }

    @Override
    public Void visitClass(ClassTree clazz, Void unused) {
      return isSuppressed(clazz) ? null : super.visitClass(clazz, unused);
    }

    @Override
    public Void visitVariable(VariableTree variable, Void unused) {
      if (trackAssignments) {
        Symbol symbol = ASTHelpers.getSymbol(variable);
        if (isEffectivelyFinal(symbol) && variable.getInitializer() != null) {
          effectivelyFinalValues.put(symbol, variable.getInitializer());
        }
      }
      return isSuppressed(variable) ? null : super.visitVariable(variable, null);
    }

    @Override
    public Void visitBinary(BinaryTree binary, Void unused) {
      if (!COMPARISON_OPERATORS.contains(binary.getKind())) {
        return super.visitBinary(binary, null);
      }
      VisitorState subState = state.withPath(getCurrentPath());
      Optional<Fixer> getter = Optional.empty();
      if (isNull(binary.getLeftOperand())) {
        getter = getFixer(binary.getRightOperand(), subState);
      }
      if (isNull(binary.getRightOperand())) {
        getter = getFixer(binary.getLeftOperand(), subState);
      }
      getter
          .map(g -> describeMatch(binary, g.generateFix(binary, subState)))
          .ifPresent(state::reportMatch);
      return super.visitBinary(binary, null);
    }

    private boolean isEffectivelyFinal(@Nullable Symbol symbol) {
      return symbol != null && (symbol.flags() & (Flags.FINAL | Flags.EFFECTIVELY_FINAL)) != 0;
    }

    private Optional<Fixer> getFixer(ExpressionTree tree, VisitorState state) {
      ExpressionTree resolvedTree =
          tree.getKind() == Kind.IDENTIFIER
              ? effectivelyFinalValues.get(ASTHelpers.getSymbol(tree))
              : tree;
      if (resolvedTree == null || !PROTO_RECEIVER.matches(resolvedTree, state)) {
        return Optional.empty();
      }
      return Arrays.stream(GetterTypes.values())
          .map(type -> type.match(resolvedTree, state))
          .filter(Objects::nonNull)
          .findFirst();
    }
  }

  private static String getMethodName(ExpressionTree tree) {
    MethodInvocationTree method = (MethodInvocationTree) tree;
    ExpressionTree expressionTree = method.getMethodSelect();
    JCFieldAccess access = (JCFieldAccess) expressionTree;
    return access.sym.getQualifiedName().toString();
  }

  private static String replaceLast(String text, String pattern, String replacement) {
    StringBuilder builder = new StringBuilder(text);
    int lastIndexOf = builder.lastIndexOf(pattern);
    return builder.replace(lastIndexOf, lastIndexOf + pattern.length(), replacement).toString();
  }

  @FunctionalInterface
  private interface Fixer {
    SuggestedFix generateFix(BinaryTree binaryTree, VisitorState state);
  }

  private enum GetterTypes {
    SCALAR {
      @Override
      Fixer match(ExpressionTree tree, VisitorState state) {
        if (tree.getKind() != Kind.METHOD_INVOCATION) {
          return null;
        }
        MethodInvocationTree method = (MethodInvocationTree) tree;
        if (!method.getArguments().isEmpty()) {
          return null;
        }
        if (RETURNS_LIST.matches(method, state)) {
          return null;
        }
        ExpressionTree expressionTree = method.getMethodSelect();
        return isGetter(expressionTree) ? (b, s) -> generateFix(method, b, s) : null;
      }

      private SuggestedFix generateFix(
          MethodInvocationTree methodInvocation, BinaryTree binaryTree, VisitorState state) {
        String methodName = getMethodName(methodInvocation);
        String hasMethod = methodName.replaceFirst("get", "has");

        // proto3 does not generate has methods for scalar types, e.g. ByteString and String.
        // Do not provide a replacement in these cases.
        Set<MethodSymbol> hasMethods =
            ASTHelpers.findMatchingMethods(
                state.getName(hasMethod),
                ms -> ms.params().isEmpty(),
                ASTHelpers.getType(ASTHelpers.getReceiver(methodInvocation)),
                state.getTypes());
        if (hasMethods.isEmpty()) {
          return SuggestedFix.builder().build();
        }
        String replacement = replaceLast(methodInvocation.toString(), methodName, hasMethod);
        return SuggestedFix.replace(
            binaryTree, binaryTree.getKind() == Kind.EQUAL_TO ? "!" + replacement : replacement);
      }
    },
    VECTOR {
      @Override
      Fixer match(ExpressionTree tree, VisitorState state) {
        if (tree.getKind() != Kind.METHOD_INVOCATION) {
          return null;
        }
        MethodInvocationTree method = (MethodInvocationTree) tree;
        if (!method.getArguments().isEmpty()) {
          return null;
        }
        if (!RETURNS_LIST.matches(method, state)) {
          return null;
        }
        ExpressionTree expressionTree = method.getMethodSelect();
        return isGetter(expressionTree) ? (b, s) -> generateFix(method, b) : null;
      }

      private SuggestedFix generateFix(ExpressionTree methodInvocation, BinaryTree binaryTree) {
        String replacement = methodInvocation + ".isEmpty()";
        return SuggestedFix.replace(
            binaryTree, binaryTree.getKind() == Kind.EQUAL_TO ? replacement : ("!" + replacement));
      }
    },
    EXTENSION_METHOD {
      @Override
      Fixer match(ExpressionTree tree, VisitorState state) {
        if (EXTENSION_METHODS_WITH_NO_FIX.matches(tree, state)) {
          return emptyFix();
        }
        if (EXTENSION_METHODS_WITH_FIX.matches(tree, state)) {
          // If the extension represents a repeated field (i.e.: it's an ExtensionLite<T, List<R>>),
          // the suggested fix from get->has isn't appropriate,so we shouldn't suggest a replacement

          MethodInvocationTree m = (MethodInvocationTree) tree;
          Type argumentType = ASTHelpers.getType(Iterables.getOnlyElement(m.getArguments()));
          Symbol extension = state.getSymbolFromString("com.google.protobuf.ExtensionLite");
          Type genericsArgument = state.getTypes().asSuper(argumentType, extension);

          // If there are not two arguments then it is a raw type
          // We can't make a fix on a raw type because there is not a way to guarantee that
          // it does not contain a repeated field
          if (genericsArgument.getTypeArguments().size() != 2) {
            return emptyFix();
          }

          // If the second element within the generic argument is a subtype of list,
          // that means it is a repeated field and therefore we cannot make a fix.
          if (ASTHelpers.isSubtype(
              genericsArgument.getTypeArguments().get(1),
              state.getTypeFromString("java.util.List"),
              state)) {
            return emptyFix();
          }
          // Now that it is guaranteed that there is not a repeated field, providing a fix is safe
          return generateFix();
        }
        return null;
      }

      private Fixer generateFix() {
        return (BinaryTree b, VisitorState s) -> {
          ExpressionTree leftOperand = b.getLeftOperand();
          ExpressionTree rightOperand = b.getRightOperand();
          ExpressionTree methodInvocation;
          if (isNull(leftOperand)) {
            methodInvocation = rightOperand;
          } else {
            methodInvocation = leftOperand;
          }
          String methodName = getMethodName(methodInvocation);
          String hasMethod = methodName.replaceFirst("get", "has");
          String replacement = replaceLast(methodInvocation.toString(), methodName, hasMethod);
          return SuggestedFix.replace(
              b, b.getKind() == Kind.EQUAL_TO ? "!" + replacement : replacement);
        };
      }
    };

    /**
     * Returns a Fixer representing a situation where we don't have a fix, but want to mark a
     * callsite as containing a bug.
     */
    private static Fixer emptyFix() {
      return (b, s) -> SuggestedFix.builder().build();
    }

    private static boolean isGetter(ExpressionTree expressionTree) {
      if (!(expressionTree instanceof JCFieldAccess)) {
        return false;
      }
      JCFieldAccess access = (JCFieldAccess) expressionTree;
      String methodName = access.sym.getQualifiedName().toString();
      return methodName.startsWith("get");
    }

    abstract Fixer match(ExpressionTree tree, VisitorState state);
  }
}
