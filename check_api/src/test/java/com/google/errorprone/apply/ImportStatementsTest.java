/*
 * Copyright 2012 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.apply;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.util.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link ImportStatements}
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class ImportStatementsTest {
  private static final EndPosTable FAKE_END_POS_MAP =
      new EndPosTable() {
        @Override
        public int getEndPos(JCTree tree) {
          return Position.NOPOS;
        }

        @Override
        public void storeEnd(JCTree tree, int endpos) {}

        @Override
        public int replaceTree(JCTree oldtree, JCTree newtree) {
          return Position.NOPOS;
        }
      };

  /** A stubbed package JCExpression to use for testing. */
  private final JCExpression basePackage = stubPackage(79);

  /** An unsorted list of JCImport stubs to use for testing. */
  private final List<JCImport> baseImportList =
      new StubImportBuilder(82)
          .addStaticImport("com.google.common.base.Preconditions.checkNotNull")
          .addStaticImport("com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED")
          .addImport("com.google.common.collect.ImmutableMap")
          .addImport("com.google.common.collect.ImmutableList")
          .addImport("org.joda.time.Interval")
          .addImport("org.joda.time.DateTime")
          .addImport("org.joda.time.DateTimeZone")
          .addImport("com.sun.tools.javac.tree.JCTree")
          .addImport("com.sun.source.tree.ImportTree")
          .addImport("com.sun.tools.javac.tree.JCTree.JCExpression")
          .addImport("com.sun.source.tree.CompilationUnitTree")
          .addImport("java.io.File")
          .addImport("java.util.Iterator")
          .addImport("java.io.IOException")
          .addImport("javax.tools.StandardJavaFileManager")
          .addImport("javax.tools.JavaFileObject")
          .addImport("javax.tools.JavaCompiler")
          .addImport("javax.tools.ToolProvider")
          .build();

  /** Makes it easy to build a consecutive list of JCImport mocks. */
  private static class StubImportBuilder {
    private int startPos;
    private final ImmutableList.Builder<JCImport> imports = ImmutableList.builder();

    StubImportBuilder(int startPos) {
      this.startPos = startPos;
    }

    /**
     * A helper method to create a JCImport stub.
     *
     * @param typeName the fully-qualified name of the type being imported
     * @return a new JCImport stub
     */
    StubImportBuilder addImport(String typeName) {
      return addImport(typeName, /* isStatic= */ false);
    }

    /**
     * A helper method to create a JCImport stub.
     *
     * @param typeName the fully-qualified name of the type being imported
     * @return a new JCImport stub
     */
    StubImportBuilder addStaticImport(String typeName) {
      return addImport(typeName, /* isStatic= */ true);
    }

    /**
     * A helper method to create a JCImport stub.
     *
     * @param typeName the fully-qualified name of the type being imported
     * @param isStatic whether the import is static
     * @return a new JCImport stub
     */
    private StubImportBuilder addImport(String typeName, boolean isStatic) {
      // craft import string
      StringBuilder returnSB = new StringBuilder("import ");
      if (isStatic) {
        returnSB.append("static ");
      }
      returnSB.append(typeName);
      returnSB.append(";\n");
      String stringRepresentation = returnSB.toString();

      // Calculate the end position of the input line.
      int endPos = startPos + stringRepresentation.length();
      int curStartPos = startPos;

      // TODO(b/67738557): consolidate helpers for creating fake trees
      JCImport result =
          new JCImport(/* qualid= */ null, /* importStatic= */ isStatic) {
            @Override
            public int getStartPosition() {
              return curStartPos;
            }

            @Override
            public int getEndPosition(EndPosTable endPosTable) {
              return endPos - 2;
            }

            @Override
            public String toString() {
              return stringRepresentation;
            }
          };

      imports.add(result);

      // Move the start position of the next import to the end of the previous one.
      startPos = endPos;

      return this;
    }

    List<JCImport> build() {
      return imports.build();
    }
  }

  /**
   * A helper method to create a stubbed package JCExpression.
   *
   * @param endPos the end position of the package JCExpression
   * @return a new package JCExpression stub
   */
  private static JCExpression stubPackage(int endPos) {
    // TODO(b/67738557): consolidate helpers for creating fake trees
    return new JCExpression() {
      @Override
      public Tag getTag() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void accept(Visitor visitor) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Kind getKind() {
        throw new UnsupportedOperationException();
      }

      @Override
      public <R, D> R accept(TreeVisitor<R, D> treeVisitor, D d) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int getStartPosition() {
        return endPos;
      }

      @Override
      public int getEndPosition(EndPosTable endPosTable) {
        return endPos;
      }
    };
  }

  private static ImportStatements createImportStatements(
      JCExpression basePackage, List<JCImport> importTrees) {
    return new ImportStatements(
        basePackage, importTrees, FAKE_END_POS_MAP, ImportOrganizer.STATIC_FIRST_ORGANIZER);
  }

  /** Test that the import statements are sorted according to the Google Style Guide. */
  @Test
  public void shouldSortImports() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);

    assertEquals(
        "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED;\n"
            + "import static com.google.common.base.Preconditions.checkNotNull;\n"
            + "\n"
            + "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;\n"
            + "import org.joda.time.Interval;",
        imports.toString());
  }

  /** Test that adding a new import inserts it in the correct position. */
  @Test
  public void shouldAddImportInCorrectPosition() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean added = imports.add("import static org.junit.Assert.assertEquals");

    assertTrue(added);
    assertEquals(
        "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED;\n"
            + "import static com.google.common.base.Preconditions.checkNotNull;\n"
            + "import static org.junit.Assert.assertEquals;\n"
            + "\n"
            + "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;\n"
            + "import org.joda.time.Interval;",
        imports.toString());
  }

  /** Test that adding multiple new imports using addAll() inserts them in the correct positions. */
  @Test
  public void shouldAddMultipleImportsInCorrectPositions() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean added =
        imports.addAll(
            Arrays.asList(
                "import static org.junit.Assert.assertEquals",
                "import javax.servlet.http.HttpServletRequest",
                "import com.google.common.flags.FlagSpec"));

    assertTrue(added);
    assertEquals(
        "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED;\n"
            + "import static com.google.common.base.Preconditions.checkNotNull;\n"
            + "import static org.junit.Assert.assertEquals;\n"
            + "\n"
            + "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.google.common.flags.FlagSpec;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.servlet.http.HttpServletRequest;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;\n"
            + "import org.joda.time.Interval;",
        imports.toString());
  }

  /** Test that adding an already-existing import doesn't change anything. */
  @Test
  public void shouldNotAddExistingImport() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean added = imports.add("import com.google.common.collect.ImmutableMap");

    assertTrue(!added);
    assertEquals(
        "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED;\n"
            + "import static com.google.common.base.Preconditions.checkNotNull;\n"
            + "\n"
            + "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;\n"
            + "import org.joda.time.Interval;",
        imports.toString());
  }

  /** Test that removing an import works and the resulting output is correctly sorted. */
  @Test
  public void shouldRemoveImportAndSort() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean removed = imports.remove("import com.sun.tools.javac.tree.JCTree");

    assertTrue(removed);
    assertEquals(
        "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED;\n"
            + "import static com.google.common.base.Preconditions.checkNotNull;\n"
            + "\n"
            + "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;\n"
            + "import org.joda.time.Interval;",
        imports.toString());
  }

  /**
   * Test that removing multiple imports using removeAll() works and the resulting output is
   * correctly sorted.
   */
  @Test
  public void shouldRemoveMultipleImportsAndSort() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean removed =
        imports.removeAll(
            Arrays.asList(
                "import com.sun.tools.javac.tree.JCTree",
                "import static com.google.common.base.Preconditions.checkNotNull",
                "import org.joda.time.Interval"));

    assertTrue(removed);
    assertEquals(
        "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED;\n"
            + "\n"
            + "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;",
        imports.toString());
  }

  /** Tests that a list of imports with no static imports is handled correctly. */
  @Test
  public void noRemainingStaticImports() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean removed =
        imports.removeAll(
            Arrays.asList(
                "import static com.google.common.base.Preconditions.checkNotNull",
                "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED"));

    assertTrue(removed);
    assertEquals(
        "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;\n"
            + "import org.joda.time.Interval;",
        imports.toString());
  }

  /** Test that removing a non-existent import doesn't change anything. */
  @Test
  public void removingNonExistingImportShouldntChangeImports() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean removed = imports.remove("import org.joda.time.format.ISODateTimeFormat;\n");

    assertTrue(!removed);
    assertEquals(
        "import static com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED;\n"
            + "import static com.google.common.base.Preconditions.checkNotNull;\n"
            + "\n"
            + "import com.google.common.collect.ImmutableList;\n"
            + "import com.google.common.collect.ImmutableMap;\n"
            + "import com.sun.source.tree.CompilationUnitTree;\n"
            + "import com.sun.source.tree.ImportTree;\n"
            + "import com.sun.tools.javac.tree.JCTree;\n"
            + "import com.sun.tools.javac.tree.JCTree.JCExpression;\n"
            + "import java.io.File;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Iterator;\n"
            + "import javax.tools.JavaCompiler;\n"
            + "import javax.tools.JavaFileObject;\n"
            + "import javax.tools.StandardJavaFileManager;\n"
            + "import javax.tools.ToolProvider;\n"
            + "import org.joda.time.DateTime;\n"
            + "import org.joda.time.DateTimeZone;\n"
            + "import org.joda.time.Interval;",
        imports.toString());
  }

  /** Test empty initial import list. Positions should match package end positions. */
  @Test
  public void emptyImportListShouldGivePositionOfPackageStmt() {
    ImportStatements imports = createImportStatements(basePackage, new ArrayList<JCImport>());
    assertEquals(81, imports.getStartPos());
    assertEquals(81, imports.getEndPos());
  }

  /**
   * Test empty initial import list. The output string should start and end with newlines because it
   * is intended to be inserted after the package statement.
   */
  @Test
  public void addingToEmptyImportListOutputShouldStartAndEndWithNewlines() {
    ImportStatements imports = createImportStatements(basePackage, new ArrayList<JCImport>());
    imports.add("import org.joda.time.Interval");
    assertEquals("\n" + "import org.joda.time.Interval;\n", imports.toString());
  }

  /**
   * Test start and end position calculations. The start position should be the start offset of the
   * first import statement, and the end position should be the end position of the last import
   * statement.
   */
  @Test
  public void startAndEndPositionsShouldComeFromImportStatements() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    assertEquals(82, imports.getStartPos());
    assertEquals(806, imports.getEndPos());
  }

  @Test
  public void addingToEmptyImportListInDefaultPackage() {
    ImportStatements imports = createImportStatements(null, new ArrayList<>());
    imports.add("import java.util.List");
    assertEquals(0, imports.getStartPos());
    assertEquals("\nimport java.util.List;\n", imports.toString());
  }

  @Test
  public void addsAllImports() {
    ImportStatements imports =
        new ImportStatements(
            null,
            new ArrayList<>(),
            FAKE_END_POS_MAP,
            new ImportOrganizer() {
              @Override
              public OrganizedImports organizeImports(List<Import> imports) {
                return new OrganizedImports();
              }
            });

    imports.add("import java.util.List");
    IllegalStateException exception = assertThrows(IllegalStateException.class, imports::toString);
    assertEquals(
        "Expected 1 import(s) in the organized imports but it contained 0", exception.getMessage());
  }
}
