package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ARG_PASSED;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ASSIGNED_AS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.NO_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.PopulateRefactorToInfo.getMapping;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.PopulateRefactorToInfo.varKind;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.errorprone.bugpatterns.refactoringexperiment.Constants;
import com.google.errorprone.bugpatterns.refactoringexperiment.ProtoBuffPersist;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDeclarationOuterClass.ClassDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.RefactorableOuterClass.Refactorable;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by ameya on 1/26/18.
 */
public final class GraphAnalyzer {

    private static String pckgName;
    /**
     * This precondition makes sure that ,for this subgraph 'graph' all the method invocations pass
     * lambda expressions only. For this we make sure, that every v node of edge uv with value
     * EDGE_ARG_PASSED is of kind LAMBDA_EXPRESSION
     */
    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_METHOD_INVOCATIONS_LAMBDA = graph ->
            !graph.edges().stream().filter(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(EDGE_ARG_PASSED))
                    .map(endpt -> endpt.nodeV()).anyMatch(v -> !(v.getKind().equals(Constants.LAMBDA_EXPRESSION)));
    /**
     * This precondition makes sure that,for this subgraph 'graph' there are no assignment
     * operations. For Goal1, we do not want to perform refactorings which propagate across
     * assignment operations. Thus, we filter a;; edges of a graph based on edge value: EDGE_ASSIGNED_AS
     */
    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_NO_ASSIGNMENTS = graph ->
            !graph.edges().stream().anyMatch(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(EDGE_ASSIGNED_AS));

    /**
     * This precondition makes sure that, the subgraph does not go through if any of the nodes are
     * mapped to NO MAPPING
     */
//    private static Predicate<ImmutableValueGraph<Node, String>> PRE_CONDITION_MAPPING_PRESENT = graph ->
//            !graph.nodes().stream().anyMatch(n -> !n.refactorTo().equals(null) && n.refactorTo().equals(NO_MAPPING));
    public static void main(String args[]) throws Exception {
        induceAndMap(pckgName).forEach(r -> ProtoBuffPersist.write(r, REFACTOR_INFO));
    }

    /**
     * @return list of refactorables
     * @throws Exception This method , creates graph from the protos in the fromFolder. Then, it
     * induces refactoring groups. It maps each refactring group as a seperate subgraph, passes them
     * through preconditions and then maps the nodes of this subgraph into Refactorable proto
     * objects.
     */
    public static ImmutableList<Refactorable> induceAndMap(String fromFolder) throws Exception {
        List<Refactorable> refactorables = new ArrayList<>();
        List<ImmutableValueGraph<Identification, String>> subGraphs = induceSubgraphs(ConstructGraph.create(getMethodDeclarations(fromFolder), getClassDeclarations(fromFolder)
                , getVariables(fromFolder), getMethodInovcation_NewClass(fromFolder)
                , getAssignments(fromFolder))).stream().filter(PRE_CONDITION_METHOD_INVOCATIONS_LAMBDA)
                .filter(PRE_CONDITION_NO_ASSIGNMENTS).collect(Collectors.toList());
        subGraphs.forEach(g -> {
            Map<Identification, String> mappings = getMapping(g);
            if(!mappings.values().contains(NO_MAPPING) && !mappings.values().contains("") ) {
                g.nodes().stream().filter(n -> !n.getKind().equals(REFACTOR_INFO) && mappings.containsKey(n))
                        .forEach(n ->
                                refactorables.add(Refactorable.newBuilder().setId(n).setRefactorTo(mappings.get(n)).build()));
            }
        });
        return ImmutableList.copyOf(refactorables);
    }

    public static Set<ImmutableValueGraph<Identification, String>> induceSubgraphs(ImmutableValueGraph<Identification, String> gr) {
        Set<ImmutableValueGraph<Identification, String>> subGraphs = new HashSet<>();
        MutableValueGraph<Identification, String> graphOfGraphs = Graphs.copyOf(gr);
        List<Identification> params = graphOfGraphs.nodes().stream().filter(x -> varKind.test(x)).collect(Collectors.toList());
        HashSet<Identification> visitedNodes = new HashSet<>();
        for (Identification n : params)
            if (!visitedNodes.contains(n)) {
                Set<Identification> reachables = Graphs.reachableNodes(graphOfGraphs.asGraph(), n).stream().collect(Collectors.toSet());
                visitedNodes.addAll(reachables);
                subGraphs.add(ImmutableValueGraph.copyOf(Graphs.inducedSubgraph(graphOfGraphs, reachables)));
            }

        return subGraphs;
    }

    // IO Suppliers

    private static ImmutableList<Variable> getVariables(String folderName) {
        return ImmutableList.copyOf(QueryProtoBuffData.getAllVrbl(folderName));
    }

    private static ImmutableList<ClassDeclaration> getClassDeclarations(String folderName) {
        return ImmutableList.copyOf(QueryProtoBuffData.getAllClassDecl(folderName));
    }

    private static ImmutableList<MethodDeclaration> getMethodDeclarations(String folderName) {
        return ImmutableList.copyOf(QueryProtoBuffData.getAllMethdDecl(folderName));
    }

    private static ImmutableList<MethodInvocation> getMethodInovcation_NewClass(String folderName) {
        List<MethodInvocation> allMethdInvc = QueryProtoBuffData.getAllMethdInvc(folderName);
        allMethdInvc.addAll(QueryProtoBuffData.getAllNewClass(folderName));
        return ImmutableList.copyOf(allMethdInvc);
    }

    private static ImmutableList<Assignment> getAssignments(String folderName) {
        return ImmutableList.copyOf(QueryProtoBuffData.getAllAsgn(folderName));
    }


}

