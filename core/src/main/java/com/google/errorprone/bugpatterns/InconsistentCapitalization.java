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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.Map.Entry;
import javax.lang.model.element.ElementKind;

/** Checker for variables under the same scope that only differ in capitalization. */
@BugPattern(
    name = "InconsistentCapitalization",
    summary =
        "It is confusing to have a field and a parameter under the same scope that differ only in "
            + "capitalization.",
    category = JDK,
    severity = WARNING,
    generateExamplesFromTestCases = false,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class InconsistentCapitalization extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ImmutableSet<Symbol> fields = FieldScanner.findFields(tree);

    if (fields.isEmpty()) {
      return Description.NO_MATCH;
    }

    ImmutableMap<String, Symbol> fieldNamesMap =
        fields.stream()
            .collect(
                toImmutableMap(symbol -> symbol.toString().toLowerCase(), x -> x, (x, y) -> x));
    ImmutableMap<TreePath, Symbol> matchedParameters =
        MatchingParametersScanner.findMatchingParameters(fieldNamesMap, state.getPath());

    if (matchedParameters.isEmpty()) {
      return Description.NO_MATCH;
    }

    for (Entry<TreePath, Symbol> entry : matchedParameters.entrySet()) {
      TreePath parameterPath = entry.getKey();
      Symbol field = entry.getValue();
      String fieldName = field.getSimpleName().toString();
      VariableTree parameterTree = (VariableTree) parameterPath.getLeaf();
      SuggestedFix.Builder fix =
          SuggestedFix.builder()
              .merge(SuggestedFixes.renameVariable(parameterTree, fieldName, state));

      if (parameterPath.getParentPath() != null) {
        String qualifiedName =
            getExplicitQualification(parameterPath, tree, state) + field.getSimpleName();
        // If the field was accessed in a non-qualified way, by renaming the parameter this may
        // cause clashes with it. Thus, it is required to qualify all uses of the field within the
        // parameter's scope just in case.
        parameterPath
            .getParentPath()
            .getLeaf()
            .accept(
                new TreeScanner<Void, Void>() {
                  @Override
                  public Void visitIdentifier(IdentifierTree tree, Void unused) {
                    if (field.equals(ASTHelpers.getSymbol(tree))) {
                      fix.replace(tree, qualifiedName);
                    }
                    return null;
                  }
                },
                null);
      }
      state.reportMatch(
          buildDescription(parameterPath.getLeaf())
              .setMessage(
                  String.format(
                      "Found the field '%s' with the same name as the parameter '%s' but with "
                          + "different capitalization.",
                      fieldName, ((VariableTree) parameterPath.getLeaf()).getName()))
              .addFix(fix.build())
              .build());
    }

    return Description.NO_MATCH;
  }

  /**
   * Returns the qualification to access a field of the given class node from within the given tree
   * path (which MUST be within the class node scope).
   */
  private static String getExplicitQualification(
      TreePath path, ClassTree tree, VisitorState state) {
    for (Tree node : path) {
      if (node.equals(tree)) {
        break;
      }
      if (node instanceof ClassTree) {
        if (ASTHelpers.getSymbol(node).isSubClass(ASTHelpers.getSymbol(tree), state.getTypes())) {
          return "super.";
        }
        return tree.getSimpleName() + ".this.";
      }
    }
    return "this.";
  }

  /** Returns true if the given symbol has static modifier and is all upper case. */
  private static boolean isUpperCaseAndStatic(Symbol symbol) {
    return symbol.isStatic() && symbol.name.contentEquals(symbol.name.toString().toUpperCase());
  }

  /**
   * Matcher for all fields of the given class node that are either instance members or not all
   * upper case.
   */
  private static class FieldScanner extends TreeScanner<Void, Void> {

    static ImmutableSet<Symbol> findFields(ClassTree tree) {
      ImmutableSet.Builder<Symbol> fieldsBuilder = ImmutableSet.builder();
      new FieldScanner(fieldsBuilder, tree).scan(tree, null);
      return fieldsBuilder.build();
    }

    private final ImmutableSet.Builder<Symbol> fields;
    private final Symbol classSymbol;

    private FieldScanner(ImmutableSet.Builder<Symbol> fields, Tree classTree) {
      this.fields = fields;
      this.classSymbol = ASTHelpers.getSymbol(classTree);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      Symbol symbol = ASTHelpers.getSymbol(tree);
      /* It is quite common to have upper case static field names that match variable names,
       * as for example between HTTP request parameters name definitions and their corresponding
       * extracted value. */
      if (symbol.getKind().equals(ElementKind.FIELD)
          && !isUpperCaseAndStatic(symbol)
          && ASTHelpers.enclosingClass(symbol).equals(classSymbol)) {
        fields.add(symbol);
      }
      return super.visitVariable(tree, unused);
    }
  }

  /**
   * Matcher for all parameters (methods, constructors, lambda expressions) that have the same name
   * as one of the provided fields but with different capitalization.
   */
  private static class MatchingParametersScanner extends TreePathScanner<Void, Void> {

    static ImmutableMap<TreePath, Symbol> findMatchingParameters(
        ImmutableMap<String, Symbol> fieldNamesMap, TreePath path) {
      ImmutableMap.Builder<TreePath, Symbol> matchedParametersBuilder = ImmutableMap.builder();
      new MatchingParametersScanner(fieldNamesMap, matchedParametersBuilder).scan(path, null);
      return matchedParametersBuilder.build();
    }

    private final ImmutableMap<String, Symbol> fields;
    private final ImmutableMap.Builder<TreePath, Symbol> matchedParameters;

    private MatchingParametersScanner(
        ImmutableMap<String, Symbol> fields,
        ImmutableMap.Builder<TreePath, Symbol> matchedParameters) {
      this.fields = fields;
      this.matchedParameters = matchedParameters;
    }

    @Override
    public Void visitMethod(MethodTree tree, Void unused) {
      // Ignore synthetic constructors:
      if (ASTHelpers.isGeneratedConstructor(tree)) {
        return null;
      }
      return super.visitMethod(tree, null);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      Symbol symbol = ASTHelpers.getSymbol(tree);
      if (!symbol.getKind().equals(ElementKind.PARAMETER)) {
        return super.visitVariable(tree, unused);
      }
      String variableName = symbol.toString();
      Symbol matchedField = fields.get(variableName.toLowerCase());
      if (matchedField != null) {
        String fieldName = matchedField.toString();
        if (!variableName.equals(fieldName)) {
          matchedParameters.put(getCurrentPath(), matchedField);
        }
      }
      return super.visitVariable(tree, unused);
    }
  }
}
