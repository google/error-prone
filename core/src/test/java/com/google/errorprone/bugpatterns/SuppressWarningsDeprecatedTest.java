package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Unit tests for {@link SuppressWarningsDeprecated}.
 *  
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class SuppressWarningsDeprecatedTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = new CompilationTestHelper(new SuppressWarningsDeprecated.Scanner());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        new File(
            this.getClass().getResource("SuppressWarningsDeprecatedPositiveCases.java").toURI()));
  }

  //@Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(
        new File(
            this.getClass().getResource("SuppressWarningsDeprecatedNegativeCases.java").toURI()));
  }
}
