/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.errorprone.fixes.AppliedFix;
import com.google.errorprone.matchers.Description;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

import java.io.IOException;

import javax.tools.JavaFileObject;

/**
 * Making our errors appear to the user and break their build.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class JavacErrorDescriptionListener implements DescriptionListener {
  private final Log log;
  private ErrorProneEndPosMap endPositions;
  private final JavaFileObject sourceFile;
  private final JavaCompiler compiler;

  // The suffix for properties in src/main/resources/com/google/errorprone/errors.properties
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  public JavacErrorDescriptionListener(Log log, ErrorProneEndPosMap endPositions,
                                       JavaFileObject sourceFile, Context context) {
    this.log = log;
    this.endPositions = endPositions;
    this.sourceFile = sourceFile;
    this.compiler = JavaCompiler.instance(context);
  }

  @Override
  public void onDescribed(Description description) {
    JavaFileObject originalSource;
    // Swap the log's source and the current file's source; then be sure to swap them back later.
    originalSource = log.useSource(sourceFile);
    try {
      CharSequence content = sourceFile.getCharContent(true);

      // If endPositions were not computed (-Xjcov option was not passed), reparse the file
      // and compute the end positions so we can generate suggested fixes.
      if (endPositions == null) {
        boolean prevGenEndPos = compiler.genEndPos;
        compiler.genEndPos = true;
        // Reset the end positions for JDK8:
        JDKCompatible.resetEndPosMap(compiler, sourceFile);
        ErrorProneEndPosMap endPosMap = JDKCompatible.getEndPosMap(compiler.parse(sourceFile));
        compiler.genEndPos = prevGenEndPos;
        endPositions = new WrappedTreeMap(log, endPosMap);
      }

      AppliedFix fix = null;
      // TODO(user): description.suggestedFix cannot be null, so remove
      if (description.suggestedFix != null) {
        fix = AppliedFix.fromSource(content, endPositions).apply(description.suggestedFix);
      }
      final String message;
      // TODO(user): description.suggestedFix cannot be null, so remove
      if (description.suggestedFix == null || fix == null) {
        message = description.getMessage();
      } else {
        if (fix.isRemoveLine()) {
          message = description.getMessage() + "\nDid you mean to remove this line?";
        } else {
          message = description.getMessage() + "\nDid you mean '" + fix.getNewCodeSnippet() + "'?";
        }
      }
      switch (description.severity) {
        case ERROR:
          log.error((DiagnosticPosition) description.node, MESSAGE_BUNDLE_KEY, message);
          break;
        case WARNING:
          log.warning((DiagnosticPosition) description.node, MESSAGE_BUNDLE_KEY, message);
          break;
        default:
          break;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (originalSource != null) {
        log.useSource(originalSource);
      }
    }
  }
}
