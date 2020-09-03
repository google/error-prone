/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.LiteralTree;
import java.util.Map;

/**
 * TODO(user): Restrict this check to Android code once the capability is available in Error
 * Prone. See b/27967984.
 *
 * @author avenet@google.com (Arnaud J. Venet)
 */
@BugPattern(
    name = "HardCodedSdCardPath",
    altNames = {"SdCardPath"},
    summary = "Hardcoded reference to /sdcard",
    severity = WARNING)
public class HardCodedSdCardPath extends BugChecker implements LiteralTreeMatcher {
  // The proper ways of retrieving the "/sdcard" and "/data/data" directories.
  static final String SDCARD = "Environment.getExternalStorageDirectory().getPath()";
  static final String DATA = "Context.getFilesDir().getPath()";

  // Maps each platform-dependent way of accessing "/sdcard" or "/data/data" to its
  // portable equivalent.
  static final ImmutableMap<String, String> PATH_TABLE =
      new ImmutableMap.Builder<String, String>()
          .put("/sdcard", SDCARD)
          .put("/mnt/sdcard", SDCARD)
          .put("/system/media/sdcard", SDCARD)
          .put("file://sdcard", SDCARD)
          .put("file:///sdcard", SDCARD)
          .put("/data/data", DATA)
          .put("/data/user", DATA)
          .build();

  @Override
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    if (tree.getKind() != STRING_LITERAL) {
      return Description.NO_MATCH;
    }

    // Hard-coded paths may come handy when writing tests. Therefore, we suppress the check
    // for code located under 'javatests'.
    if (ASTHelpers.isJUnitTestCode(state)) {
      return Description.NO_MATCH;
    }

    String literal = (String) tree.getValue();
    if (literal == null) {
      return Description.NO_MATCH;
    }

    for (Map.Entry<String, String> entry : PATH_TABLE.entrySet()) {
      String hardCodedPath = entry.getKey();
      if (!literal.startsWith(hardCodedPath)) {
        continue;
      }
      String correctPath = entry.getValue();
      String remainderPath = literal.substring(hardCodedPath.length());
      // Replace the hard-coded fragment of the path with a portable expression.
      SuggestedFix.Builder suggestedFix = SuggestedFix.builder();
      if (remainderPath.isEmpty()) {
        suggestedFix.replace(tree, correctPath);
      } else {
        suggestedFix.replace(tree, correctPath + " + \"" + remainderPath + "\"");
      }
      // Add the corresponding import statements.
      if (correctPath.equals(SDCARD)) {
        suggestedFix.addImport("android.os.Environment");
      } else {
        suggestedFix.addImport("android.content.Context");
      }
      return describeMatch(tree, suggestedFix.build());
    }

    return Description.NO_MATCH;
  }
}
