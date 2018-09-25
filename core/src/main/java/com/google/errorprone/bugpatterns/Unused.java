/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isVoidType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.SideEffectAnalysis.hasSideEffect;
import static com.sun.source.tree.Tree.Kind.POSTFIX_DECREMENT;
import static com.sun.source.tree.Tree.Kind.POSTFIX_INCREMENT;
import static com.sun.source.tree.Tree.Kind.PREFIX_DECREMENT;
import static com.sun.source.tree.Tree.Kind.PREFIX_INCREMENT;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

/** Bugpattern to detect unused declarations. */
@BugPattern(
    name = "Unused",
    altNames = {"unused", "UnusedParameters"},
    summary = "Unused.",
    providesFix = REQUIRES_HUMAN_ATTENTION,
    severity = WARNING,
    documentSuppression = false)
public final class Unused extends BugChecker implements CompilationUnitTreeMatcher {
  private static final String GWT_JAVASCRIPT_OBJECT = "com.google.gwt.core.client.JavaScriptObject";
  private static final String EXEMPT_PREFIX = "unused";
  private static final String JUNIT_PARAMS_VALUE = "value";
  private static final String JUNIT_PARAMS_ANNOTATION_TYPE = "junitparams.Parameters";

  private static final Supplier<Type> OBJECT = Suppliers.typeFromString("java.lang.Object");

  /** Method signature of special methods. */
  private static final Matcher<MethodTree> SPECIAL_METHODS =
      anyOf(
          allOf(
              methodIsNamed("readObject"),
              methodHasParameters(isSameType("java.io.ObjectInputStream"))),
          allOf(
              methodIsNamed("writeObject"),
              methodHasParameters(isSameType("java.io.ObjectOutputStream"))),
          allOf(methodIsNamed("readObjectNoData"), methodReturns(isVoidType())),
          allOf(methodIsNamed("readResolve"), methodReturns(OBJECT)),
          allOf(methodIsNamed("writeReplace"), methodReturns(OBJECT)));


  private static final ImmutableSet<String> EXEMPTING_METHOD_ANNOTATIONS =
      ImmutableSet.of(
          "com.google.inject.Provides",
          "com.google.inject.Inject",
          "javax.inject.Inject");

  /**
   * The set of annotation full names which exempt annotated element from being reported as unused.
   */
  private static final ImmutableSet<String> EXEMPTING_VARIABLE_ANNOTATIONS =
      ImmutableSet.of(
          "javax.persistence.Basic",
          "javax.persistence.Column",
          "javax.persistence.Id",
          "javax.persistence.Version",
          "javax.xml.bind.annotation.XmlElement",
          "org.junit.Rule",
          "org.mockito.Mock",
          "org.openqa.selenium.support.FindBy",
          "org.openqa.selenium.support.FindBys");

  private final ImmutableSet<String> methodAnnotationsExemptingParameters;

  /** The set of types exempting a type that is extending or implementing them. */
  private static final ImmutableSet<String> EXEMPTING_SUPER_TYPES =
      ImmutableSet.of(
          );

  /** The set of types exempting a field of type extending them. */
  private static final ImmutableSet<String> EXEMPTING_FIELD_SUPER_TYPES =
      ImmutableSet.of("org.junit.rules.TestRule");

  private static final ImmutableList<String> SPECIAL_FIELDS =
      ImmutableList.of(
          "serialVersionUID",
          // TAG fields are used by convention in Android apps.
          "TAG");

  private static final ImmutableSet<Modifier> LOGGER_REQUIRED_MODIFIERS =
      Sets.immutableEnumSet(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

  private static final ImmutableSet<String> LOGGER_TYPE_NAME = ImmutableSet.of("GoogleLogger");

  private static final ImmutableSet<String> LOGGER_VAR_NAME = ImmutableSet.of("logger");
  private final boolean reportInjectedFields;

  public Unused(ErrorProneFlags flags) {
    ImmutableSet.Builder<String> methodAnnotationsExemptingParameters =
        ImmutableSet.<String>builder()
            .add("org.robolectric.annotation.Implementation");
    flags
        .getList("Unused:methodAnnotationsExemptingParameters")
        .ifPresent(methodAnnotationsExemptingParameters::addAll);
    this.methodAnnotationsExemptingParameters = methodAnnotationsExemptingParameters.build();
    this.reportInjectedFields = flags.getBoolean("Unused:ReportInjectedFields").orElse(false);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    // Map of symbols to variable declarations. Initially this is a map of all of the local variable
    // and fields. As we go we remove those variables which are used.
    Map<Symbol, TreePath> unusedElements = new HashMap<>();

    // Map of symbols to their usage sites. In this map we also include the definition site in
    // addition to all the trees where symbol is used. This map is designed to keep the usage sites
    // of variables (parameters, fields, locals).
    //
    // We populate this map when analyzing the unused variables and then use it to generate
    // appropriate fixes for them.
    ListMultimap<Symbol, TreePath> usageSites = ArrayListMultimap.create();

    // We will skip reporting on the whole compilation if there are any native methods found.
    // Use a TreeScanner to find all local variables and fields.
    // The only reason type is atomic here is that we need to set its value from inside the closure.
    if (hasNativeMethods(state.getPath())) {
      // Skipping the analysis of this file because it has native methods.
      return Description.NO_MATCH;
    }
    AtomicBoolean ignoreUnusedMethods = new AtomicBoolean(false);

    // Use a TreeScanner to find all local variables and fields.
    class PrivateFieldLocalVarFinder extends TreePathScanner<Void, Void> {

      private boolean hasJUnitParamsParametersForMethodAnnotation(
          Collection<? extends AnnotationTree> annotations) {
        for (AnnotationTree tree : annotations) {
          JCAnnotation annotation = (JCAnnotation) tree;
          if (annotation.getAnnotationType().type != null
              && annotation
                  .getAnnotationType()
                  .type
                  .toString()
                  .equals(JUNIT_PARAMS_ANNOTATION_TYPE)) {
            if (annotation.getArguments().isEmpty()) {
              // @Parameters, which uses implicit provider methods
              return true;
            }
            for (JCExpression arg : annotation.getArguments()) {
              if (arg.getKind() != Kind.ASSIGNMENT) {
                // Implicit value annotation, e.g. @Parameters({"1"}); no exemption required.
                return false;
              }
              JCExpression var = ((JCAssign) arg).getVariable();
              if (var.getKind() == Kind.IDENTIFIER) {
                // Anything that is not @Parameters(value = ...), e.g.
                // @Parameters(source = ...) or @Parameters(method = ...)
                if (((IdentifierTree) var).getName().contentEquals(JUNIT_PARAMS_VALUE)) {
                  return true;
                }
              }
            }
          }
        }
        return false;
      }

      private boolean exemptedBySuperType(Type type, VisitorState state) {
        return EXEMPTING_SUPER_TYPES.stream()
            .anyMatch(t -> isSubtype(type, Suppliers.typeFromString(t).get(state), state));
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        if (exemptedByName(variableTree.getName())) {
          return null;
        }
        if (isSuppressed(variableTree)) {
          return null;
        }
        VarSymbol symbol = getSymbol(variableTree);
        if (symbol == null) {
          return null;
        }
        if (symbol.getKind() == ElementKind.FIELD
            && exemptedFieldBySuperType(getType(variableTree), state)) {
          return null;
        }
        super.visitVariable(variableTree, null);
        // Return if the element is exempted by an annotation.
        if (exemptedByAnnotation(
            variableTree.getModifiers().getAnnotations(), EXEMPTING_VARIABLE_ANNOTATIONS, state)) {
          return null;
        }
        switch (symbol.getKind()) {
          case FIELD:
            // We are only interested in private fields and those which are not special.
            if (isFieldEligibleForChecking(variableTree, symbol)) {
              unusedElements.put(symbol, getCurrentPath());
              usageSites.put(symbol, getCurrentPath());
            }
            break;
          case LOCAL_VARIABLE:
            unusedElements.put(symbol, getCurrentPath());
            usageSites.put(symbol, getCurrentPath());
            break;
          case PARAMETER:
            // ignore the receiver parameter
            if (variableTree.getName().contentEquals("this")) {
              return null;
            }
            if (isParameterSubjectToAnalysis(symbol)) {
              unusedElements.put(symbol, getCurrentPath());
            }
            break;
          default:
            break;
        }
        return null;
      }

      private boolean exemptedFieldBySuperType(Type type, VisitorState state) {
        return EXEMPTING_FIELD_SUPER_TYPES.stream()
            .anyMatch(t -> isSubtype(type, state.getTypeFromString(t), state));
      }

      private boolean isFieldEligibleForChecking(VariableTree variableTree, VarSymbol symbol) {
        if (reportInjectedFields
            && variableTree.getModifiers().getFlags().isEmpty()
            && ASTHelpers.hasDirectAnnotationWithSimpleName(variableTree, "Inject")) {
          return true;
        }
        return variableTree.getModifiers().getFlags().contains(Modifier.PRIVATE)
            && !SPECIAL_FIELDS.contains(symbol.getSimpleName().toString())
            && !isLoggerField(variableTree);
      }

      private boolean isLoggerField(VariableTree variableTree) {
        return variableTree.getModifiers().getFlags().containsAll(LOGGER_REQUIRED_MODIFIERS)
            && LOGGER_TYPE_NAME.contains(variableTree.getType().toString())
            && LOGGER_VAR_NAME.contains(variableTree.getName().toString());
      }

      /** Returns whether {@code sym} can be removed without updating call sites in other files. */
      private boolean isParameterSubjectToAnalysis(Symbol sym) {
        checkArgument(sym.getKind() == ElementKind.PARAMETER);
        Symbol enclosingMethod = sym.owner;

        for (String annotationName : methodAnnotationsExemptingParameters) {
          if (ASTHelpers.hasAnnotation(enclosingMethod, annotationName, state)) {
            return false;
          }
        }


        return enclosingMethod.getModifiers().contains(Modifier.PRIVATE);
      }

      @Override
      public Void visitTry(TryTree node, Void unused) {
        // Skip resources, as while these may not be referenced, they are used.
        scan(node.getBlock(), null);
        scan(node.getCatches(), null);
        scan(node.getFinallyBlock(), null);
        return null;
      }

      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        if (isSuppressed(tree) || exemptedBySuperType(getType(tree), state)) {
          return null;
        }
        return super.visitClass(tree, null);
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        // skip lambda parameters
        return scan(node.getBody(), null);
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (hasJUnitParamsParametersForMethodAnnotation(tree.getModifiers().getAnnotations())) {
          // Since this method uses @Parameters, there will be another method that appears to
          // be unused. Don't warn about unusedMethods at all in this case.
          ignoreUnusedMethods.set(true);
        }

        if (isSuppressed(tree)) {
          // @SuppressWarnings("unused") applies to the entire AST, not just the symbol it's bound
          // to.  Skip the whole method.
          return null;
        }

        if (isMethodSymbolEligibleForChecking(tree)) {
          unusedElements.put(getSymbol(tree), getCurrentPath());
        }
        return super.visitMethod(tree, unused);
      }

      private boolean isMethodSymbolEligibleForChecking(MethodTree tree) {
        if (exemptedByName(tree.getName())) {
          return false;
        }
        // Assume the method is called if annotated with a called-reflectively annotation.
        if (exemptedByAnnotation(
            tree.getModifiers().getAnnotations(), EXEMPTING_METHOD_ANNOTATIONS, state)) {
          return false;
        }
        // Skip constructors and special methods.
        MethodSymbol methodSymbol = getSymbol(tree);
        if (methodSymbol == null
            || methodSymbol.getKind() == ElementKind.CONSTRUCTOR
            || SPECIAL_METHODS.matches(tree, state)) {
          return false;
        }

        // Ignore this method if the last parameter is a GWT JavaScriptObject.
        if (!tree.getParameters().isEmpty()) {
          Type lastParamType = getType(getLast(tree.getParameters()));
          if (lastParamType != null && lastParamType.toString().equals(GWT_JAVASCRIPT_OBJECT)) {
            return false;
          }
        }

        return tree.getModifiers().getFlags().contains(Modifier.PRIVATE);
      }
    }
    new PrivateFieldLocalVarFinder().scan(state.getPath(), null);

    class FilterUsedElements extends TreePathScanner<Void, Void> {
      private boolean leftHandSideAssignment = false;
      // When this greater than zero, the usage of identifiers are real.
      private int inArrayAccess = 0;
      // This is true when we are processing a `return` statement. Elements used in return statement
      // must not be considered unused.
      private boolean inReturnStatement = false;
      // When this greater than zero, the usage of identifiers are real because they are in a method
      // call.
      private int inMethodCall = 0;

      private TreePath currentExpressionStatement = null;

      private boolean isInExpressionStatementTree() {
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        return parent != null && parent.getKind() == Kind.EXPRESSION_STATEMENT;
      }

      private boolean isUsed(@Nullable Symbol symbol) {
        return symbol != null
            && (!leftHandSideAssignment
                || inReturnStatement
                || inArrayAccess > 0
                || inMethodCall > 0)
            && unusedElements.containsKey(symbol);
      }

      @Override
      public Void visitExpressionStatement(ExpressionStatementTree tree, Void unused) {
        currentExpressionStatement = getCurrentPath();
        super.visitExpressionStatement(tree, null);
        currentExpressionStatement = null;
        return null;
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        Symbol symbol = getSymbol(tree);
        // Filtering out identifier symbol from vars map. These are real usages of identifiers.
        if (isUsed(symbol)) {
          unusedElements.remove(symbol);
        }
        if (currentExpressionStatement != null && unusedElements.containsKey(symbol)) {
          usageSites.put(symbol, currentExpressionStatement);
        }
        return null;
      }

      @Override
      public Void visitAssignment(AssignmentTree tree, Void unused) {
        // If a variable is used in the left hand side of an assignment that does not count as a
        // usage.
        if (isInExpressionStatementTree()) {
          leftHandSideAssignment = true;
          scan(tree.getVariable(), null);
          leftHandSideAssignment = false;
          scan(tree.getExpression(), null);
        } else {
          super.visitAssignment(tree, null);
        }
        return null;
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        Symbol symbol = getSymbol(memberSelectTree);
        if (isUsed(symbol)) {
          unusedElements.remove(symbol);
        } else if (currentExpressionStatement != null && unusedElements.containsKey(symbol)) {
          usageSites.put(symbol, currentExpressionStatement);
        }
        // Clear leftHandSideAssignment and descend down the tree to catch any variables in the
        // receiver of this member select, which _are_ considered used.
        boolean wasLeftHandAssignment = leftHandSideAssignment;
        leftHandSideAssignment = false;
        super.visitMemberSelect(memberSelectTree, null);
        leftHandSideAssignment = wasLeftHandAssignment;
        return null;
      }

      @Override
      public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
        super.visitMemberReference(tree, null);
        Symbol symbol = getSymbol(tree);
        if (isUsed(symbol)) {
          unusedElements.remove(symbol);
        }
        if (currentExpressionStatement != null && unusedElements.containsKey(symbol)) {
          usageSites.put(symbol, currentExpressionStatement);
        }
        return null;
      }

      @Override
      public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
        if (isInExpressionStatementTree()) {
          leftHandSideAssignment = true;
          scan(tree.getVariable(), null);
          leftHandSideAssignment = false;
          scan(tree.getExpression(), null);
        } else {
          super.visitCompoundAssignment(tree, null);
        }
        return null;
      }

      @Override
      public Void visitArrayAccess(ArrayAccessTree node, Void unused) {
        inArrayAccess++;
        super.visitArrayAccess(node, null);
        inArrayAccess--;
        return null;
      }

      @Override
      public Void visitReturn(ReturnTree node, Void unused) {
        inReturnStatement = true;
        scan(node.getExpression(), null);
        inReturnStatement = false;
        return null;
      }

      @Override
      public Void visitUnary(UnaryTree tree, Void unused) {
        // If unary expression is inside another expression, then this is a real usage of unary
        // operand.
        // Example:
        //   array[i++] = 0; // 'i' has a real usage here. 'array' might not have.
        //   list.get(i++);
        // But if it is like this:
        //   i++;
        // Then it is possible that this is not a real usage of 'i'.
        if (isInExpressionStatementTree()
            && (tree.getKind() == POSTFIX_DECREMENT
                || tree.getKind() == POSTFIX_INCREMENT
                || tree.getKind() == PREFIX_DECREMENT
                || tree.getKind() == PREFIX_INCREMENT)) {
          leftHandSideAssignment = true;
          scan(tree.getExpression(), null);
          leftHandSideAssignment = false;
        } else {
          super.visitUnary(tree, null);
        }
        return null;
      }

      @Override
      public Void visitErroneous(ErroneousTree tree, Void unused) {
        return scan(tree.getErrorTrees(), null);
      }

      /**
       * Looks at method invocations and removes the invoked private methods from {@code
       * #unusedElements}.
       */
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        inMethodCall++;
        Symbol methodSymbol = getSymbol(tree);
        if (methodSymbol != null) {
          unusedElements.remove(methodSymbol);
        }
        super.visitMethodInvocation(tree, null);
        inMethodCall--;
        return null;
      }
    }

    new FilterUsedElements().scan(state.getPath(), null);

    for (TreePath unusedPath : unusedElements.values()) {
      Tree unused = unusedPath.getLeaf();
      switch (unused.getKind()) {
        case VARIABLE:
          VariableTree unusedVar = (VariableTree) unused;
          String element;
          VarSymbol symbol = getSymbol(unusedVar);
          switch (symbol.getKind()) {
            case FIELD:
              element = "Field";
              break;
            case LOCAL_VARIABLE:
              element = "Local variable";
              break;
            case PARAMETER:
              element = "Parameter";
              break;
            default:
              element = "Variable";
              break;
          }
          ImmutableList<SuggestedFix> fixes;
          switch (symbol.getKind()) {
            case LOCAL_VARIABLE:
            case FIELD:
              fixes = buildUnusedVarFixes(symbol, usageSites.get(symbol), state);
              break;
            case PARAMETER:
              fixes = buildUnusedParameterFixes(symbol, usageSites.get(symbol), state);
              break;
            default:
              fixes = ImmutableList.of();
          }
          state.reportMatch(
              buildDescription(unused)
                  .setMessage(String.format("%s '%s' is never read.", element, unusedVar.getName()))
                  .addAllFixes(fixes)
                  .build());
          break;
        case METHOD:
          if (ignoreUnusedMethods.get()) {
            break;
          }
          state.reportMatch(
              buildDescription(unused)
                  .addFix(replaceWithComments(unusedPath, "", state))
                  .setMessage("Unused private method.")
                  .build());
          break;
        default:
          break;
      }
    }
    return Description.NO_MATCH;
  }

  private static boolean hasNativeMethods(TreePath path) {
    AtomicBoolean hasAnyNativeMethods = new AtomicBoolean(false);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (tree.getModifiers().getFlags().contains(Modifier.NATIVE)) {
          hasAnyNativeMethods.set(true);
        }
        return null;
      }
    }.scan(path, null);
    return hasAnyNativeMethods.get();
  }

  private static ImmutableList<SuggestedFix> buildUnusedVarFixes(
      Symbol varSymbol, List<TreePath> usagePaths, VisitorState state) {
    // Don't suggest a fix for fields annotated @Inject: we can warn on them, but they *could* be
    // used outside the class.
    if (ASTHelpers.hasDirectAnnotationWithSimpleName(varSymbol, "Inject")) {
      return ImmutableList.of();
    }
    ElementKind varKind = varSymbol.getKind();
    boolean encounteredSideEffects = false;
    SuggestedFix.Builder fix = SuggestedFix.builder().setShortDescription("remove unused variable");
    SuggestedFix.Builder removeSideEffectsFix =
        SuggestedFix.builder().setShortDescription("remove unused variable and any side effects");
    for (TreePath usagePath : usagePaths) {
      StatementTree statement = (StatementTree) usagePath.getLeaf();
      if (statement.getKind() == Kind.VARIABLE) {
        VariableTree variableTree = (VariableTree) statement;
        if (hasSideEffect(((VariableTree) statement).getInitializer())) {
          encounteredSideEffects = true;
          if (varKind == ElementKind.FIELD) {
            String newContent =
                String.format(
                    "%s{ %s; }",
                    varSymbol.isStatic() ? "static " : "",
                    state.getSourceForNode(variableTree.getInitializer()));
            fix.merge(replaceWithComments(usagePath, newContent, state));
            removeSideEffectsFix.replace(statement, "");
          } else {
            fix.replace(
                statement,
                String.format("%s;", state.getSourceForNode(variableTree.getInitializer())));
            removeSideEffectsFix.replace(statement, "");
          }
        } else if (isEnhancedForLoopVar(usagePath)) {
          String newContent =
              String.format(
                  "%s%s unused",
                  variableTree.getModifiers() == null ? "" : variableTree.getModifiers().toString(),
                  variableTree.getType());
          // The new content for the second fix should be identical to the content for the first
          // fix in this case because we can't just remove the enhanced for loop variable.
          fix.replace(variableTree, newContent);
          removeSideEffectsFix.replace(variableTree, newContent);
        } else {
          fix.merge(replaceWithComments(usagePath, "", state));
          removeSideEffectsFix.merge(replaceWithComments(usagePath, "", state));
        }
        continue;
      } else if (statement.getKind() == Kind.EXPRESSION_STATEMENT) {
        JCTree tree = (JCTree) ((ExpressionStatementTree) statement).getExpression();

        if (tree instanceof CompoundAssignmentTree) {
          if (hasSideEffect(((CompoundAssignmentTree) tree).getExpression())) {
            SuggestedFix replacement =
                SuggestedFix.replace(
                    tree.getStartPosition(),
                    ((JCAssignOp) tree).getExpression().getStartPosition(),
                    "");
            fix.merge(replacement);
            removeSideEffectsFix.merge(replacement);
            continue;
          }
        } else if (tree instanceof AssignmentTree) {
          if (hasSideEffect(((AssignmentTree) tree).getExpression())) {
            SuggestedFix replacement =
                SuggestedFix.replace(
                    tree.getStartPosition(),
                    ((JCAssign) tree).getExpression().getStartPosition(),
                    "");
            fix.merge(replacement);
            removeSideEffectsFix.merge(replacement);
            continue;
          }
        }
      }
      fix.replace(statement, "");
      removeSideEffectsFix.replace(statement, "");
    }
    return encounteredSideEffects
        ? ImmutableList.of(fix.build(), removeSideEffectsFix.build())
        : ImmutableList.of(fix.build());
  }

  private static ImmutableList<SuggestedFix> buildUnusedParameterFixes(
      Symbol varSymbol, List<TreePath> usagePaths, VisitorState state) {
    MethodSymbol methodSymbol = (MethodSymbol) varSymbol.owner;
    int index = methodSymbol.params.indexOf(varSymbol);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (TreePath path : usagePaths) {
      fix.delete(path.getLeaf());
    }
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (getSymbol(tree).equals(methodSymbol)) {
          removeByIndex(tree.getArguments());
        }
        return super.visitMethodInvocation(tree, null);
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (getSymbol(tree).equals(methodSymbol)) {
          removeByIndex(tree.getParameters());
        }
        return super.visitMethod(tree, null);
      }

      private void removeByIndex(List<? extends Tree> trees) {
        if (trees.size() == 1) {
          fix.delete(getOnlyElement(trees));
          return;
        }
        int startPos;
        int endPos;
        if (index >= 1) {
          startPos = state.getEndPosition(trees.get(index - 1));
          endPos = state.getEndPosition(trees.get(index));
        } else {
          startPos = ((JCTree) trees.get(index)).getStartPosition();
          endPos = ((JCTree) trees.get(index + 1)).getStartPosition();
        }
        if (index == methodSymbol.params().size() - 1 && methodSymbol.isVarArgs()) {
          endPos = state.getEndPosition(getLast(trees));
        }
        fix.replace(startPos, endPos, "");
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return ImmutableList.of(fix.build());
  }

  /**
   * Replaces the tree at {@code path} along with any Javadocs/associated single-line comments.
   *
   * <p>This is the same as just deleting the tree for non-class members. For class members, we
   * delete from the end of the previous member (or the start of the class, if it's the first).
   */
  private static SuggestedFix replaceWithComments(
      TreePath path, String replacement, VisitorState state) {
    Tree tree = path.getLeaf();
    Tree parent = path.getParentPath().getLeaf();
    if (parent instanceof ClassTree) {
      Tree previousMember = null;
      ClassTree classTree = (ClassTree) parent;
      for (Tree member : classTree.getMembers()) {
        if (member instanceof MethodTree
            && ASTHelpers.isGeneratedConstructor((MethodTree) member)) {
          continue;
        }
        if (member.equals(tree)) {
          break;
        }
        previousMember = member;
      }
      if (previousMember != null) {
        return SuggestedFix.replace(
            state.getEndPosition(previousMember), state.getEndPosition(tree), replacement);
      }
      int startOfClass = ((JCTree) classTree).getStartPosition();
      String source =
          state
              .getSourceCode()
              .subSequence(startOfClass, ((JCTree) tree).getStartPosition())
              .toString();
      return SuggestedFix.replace(
          startOfClass + source.indexOf("{") + 1, state.getEndPosition(tree), replacement);
    }
    return SuggestedFix.replace(tree, replacement);
  }

  private static boolean isEnhancedForLoopVar(TreePath variablePath) {
    Tree tree = variablePath.getLeaf();
    Tree parent = variablePath.getParentPath().getLeaf();
    return parent instanceof EnhancedForLoopTree
        && ((EnhancedForLoopTree) parent).getVariable() == tree;
  }

  /**
   * Looks at the list of {@code annotations} and see if there is any annotation which exists {@code
   * exemptingAnnotations}.
   */
  private static boolean exemptedByAnnotation(
      List<? extends AnnotationTree> annotations,
      Set<String> exemptingAnnotations,
      VisitorState state) {
    for (AnnotationTree annotation : annotations) {
      if (((JCAnnotation) annotation).type != null) {
        TypeSymbol tsym = ((JCAnnotation) annotation).type.tsym;
        if (exemptingAnnotations.contains(tsym.getQualifiedName().toString())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean exemptedByName(Name name) {
    return name.toString().toLowerCase().startsWith(EXEMPT_PREFIX);
  }
}
