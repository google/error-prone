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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.StaticImports.StaticImportInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Type;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.lang.model.element.Name;

/**
 * @author awturner@google.com (Andy Turner)
 */
@BugPattern(
    summary =
        "Importing nested classes/static methods/static fields with commonly-used names can make "
            + "code harder to read, because it may not be clear from the context exactly which "
            + "type is being referred to. Qualifying the name with that of the containing class "
            + "can make the code clearer.",
    severity = WARNING)
public class BadImport extends BugChecker implements ImportTreeMatcher {
  /**
   * Class names which are bad as a direct import if they have an enclosing class.
   *
   * <p>The common factor for these names isn't just that they may be vague class names; there are
   * many more examples of that. What's important is that they are vague <em>and</em> generally
   * clarified by the name of the outer class (that is, {@code Foo.Builder} is clearer than {@code
   * Builder}).
   */
  static final ImmutableSet<String> BAD_NESTED_CLASSES =
      ImmutableSet.of(
          "Builder",
          "BuilderFactory",
          "Callback",
          "Class",
          "Enum",
          "Factory",
          "Type",
          "Key",
          "Id",
          "Provider");

  private static final ImmutableSet<String> BAD_STATIC_IDENTIFIERS =
      ImmutableSet.of(
          "builder",
          "create",
          "copyOf",
          "from",
          "getDefaultInstance",
          "INSTANCE",
          "newBuilder",
          "newInstance",
          "of",
          "valueOf");

  private static final MultiMatcher<Tree, AnnotationTree> HAS_TYPE_USE_ANNOTATION =
      annotations(AT_LEAST_ONE, (t, state) -> isTypeAnnotation(t));

  private static final String MESSAGE_LITE = "com.google.protobuf.MessageLite";

  /**
   * Enclosing types that their nested type imports are vague.
   *
   * <p>Some types are meant to provide a namespace; therefore, imports for their nested types can
   * be confusing.
   *
   * <p>For instance, unlike its name suggests, {@code org.immutables.value.Value.Immutable} is used
   * to generate immutable value types, and its import can be misleading. So, importing {@code
   * org.immutables.value.Value} and using {@code @Value.Immutable} is more favorable than importing
   * {@code org.immutables.value.Value.Immutable} and using {@code @Immutable}.
   *
   * <p>Note that this does not disallow import an enclosing type but its nested types instead.
   */
  private final ImmutableSet<String> badEnclosingTypes;

  private final boolean warnAboutTruth8AssertThat;

  @Inject
  BadImport(ErrorProneFlags errorProneFlags) {
    this.badEnclosingTypes = errorProneFlags.getSetOrEmpty("BadImport:BadEnclosingTypes");
    this.warnAboutTruth8AssertThat = errorProneFlags.getBoolean("BadImport:Truth8").orElse(false);
  }

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    Symbol symbol;
    ImmutableSet<Symbol> symbols;
    boolean useTruth8Message = false;

    if (!tree.isStatic()) {
      symbol = getSymbol(tree.getQualifiedIdentifier());
      if (symbol == null || isAcceptableImport(symbol, BAD_NESTED_CLASSES)) {
        return Description.NO_MATCH;
      }

      symbols = ImmutableSet.of(symbol);
    } else {
      StaticImportInfo staticImportInfo = StaticImports.tryCreate(tree, state);
      if (staticImportInfo == null || staticImportInfo.members().isEmpty()) {
        return Description.NO_MATCH;
      }

      symbols = staticImportInfo.members();

      // Pick an arbitrary symbol. They've all got the same simple name, so it doesn't matter which.
      symbol = symbols.iterator().next();
      if (warnAboutTruth8AssertThat && symbol.owner.name.contentEquals("Truth8")) {
        useTruth8Message = true;
        // Now we fall through, which treats the import as an unacceptable.
      } else if (isAcceptableImport(symbol, BAD_STATIC_IDENTIFIERS)) {
        return Description.NO_MATCH;
      }
    }
    if (state.getPath().getCompilationUnit().getTypeDecls().stream()
        .anyMatch(c -> symbol.outermostClass().equals(getSymbol(c)))) {
      return Description.NO_MATCH;
    }

    if (symbol.getEnclosingElement() instanceof PackageSymbol) {
      return Description.NO_MATCH;
    }

    if (isSubtype(symbol.type, COM_GOOGLE_PROTOBUF_MESSAGELITE.get(state), state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder builder =
        SuggestedFix.builder().removeImport(symbol.getQualifiedName().toString());

    // Have to start at the symbol's enclosing element because otherwise we find the symbol again
    // immediately.
    String replacement =
        SuggestedFixes.qualifyType(getCheckState(state), builder, symbol.getEnclosingElement())
            + ".";

    String message =
        useTruth8Message
            ? "Avoid static import for Truth8.assertThat. While we usually recommend static import"
                + " for assertThat methods, static imports of Truth8.assertThat prevent us from"
                + " copying those methods to the main Truth class."
            : String.format(
                "Importing nested classes/static methods/static fields with commonly-used names can"
                    + " make code harder to read, because it may not be clear from the context"
                    + " exactly which type is being referred to. Qualifying the name with that of"
                    + " the containing class can make the code clearer. Here we recommend using"
                    + " qualified class: %s",
                replacement);
    return buildDescription(builder, symbols, replacement, state, message);
  }

  private static VisitorState getCheckState(VisitorState state) {
    // Gets the VisitorState to start from when checking how to qualify the name. This won't work
    // correctly in all cases 1) it assumes there is only 1 top level type; 2) it doesn't look for
    // all of the locations where the symbol-to-be-replaced is used in the compilation unit.
    // Really, we should gather all of the usages first, and check them all.
    // It is assumed that this will work sufficiently until proven otherwise.
    CompilationUnitTree compilationUnit = state.getPath().getCompilationUnit();
    if (compilationUnit.getTypeDecls().isEmpty()) {
      return state;
    }
    Tree tree = compilationUnit.getTypeDecls().get(0);
    if (!(tree instanceof ClassTree classTree) || classTree.getMembers().isEmpty()) {
      return state;
    }
    return state.withPath(TreePath.getPath(compilationUnit, classTree.getMembers().get(0)));
  }

  private boolean isAcceptableImport(Symbol symbol, Set<String> badNames) {
    Name ownerName = symbol.owner.getQualifiedName();
    Name simpleName = symbol.getSimpleName();
    return badEnclosingTypes.stream().noneMatch(ownerName::contentEquals)
        && badNames.stream().noneMatch(simpleName::contentEquals);
  }

  private Description buildDescription(
      SuggestedFix.Builder builder,
      Set<Symbol> symbols,
      String enclosingReplacement,
      VisitorState state,
      String message) {
    CompilationUnitTree compilationUnit = state.getPath().getCompilationUnit();
    TreePath path = TreePath.getPath(compilationUnit, compilationUnit);
    IdentifierTree firstFound =
        new SuppressibleTreePathScanner<IdentifierTree, Void>(state) {
          @Override
          public IdentifierTree reduce(IdentifierTree r1, IdentifierTree r2) {
            return (r2 != null) ? r2 : r1;
          }

          @Override
          public IdentifierTree visitIdentifier(IdentifierTree node, Void unused) {
            Symbol nodeSymbol = getSymbol(node);
            if (symbols.contains(nodeSymbol) && !isSuppressed(node, state)) {
              if (!(getCurrentPath().getParentPath().getLeaf() instanceof CaseTree)) {
                builder.prefixWith(node, enclosingReplacement);
                moveTypeAnnotations(node);
                return node;
              }
            }
            return super.visitIdentifier(node, null);
          }

          // We need to move any type annotation inside the qualified usage to preserve semantics,
          // e.g. @Nullable Builder -> SomeClass.@Nullable Builder.
          private void moveTypeAnnotations(IdentifierTree node) {
            Tree parent = getCurrentPath().getParentPath().getLeaf();
            switch (parent.getKind()) {
              case METHOD, VARIABLE, ANNOTATED_TYPE ->
                  moveTypeAnnotations(node, parent, state, builder);
              case PARAMETERIZED_TYPE -> {
                Tree grandParent = getCurrentPath().getParentPath().getParentPath().getLeaf();
                if (grandParent instanceof VariableTree || grandParent instanceof MethodTree) {
                  moveTypeAnnotations(node, grandParent, state, builder);
                }
              }
              default -> {
                // Do nothing.
              }
            }
          }

          private void moveTypeAnnotations(
              IdentifierTree node,
              Tree annotationHolder,
              VisitorState state,
              SuggestedFix.Builder builder) {
            for (AnnotationTree annotation :
                HAS_TYPE_USE_ANNOTATION.multiMatchResult(annotationHolder, state).matchingNodes()) {
              builder.delete(annotation);
              builder.prefixWith(node, state.getSourceForNode(annotation) + " ");
            }
          }
        }.scan(path, null);
    if (firstFound == null) {
      // If no usage of the symbol was found, just leave the import to be cleaned up by the unused
      // import fix.
      return Description.NO_MATCH;
    }
    return buildDescription(firstFound).setMessage(message).addFix(builder.build()).build();
  }

  private static boolean isTypeAnnotation(AnnotationTree t) {
    Symbol annotationSymbol = getSymbol(t.getAnnotationType());
    if (annotationSymbol == null) {
      return false;
    }
    Target target = annotationSymbol.getAnnotation(Target.class);
    if (target == null) {
      return false;
    }
    List<ElementType> value = Arrays.asList(target.value());
    return value.contains(ElementType.TYPE_USE) || value.contains(ElementType.TYPE_PARAMETER);
  }

  private static final Supplier<Type> COM_GOOGLE_PROTOBUF_MESSAGELITE =
      VisitorState.memoize(state -> state.getTypeFromString(MESSAGE_LITE));
}
