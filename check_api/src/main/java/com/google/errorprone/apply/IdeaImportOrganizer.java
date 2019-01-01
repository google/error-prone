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

package com.google.errorprone.apply;

import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Organizes imports based on the default format provided by IntelliJ IDEA.
 *
 * <p>This groups the imports into three groups, each delimited by a newline:
 *
 * <ol>
 *   <li>Non-static, non-{@code java.*}, non-{@code javax.*} imports.
 *   <li>{@code javax.*} and {@code java.*} imports, with {@code javax.*} imports ordered first.
 *   <li>Static imports.
 * </ol>
 */
public class IdeaImportOrganizer implements ImportOrganizer {

  private static final String JAVA_PREFIX = "java.";

  private static final String JAVAX_PREFIX = "javax.";

  @Override
  public OrganizedImports organizeImports(List<Import> imports) {
    Map<PackageType, ImmutableSortedSet<Import>> partitioned =
        imports.stream()
            .collect(Collectors.groupingBy(
                IdeaImportOrganizer::getPackageType,
                TreeMap::new,
                toImmutableSortedSet(IdeaImportOrganizer::compareImport)));

    return new OrganizedImports()
        .addGroups(
            partitioned,
            ImmutableList.of(
                PackageType.NON_STATIC,
                PackageType.JAVAX_JAVA,
                PackageType.STATIC));
  }

  private static int compareImport(Import a, Import b) {
    if (a.isStatic() || b.isStatic()) {
      return a.getType().compareTo(b.getType());
    } else if (a.getType().startsWith(JAVA_PREFIX) && b.getType().startsWith(JAVAX_PREFIX)) {
      return 1;
    } else if (a.getType().startsWith(JAVAX_PREFIX) && b.getType().startsWith(JAVA_PREFIX)) {
      return -1;
    } else {
      return a.getType().compareTo(b.getType());
    }
  }

  private static PackageType getPackageType(Import anImport) {
    if (anImport.isStatic()) {
      return PackageType.STATIC;
    } else if (anImport.getType().startsWith(JAVA_PREFIX)) {
      return PackageType.JAVAX_JAVA;
    } else if (anImport.getType().startsWith(JAVAX_PREFIX)) {
      return PackageType.JAVAX_JAVA;
    } else {
      return PackageType.NON_STATIC;
    }
  }

  private enum PackageType {
    JAVAX_JAVA,
    NON_STATIC,
    STATIC
  }
}
