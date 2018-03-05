package com.google.errorprone.bugpatterns.refactoringexperiment.testcases;

/**
 * Created by ameya on 2/28/18.
 */
public class TestGoal1_mthdInvc_hierarchy {
    public int test(TestGoalInterface f){
        return f.apply(5);
    }
    public int boo5(){
        return test(x->x + 5);
    }
    public int boo6(){
        return test(x->x + 6);
    }
    public int boo7(){
        return test(x->x + 7);
    }
}
