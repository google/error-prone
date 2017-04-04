/*
 * Copyright 2017 Google Inc. All rights reserved.
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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Sorts imports according to a supplied {@link Comparator} and separates static and non-static with
 * a blank line.
 */
public class BasicImportOrganizer implements ImportOrganizer {

  private final Comparator<String> comparator;

  BasicImportOrganizer(Comparator<String> comparator) {
    this.comparator = comparator;
  }

  private static boolean isStatic(String importString) {
    return importString.startsWith("import static ");
  }

  /**
   * A {@link Comparator} that sorts import statements so that all static imports come before all
   * non-static imports and otherwise sorted alphabetically.
   */
  static Comparator<String> staticFirst() {
    return new Ordering<String>() {
      @Override
      public int compare(String s1, String s2) {
        return ComparisonChain.start()
            .compareTrueFirst(isStatic(s1), isStatic(s2))
            .compare(s1, s2)
            .result();
      }
    };
  }

  /**
   * A {@link Comparator} that sorts import statements so that all static imports come after all
   * non-static imports and otherwise sorted alphabetically.
   */
  static Comparator<String> staticLast() {
    return new Ordering<String>() {
      @Override
      public int compare(String s1, String s2) {
        return ComparisonChain.start()
            .compareFalseFirst(isStatic(s1), isStatic(s2))
            .compare(s1, s2)
            .result();
      }
    };
  }

  @Override
  public Iterable<String> organizeImports(Iterable<String> importStrings) {
    SortedSet<String> sorted = new TreeSet<>(comparator);
    Iterables.addAll(sorted, importStrings);

    List<String> organized = new ArrayList<>();

    // output sorted imports, with a line break between static and non-static imports
    boolean first = true;
    boolean prevIsStatic = true;
    for (String importString : sorted) {
      boolean isStatic = isStatic(importString);
      if (!first && prevIsStatic != isStatic) {
        // Add a blank line.
        organized.add("");
      }
      organized.add(importString);
      prevIsStatic = isStatic;
      first = false;
    }

    return organized;
  }
}
