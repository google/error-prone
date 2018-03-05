package com.google.errorprone.bugpatterns.refactoringexperiment.testProtos;


import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.errorprone.bugpatterns.refactoringexperiment.analysis.GraphAnalyzer;
import com.google.errorprone.bugpatterns.refactoringexperiment.analysis.QueryProtoBuffData;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.RefactorableOuterClass.Refactorable;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class TestGoal1 {

    @Test
    public void test_OnlyMethodInvocation_AllLambda() throws Exception {
        List<Refactorable> output = GraphAnalyzer.induceAndMap("goal1_mthd_invc_lambda_only/").stream().collect(Collectors.toList());
        List<Refactorable> expectedOutput = QueryProtoBuffData.getAllRefactorInfo("/Users/ameya/pilot_plugin/src/main/resources/goal1_mthd_invc_lambda_only/");
        List<Refactorable> expectedOutputs = Lists.reverse(expectedOutput);
        assertEquals(output,expectedOutputs );
    }

    @Test
    public void test_AssignmentAndInvocationFail() throws Exception {
        List<Refactorable> output = GraphAnalyzer.induceAndMap("goal1_assignment_lambda_mthd_invc/").stream().collect(Collectors.toList());
        List<Refactorable> expectedOutputs = new ArrayList<>();
        assertEquals(output,expectedOutputs );
    }
}
