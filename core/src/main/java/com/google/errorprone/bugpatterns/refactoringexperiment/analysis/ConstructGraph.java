package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CONSTRUCTOR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGES_INVOKED_IN;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ARG_INDEX;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ARG_PASSED;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_PARAM_INDEX;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_PARENT_METHOD;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_PASSED_AS_ARG_TO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.FIELD;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTERFACE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LAMBDA_EXPRESSION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LOCAL_VARIABLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.METHOD_INVOCATION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.NEW_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.PARAMETER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.PRIMITIVE_WRAPPER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.createBiDirectionalRelation;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.getNode;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.getSuccessorWithEdge;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.errorprone.bugpatterns.refactoringexperiment.Constants;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDeclarationOuterClass.ClassDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by ameya on 1/31/18.
 */
public final class ConstructGraph {

    // Predicates for testing if Identification Kinds.
    public static final Predicate<Identification> isMethodKind = n -> n.getKind().equals(METHOD_INVOCATION) || n.getKind().equals(NEW_CLASS);
    public static final Predicate<Identification> isVarKind = n -> n.getKind().equals(PARAMETER) || n.getKind().equals(LOCAL_VARIABLE) || n.getKind().equals(FIELD);
    public static final Predicate<Identification> isTypeKind = n -> n.getKind().equals(CLASS) || n.getKind().equals(INTERFACE);
    public static final Predicate<Identification> isInferredKind = n -> n.getKind().startsWith(INFERRED);
    public static final Predicate<Identification> isWrapper = n -> n.getType().equals(PRIMITIVE_WRAPPER);


    private static ImmutableValueGraph<Identification, String> analyseAndEnrich(ImmutableValueGraph<Identification, String> graph) {
        MutableValueGraph<Identification, String> gr = Graphs.copyOf(graph);
        List<Identification> removeNodes = new ArrayList<>();
        for (Identification n : gr.nodes())
            if (isWrapper.test(n)) {
                recordWrapperMthdCallFromLambda(gr, n);
            } else if (isMethodKind.test(n)) {
                methodAnalysis(gr, n);
            } else if (isInferredKind.test(n)) {
                removeNodes.addAll(inferredAnalysis(gr, n));
            }
        removeNodes.forEach(x -> gr.removeNode(x));
        return ImmutableValueGraph.copyOf(gr);
    }

    /**
     * This method associates lambda expressions to the wrapper method usage inside
     * of them.
     * @param gr : Graph oon which analysis is to be performed.
     * @param n :  a possible lambda expression which might be using wrapper
     */

    private static void recordWrapperMthdCallFromLambda(MutableValueGraph<Identification, String> gr, Identification n) {
        Optional<Identification> temp = gr.nodes().stream().filter(x -> x.getKind().equals(LAMBDA_EXPRESSION))
                .filter(x -> x.equals(n.getOwner())).findFirst();
        if (temp.isPresent())
            gr.putEdgeValue(temp.get(), n, EDGES_INVOKED_IN);
    }

    /**
     * This method tries to replace inferred class with a non-inferred one.
     * @param gr : Graph on which analysis is to be performed.
     * @param n :  Inferred class identification
     * @return : List of inferred classes which have been resolved
     */

    private static List<Identification> inferredAnalysis(MutableValueGraph<Identification, String> gr, Identification n) {
        List<Identification> removeNodes = new ArrayList<>();
        if (n.getKind().equals(INFERRED_CLASS)) {
            Identification inferredCLass = gr.nodes().stream().filter(isTypeKind).filter(x -> x.getType().equals(n.getType())).findFirst().orElse(null);
            if (inferredCLass != null) {
                gr.predecessors(n).forEach(p -> gr.putEdgeValue(p, inferredCLass, gr.edgeValue(p, n).get()));
                gr.successors(n).forEach(s -> gr.putEdgeValue(inferredCLass, s, gr.edgeValue(n, s).get()));
                removeNodes.add(n);
            }
        }
        return removeNodes;
    }

    /**
     * This method tries to search for method declaration of the method invocation in the graph.
     *
     * @param gr : Graph on which analysis needs to be performed.
     * @param n : Method invocation for which method declarations has to be searched.
     *
     * To search, it induces the id of the parent by replacing its kind from METHOD_INVOCATION to
     * METHOD. After it has found the method declaration 'md' : 1. it creates EDGE_PARENT_METHOD
     * relation from 'n' -> 'md' 2. it creates EDGE_PASSED_AS_ARG_TO, EDGE_ARG_PASSED relationship
     * between the method parameters and the arguments passed to the method declaration.
     */
    private static void methodAnalysis(MutableValueGraph<Identification, String> gr, Identification n) {
        if (n.getKind().equals(METHOD_INVOCATION) || n.getKind().equals(NEW_CLASS)) {
            Identification md = getNode(n, Constants.METHOD, gr).orElse(getNode(n, CONSTRUCTOR, gr).orElse(null));
            if (md != null) {
                gr.putEdgeValue(n, md, EDGE_PARENT_METHOD);
                List<Identification> foundParams = new ArrayList<>();
                for (Identification param : getSuccessorWithEdge(md, gr, EDGE_PARAM_INDEX)) {
                    int index = Integer.parseInt(gr.edgeValue(md, param).get().replaceAll(EDGE_PARAM_INDEX, ""));
                    List<Identification> foundArgs = new ArrayList<>();
                    for (Identification arg : getSuccessorWithEdge(n, gr, EDGE_ARG_INDEX + index)) {
                        createBiDirectionalRelation(arg, param, EDGE_PASSED_AS_ARG_TO, EDGE_ARG_PASSED, true, gr);
                        foundParams.add(param);
                        foundArgs.add(arg);
                    }
                    foundArgs.forEach(x -> gr.removeEdge(x, n));
                    foundArgs.forEach(x -> gr.removeEdge(n, x));// remove ARG_INDEX edge
                }
                foundParams.forEach(x -> gr.removeEdge(x, md));
                foundParams.forEach(x -> gr.removeEdge(md, x));// remove PARAM_INDEX edge
            }
        }
    }


    /**
     * This binary operations returns a graph obtained by merging two graphs g1 and g2.
     * In order to merge graphs, it adds nodes and edges from both the graphs into
     * the new graph.
     * Gsuava graph library takes care of duplication.
     * At end of merge we can analyse the data collected and establish new relationship.
     * This operation is used to merge graphs which belong to same type.
     */
    private static BinaryOperator<ImmutableValueGraph<Identification, String>> mergeGraphWithoutAnalysis = (g1, g2) -> {
        MutableValueGraph<Identification, String> graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        g1.edges().forEach(e -> graph.putEdgeValue(e.nodeU(), e.nodeV(), g1.edgeValue(e.nodeU(), e.nodeV()).get()));
        g2.edges().forEach(e -> graph.putEdgeValue(e.nodeU(), e.nodeV(), g2.edgeValue(e.nodeU(), e.nodeV()).get()));
        g1.nodes().forEach(n -> graph.addNode(n));
        g2.nodes().forEach(n -> graph.addNode(n));
        return ImmutableValueGraph.copyOf(graph);
    };

    /**
     * This operation merges graphs obtained after merging all MethodDeclaration graphs amongst
     * themselves, method invocation graphs amongst themselves and so on for class declarations,
     * interfaces, variables, and assignments.
     * After merging all the graphs into one  graph, it performs analysis on this graph.
     */
    private static BinaryOperator<ImmutableValueGraph<Identification, String>> mergeGraphWithAnalysis = (g1, g2) ->
            analyseAndEnrich(mergeGraphWithoutAnalysis.apply(g1, g2));


    private static ImmutableValueGraph<Identification, String> methodDeclarationGraphs(ImmutableList<MethodDeclaration> methodDeclarations) {
        return methodDeclarations.stream()
                .map(ProtoToGraphMapper::mapMethodDeclToGraph).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Identification, String> variableDeclarationGraphs(ImmutableList<Variable> vars) {
        return vars.stream()
                .map(ProtoToGraphMapper::mapVarDeclToGraph).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Identification, String> methodInvcGraphs(ImmutableList<MethodInvocation> mthInvc_newClass) {
        return mthInvc_newClass.stream()
                .map(ProtoToGraphMapper::mapMethodInvcToGraph).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Identification, String> assgnmntGraphs(ImmutableList<Assignment> assgns) {
        return assgns.stream()
                .map(ProtoToGraphMapper::mapAssgnmntToGraph).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Identification, String> classDeclGraphs(ImmutableList<ClassDeclaration> classDecl) {
        return classDecl.stream()
                .map(ProtoToGraphMapper::mapClassDeclToGraph).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    /**
     * methodDeclarationGraphs(), classDeclGraphs() ... other methods in the stream
     * each these above methods do the following : 1. query protos from filesystem 2. map each proto
     * to an individual graph a. Each field in the proto is a node in the graph.(If fields is
     * repeated then multiple nodes) b. establish edges between the nodes of these individual
     * graphs.(edge value is the relationship that the field has with the id of the proto) 3. reduce
     * all individual graphs into one huge graph. This reduction requires no analysis, since we are
     * going to merge graphs which are generated from protos of same type. 4. Reduce the graphs
     * obtained from step 3, with method and sub_type analysis
     *
     * @param methdDeclarations :List of method declaration protos from filesystem.
     * @param classDeclarations :List of class declaration protos from filesystem.
     * @param variableDeclarations :List of variable declaration protos from filesystem.
     * @param methodInvocations :List of Method invocations protos from filesystem.
     * @param assignments :List of assignments protos from filesystem.
     */
    public static ImmutableValueGraph<Identification, String> create(ImmutableList<MethodDeclaration> methdDeclarations, ImmutableList<ClassDeclaration> classDeclarations
            , ImmutableList<Variable> variableDeclarations, ImmutableList<MethodInvocation> methodInvocations
            , ImmutableList<Assignment> assignments) throws Exception {

        return Stream.of(methodDeclarationGraphs(methdDeclarations),
                classDeclGraphs(classDeclarations),
                variableDeclarationGraphs(variableDeclarations),
                methodInvcGraphs(methodInvocations),
                assgnmntGraphs(assignments))
                .reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithAnalysis);
    }
}
