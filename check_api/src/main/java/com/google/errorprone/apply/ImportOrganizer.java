/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.auto.value.AutoValue;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Organizes import statements when patching files. */
public interface ImportOrganizer {

  /**
   * Organize the imports supplied, e.g. insert blank lines between various groups.
   *
   * @param imports the imports to organize, the order is undefined.
   * @return the list of organized imports.
   */
  OrganizedImports organizeImports(List<Import> imports);

  /**
   * An {@link ImportOrganizer} that sorts import statements according to the Google Java Style
   * Guide, i.e. static first, static and non-static separated by blank line.
   */
  ImportOrganizer STATIC_FIRST_ORGANIZER = new BasicImportOrganizer(StaticOrder.STATIC_FIRST);

  /**
   * An {@link ImportOrganizer} that sorts import statements so that non-static imports come first,
   * and static and non-static separated by blank line.
   */
  ImportOrganizer STATIC_LAST_ORGANIZER = new BasicImportOrganizer(StaticOrder.STATIC_LAST);

  /**
   * An {@link ImportOrganizer} that sorts import statements according to Android Code Style, with
   * static imports first.
   *
   * <p>The imports are partitioned into groups in two steps, each step sub-partitions all groups
   * from the previous step. The steps are:
   *
   * <ol>
   *   <li>Split into static and non-static, the static imports come before the non static imports.
   *   <li>The groups are then partitioned based on a prefix of the type, the groups are ordered by
   *       prefix as follows:
   *       <ol>
   *         <li>{@code android.}
   *         <li>{@code com.android.}
   *         <li>A group for each root package, in alphabetical order.
   *         <li>{@code java.}
   *         <li>{@code javax.}
   *       </ol>
   * </ol>
   */
  ImportOrganizer ANDROID_STATIC_FIRST_ORGANIZER =
      new AndroidImportOrganizer(StaticOrder.STATIC_FIRST);

  /**
   * An {@link ImportOrganizer} that sorts import statements according to Android Code Style, with
   * static imports last.
   *
   * <p>The imports are partitioned into groups in two steps, each step sub-partitions all groups
   * from the previous step. The steps are:
   *
   * <ol>
   *   <li>Split into static and non-static, the static imports come after the non static imports.
   *   <li>The groups are then partitioned based on a prefix of the type, the groups are ordered by
   *       prefix as follows:
   *       <ol>
   *         <li>{@code android.}
   *         <li>{@code com.android.}
   *         <li>A group for each root package, in alphabetical order.
   *         <li>{@code java.}
   *         <li>{@code javax.}
   *       </ol>
   * </ol>
   */
  ImportOrganizer ANDROID_STATIC_LAST_ORGANIZER =
      new AndroidImportOrganizer(StaticOrder.STATIC_LAST);

  /**
   * An {@link ImportOrganizer} that organizes imports based on the default format provided by
   * IntelliJ IDEA.
   *
   * <p>This groups the imports into three groups, each delimited by a newline:
   *
   * <ol>
   *   <li>Non-static, non-{@code java.*}, non-{@code javax.*} imports.
   *   <li>{@code javax.*} and {@code java.*} imports, with {@code javax.*} imports ordered first.
   *   <li>Static imports.
   * </ol>
   */
  ImportOrganizer IDEA_ORGANIZER = new IdeaImportOrganizer();

  ImportOrganizer HUBSPOT_IMPORT_ORGANIZER = new HubSpotImportOrganizer();

  /** Represents an import. */
  @AutoValue
  abstract class Import {
    /** True if the import is static, false otherwise. */
    public abstract boolean isStatic();

    /** Return the type to import. */
    public abstract String getType();

    /**
     * Create an {@link Import}
     *
     * @param importString in the format {@code import( static)? <type>}.
     * @return the newly created {@link Import}
     */
    static Import importOf(String importString) {
      boolean isStatic = (importString.startsWith("import static "));
      String type =
          isStatic
              ? importString.substring("import static ".length())
              : importString.substring("import ".length());
      return new AutoValue_ImportOrganizer_Import(isStatic, type);
    }

    @Override
    public final String toString() {
      return String.format("import%s %s", isStatic() ? " static" : "", getType());
    }
  }

  /**
   * Provides support for building a list of imports from groups and formatting it as a block of
   * imports.
   */
  class OrganizedImports {

    private final StringBuilder importBlock = new StringBuilder();

    private int importCount;

    /**
     * Add a group of already sorted imports.
     *
     * <p>If there are any other imports in the list then this will add a newline separator before
     * this group is added.
     *
     * @param imports the imports in the group.
     */
    private void addGroup(Collection<Import> imports) {
      if (imports.isEmpty()) {
        return;
      }

      if (importBlock.length() != 0) {
        importBlock.append('\n');
      }

      importCount += imports.size();
      imports.forEach(i -> importBlock.append(i).append(";\n"));
    }

    /**
     * Get the organized imports as a block of imports, with blank links between the separate
     * groups.
     */
    public String asImportBlock() {
      return importBlock.toString();
    }

    /**
     * Add groups of already sorted imports.
     *
     * <p>If there are any other imports in the list then this will add a newline separator before
     * any groups are added. It will also add a newline separate between each group.
     *
     * @param groups the imports in the group.
     * @param keys the keys to add, in order, if a key is not in the groups then it is ignored.
     * @return this for chaining.
     */
    public <K> OrganizedImports addGroups(
        Map<K, ? extends Collection<Import>> groups, Iterable<K> keys) {
      for (K key : keys) {
        Collection<Import> imports = groups.get(key);
        if (imports != null) {
          addGroup(imports);
        }
      }

      return this;
    }

    /** The number of imports added, excludes blank lines. */
    int getImportCount() {
      return importCount;
    }
  }
}
