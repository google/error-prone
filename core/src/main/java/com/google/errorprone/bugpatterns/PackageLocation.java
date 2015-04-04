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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.util.List;

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
    category = JDK, severity = ERROR, maturity = MATURE)
public class PackageLocation extends BugChecker implements CompilationUnitTreeMatcher {

  private static final Joiner DOT_JOINER = Joiner.on('.');
  private static final Splitter PATH_SPLITTER = Splitter.on('/').omitEmptyStrings();

  @Override
  public Description matchCompilationUnit(CompilationUnitTreeInfo info, VisitorState state) {
    if (!info.packageName().isPresent()) {
      return Description.NO_MATCH;
    }

    String fileName = getFileName(info.sourceFile().toUri());
    if (fileName == null) {
      return Description.NO_MATCH;
    }

    ImmutableList<String> packageName = asName(info.packageName().get());
    ImmutableList<String> directory = split(fileName);
    ImmutableList<String> directorySuffix =
        directory.subList(
            Math.max(0, directory.size() - packageName.size()),
            directory.size());
    if (!directorySuffix.equals(packageName)) {
      String message = String.format(
          "Declared package %s does not match expected package %s",
          DOT_JOINER.join(packageName),
          DOT_JOINER.join(directorySuffix));
      return buildDescription(info.packageName().get()).setMessage(message).build();
    }
    return Description.NO_MATCH;
  }

  private ImmutableList<String> split(String filename) {
    List<String> parts = PATH_SPLITTER.splitToList(filename);
    return ImmutableList.copyOf(parts.subList(0,  parts.size() - 1));
  }

  private static ImmutableList<String> asName(ExpressionTree tree) {
    ImmutableList.Builder<String> result = ImmutableList.builder();
    asNameImpl(result, tree);
    return result.build();
  }

  private static void asNameImpl(ImmutableList.Builder<String> acc, ExpressionTree tree) {
    if (tree instanceof JCIdent) {
      acc.add(((JCIdent) tree).getName().toString());
    } else if (tree instanceof JCFieldAccess) {
      JCFieldAccess access = (JCFieldAccess) tree;
      asNameImpl(acc, access.getExpression());
      acc.add(access.getIdentifier().toString());
    } else {
      throw new AssertionError(
          "Unexpected " + tree.getClass() + " in package name");
    }
  }

  /** Extract the filename from the URI, with special handling for jar files. */
  @Nullable
  private static String getFileName(URI uri) {
    if (!uri.getScheme().equals("jar")) {
      return uri.getPath();
    }

    try {
      return ((JarURLConnection) uri.toURL().openConnection()).getEntryName();
    } catch (IOException e) {
      return null;
    }
  }
}
