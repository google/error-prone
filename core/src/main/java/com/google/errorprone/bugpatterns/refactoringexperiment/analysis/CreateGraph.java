package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CONSTRUCTOR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.FIELD;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTERFACE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LAMBDA_EXPRESSION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LOCAL_VARIABLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.METHOD_INVOCATION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.NEW_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.PARAMETER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Edges.ASSIGNED_AS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Edges.TYPE_INFO;

import com.google.common.collect.Maps;
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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ameya on 1/31/18.
 */
public class CreateGraph {


    private static Predicate<Node> isMethodKind = n ->  n.getKind().equals(METHOD_INVOCATION) || n.getKind().equals(NEW_CLASS);

    private static Predicate<Node> varKind = n -> n.getKind().equals(PARAMETER) || n.getKind().equals(LOCAL_VARIABLE) || n.getKind().equals(FIELD);

    private static Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> analyseAndEnrich = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        for (Node n : gr.nodes())
            if (isMethodKind.test(n))
                methodAnalysis(gr, n);
            else if (varKind.test(n))
                variableAnalysis(gr, n);
        return ImmutableValueGraph.copyOf(gr);
    };


    /**
     *
     * @param gr
     * @param n
     * This method searches for class declarations for the type of variable n.
     * This method is only applicable for variables who's type is the subtype of functional interface.
     */
    private static void variableAnalysis(MutableValueGraph<Node, String> gr, Node n) {
        Optional<Node> temp = gr.successors(n).stream().filter(a -> gr.edgeValue(n, a).get().equals(TYPE_INFO)).findFirst();
        if (temp.isPresent()) {
            Optional<Node> classDecl = getClassOfType(n, gr);
            if (classDecl.isPresent()) {
                gr.putEdgeValue(n, classDecl.get(), Edges.OF_TYPE);
                gr.removeEdge(n, temp.get());// remove TYPE_INFO edge
            }
        }
    }

    private static Optional<Node> getClassOfType(Node node, MutableValueGraph<Node, String> gr) {
        return gr.nodes().stream().filter(n -> n.getKind().equals(CLASS) || n.getKind().equals(INTERFACE))
                .filter(n -> n.getType().equals(node.getType())).findFirst();
    }

    /**
     *
     * @param gr
     * @param n
     *
     * This method, searches for parent method of method invocation 'n'.
     * To search, it induces the id of the parent by replacing its kind from METHOD_INVOCATION to METHOD
     * After it has found the method declaration 'md',
     * it creates PARENT_METHOD relation from 'n' -> 'md'
     * it creates PASSED_AS_ARG_TO, ARG_PASSED relationship
     *  between the method parameters and the arguments passed to the method declaration.
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
        if(n.getKind().equals(NEW_CLASS)){
            Optional<Node> typeNode = gr.nodes().stream().filter(x -> x.getId().equals(n.getOwner())).findFirst();
            if(typeNode.isPresent())
                gr.putEdgeValue(n,typeNode.get(), Edges.OF_TYPE);
        }
    }

    private static Set<Node> getSuccessorWithEdge(Node n, MutableValueGraph<Node, String> gr, String edgeValue) {
        return gr.successors(n).stream().filter(a -> gr.edgeValue(n, a).get().contains(edgeValue)).collect(Collectors.toSet());
    }

    /**
     * This binary operations merges two graphs.
     * In order to merge graphs, it adds nodes and edges from both the graphs into
     * the new graph.
     *
     * Guava graph library takes care of duplication.
     *
     * At end of merge we can analyse the data collected and establish new relationship.
     */

    private static BinaryOperator<ImmutableValueGraph<Node, String>> mergeGraphWithoutAnalysis = (g1, g2) -> {
        MutableValueGraph<Node, String> graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        g1.edges().forEach(e -> graph.putEdgeValue(e.nodeU(), e.nodeV(), g1.edgeValue(e.nodeU(), e.nodeV()).get()));
        g2.edges().forEach(e -> graph.putEdgeValue(e.nodeU(), e.nodeV(), g2.edgeValue(e.nodeU(), e.nodeV()).get()));
        g1.nodes().forEach(n -> graph.addNode(n));
        g2.nodes().forEach(n -> graph.addNode(n));
        return ImmutableValueGraph.copyOf(graph);
    };

    private static BinaryOperator<ImmutableValueGraph<Node, String>> mergeGraphWithAnalysis = (g1, g2) ->
         analyseAndEnrich.apply(mergeGraphWithoutAnalysis.apply(g1,g2));



    private static Function<Variable, ImmutableValueGraph<Node, String>> mapVarDeclToGraph = v -> {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        Node n = new Node(v.getId());
        g.addNode(n);
        if(v.hasInitializer())
            createBiDerectionalRelation(n, addNodeToGraph(v.getInitializer(), n.getId(), g), ASSIGNED_AS, Edges.ASSIGNED_TO, false, g);
        if (Mapping.CLASS_MAPPING_FOR.containsKey(v.getFilteredType().getInterfaceName()))
            if (v.getId().getType().startsWith(v.getFilteredType().getInterfaceName())) {
                Node refactorInfo = new Node(n.getId().toBuilder().setKind(REFACTOR_INFO)
                        .setType(Mapping.CLASS_MAPPING_FOR.get(v.getFilteredType().getInterfaceName()).apply(v.getFilteredType())).build());
                g.putEdgeValue(n, refactorInfo, REFACTOR_INFO);
            } else {
                Node n1 = new Node(Identification.newBuilder().setType(v.getId().getType()).setKind(INFERRED_CLASS).build());
                g.putEdgeValue(n, n1, TYPE_INFO);
            }
        return ImmutableValueGraph.copyOf(g);
    };

    private static Function<MethodDeclaration, ImmutableValueGraph<Node, String>> mapToMethodDeclToGraph = m ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        Node n = addNodeToGraph(m.getId(), g);
        if (m.getId().getKind().equals(LAMBDA_EXPRESSION))
            m.getParametersMap().entrySet().stream().map(param -> Maps.immutableEntry(param.getKey(), addNodeToGraph(param.getValue(), g)))
                    .forEach(x -> createBiDerectionalRelation(x.getValue(), n, Edges.PARENT_LAMBDA, Edges.PARAM_LAMBDA + x.getKey(), false, g));
        else
            m.getParametersMap().entrySet().stream().map(param -> Maps.immutableEntry(param.getKey(), addNodeToGraph(param.getValue(), g)))
                    .forEach(x -> g.putEdgeValue(n, x.getValue(), Edges.PARAM_INDEX + x.getKey()));

        if (m.hasSuperMethod()) {
            Node superMethod = new Node(m.getSuperMethod().getId());
            createBiDerectionalRelation(n, superMethod, Edges.AFFECTED_BY_HIERARCHY, Edges.AFFECTED_BY_HIERARCHY, false, g);
            for (Entry<Integer, Identification> e : m.getSuperMethod().getParametersMap().entrySet()) {
                createBiDerectionalRelation(new Node(e.getValue()), addNodeToGraph(m.getParametersMap().get(e.getKey()), g),
                        Edges.AFFECTED_BY_HIERARCHY, Edges.AFFECTED_BY_HIERARCHY, false, g);
            }
        }
        return ImmutableValueGraph.copyOf(g);
    };

    private static Function<MethodInvocation, ImmutableValueGraph<Node, String>> mapToMethodInvcToGraph = m ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        Node n = addNodeToGraph(m, g);
        g.addNode(n);
        if (m.hasReceiver())
            createBiDerectionalRelation(new Node(m.getReceiver()), n, Edges.METHOD_INVOKED, Edges.REFERENCE, false, g);

        if (m.getArgumentsCount() > 0)
            for (Entry<Integer, Identification> e : m.getArgumentsMap().entrySet())
                g.putEdgeValue(n, addNodeToGraph(e.getValue(), m.getId(), g, e.getKey()), Edges.ARG_INDEX + e.getKey());
        return ImmutableValueGraph.copyOf(g);
    };


    private static Function<Assignment, ImmutableValueGraph<Node, String>> mapToAssgnmntToGraph = a ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
//        for (Identification rhs : a.getRhsList())
        createBiDerectionalRelation(addNodeToGraph(a.getLhs(), g), addNodeToGraph(a.getRhs(), a.getLhs(), g), ASSIGNED_AS, Edges.ASSIGNED_TO, false, g);
        return ImmutableValueGraph.copyOf(g);
    };


//     if (Mapping.CLASS_MAPPING_FOR.containsKey(a.getSuperType().getInterfaceName())) {
//        Node refactorInfo = new Node(n.getId().toBuilder().setKind(REFACTOR_INFO)
//                .setType(Mapping.CLASS_MAPPING_FOR.get(a.getSuperType().getInterfaceName()).apply(a.getSuperType())).build());
//        g.putEdgeValue(n, refactorInfo, REFACTOR_INFO);
//    }
private static Function<ClassDeclaration, ImmutableValueGraph<Node, String>> mapToClassDeclToGraph = a ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        Node n = new Node(a.getId());
        g.addNode(n);
        if (Mapping.CLASS_MAPPING_FOR.containsKey(a.getSuperType().getInterfaceName())) {
            Node refactorInfo = new Node(n.getId().toBuilder().setKind(REFACTOR_INFO)
                    .setType(Mapping.CLASS_MAPPING_FOR.get(a.getSuperType().getInterfaceName()).apply(a.getSuperType())).build());
            g.putEdgeValue(n, refactorInfo, REFACTOR_INFO);
        }
        return ImmutableValueGraph.copyOf(g);
    };

    private static ImmutableValueGraph<Node, String> methodDeclarationGraphs(String folderName) {
        List<MethodDeclaration> methodDeclarations = QueryProtoBuffData.getAllMethdDecl(folderName);
        ImmutableValueGraph<Node, String> methodParam = methodDeclarations.stream()
                .map(x -> mapToMethodDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
        return methodParam;
    }

    private static ImmutableValueGraph<Node, String> variableDeclarationGraphs(String folderName) {
        List<Variable> vars = QueryProtoBuffData.getAllVrbl(folderName);
        ImmutableValueGraph<Node, String> varGraph = vars.stream()
                .map(x -> mapVarDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
        return varGraph;
    }

    private static ImmutableValueGraph<Node, String> methodInvcGraphs(String folderName) {
        List<MethodInvocation> mthInvc_newClass = QueryProtoBuffData.getAllMethdInvc(folderName);
        mthInvc_newClass.addAll(QueryProtoBuffData.getAllNewClass(folderName));
        ImmutableValueGraph<Node, String> methodParam = mthInvc_newClass.stream()
                .map(x -> mapToMethodInvcToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
        return methodParam;
    }

    private static ImmutableValueGraph<Node, String> assgnmntGraphs(String folderName) {
        List<Assignment> assgns = QueryProtoBuffData.getAllAsgn(folderName);
        ImmutableValueGraph<Node, String> methodParam = assgns.stream()
                .map(x -> mapToAssgnmntToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
        return methodParam;
    }

    private static ImmutableValueGraph<Node, String> classDeclGraphs(String folderName) {
        List<ClassDeclaration> classDecl = QueryProtoBuffData.getAllClassDecl(folderName);
        ImmutableValueGraph<Node, String> methodParam = classDecl.stream()
                .map(x -> mapToClassDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
        return methodParam;
    }

    private static ImmutableValueGraph<Node, String> interfaceDeclGraphs(String folderName) {
        List<ClassDeclaration> interfaceDecl = QueryProtoBuffData.getAllInterfaceDecl(folderName);
        ImmutableValueGraph<Node, String> methodParam = interfaceDecl.stream()
                .map(x -> mapToClassDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithoutAnalysis);
        return methodParam;
    }

    /**
     * methodDeclarationGraphs(), classDeclGraphs() ... other methods in the stream
     *
     * each these above methods do the following :
     *  1. query protos from filesystem
     *  2. map each proto to an individual graph
     *      a. Each field in the proto is a node in the graph.(If fields is repeated then multiple nodes)
     *      b. establish edges between the nodes of these individual graphs.(edge value is the relationship that the field has with the id of the proto)
     *  3. reduce all individual graphs into one huge graph. This reduction requires no analysis, since we are going to
     *      merge graphs which are generated from protos of same type.
     *  4. Reduce the graphs obtained from step 3, with method and sub_type analysis
     *
     * @param folderName
     * @return
     * @throws Exception
     */

    public static ImmutableValueGraph<Node, String> create(String folderName) throws Exception {
        return Stream.of(methodDeclarationGraphs(folderName), classDeclGraphs(folderName), variableDeclarationGraphs(folderName), methodInvcGraphs(folderName),
                assgnmntGraphs(folderName), interfaceDeclGraphs(folderName))
                .reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithAnalysis);
    }

    //GRAPH HELPER METHODS

    private static void createBiDerectionalRelation(Node u, Node v, String uTov, String vTou, boolean allowSelfLoop
            , MutableValueGraph<Node, String> g) {
        if ((!u.equals(v))) {
            g.putEdgeValue(u, v, uTov);
            g.putEdgeValue(v, u, vTou);
        } else if ((u.equals(v) && allowSelfLoop)) {
            g.putEdgeValue(u, v, Edges.RECURSIVE);
            g.putEdgeValue(v, u, Edges.RECURSIVE);
        }
    }

    /**
     * This method is for navigating from method inocation to method declaration
     * @param id id [name : test, kind: method_invocation, type = (Function<Integer,Integer>)void, owner:Foo]
     * @param kind [kind : method]
     * @param g
     * @return it returns a from the graph with id [name : test, kind: method, type = (Function<Integer,Integer>)void, owner:Foo]
     *          ,if it exists.
     */

    private static Optional<Node> getNode(Identification id, String kind, MutableValueGraph<Node, String> g) {
        return g.nodes().stream().filter(x -> x.isSameAs(id, kind)).findFirst();
    }

    /**
     * when we receive a f.apply(5), we identify apply by replacing owner of apply with id of 'f'
     * @param mi
     * @param g
     * @return
     */

    private static Node addNodeToGraph(MethodInvocation mi, MutableValueGraph<Node, String> g) {
        return mi.hasReceiver() ? addNodeToGraph(new Node(mi.getId(), mi.getReceiver()), g) : addNodeToGraph(mi.getId(), g);
    }

    public static Node addNodeToGraph(Identification id, MutableValueGraph<Node, String> g) {
        return !id.hasName() ? addNodeToGraph(new Node(id.toBuilder().setName(id.getKind()).build()), g) : addNodeToGraph(new Node(id), g);
    }

    public static Node addNodeToGraph(Node n1, MutableValueGraph<Node, String> g) {
        Node n = n1;
        g.addNode(n);
        return n;
    }

    private static Node addNodeToGraph(Identification id, Identification owner, MutableValueGraph<Node, String> g) {
        return addNodeToGraph(!id.hasName() ? new Node(id.toBuilder().setName(id.getKind()).build(), owner) : new Node(id), g);
    }

    private static Node addNodeToGraph(Identification id, Identification owner, MutableValueGraph<Node, String> g, Integer key) {
        return addNodeToGraph(!id.hasName() ? new Node(id.toBuilder().setName(id.getKind() + key).build(), owner) : new Node(id), g);
    }

}
