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


    public static Predicate<Node> isMethodKind = n ->  n.getKind().equals(METHOD_INVOCATION) || n.getKind().equals(NEW_CLASS);

    public static Predicate<Node> varKind = n -> n.getKind().equals(PARAMETER) || n.getKind().equals(LOCAL_VARIABLE) || n.getKind().equals(FIELD);

    private static Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> analyseAndEnrich = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        for (Node n : gr.nodes())
            if (isMethodKind.test(n))
                methodAnalysis(gr, n);
            else if (varKind.test(n))
                variableAnalysis(gr, n);
        return ImmutableValueGraph.copyOf(gr);
    };

    private static void variableAnalysis(MutableValueGraph<Node, String> gr, Node n) {
        Optional<Node> temp = gr.successors(n).stream().filter(a -> gr.edgeValue(n, a).equals(TYPE_INFO)).findFirst();
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

    private static void methodAnalysis(MutableValueGraph<Node, String> gr, Node n) {
        Node md = getNode(n.getId(), Constants.METHOD, gr).orElse(getNode(n.getId(), CONSTRUCTOR, gr).orElse(null));
        if (md != null) {
            gr.putEdgeValue(n, md, Edges.PARENT_METHOD);
            List<Node> foundParams = new ArrayList<>();
            for (Node param : getSuccessorWithEdge(md, gr, Edges.PARAM_INDEX)) {
                int index = Character.getNumericValue(gr.edgeValue(md, param).charAt(gr.edgeValue(md, param).length()-1));
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

    public static Set<Node> getSuccessorWithEdge(Node n, MutableValueGraph<Node, String> gr, String edgeValue) {
        return gr.successors(n).stream().filter(a -> gr.edgeValue(n, a).contains(edgeValue)).collect(Collectors.toSet());
    }


    public static BinaryOperator<ImmutableValueGraph<Node, String>> mergeGraphWithCheck = (g1, g2) -> {
        MutableValueGraph<Node, String> graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        g1.edges().forEach(e -> graph.putEdgeValue(e.nodeU(), e.nodeV(), g1.edgeValue(e.nodeU(), e.nodeV())));
        g2.edges().forEach(e -> graph.putEdgeValue(e.nodeU(), e.nodeV(), g2.edgeValue(e.nodeU(), e.nodeV())));
        g1.nodes().forEach(n -> graph.addNode(n));
        g2.nodes().forEach(n -> graph.addNode(n));
        return analyseAndEnrich.apply(ImmutableValueGraph.copyOf(graph));
    };

    public static Function<Variable, ImmutableValueGraph<Node, String>> mapToVarDeclToGraph = v -> {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        Node n = new Node(v.getId());
        g.addNode(n);
//        if (v.getInitializerCount() > 0)
        if(v.hasInitializer())
//            for (Identification init : v.getInitializerList()) {
//                Node initializer = addNodeToGraph(init, n.getId(), g);
            createBiDerectionalRelation(n, addNodeToGraph(v.getInitializer(), n.getId(), g), ASSIGNED_AS, Edges.ASSIGNED_TO, false, g);
//            }
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

    public static Function<MethodDeclaration, ImmutableValueGraph<Node, String>> mapToMethodDeclToGraph = m ->
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

    public static Function<MethodInvocation, ImmutableValueGraph<Node, String>> mapToMethodInvcToGraph = m ->
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


    public static Function<Assignment, ImmutableValueGraph<Node, String>> mapToAssgnmntToGraph = a ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
//        for (Identification rhs : a.getRhsList())
        createBiDerectionalRelation(addNodeToGraph(a.getLhs(), g), addNodeToGraph(a.getRhs(), a.getLhs(), g), ASSIGNED_AS, Edges.ASSIGNED_TO, false, g);
        return ImmutableValueGraph.copyOf(g);
    };

    public static Function<ClassDeclaration, ImmutableValueGraph<Node, String>> mapToClassDeclToGraph = a ->
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

    public static ImmutableValueGraph<Node, String> methodDeclarationGraphs() {
        List<MethodDeclaration> methodDeclarations = QueryProtoBuffData.getAllMethdDecl();
        ImmutableValueGraph<Node, String> methodParam = methodDeclarations.stream()
                .map(x -> mapToMethodDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithCheck);
        return methodParam;
    }

    public static ImmutableValueGraph<Node, String> variableDeclarationGraphs() {
        List<Variable> vars = QueryProtoBuffData.getAllVrbl();
        ImmutableValueGraph<Node, String> varGraph = vars.stream()
                .map(x -> mapToVarDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithCheck);
        return varGraph;
    }

    public static ImmutableValueGraph<Node, String> methodInvcGraphs() {
        List<MethodInvocation> mthInvc_newClass = QueryProtoBuffData.getAllMethdInvc();
        mthInvc_newClass.addAll(QueryProtoBuffData.getAllNewClass());
        ImmutableValueGraph<Node, String> methodParam = mthInvc_newClass.stream()
                .map(x -> mapToMethodInvcToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithCheck);
        return methodParam;
    }

    public static ImmutableValueGraph<Node, String> assgnmntGraphs() {
        List<Assignment> assgns = QueryProtoBuffData.getAllAsgn();
        ImmutableValueGraph<Node, String> methodParam = assgns.stream()
                .map(x -> mapToAssgnmntToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithCheck);
        return methodParam;
    }

    public static ImmutableValueGraph<Node, String> classDeclGraphs() {
        List<ClassDeclaration> classDecl = QueryProtoBuffData.getAllClassDecl();
        ImmutableValueGraph<Node, String> methodParam = classDecl.stream()
                .map(x -> mapToClassDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithCheck);
        return methodParam;
    }

    public static ImmutableValueGraph<Node, String> interfaceDeclGraphs() {
        List<ClassDeclaration> interfaceDecl = QueryProtoBuffData.getAllInterfaceDecl();
        ImmutableValueGraph<Node, String> methodParam = interfaceDecl.stream()
                .map(x -> mapToClassDeclToGraph.apply(x)).reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithCheck);
        return methodParam;
    }

    public static ImmutableValueGraph<Node, String> create() throws Exception {
        return Stream.of(methodDeclarationGraphs(), classDeclGraphs(), variableDeclarationGraphs(), methodInvcGraphs(),
                assgnmntGraphs(), interfaceDeclGraphs())
                .reduce(ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(true).build()),
                        mergeGraphWithCheck);
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

    private static Optional<Node> getNode(Identification id, String kind, MutableValueGraph<Node, String> g) {
        return g.nodes().stream().filter(x -> x.isSameAs(id, kind)).findFirst();
    }

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
