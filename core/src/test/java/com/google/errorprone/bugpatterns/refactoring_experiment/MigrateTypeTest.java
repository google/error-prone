package com.google.errorprone.bugpatterns.refactoring_experiment;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.bugpatterns.CanonicalDuration;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Created by ameya on 3/14/18.
 */
@RunWith(JUnit4.class)
public class MigrateTypeTest {
    public void migrateTypeTestGoal1_positive() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new CanonicalDuration(), getClass())
                .addInputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "public int test(Function<Integer,Integer> f){",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "}")
                .addOutputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "import java.util.function.IntUnaryOperator;",
                        "class TestGoal1 {",
                        "public int test(IntUnaryOperator f){",
                        "return f.applyAsInt(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }

    public void migrateTypeTestGoal1_negative() throws Exception {
        BugCheckerRefactoringTestHelper.newInstance(new CanonicalDuration(), getClass())
                .addInputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "Function<Integer,Integer> f;",
                        "public int test(Function<Integer,Integer> f){",
                        "this.f = f;",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "}")
                .addOutputLines(
                        "TestGoal1.java",
                        "import java.util.function.Function;",
                        "class TestGoal1 {",
                        "Function<Integer,Integer> f;",
                        "public int test(Function<Integer,Integer> f){",
                        "this.f = f;",
                        "return f.apply(5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "public int boo5(){",
                        "return test(x->x + 5);",
                        "}",
                        "}")
                .doTest(TEXT_MATCH);
    }
}
