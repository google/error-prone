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
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.SPECIALIZE_TO_PRIMITIVE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.getMethodMappingFor;

import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by ameya on 2/28/18.
 */
public class PopulateRefactorToInfo {


    public static Predicate<Node> varKind = n -> n.getKind().equals(PARAMETER) || n.getKind().equals(LOCAL_VARIABLE)
            || n.getKind().equals(FIELD);

    public static Predicate<Node> mthdKind = n -> n.getKind().equals(METHOD_INVOCATION) || n.getKind().equals(NEW_CLASS);

    private static Predicate<Node> typeKind = n -> n.getKind().equals(CLASS) || n.getKind().equals(INTERFACE);

    private static BiPredicate<Node, ImmutableValueGraph<Node, String>> mthdRet = (n, g) ->
            n.getKind().equals("METHOD") &&
                    g.successors(n).stream().anyMatch(a -> g.edgeValue(n, a).get().equals(Edges.RETURNS));


    private static final Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> POPULATE_OBJ_REF = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        gr.nodes().stream().filter(varKind).forEach(n ->
                n.setRefactorTo(gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst().map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(varKind).forEach(n -> {
            if (!n.refactorTo().equals("")) {
                gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(Edges.METHOD_INVOKED))
                        .forEach(m -> m.setRefactorTo(getMethodMappingFor(getClassName(n.refactorTo()),m.getName())));
                gr.successors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(n, x).get().equals(Edges.ASSIGNED_AS))
                        .forEach(m -> {
                            if (m.refactorTo() == null) m.setRefactorTo(n.refactorTo());
                        });
                gr.predecessors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(x, n).get().equals(Edges.ARG_PASSED))
                        .filter(x -> !gr.successors(x).stream().anyMatch(y -> gr.edgeValue(x, y).get().equals(Edges.OF_TYPE)))
                        .forEach(m -> {
                            if (m.refactorTo() == null) m.setRefactorTo(n.refactorTo());
                        });
                gr.predecessors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(x, n).get().equals(Edges.ASSIGNED_TO))
                        .forEach(m -> {
                            if (m.refactorTo() == null) m.setRefactorTo(n.refactorTo());
                        });
            }
        });
        return ImmutableValueGraph.copyOf(gr);
    };

    private static final Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> POPULATE_SUB_TYPE = graph -> {
        MutableValueGraph<Node, String> gr = Graphs.copyOf(graph);
        gr.nodes().stream().filter(typeKind).forEach(n ->
                n.setRefactorTo(gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst().map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(typeKind).filter(t -> !t.refactorTo().equals("")).forEach(n -> {
            if (!n.refactorTo().equals("")) {
                gr.nodes().stream().filter(mthdKind).filter(x -> x.getOwner().getType().equals(n.getType()))
                        .forEach(m -> m.setRefactorTo(getMethodMappingFor(getClassName(n.refactorTo()),m.getName())) );
                gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals("OVERRIDES"))
                        .forEach(m -> {
                            m.setRefactorTo(getMethodMappingFor(getClassName(n.refactorTo()),m.getName()));
                            gr.successors(m).stream().filter(x -> gr.edgeValue(m, x).get().contains(PARAM_LAMBDA)).forEach(
                                    i -> i.setRefactorTo(SPECIALIZE_TO_PRIMITIVE.get(i.getType())));
                        });
            }
        });
        return ImmutableValueGraph.copyOf(gr);
    };

    // populates the object references and sub_types with their dependents
    public static final Function<ImmutableValueGraph<Node, String>, ImmutableValueGraph<Node, String>> POPULATE_MAPPING =
            POPULATE_OBJ_REF.compose(POPULATE_SUB_TYPE);

    private static String getClassName(String refactorTo) {
        return refactorTo.contains("<") ? refactorTo.substring(0, refactorTo.indexOf("<"))
                : refactorTo;

    }


}
