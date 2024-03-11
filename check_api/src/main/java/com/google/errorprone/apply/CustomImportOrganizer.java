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
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.InvalidCommandLineOptionException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Organizes imports based on a specified custom Code Style.
 * 
 * <p>The custom ordering is specified using a <code>-XepPatchImportOrder</code> with the format:</p>
 * <code>custom:static={first|last},order={':'-separated prefixes}</code>
 * <p>
 * The <code>order</code> parameter is a colon-separated list indicating the desired order of import groups by their package prefixes.<br>
 * The special <code>OTHER</code> prefix is used to place imports that do not match any specified prefix.<p>
 *
 * <p>The imports are partitioned into groups in two steps, each step sub-partitions all groups from
 * the previous step. The steps are:
 *
 * <ol>
 *   <li>Split into static and non-static, the static imports come before or after the non static
 *       imports depending on the <code>static</code> parameter.
 *   <li>Further partitioning within each group based on the package prefix order defined in <code>order</code>.
 *   For example, <br><code>"-XepPatchImportOrder=custom:static=first,order=java:javax:OTHER:org.a:org.b:com"</code><br>
 *   results in the following order:
 *       <ol>
 *         <li>{@code java.}</li>
 *         <li>{@code javax.}</li>
 *         <li>AImports not matching any other specified prefix.</li>
 *         <li>{@code org.a.}</li>
 *         <li>{@code org.b.}</li>
 *         <li>{@code com.}</li>
 *       </ol>
 * </ol>
 *
 * <p>Each group is separated from the previous/next groups with a blank line.
 */
public class CustomImportOrganizer implements ImportOrganizer {
  
  private static final String OTHER = "OTHER";
  private static final String STATIC_PREFIX = "static=";
  private static final String ORDER_PREFIX = "order=";

  private StaticOrder staticOrder;
  private ImmutableList<String> roots;

  public CustomImportOrganizer(String spec) {
    for (String keyValue: spec.split(",")) {
      if (keyValue.startsWith(STATIC_PREFIX)) {
        String value = keyValue.substring(STATIC_PREFIX.length());
        if (value.equals("first")) {
          staticOrder = StaticOrder.STATIC_FIRST;
        } else if (value.equals("last")) {
          staticOrder = StaticOrder.STATIC_LAST;
        }
      } else if (keyValue.startsWith(ORDER_PREFIX)) {
        String value = keyValue.substring(ORDER_PREFIX.length());
        roots = ImmutableList.copyOf(value.split(":"));
      }
    }
    if (staticOrder == null) {
      throw new InvalidCommandLineOptionException("missing \"" + STATIC_PREFIX + "\" parameter: " + spec);
    }
    if (roots == null) {
      throw new InvalidCommandLineOptionException("missing \"" + ORDER_PREFIX + "\" parameter: " + spec);
    }
    if (!roots.contains(OTHER)) {
      throw new InvalidCommandLineOptionException("missing \"" + OTHER + "\" order prefix: " + spec);
    }
  }

  @Override
  public OrganizedImports organizeImports(List<Import> imports) {
    OrganizedImports organized = new OrganizedImports();

    // Group into static and non-static.
    Map<Boolean, List<Import>> partionedByStatic =
        imports.stream().collect(Collectors.partitioningBy(Import::isStatic));

    for (Boolean key : staticOrder.groupOrder()) {
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
                    anImport -> rootPackage(anImport),
                    // Ensure that the results are sorted.
                    TreeMap::new,
                    // Each group is a set sorted by type.
                    toImmutableSortedSet(Comparator.comparing(Import::getType))));

    organized.addGroups(groupedByRoot, roots);
  }

  private String rootPackage(Import anImport) {
    String type = anImport.getType();

    for (String root: roots) {
      if (type.startsWith(root + ".")) {
          return root;
      }
    }
    
    return OTHER;
  }
}
