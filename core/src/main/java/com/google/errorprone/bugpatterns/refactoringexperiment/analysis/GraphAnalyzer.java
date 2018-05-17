package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGES_INVOKED_IN;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ARG_PASSED;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ASSIGNED_AS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_PARAM_INDEX;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_PASSED_AS_ARG_TO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED_METHOD;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED_VAR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LAMBDA_EXPRESSION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.METHOD_INVOCATION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.NEW_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ConstructGraph.isVarKind;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.NO_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.PopulateRefactorToInfo.getMapping;

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

import com.sun.source.tree.Tree.Kind;

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

    public static String pckgName;
    /**
     * This precondition makes sure that ,for this subgraph 'graph' all the method invocations pass
     * lambda expressions only. For this we make sure, that every v node of edge uv with value
     * EDGE_ARG_PASSED is of kind LAMBDA_EXPRESSION or VARIABLE
     */
    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_METHOD_INVOCATIONS_LAMBDA = graph ->
            !graph.edges().stream().filter(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(EDGE_ARG_PASSED))
                    .map(endpt -> endpt.nodeV()).anyMatch(v -> ! (v.getKind().equals(LAMBDA_EXPRESSION) ||isVarKind.test(v)));
    /**
     * This precondition makes sure that,for this subgraph 'graph' there are no assignment
     * operations. For Goal1, we do not want to perform refactorings which propagate across
     * assignment operations. Thus, we filter all edges of a graph based on edge value: EDGE_ASSIGNED_AS
     */
//    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_NO_ASSIGNMENTS = graph ->
//            !graph.edges().stream().anyMatch(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(EDGE_ASSIGNED_AS));

    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_ASSIGNED_AS_LAMBDA = graph ->
            !graph.edges().stream().filter(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(EDGE_ASSIGNED_AS))
                    .map(endpt -> endpt.nodeV()).anyMatch(v -> !((v.getKind().equals(LAMBDA_EXPRESSION) ||isVarKind.test(v))));


    /**
     * This precondition makes sure that we do not refactor a instance ,if a var is being passed to
     * a generic method or a 3rd party library.
     */
    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_OBJ_REF_NOT_PASSED_TO_ORPHAN_METHOD_INVOCATION =
            graph ->
                    !graph.nodes().stream().filter(isVarKind)
                            .anyMatch(x -> graph.successors(x).stream().filter(y -> y.getKind().equals(METHOD_INVOCATION) ||y.getKind().equals(NEW_CLASS))
                                    .anyMatch(y->graph.edgeValue(x,y).get().contains(EDGE_PASSED_AS_ARG_TO)));

    /**
     * This precondition makes sure that we do not have any inferred identifications in the
     * output subgraph
     */

    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_NO_INFERRED = graph ->
            !graph.nodes().stream().anyMatch(x -> x.getKind().equals(INFERRED_CLASS) || x.getKind().equals(INFERRED_METHOD)
                    || x.getKind().equals(INFERRED_VAR));

    /**
     *This pre-condition makes sure that no wrapper methods are used in lambda exp when we try to specialise
     * Dunctional interfaces
     */

    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_NO_WRAPPER = graph ->
            !graph.nodes().stream().filter(x -> x.getKind().equals(LAMBDA_EXPRESSION))
                    .anyMatch(x -> graph.successors(x).stream().anyMatch(y->graph.edgeValue(x,y).get().equals(EDGES_INVOKED_IN)));

    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_NO_ENHANCED_FOR_LOOP = graph ->
            !graph.nodes().stream().anyMatch(x -> x.getKind().equals(Kind.ENHANCED_FOR_LOOP.name()));

    private static final Predicate<ImmutableValueGraph<Identification, String>> PRE_CONDITION_NO_PARAMS_WITHOUT_ARG = graph ->
            !graph.nodes().stream().filter(isVarKind).anyMatch(x -> graph.predecessors(x).stream().anyMatch(s -> graph.edgeValue(s,x).get().contains(EDGE_PARAM_INDEX)));

    private static final Predicate<ImmutableValueGraph<Identification, String>> EXCLUDE_PACKAGES = graph ->
            !graph.nodes().stream().anyMatch(x -> Constants.exclude.stream().anyMatch(y -> y.equals(getPckgName(x))));

    public static String getPckgName(Identification n){
        return  n.getKind().equals("PACKAGE") ? n.getName() : getPckgName(n.getOwner());
    }


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
                .filter(PRE_CONDITION_ASSIGNED_AS_LAMBDA)
                .filter(PRE_CONDITION_OBJ_REF_NOT_PASSED_TO_ORPHAN_METHOD_INVOCATION)
                .filter(PRE_CONDITION_NO_INFERRED)
                .filter(PRE_CONDITION_NO_WRAPPER)
                .filter(PRE_CONDITION_NO_PARAMS_WITHOUT_ARG)
                .filter(PRE_CONDITION_NO_ENHANCED_FOR_LOOP)
                .filter(EXCLUDE_PACKAGES)
                .filter(EXCLUDE_PACKAGES)
                .collect(Collectors.toList());
        subGraphs.forEach(g -> {
            Map<Identification, String> mappings = getMapping(g);
            if(!mappings.values().contains(NO_MAPPING)) {
                g.nodes().stream().filter(n -> !n.getKind().equals(REFACTOR_INFO) && mappings.containsKey(n))
                        .forEach(n -> {
                            Refactorable refactorable = Refactorable.newBuilder().setId(n).setRefactorTo(mappings.get(n)).build();
                            refactorables.add(refactorable);
                            System.out.println(refactorable);
                        });
            }
        });
        System.out.println(refactorables.size());
        return ImmutableList.copyOf(refactorables);
    }

    public static Set<ImmutableValueGraph<Identification, String>> induceSubgraphs(ImmutableValueGraph<Identification, String> gr) {
        Set<ImmutableValueGraph<Identification, String>> subGraphs = new HashSet<>();
        MutableValueGraph<Identification, String> graphOfGraphs = Graphs.copyOf(gr);
        List<Identification> params = graphOfGraphs.nodes().stream().filter(x -> isVarKind.test(x)).collect(Collectors.toList());
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

