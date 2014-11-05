/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.scanner;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.errorprone.BugCheckerSupplier;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.bugpatterns.BugChecker;

import java.io.IOException;

/**
 * Static helper class that provides {@link ScannerSupplier}s and {@link BugCheckerSupplier}s
 * for the built-in error-prone checks, as opposed to plugin checks or checks used in tests.
 */
public class BuiltInCheckerSuppliers {

  /**
   * Returns a {@link ScannerSupplier} with all {@link BugChecker}s in error-prone.
   */
  public static ScannerSupplier allChecks() {
    return new ScannerSupplierImpl(BUILT_IN_CHECKERS);
  }

  /**
   * Returns a {@link ScannerSupplier} with all {@link BugChecker}s in error-prone that have
   * {@code maturity == MaturityLevel.MATURE}.
   */
  public static ScannerSupplier matureChecks() {
    return allChecks().filter(MATURE);
  }

  /**
   * Returns a {@link BugCheckerSupplier} for the checker with the given name. Returns null if no
   * checker with that name can be found.
   */
  public static BugCheckerSupplier forName(String name) {
    return BUILT_IN_CHECKERS.get(name);
  }

  /**
   * A map of check name to {@link BugCheckerSupplier} for all {@link BugChecker}s known
   * reflectively to error-prone.
   */
  private static final ImmutableBiMap<String, BugCheckerSupplier> BUILT_IN_CHECKERS;
  static {
    ImmutableBiMap.Builder<String, BugCheckerSupplier> bimapBuilder = ImmutableBiMap.builder();
    ClassPath classPath;
    try {
      classPath = ClassPath.from(ScannerSupplier.class.getClassLoader());
    } catch (IOException e) {
      throw new LinkageError();
    }
    for (ClassInfo classInfo : classPath.getAllClasses()) {
      // We could allow classes in other packages to be auto-discovered, but loading everything
      // on the classpath is slower and requires more error handling.
      if (!classInfo.getPackageName().startsWith("com.google.errorprone.bugpatterns")) {
        continue;
      }
      Class<?> clazz = classInfo.load();
      if (clazz.isAnnotationPresent(BugPattern.class) && BugChecker.class.isAssignableFrom(clazz)) {
        BugCheckerSupplier checkerSupplier =
            BugCheckerSupplier.fromClass(clazz.asSubclass(BugChecker.class));
        bimapBuilder.put(checkerSupplier.canonicalName(), checkerSupplier);
      }
    }
    // build() enforces that check names are unique since ImmutableBiMap does not allow duplicate
    // keys or values.
    BUILT_IN_CHECKERS = bimapBuilder.build();
  }

  /**
   * A predicate for mature checks.
   */
  private static final Predicate<BugCheckerSupplier> MATURE = new Predicate<BugCheckerSupplier>() {
    @Override
    public boolean apply(BugCheckerSupplier input) {
      return (input.maturity() == MaturityLevel.MATURE);
    }
  };

  // May not be instantiated
  private BuiltInCheckerSuppliers() {}
}
