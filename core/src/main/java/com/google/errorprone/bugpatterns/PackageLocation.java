/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.Suppressibility.CUSTOM_ANNOTATION;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.SuppressPackageLocation;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.Nullable;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "PackageLocation",
  summary = "Package names should match the directory they are declared in",
  category = JDK,
  severity = WARNING,
  maturity = MATURE,
  suppressibility = CUSTOM_ANNOTATION,
  customSuppressionAnnotations = SuppressPackageLocation.class,
  documentSuppression = false
)
public class PackageLocation extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (tree.getPackageName() == null) {
      return Description.NO_MATCH;
    }

    // package-info annotations are special
    // TODO(cushon): fix the core suppression logic to handle this
    if (ASTHelpers.hasAnnotation(tree.getPackage(), SuppressPackageLocation.class, state)) {
      return Description.NO_MATCH;
    }

    String packageName = tree.getPackageName().toString();

    Path actualPath = getParentPath(tree.getSourceFile().toUri());
    if (actualPath == null) {
      return Description.NO_MATCH;
    }
    ImmutableList<String> actualPathElements =
        FluentIterable.from(actualPath)
            .transform(
                new Function<Path, String>() {
                  @Override
                  public String apply(Path input) {
                    return input.toString();
                  }
                })
            .toList();
    ImmutableList<String> expectedPathElements = ImmutableList.copyOf(packageName.split("\\."));
    String separator = getPathSeparator(tree.getSourceFile().toUri());
    if (separator == null) {
      return Description.NO_MATCH;
    }
    Joiner pathJoiner = Joiner.on(separator);

    if (endsWith(actualPathElements, expectedPathElements)) {
      return Description.NO_MATCH;
    }

    String message =
        String.format(
            "Expected package %s to be declared in a directory ending with %s, instead found %s",
            packageName, pathJoiner.join(expectedPathElements), actualPath);
    return buildDescription(tree.getPackageName()).setMessage(message).build();
  }

  /**
   * Extracts the parent of the path from the URI (e.g. the directory), with special handling for
   * jar files.
   */
  @Nullable
  private static Path getParentPath(URI uri) {
    if (!uri.getScheme().equals("jar")) {
      return Paths.get(uri).getParent();
    }

    try {
      return Paths.get(((JarURLConnection) uri.toURL().openConnection()).getEntryName())
          .getParent();
    } catch (IOException e) {
      return null;
    }
  }

  Matcher<MethodInvocationTree> myMatcher = new Matcher<MethodInvocationTree>() {
    @Override public boolean matches(MethodInvocationTree tree, VisitorState state) {
      MethodSymbol sym = ASTHelpers.getSymbol(tree);
      return ASTHelpers.isSubtype(sym.getReturnType(), state.getTypeFromString("java.util.Future"), state);
    }
  };

  /** Gets the path elements for the given path. */
  @Nullable
  private static ImmutableList<String> getPathElements(URI uri) {
    Path path;
    if (!uri.getScheme().equals("jar")) {
      path = Paths.get(uri);
    } else {
      try {
        path = Paths.get(((JarURLConnection) uri.toURL().openConnection()).getEntryName());
      } catch (IOException e) {
        return null;
      }
    }

    ImmutableList.Builder<String> result = new ImmutableList.Builder<>();
    for (Path element : path.getParent()) {
      result.add(element.toString());
    }
    return result.build();
  }

  @Nullable
  private static String getPathSeparator(URI uri) {
    if (!uri.getScheme().equals("jar")) {
      return Paths.get(uri).getFileSystem().getSeparator();
    }

    try {
      String jarEntryPath = ((JarURLConnection) uri.toURL().openConnection()).getEntryName();
      // It's possible for paths to zip file entries to use '\' as the separator.  This is a hacky
      // way to check for that.
      return jarEntryPath.contains("\\") ? "\\" : "/";
    } catch (IOException e) {
      return null;
    }
  }

  /** Returns true iff the first list ends with the second list. */
  private static <T> boolean endsWith(List<T> first, List<T> second) {
    if (first.size() < second.size()) {
      return false;
    }
    int i = first.size() - 1;
    int j = second.size() - 1;
    while (j >= 0) {
      if (!first.get(i).equals(second.get(j))) {
        return false;
      }
      i--;
      j--;
    }
    return true;
  }
}
