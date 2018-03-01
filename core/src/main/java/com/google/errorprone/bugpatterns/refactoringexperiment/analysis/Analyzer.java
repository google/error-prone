package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.MappingUtil.POPULATE_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.MappingUtil.varKind;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.errorprone.bugpatterns.refactoringexperiment.Constants;
import com.google.errorprone.bugpatterns.refactoringexperiment.ProtoBuffPersist;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDeclarationOuterClass.ClassDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.RefactorableOuterClass.Refactorable;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by ameya on 1/26/18.
 */
public class Analyzer {

    private static String pckgName;
    /**
     *This precondition makes sure that ,for this subgraph 'graph' all the method invocations pass lambda expressions only.
     */
    private static Predicate<ImmutableValueGraph<Node, String>> PRE_CONDITION_METHOD_INVOCATIONS_LAMBDA = graph ->
            !graph.edges().stream().filter(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(Edges.ARG_PASSED))
                    .filter(endpt -> !graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(Edges.ASSIGNED_AS))
                    .map(endpt -> endpt.nodeV()).anyMatch(v -> !(v.getKind().equals(Constants.LAMBDA_EXPRESSION)));
    /**
     *This precondition makes sure that,for this subgraph 'graph' there are no assignment operations.
     */
    private static Predicate<ImmutableValueGraph<Node, String>> PRE_CONDITION_NO_ASSIGNMENTS = graph ->
            !graph.edges().stream().anyMatch(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(Edges.ASSIGNED_AS));

    public static void main(String args[]) throws Exception {
        induceAndMap(pckgName).forEach(r -> ProtoBuffPersist.write(r, REFACTOR_INFO));
    }

    /**
     *
     * @param fromFolder
     * @return list of refactorables
     * @throws Exception
     *
     * This method , creates graph from the protos in the fromFolder.
     * Then, it induces refactoring groups.
     * It maps each refactring group as a seperate subgraph, passes them through preconditions
     * and then maps the nodes of this subgraph into Refactorable proto objects.
     *
     */
    public static ImmutableList<Refactorable> induceAndMap(String fromFolder) throws Exception {
        List<Refactorable> refactorables = new ArrayList<>();
        induceSubgraphs(CreateGraph.create(getMethodDeclarations(fromFolder), getClassDeclarations(fromFolder)
                , getVariables(fromFolder),getMethodInovcation_NewClass(fromFolder)
                , getAssignments(fromFolder), getInterfaces(fromFolder))).stream().map(POPULATE_MAPPING).filter(PRE_CONDITION_METHOD_INVOCATIONS_LAMBDA).filter(PRE_CONDITION_NO_ASSIGNMENTS)
                .forEach(g ->
                    g.nodes().stream().filter(n ->!n.getKind().equals(REFACTOR_INFO) && n.refactorTo() != null ).forEach(n ->
                                refactorables.add(Refactorable.newBuilder().setId(n.getId()).setRefactorTo(n.refactorTo()).build())));
        return ImmutableList.copyOf(refactorables);
    }

    private static Set<ImmutableValueGraph<Node, String>> induceSubgraphs(ImmutableValueGraph<Node, String> gr) {
        Set<ImmutableValueGraph<Node, String>> subGraphs = new HashSet<>();
        MutableValueGraph<Node, String> graphOfGraphs = Graphs.copyOf(gr);
        List<Node> params = graphOfGraphs.nodes().stream().filter(x -> varKind.test(x)).collect(toImmutableList());
        for (Node n : params)
            if (!n.isVisited()) {
                Set<Node> reachables = Graphs.reachableNodes(graphOfGraphs.asGraph(), n).stream().collect(toImmutableSet());
                reachables.stream().forEach(x -> x.setVisited(true));
                subGraphs.add(ImmutableValueGraph.copyOf(Graphs.inducedSubgraph(graphOfGraphs, reachables)));
            }

        return subGraphs;
    }

    // IO Suppliers

    private static ImmutableList<Variable> getVariables(String folderName){
        return ImmutableList.copyOf(QueryProtoBuffData.getAllVrbl(folderName));
    }

    private static ImmutableList<ClassDeclaration> getInterfaces(String folderName){
        return ImmutableList.copyOf(QueryProtoBuffData.getAllInterfaceDecl(folderName));
    }
    private static ImmutableList<ClassDeclaration> getClassDeclarations(String folderName){
        return ImmutableList.copyOf(QueryProtoBuffData.getAllClassDecl(folderName));
    }
    private static ImmutableList<MethodDeclaration> getMethodDeclarations(String folderName){
        return ImmutableList.copyOf(QueryProtoBuffData.getAllMethdDecl(folderName));
    }
    private static ImmutableList<MethodInvocation> getMethodInovcation_NewClass(String folderName){
        List<MethodInvocation> allMethdInvc = QueryProtoBuffData.getAllMethdInvc(folderName);
        allMethdInvc.addAll(QueryProtoBuffData.getAllNewClass(folderName));
        return ImmutableList.copyOf(allMethdInvc);
    }
    private static ImmutableList<Assignment> getAssignments(String folderName){
        return ImmutableList.copyOf(QueryProtoBuffData.getAllAsgn(folderName));
    }




}

