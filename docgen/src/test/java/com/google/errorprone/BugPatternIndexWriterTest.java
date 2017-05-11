/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.DocGenTool.Target;
import java.io.StringWriter;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BugPatternIndexWriterTest {

  @Test
  public void dumpInternal() throws Exception {
    StringWriter writer = new StringWriter();

    BugPatternInstance pattern1 = new BugPatternInstance();
    pattern1.severity = SeverityLevel.ERROR;
    pattern1.name = "BugPatternA";
    pattern1.summary = "Here's the \"interesting\" summary";

    BugPatternInstance pattern2 = new BugPatternInstance();
    pattern2.severity = SeverityLevel.ERROR;
    pattern2.name = "BugPatternB";
    pattern2.summary = "{summary2}";

    BugPatternInstance pattern3 = new BugPatternInstance();
    pattern3.severity = SeverityLevel.ERROR;
    pattern3.name = "BugPatternC";
    pattern3.summary = "mature";

    new BugPatternIndexWriter()
        .dump(
            Arrays.asList(pattern3, pattern2, pattern1),
            writer,
            Target.INTERNAL,
            ImmutableSet.of("BugPatternC"));
    assertThat(writer.toString())
        .isEqualTo(
            "# Bug patterns\n\n"
                + "[TOC]\n\n"
                + "This list is auto-generated from our sources. Each bug pattern includes code\n"
                + "examples of both positive and negative cases; these examples are used in our\n"
                + "regression test suite.\n"
                + "\n"
                + "Patterns which are marked __Experimental__ will not be evaluated against your\n"
                + "code, unless you specifically configure Error Prone. The default checks are\n"
                + "marked __On by default__, and each release promotes some experimental checks\n"
                + "after we've vetted them against Google's codebase.\n"
                + "\n"
                + "## On by default : ERROR\n"
                + "\n"
                + "__[BugPatternC](bugpattern/BugPatternC.md)__ \\\n"
                + "mature\n"
                + "\n"
                + "## Experimental : ERROR\n"
                + "\n"
                + "__[BugPatternA](bugpattern/BugPatternA.md)__ \\\n"
                + "Here&#39;s the &quot;interesting&quot; summary\n"
                + "\n"
                + "__[BugPatternB](bugpattern/BugPatternB.md)__ \\\n"
                + "{summary2}\n"
                + "\n");
  }

  @Test
  public void dumpExternal() throws Exception {
    StringWriter writer = new StringWriter();

    BugPatternInstance pattern1 = new BugPatternInstance();
    pattern1.severity = SeverityLevel.ERROR;
    pattern1.name = "BugPatternA";
    pattern1.summary = "Here's the \"interesting\" summary";

    BugPatternInstance pattern2 = new BugPatternInstance();
    pattern2.severity = SeverityLevel.ERROR;
    pattern2.name = "BugPatternB";
    pattern2.summary = "{summary2}";

    BugPatternInstance pattern3 = new BugPatternInstance();
    pattern3.severity = SeverityLevel.ERROR;
    pattern3.name = "BugPatternC";
    pattern3.summary = "mature";

    new BugPatternIndexWriter()
        .dump(
            Arrays.asList(pattern3, pattern2, pattern1),
            writer,
            Target.EXTERNAL,
            ImmutableSet.of("BugPatternC"));
    assertThat(writer.toString())
        .isEqualTo(
            "---\n"
                + "title: Bug Patterns\n"
                + "layout: bugpatterns\n"
                + "---\n\n\n"
                + "# Bug patterns\n"
                + "\n"
                + "This list is auto-generated from our sources. Each bug pattern includes code\n"
                + "examples of both positive and negative cases; these examples are used in our\n"
                + "regression test suite.\n"
                + "\n"
                + "Patterns which are marked __Experimental__ will not be evaluated against your\n"
                + "code, unless you specifically configure Error Prone. The default checks are\n"
                + "marked __On by default__, and each release promotes some experimental checks\n"
                + "after we've vetted them against Google's codebase.\n"
                + "\n"
                + "## On by default : ERROR\n"
                + "\n"
                + "__[BugPatternC](bugpattern/BugPatternC)__<br>\n"
                + "mature\n"
                + "\n"
                + "## Experimental : ERROR\n"
                + "\n"
                + "__[BugPatternA](bugpattern/BugPatternA)__<br>\n"
                + "Here&#39;s the &quot;interesting&quot; summary\n"
                + "\n"
                + "__[BugPatternB](bugpattern/BugPatternB)__<br>\n"
                + "{summary2}\n"
                + "\n");
  }
}
