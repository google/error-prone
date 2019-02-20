/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.base.CharMatcher;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.SuppressPackageLocation;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "PackageLocation",
    summary = "Package names should match the directory they are declared in",
    severity = SUGGESTION,
    documentSuppression = false,
    suppressionAnnotations = SuppressPackageLocation.class,
    tags = StandardTags.STYLE)
public class PackageLocation extends BugChecker implements CompilationUnitTreeMatcher {

  private static final CharMatcher DOT_MATCHER = CharMatcher.is('.');

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

    PackageSymbol packageSymbol = ((JCCompilationUnit) state.getPath().getCompilationUnit()).packge;
    if (packageSymbol == null) {
      return Description.NO_MATCH;
    }
    String packageName = packageSymbol.fullname.toString();
    String actualFileName = ASTHelpers.getFileName(tree);
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
