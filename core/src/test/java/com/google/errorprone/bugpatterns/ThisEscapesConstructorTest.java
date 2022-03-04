package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ThisEscapesConstructorTest {

  private static CompilationTestHelper compilationTestHelper;

  @Before
  public void setup() {
    compilationTestHelper = CompilationTestHelper.newInstance(ThisEscapesConstructor.class, getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationTestHelper.addSourceFile("ThisEscapesConstructorPositiveCases.java").doTest();
  }
  
  @Test
  public void testNegativeCases() {
    compilationTestHelper.addSourceFile("ThisEscapesConstructorNegativeCases.java").doTest();
  }
  
 

//   @Test
//   public void testNegativeCases() {
//     compilationTestHelper.addSourceFile("ThisEscapesConstructorNegativeCases.java").doTest();
//   }

}
