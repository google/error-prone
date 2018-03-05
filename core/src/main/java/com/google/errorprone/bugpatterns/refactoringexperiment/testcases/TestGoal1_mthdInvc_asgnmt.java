package com.google.errorprone.bugpatterns.refactoringexperiment.testcases;

import java.util.function.Function;

/**
 * Created by ameya on 2/28/18.
 */
public class TestGoal1_mthdInvc_asgnmt {

    Function<Integer,Integer> f;
    public int test(Function<Integer,Integer> f){
        this.f = f;
        return f.apply(5);
    }
    public int boo5(){
        return test(x->x + 5);
    }

}
