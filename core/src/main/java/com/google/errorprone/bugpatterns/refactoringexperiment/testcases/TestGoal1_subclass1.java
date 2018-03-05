package com.google.errorprone.bugpatterns.refactoringexperiment.testcases;

/**
 * Created by ameya on 2/28/18.
 */
public class TestGoal1_subclass1 implements TestGoalInterface {
    @Override
    public Integer apply(Integer integer) {
        return 5 + integer;
    }
}
