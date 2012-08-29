package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Unit tests for {@link OrderingFrom}.
 *  
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class OrderingFromTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = new CompilationTestHelper(new OrderingFrom.Scanner());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        new File(this.getClass().getResource("OrderingFromPositiveCases.java").toURI()));
  }

  //@Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(
        new File(this.getClass().getResource("OrderingFromNegativeCases.java").toURI()));
  }
}
