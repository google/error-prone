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


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.errorprone.BugPattern.Instance;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;

public class BugPatternIndexYamlWriterTest {
  @Test
  public void dump() throws Exception {
    StringWriter writer = new StringWriter();

    Instance pattern1 = new Instance();
    pattern1.severity = SeverityLevel.ERROR;
    pattern1.maturity = MaturityLevel.EXPERIMENTAL;
    pattern1.name = "BugPatternA";
    pattern1.summary = "Here's the \"interesting\" summary";

    Instance pattern2 = new Instance();
    pattern2.severity = SeverityLevel.ERROR;
    pattern2.maturity = MaturityLevel.EXPERIMENTAL;
    pattern2.name = "BugPatternB";
    pattern2.summary = "{summary2}";

    Instance pattern3 = new Instance();
    pattern3.severity = SeverityLevel.ERROR;
    pattern3.maturity = MaturityLevel.MATURE;
    pattern3.name = "BugPatternC";
    pattern3.summary = "mature";

    new BugPatternIndexYamlWriter().dump(Arrays.asList(pattern3, pattern2, pattern1), writer);
    assertThat(writer.toString(),
        is("'On by default : ERROR':\n" +
            "- {name: BugPatternC, summary: mature}\n" +
            "'Experimental : ERROR':\n" +
            "- {name: BugPatternA, summary: Here's the \"interesting\" summary}\n" +
            "- {name: BugPatternB, summary: '{summary2}'}\n"));

  }
}