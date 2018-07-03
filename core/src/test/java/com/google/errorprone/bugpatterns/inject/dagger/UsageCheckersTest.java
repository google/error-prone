package com.google.errorprone.bugpatterns.inject.dagger;
  
import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
@SuppressWarnings("CheckTestExtendsBaseClass")
public class UsageCheckersTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private CompilationTestHelper compilationHelper;

  @Before
  public void setup() {
    compilationHelper = CompilationTestHelper.newInstance(UsageCheckers.class, getClass());
    compilationHelper.setArgs(Arrays.asList("-d", temporaryFolder.getRoot().getAbsolutePath()));
  }
  

  @Test
  public void test_unusedInjectUseCases() {
    compilationHelper.addSourceFile("UnusedInjectUseCases.java").doTest();
  }
  
  @Test
  public void test_unusedParamUseCases() {
    compilationHelper.addSourceFile("UnusedParamUseCases.java").doTest();
  }
}
