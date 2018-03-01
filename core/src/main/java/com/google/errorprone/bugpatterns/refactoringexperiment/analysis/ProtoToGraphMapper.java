package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LAMBDA_EXPRESSION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Edges.ASSIGNED_AS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Edges.TYPE_INFO;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDeclarationOuterClass.ClassDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by ameya on 2/28/18.
 */
public class ProtoToGraphMapper {

    public static Function<Variable, ImmutableValueGraph<Node, String>> mapVarDeclToGraph = v -> {
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

    public static void createBiDerectionalRelation(Node u, Node v, String uTov, String vTou, boolean allowSelfLoop
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
     * This method is for navigating from method invocation to method declaration
     * @param id : Identification of the method invocation.
     *  eg.[name : test, kind: METHOD_INVOCATION, type = (Function<Integer,Integer>)void, owner:Foo]
     * @param kind [kind : METHOD]
     * @param g
     * @return it returns a from the graph with id [name : test, kind: method, type = (Function<Integer,Integer>)void, owner:Foo]
     *          ,if it exists.
     */
    public static Optional<Node> getNode(Identification id, String kind, MutableValueGraph<Node, String> g) {
        return g.nodes().stream().filter(x -> x.isSameAs(id, kind)).findFirst();
    }

    /**
     * This method creates a node for a method invocation.
     * When we receive a f.apply(5), we identify apply by replacing owner of apply with id of 'f'
     * If the method invocations has an receiver, then we create a method invocation node from the
     * received proto, such that the owner of the method invocation is the receiver.
     * This helps us uniquely identify, the object references to which method invocations belong.
     * @param mi . MethodInovcation proto for which node needs to be created in the graph.
     * @param g  . The Graph in which the created node needs to be persisted.
     * @return : the method invocation node added to the graph.
     */

    private static Node addNodeToGraph(MethodInvocation mi, MutableValueGraph<Node, String> g) {
        return mi.hasReceiver() ? addNodeToGraph(new Node(mi.getId(), mi.getReceiver()), g) : addNodeToGraph(mi.getId(), g);
    }

    private static Node addNodeToGraph(Identification id, MutableValueGraph<Node, String> g) {
        return !id.hasName() ? addNodeToGraph(new Node(id.toBuilder().setName(id.getKind()).build()), g) : addNodeToGraph(new Node(id), g);
    }

    private static Node addNodeToGraph(Node n1, MutableValueGraph<Node, String> g) {
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

    /**
     * It returns the set of successors of a given node n, in the given graph, which have given edge value
     * @param n  : Node fow which the successors has to be found
     * @param gr : Graph in which the
     */
    public static ImmutableSet<Node> getSuccessorWithEdge(Node n, MutableValueGraph<Node, String> gr, String edgeValue) {
        return gr.successors(n).stream().filter(a -> gr.edgeValue(n, a).get().contains(edgeValue)).collect(toImmutableSet());
    }
}
