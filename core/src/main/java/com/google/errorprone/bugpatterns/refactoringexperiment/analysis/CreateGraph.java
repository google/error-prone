package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CONSTRUCTOR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTERFACE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.NEW_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Edges.TYPE_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.PopulateRefactorToInfo.mthdKind;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.PopulateRefactorToInfo.varKind;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.createBiDerectionalRelation;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.getNode;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.getSuccessorWithEdge;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.mapToAssgnmntToGraph;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.mapToClassDeclToGraph;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.mapToMethodDeclToGraph;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.mapToMethodInvcToGraph;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ProtoToGraphMapper.mapVarDeclToGraph;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.errorprone.bugpatterns.refactoringexperiment.Constants;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDeclarationOuterClass.ClassDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by ameya on 1/31/18.
 */
public class CreateGraph {

    private static Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> analyseAndEnrich = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        for (Node n : gr.nodes())
            if (mthdKind.test(n))
                methodAnalysis(gr, n);
            else if (varKind.test(n))
                variableAnalysis(gr, n);
        return ImmutableValueGraph.copyOf(gr);
    };


    /**
     * This method searches for class declarations for the type of variable n.
     * eg. It searches for Class/interface declaration of MyFunction, for a variable.
     * declared as MyFunction f;
     *
     * @param gr : Graph on which analysis needs to be performed.
     * @param n : Method invocation for which method declarations has to be searched.
     *
     * This analysis is only applicable for variables who's type is the subtype of functional
     * interface.
     */
    private static void variableAnalysis(MutableValueGraph<Node, String> gr, Node n) {
        Optional<Node> temp = gr.successors(n).stream().filter(a -> gr.edgeValue(n, a).get().equals(TYPE_INFO)).findFirst();
        if (temp.isPresent()) {
            Optional<Node> classDecl = gr.nodes().stream().filter(x -> x.getKind().equals(CLASS) || x.getKind().equals(INTERFACE))
                    .filter(x -> x.getType().equals(n.getType())).findFirst();
            if (classDecl.isPresent()) {
                gr.putEdgeValue(n, classDecl.get(), Edges.OF_TYPE);
                gr.removeEdge(n, temp.get());// remove TYPE_INFO edge
            }
        }
    }

    /**
     * This method tries to search for method declaration of the method invocation in the graph.
     *
     * @param gr : Graph on which analysis needs to be performed.
     * @param n : Method invocation for which method declarations has to be searched.
     *
     * To search, it induces the id of the parent by replacing its kind from METHOD_INVOCATION to
     * METHOD. After it has found the method declaration 'md' : 1. it creates PARENT_METHOD relation
     * from 'n' -> 'md' 2. it creates PASSED_AS_ARG_TO, ARG_PASSED relationship between the method
     * parameters and the arguments passed to the method declaration.
     */
    private static void methodAnalysis(MutableValueGraph<Node, String> gr, Node n) {
        Node md = getNode(n.getId(), Constants.METHOD, gr).orElse(getNode(n.getId(), CONSTRUCTOR, gr).orElse(null));
        if (md != null) {
            gr.putEdgeValue(n, md, Edges.PARENT_METHOD);
            List<Node> foundParams = new ArrayList<>();
            for (Node param : getSuccessorWithEdge(md, gr, Edges.PARAM_INDEX)) {
                int index = Integer.parseInt(gr.edgeValue(md, param).get().replaceAll(Edges.PARAM_INDEX, ""));
                List<Node> foundArgs = new ArrayList<>();
                for (Node arg : getSuccessorWithEdge(n, gr, Edges.ARG_INDEX + index)) {
                    createBiDerectionalRelation(arg, param, Edges.PASSED_AS_ARG_TO, Edges.ARG_PASSED, true, gr);
                    foundParams.add(param);
                    foundArgs.add(arg);
                }
                foundArgs.forEach(x -> gr.removeEdge(n, x)); // remove ARG_INDEX edge
            }
            foundParams.forEach(x -> gr.removeEdge(md, x));// remove PARAM_INDEX edge
        }
        if (n.getKind().equals(NEW_CLASS)) {
            Optional<Node> typeNode = gr.nodes().stream().filter(x -> x.getId().equals(n.getOwner())).findFirst();
            if (typeNode.isPresent())
                gr.putEdgeValue(n, typeNode.get(), Edges.OF_TYPE);
        }
    }


    /**
     * This binary operations returns a graph obtained by merging two graphs g1 and g2.
     * In order to merge graphs, it adds nodes and edges from both the graphs into
     * the new graph.
     * Guava graph library takes care of duplication.
     * At end of merge we can analyse the data collected and establish new relationship.
     * This operation is used to merge graphs which belong to same type.
     */
    private static BinaryOperator<ImmutableValueGraph<Node, String>> mergeGraphWithoutAnalysis = (g1, g2) -> {
        MutableValueGraph<Node, String> graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
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
    private static BinaryOperator<ImmutableValueGraph<Node, String>> mergeGraphWithAnalysis = (g1, g2) ->
            analyseAndEnrich.apply(mergeGraphWithoutAnalysis.apply(g1, g2));


    private static ImmutableValueGraph<Node, String> methodDeclarationGraphs(ImmutableList<MethodDeclaration> methodDeclarations) {
        return methodDeclarations.stream()
                .map(x -> mapToMethodDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Node, String> variableDeclarationGraphs(ImmutableList<Variable> vars) {
        return vars.stream()
                .map(x -> mapVarDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Node, String> methodInvcGraphs(ImmutableList<MethodInvocation> mthInvc_newClass) {
        return mthInvc_newClass.stream()
                .map(x -> mapToMethodInvcToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Node, String> assgnmntGraphs(ImmutableList<Assignment> assgns) {
        return assgns.stream()
                .map(x -> mapToAssgnmntToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
    }

    private static ImmutableValueGraph<Node, String> classDeclGraphs(ImmutableList<ClassDeclaration> classDecl) {
        return classDecl.stream()
                .map(x -> mapToClassDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
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
    public static ImmutableValueGraph<Node, String> create(ImmutableList<MethodDeclaration> methdDeclarations, ImmutableList<ClassDeclaration> classDeclarations
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

    //GRAPH HELPER METHODS


}
