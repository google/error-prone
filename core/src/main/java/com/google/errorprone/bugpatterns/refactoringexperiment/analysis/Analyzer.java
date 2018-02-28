package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.FIELD;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTERFACE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LOCAL_VARIABLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.METHOD_INVOCATION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.NEW_CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.PARAMETER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Edges.PARAM_LAMBDA;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.METHOD_MAPPING_FOR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.SPECIALIZE_TO_PRIMITIVE;

import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.errorprone.bugpatterns.refactoringexperiment.Constants;
import com.google.errorprone.bugpatterns.refactoringexperiment.ProtoBuffPersist;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.RefactorableOuterClass.Refactorable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by ameya on 1/26/18.
 */
public class Analyzer {


    public static Predicate<Node> varKind = n -> n.getKind().equals(PARAMETER) || n.getKind().equals(LOCAL_VARIABLE)
            || n.getKind().equals(FIELD);

    public static Predicate<Node> mthdKind = n -> n.getKind().equals(METHOD_INVOCATION) || n.getKind().equals(NEW_CLASS);

    public static Predicate<Node> typeKind = n -> n.getKind().equals(CLASS) || n.getKind().equals(INTERFACE);

    public static BiPredicate<Node, ImmutableValueGraph<Node, String>> mthdRet = (n, g) ->
            n.getKind().equals("METHOD") &&
                    g.successors(n).stream().anyMatch(a -> g.edgeValue(n, a).get().equals(Edges.RETURNS));

    private static final Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> POPULATE_OBJ_REF = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        gr.nodes().stream().filter(varKind).forEach(n ->
                n.setRefactorTo(gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst().map(x -> x.getType()).orElse("")));

        gr.nodes().stream().filter(varKind).forEach(n ->{
            if (!n.refactorTo().equals("")) {
                gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(Edges.METHOD_INVOKED))
                        .forEach(m -> m.setRefactorTo(METHOD_MAPPING_FOR.get(getClassName(n.refactorTo())).get(m.getName())));
                gr.successors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(n, x).get().equals(Edges.ASSIGNED_AS))
                        .forEach(m -> {if(m.refactorTo()==null) m.setRefactorTo(n.refactorTo());});
                gr.predecessors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(x, n).get().equals(Edges.ARG_PASSED))
                        .filter(x -> !gr.successors(x).stream().anyMatch(y -> gr.edgeValue(x,y).get().equals(Edges.OF_TYPE)))
                        .forEach(m -> {if(m.refactorTo()==null) m.setRefactorTo(n.refactorTo());});
                gr.predecessors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(x, n).get().equals(Edges.ASSIGNED_TO) )
                        .forEach(m -> {if(m.refactorTo()==null) m.setRefactorTo(n.refactorTo());});
            }
        });
        return ImmutableValueGraph.copyOf(gr);
    };

    private static final Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> POPULATE_SUB_TYPE = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        gr.nodes().stream().filter(typeKind).forEach(n ->
                n.setRefactorTo(gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst().map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(typeKind).forEach(n ->{
            if (!n.refactorTo().equals("")) {
                gr.nodes().stream().filter(mthdKind).filter(x -> x.getOwner().getType().equals(n.getType()))
                        .forEach(m -> m.setRefactorTo(METHOD_MAPPING_FOR.get(getClassName(n.refactorTo())).get(m.getName())));
                gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals("OVERRIDES"))
                        .forEach(m ->{
                            m.setRefactorTo(METHOD_MAPPING_FOR.get(getClassName(n.refactorTo())).get(m.getName()));
                            gr.successors(m).stream().filter(x -> gr.edgeValue(m, x).get().contains(PARAM_LAMBDA)).forEach(
                                    i -> i.setRefactorTo(SPECIALIZE_TO_PRIMITIVE.get(i.getType())));
                        });
            }
        });
        return ImmutableValueGraph.copyOf(gr);
    };

    private static final Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> POPULATE_MTHD_RET = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        gr.nodes().stream().filter(x -> mthdRet.test(x, graph)).forEach(n ->
                n.setRefactorTo(gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst().map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(x -> mthdRet.test(x, graph)).forEach(n ->{
            if (!n.refactorTo().equals("")) {
                gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(Edges.METHOD_INVOKED))
                        .forEach(m -> m.setRefactorTo(METHOD_MAPPING_FOR.get(getClassName(n.refactorTo())).get(m.getName())));
                gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(Edges.ASSIGNED_TO) || gr.edgeValue(n, x).get().equals(Edges.PASSED_AS_ARG_TO))
                        .forEach(m -> m.setRefactorTo(n.refactorTo()));
            }
        });
        return ImmutableValueGraph.copyOf(gr);
    };


    private static final Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> POPULATE_MAPPING =
            POPULATE_OBJ_REF.compose(POPULATE_SUB_TYPE).compose(POPULATE_MTHD_RET);


    public static Predicate<ImmutableValueGraph<Node, String>> PRE_CONDITION_1 = graph ->
            !graph.edges().stream().filter(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(Edges.ARG_PASSED))
                    .filter(endpt -> !graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(Edges.ASSIGNED_AS))
                    .map(endpt -> endpt.nodeV()).anyMatch(v -> !(v.getKind().equals(Constants.LAMBDA_EXPRESSION)));

    public static Predicate<ImmutableValueGraph<Node, String>> PRE_CONDITION_2 = graph ->
            !graph.edges().stream().anyMatch(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).get().equals(Edges.ASSIGNED_AS));


    private static String pckgName;

    public static void main(String args[]) throws Exception {
        induceAndMap(pckgName).forEach(r -> ProtoBuffPersist.write(r, REFACTOR_INFO));
    }

    public static List<Refactorable> induceAndMap(String fromFolder) throws Exception {
        List<Refactorable> refactorables = new ArrayList<>();
        induceSubgraphs(CreateGraph.create(fromFolder)).stream().map(POPULATE_MAPPING).filter(PRE_CONDITION_1).filter(PRE_CONDITION_2)
                .forEach(g -> {
                    g.nodes().forEach(n -> {
                        if (!n.getKind().equals(REFACTOR_INFO)) {
                            System.out.println(n.toString());
                            if (n.refactorTo() != null)
                                refactorables.add(Refactorable.newBuilder().setId(n.getId()).setRefactorTo(n.refactorTo()).build());
                        }
                    });
                    System.out.println("**");
                });
        return refactorables;
    }

    public static Set<ImmutableValueGraph<Node, String>> induceSubgraphs(ImmutableValueGraph<Node, String> gr) {
        Set<ImmutableValueGraph<Node, String>> subGraphs = new HashSet<>();
        MutableValueGraph<Node, String> graphOfGraphs = Graphs.copyOf(gr);
        List<Node> params = graphOfGraphs.nodes().stream().filter(x -> varKind.test(x)).collect(Collectors.toList());
        for (Node n : params)
            if (!n.isVisited()) {
                Set<Node> reachables = Graphs.reachableNodes(graphOfGraphs.asGraph(), n).stream().collect(Collectors.toSet());
                reachables.stream().forEach(x -> x.setVisited(true));
                subGraphs.add(ImmutableValueGraph.copyOf(Graphs.inducedSubgraph(graphOfGraphs, reachables)));
            }

        return subGraphs;
    }

    private static String getClassName(String refactorTo) {
        return refactorTo.contains("<") ? refactorTo.substring(0, refactorTo.indexOf("<"))
                : refactorTo;

    }

}

