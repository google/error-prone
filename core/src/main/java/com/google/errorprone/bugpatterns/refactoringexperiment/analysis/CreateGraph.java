package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.HierarchyUtil.methodsAffectedHierarchy;
import com.google.common.collect.Maps;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.errorprone.bugpatterns.refactoringexperiment.Constants;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by ameya on 1/31/18.
 */
public class CreateGraph {


    public static MutableValueGraph<Node, String> g = ValueGraphBuilder.directed().allowsSelfLoops(true).build();


    public static MutableValueGraph<Node, String> create() throws Exception {
        if (g.nodes().isEmpty()) {
            populateFromMethodDeclarations();
            populateFromVariables();
            populate_methodInvc_newClass();
            populateFromAssignment();
            removeEdges();
        }
        return g;
    }


    // 1.populate graph with method declaration node, and parameters. Link the two with <PARENT_METHOD,PARAM_INDEX+ index> relation
    //              PARAM_INDEX+ index is a helper edge which will be removed when entire graph is constructed.
    // 2.create links between methods connected through hierarchy. At this step i am sure i have nodes for every method declaration.
    public static void populateFromMethodDeclarations() {
        List<MethodDeclaration> mthdDecls = QueryProtoBuffData.getAllMethdDecl();
        for (MethodDeclaration m : mthdDecls) {
            Node n = addNodeToGraph(m.getId());
            m.getParametersMap().entrySet().stream().map(param -> Maps.immutableEntry(param.getKey(), newNode(param.getValue())))
                        .forEach(x -> createBiDerectionalRelation(x.getValue(), n, Edges.PARENT_METHOD, Edges.PARAM_INDEX + x.getKey(), false));

        }

        for (MethodDeclaration m : mthdDecls)
            if (m.hasSuperMethodIn())
                for (MethodDeclaration mh : methodsAffectedHierarchy(m.getId(), mthdDecls))
                    createBiDerectionalRelation(getNode(m.getId()).get(), getNode(mh.getId()).get(), Edges.AFFECTED_BY_HIERARCHY, Edges.AFFECTED_BY_HIERARCHY, false);
    }

    // m (int x)    m - param index 0 -> x | x - parentmethod -> m

    //1. If local or field add node to graph
    //          manage initialiser if any, like rhs of assignment
    //2. If Parameter, the node already exists, so get the params connected by hierarchy. At this step i know i have all nodes for any parameter

    private static void populateFromVariables() throws Exception {
        // local, field, param
        List<Variable> vars = QueryProtoBuffData.getAllVrbl();
        for (Variable v : vars) {
            Node n;
            if (!v.getId().getKind().equals(Constants.PARAMETER)) {
                n = addNodeToGraph(v.getId());
                if (v.hasInitializer()) {
                    Node initializer = addNodeToGraphIfAbsent(v.getInitializer(), n.getId());
                    createBiDerectionalRelation(n, initializer, Edges.INITIALIZED_AS, Edges.ASSIGNED_TO, false);
                }
            } else {
                n = getNode(v.getId()).orElseThrow(() -> new Exception());
                for (Node p : paramsAffectedHierarchy(n))
                    createBiDerectionalRelation(p, n, Edges.AFFECTED_BY_HIERARCHY, Edges.AFFECTED_BY_HIERARCHY, false);
            }
            n.setRefactorTo(Mapping.getMappedTypeFor(v.getFilteredType()));
        }
    }


    // x = 1
    // x intialised as 1 | 1 is assigned to x


    // We process newClass and method invocations together
    //    1. create nodes for all method invocations in the graph.
    //          If it has a receiver, then create a method invocation with : owner = mi.getReceiver().getName() + COLUMN_SPERATOR + mi.getReceiver().getOwner()
    //                                  kind = mi.id.kind type = mi.id.type and name : mi.id.name
    //          else create node with m.id
    //    2.  Find parent method of the method invocation
    //          if FOUND : establish relation between mi and md <PARENT_METHOD,METHOD_INVOCATION>
    //                      for each argument of mi, link it with corresponding parameter in md with  relation <PASSED_AS_ARG_TO,ARG_PASSED>
    //          else if HAS RECEIVER: Establish relationship between receiver and mi. <METHOD_INVOKED,REFERENCE>
    //                       Receivers could be of two types:  Function<I,I> f = lambda; f.apply (5); ...here type is variable
    //                                                         public Function<I,I> m1 { return lambda;}  mi().apply(5).....here type is method invocations
    //    3. Else : we mark this method as non editable, and establish PASSED_TO_NON_EDITABLE relation between arg -> methodInvocation

    private static void populate_methodInvc_newClass() throws Exception {
        List<MethodInvocation> mthInvc_newClass = QueryProtoBuffData.getAllMethdInvc();
        mthInvc_newClass.addAll(QueryProtoBuffData.getAllNewClass());
        for (MethodInvocation m : mthInvc_newClass) {
            Node n = addNodeToGraphIfAbsent(m);
            Optional<Node> md = getNode(m.getId(), Constants.METHOD);
            if (md.isPresent()) {
                createBiDerectionalRelation(n, md.get(), Edges.PARENT_METHOD, Edges.METHOD_INVOCATION, false);
                for (Entry<Integer, Identification> argument : m.getArgumentsMap().entrySet()) {
                    Node param = getAdjacentNodeWithOutgoingEdgeValue(md.get(), Edges.PARAM_INDEX + argument.getKey());
                    Node arg = addNodeToGraphIfAbsent(argument.getValue(), md.get().getId());
                    createBiDerectionalRelation(arg, param, Edges.PASSED_AS_ARG_TO, Edges.ARG_PASSED, true);
                }
            } else if (m.hasReceiver()) {
                Node receiver = addNodeToGraphIfAbsent(m.getReceiver());
                createBiDerectionalRelation(receiver, n,
                        Edges.METHOD_INVOKED, Edges.REFERENCE, false);
            } else {
                for (Entry<Integer, Identification> arg : m.getArgumentsMap().entrySet()) {
                    Optional<Node> a = getNode(arg.getValue());
                    if (a.isPresent())
                        g.putEdgeValue(a.get(), n, Edges.PASSED_TO_NON_EDITABLE);
                }
            }
        }
    }

    //1. We get LHS. We have already created a node for it.
    //2. Establish relation between node on RHS and LHS <ASSIGNED_AS,ASSIGNED_TO>
    // v = lambda
    private static void populateFromAssignment() throws Exception {
        List<Assignment> assgns = QueryProtoBuffData.getAllAsgn();
        for (Assignment a : assgns) {
            Node lhs = getNode(a.getLhs()).map(x -> x).orElseThrow(() -> new Exception());
            Node rhs = addNodeToGraphIfAbsent(a.getRhs(), lhs.getId());
            createBiDerectionalRelation(lhs, rhs, Edges.ASSIGNED_AS, Edges.ASSIGNED_TO, false);
        }
    }


    private static List<Node> getAdjacentNodesWithOutgoingEdgeValue(Node n, String edgeValue) {
        return g.successors(n).stream().filter(a -> g.edgeValue(n, a).toString().equals(edgeValue)).collect(Collectors.toList());
    }

    private static Node getAdjacentNodeWithOutgoingEdgeValue(Node n, String edgeValue) {
        //TODO: Exception handling when no edge found
        Optional<Node> n1 = g.successors(n).stream().filter(a -> g.edgeValue(n, a).toString().equals(edgeValue)).findFirst();
        if (n1.isPresent())
            return n1.get();
        return null;
    }


    //We already have linked the method declarations by AFFECTED_BY_HIERARCHY relationship.
    //To make things more precise,establish the relation between the corresponding parameter nodes of the method declaration
    public static List<Node> paramsAffectedHierarchy(Node n) {
        Node parentMethod = getAdjacentNodeWithOutgoingEdgeValue(n, Edges.PARENT_METHOD);
        int index = Integer.parseInt(g.edgeValue(parentMethod, n).replaceAll(Edges.PARAM_INDEX, ""));
        return getAdjacentNodesWithOutgoingEdgeValue(parentMethod, Edges.AFFECTED_BY_HIERARCHY).stream()
                .map(h -> getAdjacentNodeWithOutgoingEdgeValue(h, Edges.PARAM_INDEX + index))
                .collect(Collectors.toList());
    }


    // Checks if graph is present, else add node
    private static Node addNodeToGraphIfAbsent(Identification id) {
        Optional<Node> n = getNode(id);
        return n.isPresent() ?
                n.get() : addNodeToGraph(id);
    }

    public static Node addNodeToGraph(Identification id) {
        Node n = new Node(id);
        g.addNode(n);
        return n;
    }

    private static Optional<Node> getNode(Identification id) {
        return g.nodes().stream().filter(x -> x.isSameAs(id)).findFirst();
    }


    private static Node addNodeToGraphIfAbsent(Identification value, Identification owner) {
        if (!value.hasName()) {
            Optional<Node> n = getNode(value, owner);
            return n.isPresent() ?
                    n.get() : addNodeToGraph(value, owner);
        } else
            return addNodeToGraphIfAbsent(value);

    }

    // This is helper method, creates/gets node for methodinvocation based on receiver if present.
    private static Node addNodeToGraphIfAbsent(MethodInvocation mi) {
        if (mi.hasReceiver()) {
            Identification owner = mi.getReceiver();
            // String owner = mi.getReceiver().getName() + COLUMN_SPERATOR + mi.getReceiver().getOwner();
            Optional<Node> n = getNode(mi.getId(), owner);
            return n.isPresent() ?
                    n.get() : addNodeToGraph(mi.getId(), owner);
        } else
            return addNodeToGraphIfAbsent(mi.getId());
    }


    // This is to search method invocations which has receiver
    private static Optional<Node> getNode(Identification id, Identification owner) {
        return g.nodes().stream().filter(x -> x.isSameAs(owner, id)).findFirst();
    }

    // This is to search unnamed expressions like :
    // lambda epressions, anonymous classes, null literals
    private static Optional<Node> getNode(Node param, String kind) {
        return g.nodes().stream().filter(x -> x.isSameAs(param, kind)).findFirst();
    }


    // We need this to query for Parent method declaration of method invocation in context.
    // Since md and mi will be having the same name,type,owner.
    private static Optional<Node> getNode(Identification id, String kind) {
        return g.nodes().stream().filter(x -> x.isSameAs(id, kind)).findFirst();
    }

    private static Node newNode(Identification id) {
        return new Node(id);
    }


    private static void createBiDerectionalRelation(Node u, Node v, String uTov, String vTou, boolean allowSelfLoop) {
        if ((!u.equals(v))) {
            g.putEdgeValue(u, v, uTov);
            g.putEdgeValue(v, u, vTou);
        } else if ((u.equals(v) && allowSelfLoop)) {
            g.putEdgeValue(u, v, Edges.RECURSSIVE);
            g.putEdgeValue(v, u, Edges.RECURSSIVE);
        }
    }


    public static Node addNodeToGraph(Identification id, Identification owner) {
        Node n = new Node(id, owner);
        g.addNode(n);
        return n;
    }


    private static void removeEdges() {
        g.edges().stream().filter(x -> g.edgeValue(x.nodeU(), x.nodeV()).contains(Edges.PARAM_INDEX)).forEach(x -> g.removeEdge(x.nodeU(), x.nodeV()));
    }

}
