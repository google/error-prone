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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
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
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;
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
    severity = ERROR)
public class ProtoFieldNullComparison extends BugChecker implements CompilationUnitTreeMatcher {

  // TODO(b/111109484): Try to consolidate these with NullnessPropagationTransfer.
  private static final Matcher<ExpressionTree> CHECK_NOT_NULL =
      anyOf(
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
          staticMethod().onClass("com.google.common.base.Verify").named("verifyNotNull"),
          staticMethod().onClass("java.util.Objects").named("requireNonNull"));

  private static final Matcher<ExpressionTree> ASSERT_NOT_NULL =
      anyOf(
          staticMethod().onClass("junit.framework.Assert").named("assertNotNull"),
          staticMethod().onClass("org.junit.Assert").named("assertNotNull"));

  private static final Matcher<MethodInvocationTree> TRUTH_NOT_NULL =
      allOf(
          instanceMethod().onDescendantOf("com.google.common.truth.Subject").named("isNotNull"),
          receiverOfInvocation(
              anyOf(
                  staticMethod().onClass("com.google.common.truth.Truth").namedAnyOf("assertThat"),
                  instanceMethod()
                      .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
                      .named("that"))));

  private static final Matcher<Tree> RETURNS_LIST = Matchers.isSubtypeOf("java.util.List");

  private static final Set<Kind> COMPARISON_OPERATORS =
      EnumSet.of(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO);

  private static final Matcher<ExpressionTree> EXTENSION_METHODS_WITH_FIX =
      instanceMethod()
          .onDescendantOf("com.google.protobuf.GeneratedMessage.ExtendableMessage")
          .named("getExtension")
          .withParameters("com.google.protobuf.ExtensionLite");

  private static final Matcher<ExpressionTree> EXTENSION_METHODS_WITH_NO_FIX =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.google.protobuf.MessageOrBuilder")
              .named("getRepeatedField")
              .withParameters("com.google.protobuf.Descriptors.FieldDescriptor", "int"),
          instanceMethod()
              .onDescendantOf("com.google.protobuf.GeneratedMessage.ExtendableMessage")
              .named("getExtension")
              .withParameters("com.google.protobuf.ExtensionLite", "int"),
          instanceMethod()
              .onDescendantOf("com.google.protobuf.MessageOrBuilder")
              .named("getField")
              .withParameters("com.google.protobuf.Descriptors.FieldDescriptor"));

  private static final Matcher<ExpressionTree> OF_NULLABLE =
      anyOf(
          staticMethod().onClass("java.util.Optional").named("ofNullable"),
          staticMethod().onClass("com.google.common.base.Optional").named("fromNullable"));

  private static boolean isNull(ExpressionTree tree) {
    return tree.getKind() == Kind.NULL_LITERAL;
  }

  /** Matcher for generated protobufs. */
  private static final Matcher<ExpressionTree> PROTO_RECEIVER =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.protobuf.GeneratedMessageLite", "com.google.protobuf.GeneratedMessage");

  private final boolean matchTestAssertions;
  private final boolean matchOptionals;

  public ProtoFieldNullComparison(ErrorProneFlags flags) {
    this.matchTestAssertions =
        flags.getBoolean("ProtoFieldNullComparison:MatchTestAssertions").orElse(false);
    this.matchOptionals = flags.getBoolean("ProtoFieldNullComparison:MatchOptionals").orElse(true);

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
      Symbol symbol = ASTHelpers.getSymbol(variable);
      if (variable.getInitializer() != null && symbol != null && isConsideredFinal(symbol)) {
        getInitializer(variable.getInitializer())
            .ifPresent(e -> effectivelyFinalValues.put(symbol, e));
      }
      return isSuppressed(variable) ? null : super.visitVariable(variable, null);
    }

    private Optional<ExpressionTree> getInitializer(ExpressionTree tree) {
      return Optional.ofNullable(
          new SimpleTreeVisitor<ExpressionTree, Void>() {
            @Override
            public ExpressionTree visitMethodInvocation(MethodInvocationTree node, Void unused) {
              return PROTO_RECEIVER.matches(node, state) ? node : null;
            }

            @Override
            public ExpressionTree visitParenthesized(ParenthesizedTree node, Void unused) {
              return visit(node.getExpression(), null);
            }

            @Override
            public ExpressionTree visitTypeCast(TypeCastTree node, Void unused) {
              return visit(node.getExpression(), null);
            }
          }.visit(tree, null));
    }

    @Override
    public Void visitBinary(BinaryTree binary, Void unused) {
      if (!COMPARISON_OPERATORS.contains(binary.getKind())) {
        return super.visitBinary(binary, null);
      }
      VisitorState subState = state.withPath(getCurrentPath());
      Optional<Fixer> fixer = Optional.empty();
      if (isNull(binary.getLeftOperand())) {
        fixer = getFixer(binary.getRightOperand(), subState);
      }
      if (isNull(binary.getRightOperand())) {
        fixer = getFixer(binary.getLeftOperand(), subState);
      }
      fixer
          .map(f -> describeMatch(binary, ProblemUsage.COMPARISON.fix(f, binary, subState)))
          .ifPresent(state::reportMatch);
      return super.visitBinary(binary, null);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
      VisitorState subState = state.withPath(getCurrentPath());
      ExpressionTree argument;
      ProblemUsage problemType;
      if (CHECK_NOT_NULL.matches(node, subState)) {
        argument = node.getArguments().get(0);
        problemType = ProblemUsage.CHECK_NOT_NULL;
      } else if (matchTestAssertions && ASSERT_NOT_NULL.matches(node, subState)) {
        argument = getLast(node.getArguments());
        problemType = ProblemUsage.JUNIT;
      } else if (matchOptionals && OF_NULLABLE.matches(node, subState)) {
        argument = getOnlyElement(node.getArguments());
        problemType = ProblemUsage.OPTIONAL;
      } else if (matchTestAssertions && TRUTH_NOT_NULL.matches(node, subState)) {
        argument = getOnlyElement(((MethodInvocationTree) getReceiver(node)).getArguments());
        problemType = ProblemUsage.TRUTH;
      } else {
        return super.visitMethodInvocation(node, null);
      }
      getFixer(argument, subState)
          .map(f -> describeMatch(node, problemType.fix(f, node, subState)))
          .ifPresent(state::reportMatch);

      return super.visitMethodInvocation(node, null);
    }

    private Optional<Fixer> getFixer(ExpressionTree tree, VisitorState state) {
      ExpressionTree resolvedTree = getEffectiveTree(tree);
      if (resolvedTree == null || !PROTO_RECEIVER.matches(resolvedTree, state)) {
        return Optional.empty();
      }
      return Arrays.stream(GetterTypes.values())
          .map(type -> type.match(resolvedTree, state))
          .filter(Objects::nonNull)
          .findFirst();
    }

    @Nullable
    private ExpressionTree getEffectiveTree(ExpressionTree tree) {
      return tree.getKind() == Kind.IDENTIFIER
          ? effectivelyFinalValues.get(ASTHelpers.getSymbol(tree))
          : tree;
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

  /** Generates a replacement hazzer, if available. */
  @FunctionalInterface
  private interface Fixer {
    /** @param negated whether the hazzer should be negated. */
    Optional<String> getHazzer(boolean negated, VisitorState state);
  }

  private enum GetterTypes {
    /** {@code proto.getFoo()} */
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
        return isGetter(expressionTree) ? (n, s) -> generateFix(method, n, s) : null;
      }

      private Optional<String> generateFix(
          MethodInvocationTree methodInvocation, boolean negated, VisitorState state) {
        String methodName = ASTHelpers.getSymbol(methodInvocation).getQualifiedName().toString();
        String hasMethod = methodName.replaceFirst("get", "has");

        // proto3 does not generate has methods for scalar types, e.g. ByteString and String.
        // Do not provide a replacement in these cases.
        Set<MethodSymbol> hasMethods =
            ASTHelpers.findMatchingMethods(
                state.getName(hasMethod),
                ms -> ms.params().isEmpty(),
                getType(getReceiver(methodInvocation)),
                state.getTypes());
        if (hasMethods.isEmpty()) {
          return Optional.empty();
        }
        String replacement = replaceLast(methodInvocation.toString(), methodName, hasMethod);
        return Optional.of(negated ? ("!" + replacement) : replacement);
      }

      private String replaceLast(String text, String pattern, String replacement) {
        StringBuilder builder = new StringBuilder(text);
        int lastIndexOf = builder.lastIndexOf(pattern);
        return builder.replace(lastIndexOf, lastIndexOf + pattern.length(), replacement).toString();
      }
    },
    /** {@code proto.getRepeatedFoo(index)} */
    VECTOR_INDEXED {
      @Override
      Fixer match(ExpressionTree tree, VisitorState state) {
        if (tree.getKind() != Kind.METHOD_INVOCATION) {
          return null;
        }
        MethodInvocationTree method = (MethodInvocationTree) tree;
        if (method.getArguments().size() != 1 || !isGetter(method.getMethodSelect())) {
          return null;
        }
        if (!isSameType(
            getType(getOnlyElement(method.getArguments())), state.getSymtab().intType, state)) {
          return null;
        }
        return (n, s) -> Optional.of(generateFix(method, n));
      }

      private String generateFix(MethodInvocationTree methodInvocation, boolean negated) {
        String methodName = ASTHelpers.getSymbol(methodInvocation).getQualifiedName().toString();
        String countMethod = methodName + "Count";
        return String.format(
            "%s.%s() %s %s",
            getReceiver(methodInvocation),
            countMethod,
            negated ? "<=" : ">",
            getOnlyElement(methodInvocation.getArguments()));
      }
    },
    /** {@code proto.getRepeatedFooList()} */
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
        return isGetter(expressionTree) ? (n, s) -> Optional.of(generateFix(n, method)) : null;
      }

      private String generateFix(boolean negated, ExpressionTree methodInvocation) {
        String replacement = methodInvocation + ".isEmpty()";
        return negated ? replacement : ("!" + replacement);
      }
    },
    /** {@code proto.getField(f)} or {@code proto.getExtension(outer, extension)}; */
    EXTENSION_METHOD {
      @Override
      Fixer match(ExpressionTree tree, VisitorState state) {
        if (EXTENSION_METHODS_WITH_NO_FIX.matches(tree, state)) {
          return GetterTypes::emptyFix;
        }
        if (EXTENSION_METHODS_WITH_FIX.matches(tree, state)) {
          // If the extension represents a repeated field (i.e.: it's an ExtensionLite<T, List<R>>),
          // the suggested fix from get->has isn't appropriate,so we shouldn't suggest a replacement

          MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
          Type argumentType =
              ASTHelpers.getType(Iterables.getOnlyElement(methodInvocation.getArguments()));
          Symbol extension = state.getSymbolFromString("com.google.protobuf.ExtensionLite");
          Type genericsArgument = state.getTypes().asSuper(argumentType, extension);

          // If there are not two arguments then it is a raw type
          // We can't make a fix on a raw type because there is not a way to guarantee that
          // it does not contain a repeated field
          if (genericsArgument.getTypeArguments().size() != 2) {
            return GetterTypes::emptyFix;
          }

          // If the second element within the generic argument is a subtype of list,
          // that means it is a repeated field and therefore we cannot make a fix.
          if (ASTHelpers.isSubtype(
              genericsArgument.getTypeArguments().get(1), state.getSymtab().listType, state)) {
            return GetterTypes::emptyFix;
          }
          // Now that it is guaranteed that there is not a repeated field, providing a fix is safe
          return generateFix(methodInvocation);
        }
        return null;
      }

      private Fixer generateFix(MethodInvocationTree methodInvocation) {
        return (negated, state) -> {
          String methodName = getMethodName(methodInvocation);
          String hasMethod = methodName.replaceFirst("get", "has");
          String replacement = replaceLast(methodInvocation.toString(), methodName, hasMethod);
          return Optional.of(negated ? "!" + replacement : replacement);
        };
      }
    };

    /**
     * Returns a Fixer representing a situation where we don't have a fix, but want to mark a
     * callsite as containing a bug.
     */
    private static Optional<String> emptyFix(boolean n, VisitorState s) {
      return Optional.empty();
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

  private enum ProblemUsage {
    /** Matches direct comparisons to null. */
    COMPARISON {
      @Override
      SuggestedFix fix(Fixer fixer, ExpressionTree tree, VisitorState state) {
        Optional<String> replacement = fixer.getHazzer(tree.getKind() == Kind.EQUAL_TO, state);
        return replacement
            .map(r -> SuggestedFix.replace(tree, r))
            .orElse(SuggestedFix.builder().build());
      }
    },
    /** Matches comparisons with Truth, i.e. {@code assertThat(proto.getField()).isNull()}. */
    TRUTH {
      @Override
      SuggestedFix fix(Fixer fixer, ExpressionTree tree, VisitorState state) {
        return fixer
            .getHazzer(/* negated= */ false, state)
            .map(
                r -> {
                  MethodInvocationTree receiver = (MethodInvocationTree) getReceiver(tree);
                  return SuggestedFix.replace(
                      tree,
                      String.format(
                          "%s(%s).isTrue()",
                          state.getSourceForNode(receiver.getMethodSelect()), r));
                })
            .orElse(SuggestedFix.builder().build());
      }
    },
    /** Matches comparisons with JUnit, i.e. {@code assertNotNull(proto.getField())}. */
    JUNIT {
      @Override
      SuggestedFix fix(Fixer fixer, ExpressionTree tree, VisitorState state) {
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
        return fixer
            .getHazzer(/* negated= */ false, state)
            .map(
                r -> {
                  int startPos = getStartPosition(methodInvocationTree);
                  return SuggestedFix.builder()
                      .replace(getLast(methodInvocationTree.getArguments()), r)
                      .replace(startPos, startPos + "assertNotNull".length(), "assertTrue")
                      .build();
                })
            .orElse(SuggestedFix.builder().build());
      }
    },
    /** Matches precondition checks, i.e. {@code checkNotNull(proto.getField())}. */
    CHECK_NOT_NULL {
      @Override
      SuggestedFix fix(Fixer fixer, ExpressionTree tree, VisitorState state) {
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
        Tree parent = state.getPath().getParentPath().getLeaf();
        return parent.getKind() == Kind.EXPRESSION_STATEMENT
            ? SuggestedFix.delete(parent)
            : SuggestedFix.replace(
                tree, state.getSourceForNode(methodInvocationTree.getArguments().get(0)));
      }
    },
    /** Matches comparisons with JUnit, i.e. {@code assertNotNull(proto.getField())}. */
    OPTIONAL {
      @Override
      SuggestedFix fix(Fixer fixer, ExpressionTree tree, VisitorState state) {
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
        return SuggestedFixes.renameMethodInvocation(methodInvocationTree, "of", state);
      }
    };

    abstract SuggestedFix fix(Fixer fixer, ExpressionTree tree, VisitorState state);
  }
}
