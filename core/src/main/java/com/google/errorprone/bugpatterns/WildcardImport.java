/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.TreeScanner;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Enforce § 3.3.1 of the style guide.
 *
 * <p>https://google-styleguide.googlecode.com/svn/trunk/javaguide.html#s3.3.1-wildcard-imports
 *
 * @author cushon@google.com (Liam Miller-Cushon)
*/
@BugPattern(name = "WildcardImport",
    summary =  "Use of wildcard imports is forbidden",
    explanation = "Wildcard imports are forbidden by §3.3.1 of the Google Java Style Guide.\n\n"
        + " They make code brittle and difficult to reason about, both for programmers and for"
        + " tools."
        + " In the following example, the processing of the first import requires reasoning about"
        + " the classes `Outer` and `Nested`, including their supertypes, and ends up depending"
        + " on information in the second import statement. This code is (incorrectly) rejected"
        + " by the latest version of javac. Also, note that `I` is actually being imported via"
        + " the name `p.q.C.I` even though it's not declared in `C`! Its canonical name is"
        + " `p.q.D.I`. Regular single-type imports require that all types are imported by"
        + " their canonical name, but static imports do not.\n\n"
        + "```java\n"
        + "package p;\n\n"
        + "import static p.Outer.Nested.*;\n"
        + "import static p.q.C.*;\n\n"
        + "public class Outer {\n"
        + "  public static class Nested implements I {\n"
        + "  }\n"
        + "}\n"
        + "```\n"
        + "```java\n"
        + "package p.q;\n\n"
        + "public class C extends D {\n"
        + "}\n"
        + "```\n"
        + "```java\n"
        + "package p.q;\n\n"
        + "public class D {\n"
        + "  public interface I {\n"
        + "  }\n"
        + "}\n"
        + "```\n",
    category = GUAVA, severity = ERROR, maturity = EXPERIMENTAL)
public class WildcardImport extends BugChecker implements ClassTreeMatcher {

  private static final String ASTERISK = "*";

  /**
   * An abstraction over the logic for generating fixes. The 'test' version exists because we
   * don't have a good way to test import-only fixes, so it pretty-prints the replacements as a
   * suffix of each on-demand import being deleted.
   */
  // TODO(user): create a way to test import-only fixes.
  private final FixStrategy fixStrategy;

  public WildcardImport() {
    this(FixStrategies.PRODUCTION);
  }

  @VisibleForTesting
  public WildcardImport(FixStrategy fixStrategy) {
    this.fixStrategy = fixStrategy;
  }

  /** A type or member that needs to be imported. */
  @AutoValue
  abstract static class TypeToImport {

    /**
     * Returns the fully-qualified canonical name.
     */
    abstract String typeName();

    /**
     * Returns true if the import needs to be static (i.e. the imported is for a field or method).
     */
    abstract boolean isStatic();

    /**
     * Generates an import statement.
     */
    String generateImportStatement() {
      return "import " + (isStatic() ? "static " : "") + typeName() + ";";
    }

    @Override
    public String toString() {
      return generateImportStatement();
    }

    static TypeToImport create(String type, boolean stat) {
      return new AutoValue_WildcardImport_TypeToImport(type, stat);
    }
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    // Only match on top-level classes. The analysis walks the entire class declaration looking
    // for types that need to be imported, so matching on nested classes will produce incomplete
    // results.
    if (sym.getEnclosingElement() != null
        && sym.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
      return Description.NO_MATCH;
    }

    // First, look for on-demand imports. If there aren't any, we're done. This ensures that we
    // only walk the class declaration if there's an error.
    CompilationUnitTree unit = state.getPath().getCompilationUnit();
    ImmutableList<ImportTree> wildcardImports = getWildcardImports(unit.getImports());
    if (wildcardImports.isEmpty()) {
      return Description.NO_MATCH;
    }

    // Find all of the types that need to be imported.
    Set<TypeToImport> typesToImport = ImportCollector.collect((JCClassDecl) tree);

    // Group the imported types by the on-demand import they replace.
    Multimap<ImportTree, TypeToImport> toFix = groupImports(wildcardImports, typesToImport);

    // Generate fixes.
    fixStrategy.apply(tree, state, wildcardImports, toFix, this);

    // TODO(user): this is kind of a hack. We allow the fix strategy to generate multiple small
    // fixes, so by the time we get here all of the fixes were emitted and there's nothing to do.
    return Description.NO_MATCH;
  }

  /**
   * Creates a multimap from existing on-demand imports to the single-type imports we're going to
   * add, e.g.: java.util.* -> [java.util.List, java.util.ArrayList]
   */
  private Multimap<ImportTree, TypeToImport> groupImports(
      ImmutableList<ImportTree> wildcardImports, Set<TypeToImport> typesToImport) {
    Multimap<ImportTree, TypeToImport> toFix = LinkedListMultimap.create();
    for (TypeToImport type : typesToImport) {
      ImportTree wildcard = findMatchingWildcardImport(wildcardImports, type);
      if (wildcard != null) {
        toFix.put(wildcard, type);
      }
      // The 'else' case here is interesting - it's possible that one of the types / members
      // referenced doesn't match up to any on-demand imports. It's probably something that was
      // already visible in the scope where it was accessed. There don't appear to be any good
      // ways to answer the question "did we need imports to resolve this name" via accessible
      // javac state, and the current approach works very well in practice.
      //
      // However, there's a chance that we're dropping a real import because the canonical name of
      // the type doesn't match the static on-demand import that was used to import it. ¯\_(ツ)_/¯
    }
    return toFix;
  }

  /**
   * Find an on-demand import matching the given single-type import specification.
   */
  @Nullable
  private ImportTree findMatchingWildcardImport(
      ImmutableList<ImportTree> wildcardImports, TypeToImport type) {
    for (ImportTree importTree : wildcardImports) {
      // Get the name of the on-demand import's scope, e.g. 'java.util.*' -> 'java.util'. It's
      // guaranteed to be a MemberSelectTree by getWildcardImports().
      String importBase =
          ((MemberSelectTree) importTree.getQualifiedIdentifier()).getExpression().toString();
      if (type.typeName().startsWith(importBase)) {
        // Only associate the single-type import with this on-demand import if the portion of
        // the single-type import after the on-demand import's scope is a simple name.
        //
        // e.g. if the on-demand import is 'java.nio.*', we would associate 'java.nio.Path'
        // but not 'java.nio.charset.Charset'.
        int next = type.typeName().indexOf('.', importBase.length() + 1);
        if (next == -1) {
          return importTree;
        }
      }
    }
    return null;
  }

  /**
   * Collect all on-demand imports.
   */
  private static ImmutableList<ImportTree> getWildcardImports(List<? extends ImportTree> imports) {
    ImmutableList.Builder<ImportTree> result = ImmutableList.builder();
    for (ImportTree tree : imports) {
      // javac represents on-demand imports as a member select where the selected name is '*'.
      Tree ident = tree.getQualifiedIdentifier();
      if (!(ident instanceof MemberSelectTree)) {
        continue;
      }
      MemberSelectTree select = (MemberSelectTree) ident;
      if (select.getIdentifier().toString().equals(ASTERISK)) {
        result.add(tree);
      }
    }
    return result.build();
  }

  /**
   * Walk a top-level class declaration (possibly traversing into nested classes), and collect
   * all types and static members that are referred to by name and may need to be imported.
   */
  static class ImportCollector extends TreeScanner {

    private Set<TypeToImport> seen = new LinkedHashSet<>();

    public static Set<TypeToImport> collect(JCClassDecl tree) {
      ImportCollector collector = new ImportCollector();
      collector.scan(tree);
      return collector.seen;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl method) {
      if (ASTHelpers.isGeneratedConstructor(method)) {
        // Skip types in the signatures of synthetic constructors
        scan(method.body);
      } else {
        super.visitMethodDef(method);
      }
    }

    @Override
    public void visitIdent(JCTree.JCIdent tree) {
      if (tree.sym != null) {
        switch (tree.sym.kind) {
          case Kinds.TYP:
            addType(tree.sym.type);
            break;
          case Kinds.VAR:
          case Kinds.MTH:
            addStaticMember(tree.sym);
            break;
          default:  // falls through
        }
      }
    }

    /**
     * Record a static field or method that may need to be imported.
     */
    private void addStaticMember(Symbol sym) {
      if (!sym.isStatic()) {
        return;
      }
      if (sym.owner == null) {
        return;
      }
      if (sym.isPrivate()) {
        return;
      }
      String canonicalName = sym.owner.getQualifiedName() + "." + sym.getQualifiedName();
      seen.add(TypeToImport.create(canonicalName, true));
    }

    /**
     * Record a type reference that may need to be imported.
     */
    private void addType(Type type) {
      if (type == null) {
        return;
      }
      Symbol.TypeSymbol sym = type.tsym;
      if (sym == null) {
        return;
      }
      if (sym.isPrivate()) {
        return;
      }
      seen.add(TypeToImport.create(sym.getQualifiedName().toString(), false));
    }
  }

  /**
   * Interface for the fix generating logic.
   */
  public interface FixStrategy {
    void apply(
        ClassTree tree,
        VisitorState state,
        ImmutableList<ImportTree> wildcardImports,
        Multimap<ImportTree, TypeToImport> toFix,
        BugChecker checker);
  }

  /**
   * Fix strategies for testing and production. (See the TODO above about better testing for
   * import-only fixes.)
   */
  public enum FixStrategies implements FixStrategy {
    PRODUCTION {
      /**
       * Delete all of the on-demand imports, and add all of the new imports.
       */
      @Override
      public void apply(
          ClassTree tree,
          VisitorState state,
          ImmutableList<ImportTree> wildcardImports,
          Multimap<ImportTree, TypeToImport> toFix,
          BugChecker checker) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        for (ImportTree importToDelete : wildcardImports) {
          String importSpecification = importToDelete.getQualifiedIdentifier().toString();
          if (importToDelete.isStatic()) {
            fix.removeStaticImport(importSpecification);
          } else {
            fix.removeImport(importSpecification);
          }
        }
        for (TypeToImport toImport : toFix.values()) {
          if (toImport.isStatic()) {
            fix.addStaticImport(toImport.typeName());
          } else {
            fix.addImport(toImport.typeName());
          }
        }
        if (!fix.isEmpty()) {
          state.reportMatch(
              buildDescriptionFromChecker(wildcardImports.get(0), checker)
                  .addFix(fix.build()).build());
        }
      }
    },
    TEST {
      /**
       * Match up the replacements with each on-demand import being deleted, and pretty print
       * them as a suffix.
       */
      @Override
      public void apply(
          ClassTree tree,
          VisitorState state,
          ImmutableList<ImportTree> wildcardImports,
          Multimap<ImportTree, TypeToImport> toFix,
          BugChecker checker) {
        for (ImportTree toDelete : wildcardImports) {
          Iterable<TypeToImport> replacements = toFix.get(toDelete);
          Fix fix = SuggestedFix.postfixWith(toDelete, replacements.toString());
          state.reportMatch(buildDescriptionFromChecker(toDelete, checker).addFix(fix).build());
        }
      }
    }
  }
}
