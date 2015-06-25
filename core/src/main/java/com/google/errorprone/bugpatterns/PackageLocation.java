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
import static com.google.errorprone.BugPattern.Suppressibility.UNSUPPRESSIBLE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeInfo;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nullable;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "PackageLocation",
    summary = "Package names should match the directory they are declared in",
    explanation = "Java files should be located in a directory that matches the fully qualified"
        + " name of the package. For example, classes in the package"
        + " `edu.oswego.cs.dl.util.concurrent` should be located in:"
        + " `.../edu/oswego/cs/dl/util/concurrent`.",
    category = JDK, severity = ERROR, maturity = MATURE, suppressibility = UNSUPPRESSIBLE)
public class PackageLocation extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTreeInfo info, VisitorState state) {
    if (!info.packageName().isPresent()) {
      return Description.NO_MATCH;
    }

    String packageName = info.packageName().get().toString();
    Path directory = getFilePath(info.sourceFile().toUri()).getParent();
    Path expected = Paths.get(packageName.replace('.', '/'));

    if (directory.endsWith(expected)) {
      return Description.NO_MATCH;
    }

    String message = String.format(
        "Expected package %s to be declared in a directory ending with %s, instead found %s",
        packageName,
        expected,
        directory);
    return buildDescription(info.packageName().get()).setMessage(message).build();
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
