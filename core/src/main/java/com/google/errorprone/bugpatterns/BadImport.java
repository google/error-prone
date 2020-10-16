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
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.StaticImports.StaticImportInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Name;

/** @author awturner@google.com (Andy Turner) */
@BugPattern(
    name = "BadImport",
    summary =
        "Importing nested classes/static methods/static fields with commonly-used names can make "
            + "code harder to read, because it may not be clear from the context exactly which "
            + "type is being referred to. Qualifying the name with that of the containing class "
            + "can make the code clearer.",
    severity = WARNING)
public class BadImport extends BugChecker implements ImportTreeMatcher {

  private static final String MESSAGE =
      "Importing nested classes/static methods/static fields with commonly-used names can make "
          + "code harder to read, because it may not be clear from the context exactly which "
          + "type is being referred to. Qualifying the name with that of the containing class "
          + "can make the code clearer. Here we recommend using qualified class: %s";

  static final ImmutableSet<String> BAD_NESTED_CLASSES =
      ImmutableSet.of(
          "Builder",
          "Callback",
          "Class",
          "Entry",
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
          "of",
          "valueOf");

  private static final MultiMatcher<Tree, AnnotationTree> HAS_TYPE_USE_ANNOTATION =
      annotations(AT_LEAST_ONE, (t, state) -> isTypeAnnotation(t));

  private static final String MESSAGE_LITE = "com.google.protobuf.MessageLite";

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    Symbol symbol;
    ImmutableSet<Symbol> symbols;

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
      if (isAcceptableImport(symbol, BAD_STATIC_IDENTIFIERS)) {
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

    if (isSubtype(symbol.type, state.getTypeFromString(MESSAGE_LITE), state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder builder =
        SuggestedFix.builder().removeImport(symbol.getQualifiedName().toString());

    // Have to start at the symbol's enclosing element because otherwise we find the symbol again
    // immediately.
    String replacement =
        SuggestedFixes.qualifyType(getCheckState(state), builder, symbol.getEnclosingElement())
            + ".";

    return buildDescription(builder, symbols, replacement, state);
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
    if (!(tree instanceof ClassTree) || ((ClassTree) tree).getMembers().isEmpty()) {
      return state;
    }
    return state.withPath(
        TreePath.getPath(compilationUnit, ((ClassTree) tree).getMembers().get(0)));
  }

  private static boolean isAcceptableImport(Symbol symbol, Set<String> badNames) {
    Name simpleName = symbol.getSimpleName();
    return badNames.stream().noneMatch(simpleName::contentEquals);
  }

  private Description buildDescription(
      SuggestedFix.Builder builder,
      Set<Symbol> symbols,
      String enclosingReplacement,
      VisitorState state) {
    CompilationUnitTree compilationUnit = state.getPath().getCompilationUnit();
    TreePath path = TreePath.getPath(compilationUnit, compilationUnit);
    IdentifierTree firstFound =
        new SuppressibleTreePathScanner<IdentifierTree, Void>() {
          @Override
          public IdentifierTree reduce(IdentifierTree r1, IdentifierTree r2) {
            return (r2 != null) ? r2 : r1;
          }

          @Override
          public IdentifierTree visitIdentifier(IdentifierTree node, Void unused) {
            Symbol nodeSymbol = getSymbol(node);
            if (symbols.contains(nodeSymbol) && !isSuppressed(node)) {
              if (getCurrentPath().getParentPath().getLeaf().getKind() != Kind.CASE) {
                builder.prefixWith(node, enclosingReplacement);
                moveTypeAnnotations(node);
                return node;
              }
            }
            return super.visitIdentifier(node, unused);
          }

          // We need to move any type annotation inside the qualified usage to preserve semantics,
          // e.g. @Nullable Builder -> SomeClass.@Nullable Builder.
          private void moveTypeAnnotations(IdentifierTree node) {
            Tree parent = getCurrentPath().getParentPath().getLeaf();
            switch (parent.getKind()) {
              case METHOD:
              case VARIABLE:
              case ANNOTATED_TYPE:
                moveTypeAnnotations(node, parent, state, builder);
                break;
              case PARAMETERIZED_TYPE:
                Tree grandParent = getCurrentPath().getParentPath().getParentPath().getLeaf();
                if (grandParent.getKind() == Kind.VARIABLE
                    || grandParent.getKind() == Kind.METHOD) {
                  moveTypeAnnotations(node, grandParent, state, builder);
                }
                break;
              default:
                // Do nothing.
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
    return buildDescription(firstFound)
        .setMessage(String.format(MESSAGE, enclosingReplacement))
        .addFix(builder.build())
        .build();
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
}
