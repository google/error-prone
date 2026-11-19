/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import javax.inject.Inject;
import javax.tools.StandardLocation;

/// A [BugChecker]; see the associated [BugPattern] annotation for details.
@BugPattern(
    summary = "Top-level class shadows a class on the compilation classpath",
    severity = WARNING)
public final class ClasspathShadowing extends AbstractClassShadowing {

  @Inject
  ClasspathShadowing(ErrorProneFlags errorProneFlags) {
    super(
        StandardLocation.CLASS_PATH,
        StandardLocation.MODULE_PATH,
        /* systemModules= */ false,
        errorProneFlags);
  }
}
