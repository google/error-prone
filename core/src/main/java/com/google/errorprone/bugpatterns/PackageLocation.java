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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.Suppressibility.CUSTOM_ANNOTATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.SuppressPackageLocation;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.CompilationUnitTree;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nullable;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "PackageLocation",
  summary = "Package names should match the directory they are declared in",
  category = JDK,
  severity = ERROR,
  maturity = MATURE,
  suppressibility = CUSTOM_ANNOTATION,
  customSuppressionAnnotation = SuppressPackageLocation.class,
  documentSuppression = false
)
public class PackageLocation extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (tree.getPackageName() == null) {
      return Description.NO_MATCH;
    }

    // package-info annotations are special
    // TODO(cushon): fix the core suppression logic handle this
    if (ASTHelpers.hasAnnotation(tree.getPackage(), SuppressPackageLocation.class)) {
      return Description.NO_MATCH;
    }

    String packageName = tree.getPackageName().toString();
    Path directory = getFilePath(tree.getSourceFile().toUri()).getParent();
    Path expected = Paths.get(packageName.replace('.', '/'));

    if (directory.endsWith(expected)) {
      return Description.NO_MATCH;
    }

    String message = String.format(
        "Expected package %s to be declared in a directory ending with %s, instead found %s",
        packageName,
        expected,
        directory);
    return buildDescription(tree.getPackageName()).setMessage(message).build();
  }

  /** Extract the filename from the URI, with special handling for jar files. */
  @Nullable
  private static Path getFilePath(URI uri) {
    if (!uri.getScheme().equals("jar")) {
      return Paths.get(uri.getPath());
    }

    try {
      return Paths.get(((JarURLConnection) uri.toURL().openConnection()).getEntryName());
    } catch (IOException e) {
      return null;
    }
  }
}
