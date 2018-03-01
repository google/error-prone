package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INFERRED_CLASS;
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

    /**
     * This function maps a variable  proto to a independent graph.
     * @param v: a variable proto to be mapped to graph.
     * Mapping works as follows:
     * 1. Create node from id of the variable proto and add it to graph.
     * 2. If proto has an initializer:
     *      a. Create a new node for the initializer from its Id.
     *      b. Add it to the graph.
     *      c. establish edge : Variable node <---ASSIGNED_AS--ASSIGNED_TO---> Initializer node.
     * 3. If proto has Filtered Type:
     *      a.Create a new node with name, owner same as variable id, kind REFACTOR_INFO
     *              and type as the mapped type from filtered type.
     *      b.Add this node to graph.
     *      c.establish edge : Variable node --- REFACTOR_INFO ---> RefactorInfo node.
     *   Else(this means that variable type is sub_type of functional interface)
     *      a.Create a temporary node, with type = varriable type and kind = INFERRED_CLASS.
     *      b. establish edge : variable node --- TYPE_INFO ---> InferredClass node.
     */

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

    /**
     * This function maps a method declaration proto to a independent graph.
     * @param m: a method declaration proto to be mapped to graph
     * Mapping works as follows:
     * 1. Create node from id of the method declaration proto and add it to graph
     * 2. If proto has parameters:
     *      a.Create a new node with parameter ID
     *      b.Add this node to graph
     *      c.establish edge : method declaration node --- PARAM_INDEX : {index} ---> RefactorInfo node
     * 3. If proto has a super method:
     *      a.Create a node for the super method declaration
     *      b.establish edge : Method Declaration <--- AFFECTED_BY_HIERARCHY--- AFFECTED_BY_HIERARCHY ---> Super method declaration
     *      c.establish a similar bidirectional relationship between the parameters of the
     *          method declaration and super method declaration.
     */

    public static Function<MethodDeclaration, ImmutableValueGraph<Node, String>> mapToMethodDeclToGraph = m ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        Node n = addNodeToGraph(m.getId(), g);
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

    /**
     * This function maps a method invocation proto to a independent graph.
     * @param m: a method invocation proto to be mapped to graph
     * Mapping works as follows:
     * 1. Create a node from method invocation
     *         if method invocation proto has a receiver
     *              a.create a node from id of method invocation replacing the owner of with id of the receiver.
     *                  and add to graph
     *              b.create a node from id of the receiver and add to graph
     *              c.establish edge :method invocation node <--- METHOD_INVOKED--- REFERENCE ---> receiver node
     *        else create a node from the id of the method invocation
     * 2. If proto has arguments:
     *      a.Create a new node with arguments ID
     *      b.Add this node to graph
     *      c.establish edge : method declaration node --- ARG_INDEX : {index} ---> RefactorInfo node
     */
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

    /**
     * This function maps a assignment proto to a independent graph.
     * @param a: an assignment proto to be mapped to graph.
     * Mapping works as follows:
     * 1. create a node for RHS and LHS of the assignment operations from their Id and add to graph.
     * 2. establish edge: RHS <--- ASSIGNED_AS--- ASSIGNED_TO ---> LHS
     */
    public static Function<Assignment, ImmutableValueGraph<Node, String>> mapToAssgnmntToGraph = a ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        createBiDerectionalRelation(addNodeToGraph(a.getLhs(), g), addNodeToGraph(a.getRhs(), a.getLhs(), g), ASSIGNED_AS, Edges.ASSIGNED_TO, false, g);
        return ImmutableValueGraph.copyOf(g);
    };

    /**
     * This function maps a class declaration proto to a independent graph.
     * @param c: an class declaration proto to be mapped to graph.
     * Mapping works as follows:
     * 1. create a node from class declaration id and add to graph.
     * 2. Create a new node with name, owner same as class declaration id, kind REFACTOR_INFO
     *              and type as the mapped type from filtered type.
     *      b.Add this node to graph.
     *      c.establish edge : class declaration node --- REFACTOR_INFO ---> RefactorInfo node.
     */
    public static Function<ClassDeclaration, ImmutableValueGraph<Node, String>> mapToClassDeclToGraph = c ->
    {
        MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        Node n = new Node(c.getId());
        g.addNode(n);
        if (Mapping.CLASS_MAPPING_FOR.containsKey(c.getSuperType().getInterfaceName())) {
            Node refactorInfo = new Node(n.getId().toBuilder().setKind(REFACTOR_INFO)
                    .setType(Mapping.CLASS_MAPPING_FOR.get(c.getSuperType().getInterfaceName()).apply(c.getSuperType())).build());
            g.putEdgeValue(n, refactorInfo, REFACTOR_INFO);
        }
        return ImmutableValueGraph.copyOf(g);
    };


    /**
     * This method establishes edges : Node u <---vTou---uTo--->Node v
     * @param u
     * @param v
     * @param uTov : edge value for edge from u to v,
     * @param vTou : edge value for edge from v to u.
     * @param allowSelfLoop :
     * @param g : Graph in which the relationship needs to be established,
     * We might have recursive function, where the parameter is passed back to itself as an argument.
     * We identify such relations with edge value RECURSIVE.
     */
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
