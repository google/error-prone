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

package com.google.errorprone.matchers;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.junit.Assert.assertEquals;

import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.Fix;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class DescriptionTest {

  @BugPattern(name = "DeadException",
    summary = "Exception created but not thrown",
    explanation = "", category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
   public class MyChecker extends BugChecker {
     Description getDescription() {
       return describeMatch(null, Fix.NO_FIX);
     }
   }

  @Test
  public void testDescriptionFromBugPattern() {
    Description description = new MyChecker().getDescription();
    assertEquals("DeadException", description.checkName);
    assertEquals("Exception created but not thrown\n  (see http://code.google.com/p/error-prone/wik"
        + "i/DeadException)", description.rawMessage);
    assertEquals("[DeadException] Exception created but not thrown\n  (see http://code.google.com/p"
        + "/error-prone/wiki/DeadException)", description.message);
  }

  @Test
  public void testCustomDescription() {
    Description description = new Description(null, "message", Fix.NO_FIX, ERROR);
    assertEquals("Undefined", description.checkName);
    assertEquals("message", description.rawMessage);
    assertEquals("message", description.message);
  }
}
