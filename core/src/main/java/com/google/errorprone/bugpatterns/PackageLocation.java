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
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.BugPattern.Suppressibility.CUSTOM_ANNOTATION;

import com.google.common.base.CharMatcher;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.SuppressPackageLocation;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "PackageLocation",
  summary = "Package names should match the directory they are declared in",
  category = JDK,
  severity = SUGGESTION,
  suppressibility = CUSTOM_ANNOTATION,
  documentSuppression = false,
  customSuppressionAnnotations = SuppressPackageLocation.class,
  tags = StandardTags.STYLE
)
public class PackageLocation extends BugChecker implements CompilationUnitTreeMatcher {

  private static final CharMatcher DOT_MATCHER = CharMatcher.is('.');

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    // Android projects often put different configurations (e.g. dev vs. prod) of a class at paths
    // with a {dev, prod} prefix.  Opt them out of this check.
    if (state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }

    if (tree.getPackageName() == null) {
      return Description.NO_MATCH;
    }

    // package-info annotations are special
    // TODO(cushon): fix the core suppression logic to handle this
    if (ASTHelpers.hasAnnotation(tree.getPackage(), SuppressPackageLocation.class, state)) {
      return Description.NO_MATCH;
    }

    String packageName = tree.getPackageName().toString();
    String actualFileName = ASTHelpers.getFileNameFromUri(tree.getSourceFile().toUri());
    if (actualFileName == null) {
      return Description.NO_MATCH;
    }
    String actualPath = actualFileName.substring(0, actualFileName.lastIndexOf('/'));
    String expectedSuffix = "/" + DOT_MATCHER.replaceFrom(packageName, '/');
    if (actualPath.endsWith(expectedSuffix)) {
      return Description.NO_MATCH;
    }

    String message =
        String.format(
            "Expected package %s to be declared in a directory ending with %s, instead found %s",
            packageName, expectedSuffix, actualPath);
    return buildDescription(tree.getPackageName()).setMessage(message).build();
  }
}
