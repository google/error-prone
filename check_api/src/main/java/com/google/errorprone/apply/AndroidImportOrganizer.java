/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Organizes imports based on Android Code Style
 *
 * <p>As the style rules do not specify where static imports are placed this supports first or last.
 *
 * <p>The imports are partitioned into groups in two steps, each step sub-partitions all groups from
 * the previous step. The steps are:
 *
 * <ol>
 *   <li>Split into static and non-static, the static imports come before or after the non static
 *       imports depending on the {@link StaticOrder} specified.
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
 *
 * <p>Each group is separate from the previous/next groups with a blank line.
 */
class AndroidImportOrganizer implements ImportOrganizer {

  private static final String ANDROID = "android";

  private static final String COM_ANDROID = "com.android";

  private static final String JAVA = "java";

  private static final String JAVAX = "javax";

  private static final ImmutableSet<String> SPECIAL_ROOTS =
      ImmutableSet.of(ANDROID, COM_ANDROID, JAVA, JAVAX);

  private final StaticOrder order;

  AndroidImportOrganizer(StaticOrder order) {
    this.order = order;
  }

  @Override
  public OrganizedImports organizeImports(List<Import> imports) {
    OrganizedImports organized = new OrganizedImports();

    // Group into static and non-static.
    Map<Boolean, List<Import>> partionedByStatic =
        imports.stream().collect(Collectors.partitioningBy(Import::isStatic));

    for (Boolean key : order.groupOrder()) {
      organizePartition(organized, partionedByStatic.get(key));
    }

    return organized;
  }

  private void organizePartition(OrganizedImports organized, List<Import> imports) {

    Map<String, ImmutableSortedSet<Import>> groupedByRoot =
        imports.stream()
            .collect(
                Collectors.groupingBy(
                    // Group by root package.
                    AndroidImportOrganizer::rootPackage,
                    // Ensure that the results are sorted.
                    TreeMap::new,
                    // Each group is a set sorted by type.
                    toImmutableSortedSet(Comparator.comparing(Import::getType))));

    // Get the third party roots by removing the roots that are handled specially and sorting.
    Set<String> thirdParty =
        groupedByRoot.keySet().stream()
            .filter(r -> !SPECIAL_ROOTS.contains(r))
            .collect(toImmutableSortedSet(Ordering.natural()));

    // Construct a list of the possible roots in the correct order.
    List<String> roots =
        ImmutableList.<String>builder()
            .add(ANDROID)
            .add(COM_ANDROID)
            .addAll(thirdParty)
            .add(JAVA)
            .add(JAVAX)
            .build();

    organized.addGroups(groupedByRoot, roots);
  }

  private static String rootPackage(Import anImport) {
    String type = anImport.getType();

    if (type.startsWith("com.android.")) {
      return "com.android";
    }

    int index = type.indexOf('.');
    if (index == -1) {
      // Treat the default package as if it has an empty root.
      return "";
    } else {
      return type.substring(0, index);
    }
  }
}
