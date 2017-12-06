package com.google.errorprone.bugpatterns;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.errorprone.CompilationTestHelper;

@RunWith(value = JUnit4.class)
public class LinkedListCheckerTest {

    private CompilationTestHelper compilationHelper;

    @Before
    public void setUp() {
        compilationHelper = CompilationTestHelper.newInstance(LinkedListChecker.class, getClass());
    }

    @Test
    public void normalList() {
        compilationHelper
                         .addSourceLines(
                             "NormalList.java",
                             "import java.util.ArrayList;", 
                             "import java.util.List;",
                             "public class NormalList {",
                             "  public void normal() {",
                             "    List<String> list = new ArrayList();",
                             "    list.add(\"1\");",
                             "    list.get(0);",
                             "  }",
                             "}").doTest();
    }

    @Test
    public void add() {
        compilationHelper.addSourceFile("AddLinkedList.java").doTest();
    }
    
    @Test
    public void addAdd() {
        compilationHelper.addSourceFile("AddAllLinkedList.java").doTest();
    }

    @Test
    public void set() {
        compilationHelper.addSourceFile("SetLinkedList.java").doTest();
    }

    @Test
    public void remove() {
        compilationHelper.addSourceFile("RemoveLinkedList.java").doTest();
    }

    @Test
    public void hierarchy() {
        compilationHelper.addSourceFile("MyLinkedList.java").doTest();
    }

}
