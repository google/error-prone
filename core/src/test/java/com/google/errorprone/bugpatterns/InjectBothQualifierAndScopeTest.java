package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectBothQualifierAndScopeTest {
  
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        new CompilationTestHelper(InjectBothQualifierAndScope.class);
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(new File(this.getClass()
        .getResource("InjectBothQualifierAndScopePositiveCases.java").toURI()));
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(new File(this.getClass()
        .getResource("InjectBothQualifierAndScopeNegativeCases.java").toURI()));
  }
}
