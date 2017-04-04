/*
 * Copyright 2012 Google Inc. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
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
  private static final EndPosTable FAKE_END_POS_MAP = new EndPosTable() {
    @Override
    public int getEndPos(JCTree tree) {
      return Position.NOPOS;
    }

    @Override
    public void storeEnd(JCTree tree, int endpos) {
    }

    @Override
    public int replaceTree(JCTree oldtree, JCTree newtree) {
      return Position.NOPOS;
    }
  };

  /**
   * A stubbed package JCExpression to use for testing.
   */
  private final JCExpression basePackage = stubPackage(79);

  /**
   * An unsorted list of JCImport stubs to use for testing.
   */
  private final List<JCImport> baseImportList = ImmutableList.of(
      stubImport("com.google.common.base.Preconditions.checkNotNull", true, 82, 145),
      stubImport("com.google.ads.pebl.AdGroupCriterionPredicate.PAUSED", true, 147, 213),
      stubImport("com.google.common.collect.ImmutableMap", false, 215, 260),
      stubImport("com.google.common.collect.ImmutableList", false, 262, 308),
      stubImport("org.joda.time.Interval", false, 310, 339),
      stubImport("org.joda.time.DateTime", false, 341, 370),
      stubImport("org.joda.time.DateTimeZone", false, 372, 405),
      stubImport("com.sun.tools.javac.tree.JCTree", false, 407, 445),
      stubImport("com.sun.source.tree.ImportTree", false, 447, 484),
      stubImport("com.sun.tools.javac.tree.JCTree.JCExpression", false, 486, 537),
      stubImport("com.sun.source.tree.CompilationUnitTree", false, 539, 585),
      stubImport("java.io.File", false, 587, 606),
      stubImport("java.util.Iterator", false, 608, 633),
      stubImport("java.io.IOException", false, 635, 661),
      stubImport("javax.tools.StandardJavaFileManager", false, 663, 705),
      stubImport("javax.tools.JavaFileObject", false, 707, 740),
      stubImport("javax.tools.JavaCompiler", false, 742, 773),
      stubImport("javax.tools.ToolProvider", false, 775, 806));


  /**
   * A helper method to create a stubbed package JCExpression.
   *
   * @param endPos the end position of the package JCExpression
   * @return a new package JCExpression stub
   */
  private static JCExpression stubPackage(int endPos) {
    JCExpression result = mock(JCExpression.class);
    when(result.getEndPosition(any(EndPosTable.class))).thenReturn(endPos);
    return result;
  }

  /**
   * A helper method to create a JCImport stub.
   *
   * @param typeName the fully-qualified name of the type being imported
   * @param isStatic whether the import is static
   * @param startPos the start position of the import statement
   * @param endPos the end position of the import statement
   * @return a new JCImport stub
   */
  private static JCImport stubImport(String typeName, boolean isStatic, int startPos, int endPos) {
    JCImport result = mock(JCImport.class);
    when(result.isStatic()).thenReturn(isStatic);
    when(result.getStartPosition()).thenReturn(startPos);
    when(result.getEndPosition(any(EndPosTable.class))).thenReturn(endPos);

    // craft import string
    StringBuilder returnSB = new StringBuilder("import ");
    if (isStatic) {
      returnSB.append("static ");
    }
    returnSB.append(typeName);
    returnSB.append(";\n");
    when(result.toString()).thenReturn(returnSB.toString());
    return result;
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

  /**
   * Test that adding a new import inserts it in the correct position.
   */
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

  /**
   * Test that adding multiple new imports using addAll() inserts them
   * in the correct positions.
   */
  @Test
  public void shouldAddMultipleImportsInCorrectPositions() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean added = imports.addAll(Arrays.asList("import static org.junit.Assert.assertEquals",
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

  /**
   * Test that adding an already-existing import doesn't change anything.
   */
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

  /**
   * Test that removing an import works and the resulting output is
   * correctly sorted.
   */
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
   * Test that removing multiple imports using removeAll() works
   * and the resulting output is correctly sorted.
   */
  @Test
  public void shouldRemoveMultipleImportsAndSort() {
    ImportStatements imports = createImportStatements(basePackage, baseImportList);
    boolean removed = imports.removeAll(Arrays.asList("import com.sun.tools.javac.tree.JCTree",
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

  /**
   * Test that removing a non-existent import doesn't change anything.
   */
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

  /**
   * Test empty initial import list. Positions should match package end
   * positions.
   */
  @Test
  public void emptyImportListShouldGivePositionOfPackageStmt() {
    ImportStatements imports = createImportStatements(basePackage, new ArrayList<JCImport>());
    assertEquals(81, imports.getStartPos());
    assertEquals(81, imports.getEndPos());
  }

  /**
   * Test empty initial import list. The output string should start and
   * end with newlines because it is intended to be inserted after the
   * package statement.
   */
  @Test
  public void addingToEmptyImportListOutputShouldStartAndEndWithNewlines() {
    ImportStatements imports = createImportStatements(basePackage, new ArrayList<JCImport>());
    imports.add("import org.joda.time.Interval");
    assertEquals("\n"
        + "import org.joda.time.Interval;\n",
        imports.toString());
  }

  /**
   * Test start and end position calculations. The start position should be
   * the start offset of the first import statement, and the end position
   * should be the end position of the last import statement.
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
}
