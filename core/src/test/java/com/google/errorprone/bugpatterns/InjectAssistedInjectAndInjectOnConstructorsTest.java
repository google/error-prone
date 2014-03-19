package com.google.errorprone.bugpatterns;

import static com.google.errorprone.CompilationTestHelper.sources;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@RunWith(JUnit4.class)
public class InjectAssistedInjectAndInjectOnConstructorsTest {
  
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        new CompilationTestHelper(InjectAssistedInjectAndInjectOnConstructors.class);
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileSucceedsWithMessages(
        sources(getClass(), "InjectAssistedInjectAndInjectOnConstructorsPositiveCases.java"));
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(
        sources(getClass(), "InjectAssistedInjectAndInjectOnConstructorsNegativeCases.java"));
  }
}
