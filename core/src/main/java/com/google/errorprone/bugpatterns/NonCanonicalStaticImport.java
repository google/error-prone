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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.UnnecessaryStaticImport.StaticTypeImportInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ImportTree;

/**
 * Types shouldn't be statically by their non-canonical name.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "NonCanonicalStaticImport",
    summary = "Static import of type uses non-canonical name",
    explanation = "Types should always be imported by their canonical name. The canonical name of"
        + " a top-level class is the fully-qualified name of the package, followed by a '.',"
        + " followed by the name of the class. The canonical name of a member class is the"
        + " canonical name of its declaring class, followed by a '.', followed by the name of the"
        + " member class.\n\n"
        + "Fully-qualified member class names are not guaranteed to be canonical."
        + " Consider some member class M declared in a class C. There may be another class D "
        + " that extends C and inherits M."
        + " Therefore M can be accessed using the fully-qualified name of D, followed by a '.',"
        + " followed by 'M'. Since M is not declared in D, this name is not canonical.\n\n"
        + "The JLS §7.5.3 requires all single static imports to _start_ with a canonical type"
        + " name, but the fully-qualified name of the imported member is not required to be"
        + " canonical.\n\n"
        + " Importing types using non-canonical names is unnecessary and unclear, and should be"
        + " avoided\n\n"
        + "Example:\n\n"
        + "    package a;\n\n"
        + "    class One {\n"
        + "      static class Inner {}\\n"
        + "    }\n\n"
        + "    package a;\n"
        + "    class Two extends One {}\n\n"
        + "An import of `Inner` should always refer to it using the canonical name `a.One.Inner`,"
        + " not `a.Two.Inner`.",
    category = JDK, severity = WARNING, maturity = MATURE)
public class NonCanonicalStaticImport extends BugChecker implements ImportTreeMatcher {

  @Override
  public Description matchImport(ImportTree tree, VisitorState state) {
    StaticTypeImportInfo importInfo = StaticTypeImportInfo.tryCreate(tree, state);
    if (importInfo == null || importInfo.isCanonical()) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.replace(tree, importInfo.importStatement()));
  }
}

