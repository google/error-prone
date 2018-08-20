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

import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;

import com.google.common.collect.ImmutableSortedSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sorts imports according to a supplied {@link StaticOrder} and separates static and non-static
 * with a blank line.
 */
class BasicImportOrganizer implements ImportOrganizer {

  private final StaticOrder order;

  BasicImportOrganizer(StaticOrder order) {
    this.order = order;
  }

  @Override
  public OrganizedImports organizeImports(List<Import> imports) {

    // Group into static and non-static. Each group is a set sorted by type.
    Map<Boolean, ImmutableSortedSet<Import>> partionedByStatic =
        imports.stream()
            .collect(
                Collectors.partitioningBy(
                    Import::isStatic, toImmutableSortedSet(Comparator.comparing(Import::getType))));

    return new OrganizedImports()
        // Add groups, in the appropriate order.
        .addGroups(partionedByStatic, order.groupOrder());
  }
}
