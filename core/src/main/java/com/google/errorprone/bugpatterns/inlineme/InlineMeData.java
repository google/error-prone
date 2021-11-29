/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inlineme;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.MoreAnnotations.getValue;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLambda.ParameterKind;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

@AutoValue
abstract class InlineMeData {

  private static final String INLINE_ME = "InlineMe";

  /** Builds the {@code @InlineMe} annotation as it would be found in source code. */
  static String buildAnnotation(
      String replacement, Set<String> imports, Set<String> staticImports) {

    String annotation =
        "@InlineMe(replacement = \""
            + SourceCodeEscapers.javaCharEscaper().escape(replacement)
            + "\"";
    if (!imports.isEmpty()) {
      annotation += ", imports = " + quote(imports);
    }
    if (!staticImports.isEmpty()) {
      annotation += ", staticImports = " + quote(staticImports);
    }
    annotation += ")\n";
    return annotation;
  }

  String buildAnnotation() {
    return buildAnnotation(replacement(), imports(), staticImports());
  }

  private static String quote(Set<String> imports) {
    String quoted = "\"" + Joiner.on("\", \"").join(imports) + "\"";
    if (imports.size() == 1) {
      return quoted;
    }
    return "{" + quoted + "}";
  }

  // TODO(glorioso): be tolerant of trailing semicolon
  abstract String replacement();

  abstract ImmutableSet<String> imports();

  abstract ImmutableSet<String> staticImports();

  static Optional<InlineMeData> createFromSymbol(MethodSymbol symbol) {
    // if the API doesn't have the @InlineMe annotation, then return no match
    if (!hasDirectAnnotationWithSimpleName(symbol, INLINE_ME)) {
      return Optional.empty();
    }

    Attribute.Compound inlineMe =
        symbol.getRawAttributes().stream()
            .filter(a -> a.type.tsym.getSimpleName().contentEquals(INLINE_ME))
            .collect(onlyElement());

    // TODO(kak): we should validate that the annotation doesn't contain any elements other than
    // `replacement` (required), `imports` and `staticImports`.

    ImmutableSet<String> imports = getStrings(inlineMe, "imports");
    ImmutableSet<String> staticImports = getStrings(inlineMe, "staticImports");

    return getValue(inlineMe, "replacement")
        .flatMap(MoreAnnotations::asStringValue)
        .map(InlineMeData::trimTrailingSemicolons)
        .map(replacement -> create(replacement, imports, staticImports));
  }

  private static InlineMeData create(
      String replacement, Iterable<String> imports, Iterable<String> staticImports) {
    return new AutoValue_InlineMeData(
        replacement, ImmutableSet.copyOf(imports), ImmutableSet.copyOf(staticImports));
  }

  // TODO(b/176439392): This is a big one:
  //   Right now, we enforce only *one* style of inlining, by requiring the body to match our
  //   implementation (see TODO in ImportAndQualificationFinder). However, it might be appropriate
  //   for us to allow multiple "flavors" of inlining, and ensure that the annotation is *one of*
  //   one of the multiple flavors of inlining.

  //  TODO(b/176094331): importing the *outer* token for a nested class like Foo.Builder.something()
  static InlineMeData buildExpectedInlineMeAnnotation(
      VisitorState state, ExpressionTree expression) {
    ClassSymbol classSymbol = getSymbol(findEnclosingNode(state.getPath(), ClassTree.class));

    // Scan the statement to collect identifiers that need to be qualified - unqualified references
    // to field or instance methods, as well as collecting the imports we need to use.
    ImportAndQualificationFinder qualifier = new ImportAndQualificationFinder(classSymbol, state);
    qualifier.scan(TreePath.getPath(state.getPath(), expression), null);
    return create(
        prettyPrint(
            new QualifyingTreeCopier(state, qualifier.qualifications)
                .copy((JCExpression) expression)),
        qualifier.imports,
        qualifier.staticImports);
  }

  private static String prettyPrint(JCTree tree) {
    StringWriter w = new StringWriter();
    tree.accept(new GooglePrinter(w));
    return w.toString();
  }

  /** Copies statements, inserting appropriate qualifiers so make it inline-ready. */
  private static class QualifyingTreeCopier extends TreeCopier<Void> {
    private final TreeMaker treeMaker;
    private final VisitorState state;
    private final IdentityHashMap<IdentifierTree, JCExpression> qualifications;

    public QualifyingTreeCopier(
        VisitorState state, IdentityHashMap<IdentifierTree, JCExpression> qualifications) {
      super(state.getTreeMaker());
      this.state = state;
      this.treeMaker = state.getTreeMaker();
      this.qualifications = qualifications;
    }

    // For some reason, paramKind isn't copied in the normal tree copier.
    // TODO(glorioso): File bug upstream? Or maybe this is intended due to desugaring???
    @Override
    public JCTree visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void unused) {
      JCLambda expr = (JCLambda) lambdaExpressionTree;
      JCLambda lambda = (JCLambda) super.visitLambdaExpression(lambdaExpressionTree, unused);
      lambda.paramKind = expr.paramKind;
      return lambda;
    }

    @Override
    public JCTree visitIdentifier(IdentifierTree identifierTree, Void unused) {
      if (qualifications.containsKey(identifierTree)) {
        return treeMaker.Select(
            qualifications.get(identifierTree), state.getName(identifierTree.toString()));
      }
      return super.visitIdentifier(identifierTree, unused);
    }
  }

  private static class GooglePrinter extends Pretty {
    private final StringWriter writer;

    public GooglePrinter(StringWriter writer) {
      super(writer, false /* don't dump extra comments */);
      this.writer = writer;
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
      // TODO(glorioso): we *should* use package-private open precedence methods
      try {
        print("(");
        printExpr(jcTypeCast.clazz);
        print(") ");
        printExpr(jcTypeCast.expr);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
      // We manually print lambdas to match our own style
      try {
        boolean paramsAreExplicit = jcLambda.paramKind == ParameterKind.EXPLICIT;
        boolean paramsNeedParentheses = jcLambda.params.size() != 1 || paramsAreExplicit;
        if (paramsNeedParentheses) {
          print("(");
        }
        if (paramsAreExplicit) {
          printExprs(jcLambda.params);
        } else {
          Joiner.on(", ").appendTo(writer, jcLambda.params.map(JCVariableDecl::getName));
        }
        if (paramsNeedParentheses) {
          print(")");
        }
        print(" -> ");
        printExpr(jcLambda.body);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private static class ImportAndQualificationFinder extends TreePathScanner<Void, Void> {
    final IdentityHashMap<IdentifierTree, JCExpression> qualifications = new IdentityHashMap<>();
    final Set<String> imports = new TreeSet<>();
    final Set<String> staticImports = new TreeSet<>();

    private final ClassSymbol classSymbol;
    private final TreeMaker treeMaker;
    private final VisitorState state;

    ImportAndQualificationFinder(ClassSymbol classSymbol, VisitorState state) {
      this.classSymbol = classSymbol;
      this.treeMaker = state.getTreeMaker();
      this.state = state;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {
      // We only want to import a MemberSelect if this is the "top" of the member select
      // chain.
      if (!(node.getExpression() instanceof MemberSelectTree)) {
        Symbol symbol = getSymbol(node);
        if (symbol.isStatic()) {
          maybeAddImport(symbol.owner);
        }
      }

      return super.visitMemberSelect(node, null);
    }

    private void maybeAddImport(Symbol symbol) {
      if (symbol != null) {
        addImport(symbol.getQualifiedName().toString());
      }
    }

    private void addImport(String clazzName) {
      if (!clazzName.isEmpty() && !clazzName.startsWith("java.lang")) {
        imports.add(clazzName);
      }
    }

    @Override
    public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
      if (identifierTree.getName().contentEquals("this")) {
        return super.visitIdentifier(identifierTree, unused);
      }
      Symbol symbol = getSymbol(identifierTree);
      if (symbol == null || ASTHelpers.isLocal(symbol)) {
        return super.visitIdentifier(identifierTree, unused);
      }

      Tree parentNode = getCurrentPath().getParentPath().getLeaf();
      if (nameUsageDoesntRequireQualificationOrImport(parentNode)) {
        return super.visitIdentifier(identifierTree, unused);
      }

      // TODO(glorioso): This suggestion has the following behavior:
      //   * instance methods: foo() -> this.foo(), no import needed
      //   * static methods in other classes: foo() -> foo(), static import Owner.foo;
      //   * static methods in this class: myFoo() -> Me.myFoo(), import Me;
      //   That seems wrong. Perhaps move the import logic from the other bits here?
      boolean isMemberOfThisClass = isMemberOfThisClass(symbol, parentNode);
      boolean nameUsageRequiresNoQualification = nameUsageDoesntRequireQualification(parentNode);
      if (symbol.isStatic()) {
        if (isMemberOfThisClass) {
          addImport(classSymbol.getQualifiedName().toString());
          if (!nameUsageRequiresNoQualification) {
            qualifications.put(identifierTree, treeMaker.Ident(classSymbol));
          }
        } else {
          if (parentNode instanceof NewClassTree) {
            // This Identifier is the class being constructed
            addImport(symbol.getQualifiedName().toString());
          } else {
            // Regular static methods
            staticImports.add(symbol.owner.getQualifiedName() + "." + symbol.getQualifiedName());
          }
        }
      } else {
        if (isMemberOfThisClass) {
          if (!nameUsageRequiresNoQualification) {
            qualifications.put(identifierTree, treeMaker.This(classSymbol.type));
          }
        } else {
          addImport(symbol.getQualifiedName().toString());
        }
      }
      return super.visitIdentifier(identifierTree, unused);
    }

    private boolean isMemberOfThisClass(Symbol symbol, Tree parentNode) {
      return symbol.owner != null
          && classSymbol.isSubClass(symbol.owner, state.getTypes())
          && !(parentNode instanceof NewClassTree);
    }

    private static boolean nameUsageDoesntRequireQualificationOrImport(Tree parentNode) {
      return parentNode instanceof MemberSelectTree;
    }

    private static boolean nameUsageDoesntRequireQualification(Tree parentNode) {
      return parentNode instanceof NewClassTree
          || parentNode instanceof MemberReferenceTree
          || parentNode instanceof TypeCastTree
          || parentNode instanceof NewArrayTree;
    }
  }

  private static ImmutableSet<String> getStrings(Attribute.Compound attribute, String name) {
    return getValue(attribute, name)
        .map(MoreAnnotations::asStrings)
        .orElse(Stream.empty())
        .collect(toImmutableSet());
  }

  private static final CharMatcher SEMICOLON = CharMatcher.is(';');

  private static String trimTrailingSemicolons(String s) {
    return SEMICOLON.trimTrailingFrom(s);
  }
}
