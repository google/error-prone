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
    private static String COLUMN_SPERATOR = "|";


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

    private static void populateFromAssignment() throws Exception {
        List<Assignment> assgns = QueryProtoBuffData.getAllAsgn();
        for (Assignment a : assgns) {
            Node lhs = getNode(a.getLhs()).map(x -> x).orElseThrow(() -> new Exception());
            Node rhs = populateIfUnNamed(addNodeToGraphIfAbsent(a.getRhs()),lhs);
            createBiDerectionalRelation(lhs, rhs, Relationships.assigned_as, Relationships.assigned_to, false);
        }
    }



    //     TODO ; perform argument analysis
//    TODO: checkfor f.apply(); link it to variables and method parameters and method invocation
//    1. create nodes for all method invocations in the graph.
//    2. Iterate over all instances
    private static void populate_methodInvc_newClass() throws Exception {
        List<MethodInvocation> mthInvc_newClass = QueryProtoBuffData.getAllMethdInvc();
        mthInvc_newClass.addAll(QueryProtoBuffData.getAllNewClass());
        for (MethodInvocation m : mthInvc_newClass) {
            Node n = addNodeToGraphIfAbsent(m);
            Optional<Node> md = getNode(m.getId(), Constants.METHOD);
            if (md.isPresent()) {
                createBiDerectionalRelation(n, md.get(), Relationships.parent_method, Relationships.method_invocations, false);
                for (Entry<Integer, Identification> argument : m.getArgumentsMap().entrySet()) {
                    Node param = getAdjacentNodeWithOutgoingEdgeValue(md.get(), Relationships.param_index + argument.getKey());
                    Node arg = populateIfUnNamed(addNodeToGraphIfAbsent(argument.getValue()),param);
                    createBiDerectionalRelation(arg, param, Relationships.passed_as_arg_to, Relationships.arg_passed, true);
                }
            } else if (m.hasReceiver()) {
                Node receiver = addNodeToGraphIfAbsent(m.getReceiver());
                createBiDerectionalRelation(receiver, n,
                        Relationships.method_invoked, Relationships.reference, false);
            } else {
                for (Entry<Integer, Identification> arg : m.getArgumentsMap().entrySet()) {
                    Optional<Node> a = getNode(arg.getValue());
                    if (a.isPresent())
                        g.putEdgeValue(a.get(), n, Relationships.passed_to_non_editable);
                }
            }
        }
    }


    // 1.populate graph with method declaration node, and parameters. Link the two.
    // 2.create links between methods connected through hierarchy. At this step i am sure i have nodes for every method.
    public static void populateFromMethodDeclarations() {
        List<MethodDeclaration> mthdDecls = QueryProtoBuffData.getAllMethdDecl();
        for (MethodDeclaration m : mthdDecls) {
            Node n = addNodeToGraph(m.getId());
            m.getParametersMap().entrySet().stream().map(param -> Maps.immutableEntry(param.getKey(), newNode(param.getValue())))
                    .forEach(x -> createBiDerectionalRelation(x.getValue(), n, Relationships.parent_method, Relationships.param_index + x.getKey(), false));
        }
        for (MethodDeclaration m : mthdDecls)
            if (m.hasSuperMethodIn())
                for (MethodDeclaration mh : methodsAffectedHierarchy(m.getId(), mthdDecls))
                    createBiDerectionalRelation(getNode(m.getId()).get(), getNode(mh.getId()).get(), Relationships.affected_by_hierarchy, Relationships.affected_by_hierarchy, false);
    }

    //1. If lcoal or field add node to graph
    //2. If Parameter, the node already exists, so get the params connected by hierarchy. At this step i know i have all nodes for any parameter
    private static void populateFromVariables() throws Exception {
        List<Variable> vars = QueryProtoBuffData.getAllVrbl();
        for (Variable v : vars) {
            if (!v.getId().getKind().equals(Constants.PARAMETER))
                addNodeToGraph(v.getId());
            else {
                Node n = getNode(v.getId()).orElseThrow(() -> new Exception());
                for (Node p : paramsAffectedHierarchy(n))
                    createBiDerectionalRelation(p, n, Relationships.affected_by_hierarchy, Relationships.affected_by_hierarchy, false);
            }
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

    public static List<Node> paramsAffectedHierarchy(Node n) {
        Node parentMethod = getAdjacentNodeWithOutgoingEdgeValue(n, Relationships.parent_method);
        int index = Integer.parseInt(g.edgeValue(parentMethod, n).replaceAll(Relationships.param_index, ""));
        return getAdjacentNodesWithOutgoingEdgeValue(parentMethod, Relationships.affected_by_hierarchy).stream()
                .map(h -> getAdjacentNodeWithOutgoingEdgeValue(h, Relationships.param_index + index))
                .collect(Collectors.toList());
    }

    private static Node addNodeToGraphIfAbsent(Identification id) {
        Optional<Node> n = getNode(id);
        return n.isPresent() ?
                n.get() : addNodeToGraph(id);
    }

    private static Node addNodeToGraphIfAbsent(MethodInvocation mi) {
        if (mi.hasReceiver()) {
            String owner = mi.getReceiver().getName() + COLUMN_SPERATOR + mi.getReceiver().getOwner();
            Optional<Node> n = getNode(owner, mi.getId());
            return n.isPresent() ?
                    n.get() : addNodeToGraph(mi.getId(), owner);
        }
        else{
            Optional<Node> n = getNode(mi.getId());
            return n.isPresent() ?
                    n.get() : addNodeToGraph(mi.getId());
        }
    }

    private static Optional<Node> getNode(Identification id) {
        return g.nodes().stream().filter(x -> x.equals(id)).findFirst();
    }

    private static Optional<Node> getNode(String owner,Identification id) {
        return g.nodes().stream().filter(x -> x.isRelated(owner,id)).findFirst();
    }

    private static Optional<Node> getNode(Identification id,String kind) {
        return g.nodes().stream().filter(x -> x.isRelated(id,kind)).findFirst();
    }

    private static Node newNode(Identification id) {
        return new Node(id);
    }


    private static void createBiDerectionalRelation(Node u, Node v, String uTov, String vTou, boolean allowSelfLoop) {
        if ((!u.equals(v))) {
            g.putEdgeValue(u, v, uTov);
            g.putEdgeValue(v, u, vTou);
        } else if ((u.equals(v) && allowSelfLoop)) {
            g.putEdgeValue(u, v, Relationships.recurssive);
            g.putEdgeValue(v, u, Relationships.recurssive);
        }
    }

    public static Node addNodeToGraph(Identification id) {
        Node n = new Node(id);
        g.addNode(n);
        return n;
    }

    public static Node addNodeToGraph(Identification id, String owner) {
        Node n = new Node(id, owner);
        g.addNode(n);
        return n;
    }

    private static void removeEdges() {
        g.edges().stream().filter(x -> g.edgeValue(x.nodeU(), x.nodeV()).contains(Relationships.param_index)).forEach(x -> g.removeEdge(x.nodeU(), x.nodeV()));
    }

    private static Node populateIfUnNamed(Node n1, Node n2) {
        if(n1.name.isEmpty()){
            n1.name = n2.name;
            n1.owner = n2.owner;
            n1.type = n2.type;
        }
        return n1;
    }
}
