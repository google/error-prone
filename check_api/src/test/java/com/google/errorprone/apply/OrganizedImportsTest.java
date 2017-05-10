/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
package com.google.errorprone.apply;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.apply.ImportOrganizer.Import;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public class OrganizedImportsTest {

  private static final Comparator<Import> IMPORT_COMPARATOR =
      Comparator.comparing(Import::isStatic).thenComparing(Import::getType);

  @Test
  public void emptyList() {
    ImportOrganizer.OrganizedImports organizedImports = new ImportOrganizer.OrganizedImports();
    assertEquals("", organizedImports.asImportBlock());
  }

  private ImmutableSortedSet<Import> buildSortedImportSet(Import... element) {
    return ImmutableSortedSet.orderedBy(IMPORT_COMPARATOR).add(element).build();
  }

  @Test
  public void singleGroup() {
    Map<String, Set<Import>> groups = new TreeMap<>();
    groups.put("first", buildSortedImportSet(Import.importOf("import first")));
    ImportOrganizer.OrganizedImports organizedImports =
        new ImportOrganizer.OrganizedImports().addGroups(groups, groups.keySet());
    assertEquals("import first;\n", organizedImports.asImportBlock());
  }

  @Test
  public void multipleGroups() {
    Map<String, Set<Import>> groups = new TreeMap<>();
    groups.put("first", buildSortedImportSet(Import.importOf("import first")));
    groups.put("second", buildSortedImportSet(Import.importOf("import second")));
    ImportOrganizer.OrganizedImports organizedImports =
        new ImportOrganizer.OrganizedImports().addGroups(groups, groups.keySet());
    assertEquals("import first;\n\nimport second;\n", organizedImports.asImportBlock());
  }

  @Test
  public void importCount() {
    Map<String, Set<Import>> groups = new TreeMap<>();
    groups.put("first", buildSortedImportSet(Import.importOf("import first")));
    groups.put("second", buildSortedImportSet(Import.importOf("import second")));
    ImportOrganizer.OrganizedImports organizedImports =
        new ImportOrganizer.OrganizedImports().addGroups(groups, groups.keySet());
    assertEquals(2, organizedImports.getImportCount());
  }
}
