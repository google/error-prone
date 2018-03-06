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
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.NO_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.SPECIALIZE_TO_PRIMITIVE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.getMethodMappingFor;

import com.google.common.graph.ImmutableValueGraph;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Created by ameya on 2/28/18.
 */
public class PopulateRefactorToInfo {


    public static Predicate<Identification> varKind = n -> n.getKind().equals(PARAMETER) || n.getKind().equals(LOCAL_VARIABLE)
            || n.getKind().equals(FIELD);

    public static Predicate<Identification> mthdKind = n -> n.getKind().equals(METHOD_INVOCATION) || n.getKind().equals(NEW_CLASS);

    private static Predicate<Identification> typeKind = n -> n.getKind().equals(CLASS) || n.getKind().equals(INTERFACE);

    private static BiPredicate<Identification, ImmutableValueGraph<Identification, String>> mthdRet = (n, g) ->
            n.getKind().equals("METHOD") &&
                    g.successors(n).stream().anyMatch(a -> g.edgeValue(n, a).get().equals(Edges.RETURNS));


    public static final Map<Identification, String> populateObjeRef(ImmutableValueGraph<Identification, String> gr) {
        Map<Identification, String> graphToMapping = new HashMap<>();
        gr.nodes().stream().filter(varKind).forEach(n ->
                graphToMapping.put(n, gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO))
                        .findFirst().map(x -> x.getType()).orElse("")));

        gr.nodes().stream().filter(varKind).forEach(n -> {
            if (!graphToMapping.get(n).equals("")) {

                gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(Edges.METHOD_INVOKED))
                        .forEach(m -> graphToMapping.put(m, getMethodMappingFor(getClassName(graphToMapping.get(n)), m.getName())));

                gr.successors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(n, x).get().equals(Edges.ASSIGNED_AS))
                        .forEach(m -> {
                            if (!graphToMapping.containsKey(m))
                                graphToMapping.put(m, graphToMapping.get(n));
                        });
                gr.predecessors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(x, n).get().equals(Edges.ARG_PASSED))
                        .filter(x -> !gr.successors(x).stream().anyMatch(y -> gr.edgeValue(x, y).get().equals(Edges.OF_TYPE)))
                        .forEach(m -> {
                            if (!graphToMapping.containsKey(m))
                                graphToMapping.put(m, graphToMapping.get(n));
                        });
                gr.predecessors(n).stream().filter(x -> !x.getKind().equals(REFACTOR_INFO))
                        .filter(x -> gr.edgeValue(x, n).get().equals(Edges.ASSIGNED_TO))
                        .forEach(m -> {
                            if (!graphToMapping.containsKey(m))
                                graphToMapping.put(m, graphToMapping.get(n));
                        });
            }
        });
        return graphToMapping;
    }

    public static Map<Identification, String> populateSubType(ImmutableValueGraph<Identification, String> gr) {
        Map<Identification, String> graphToMapping = new HashMap<>();
        gr.nodes().stream().filter(typeKind).forEach(n ->
                graphToMapping.put(n, gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst().map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(typeKind).filter(t -> !graphToMapping.get(t).equals("")).forEach(n -> {

            gr.nodes().stream().filter(m -> m.getKind().equals(METHOD_INVOCATION)).filter(x -> x.getOwner().getType().equals(n.getType()))
                    .forEach(m -> graphToMapping.put(m, getMethodMappingFor(getClassName(graphToMapping.get(n)), m.getName())));

            gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals("OVERRIDES"))
                    .forEach(m -> {
                        graphToMapping.put(m, getMethodMappingFor(getClassName(graphToMapping.get(n)), m.getName()));
                        gr.successors(m).stream().filter(x -> gr.edgeValue(m, x).get().contains(PARAM_LAMBDA)).forEach(
                                i -> graphToMapping.put(i, SPECIALIZE_TO_PRIMITIVE.get(i.getType())));
                    });

        });
        return graphToMapping;
    }

    // populates the object references and sub_types with their dependents
    public static final Map<Identification, String> getMapping(ImmutableValueGraph<Identification, String> gr) {
        Map<Identification, String> graphToMapping = new HashMap<>();
        graphToMapping.putAll(populateObjeRef(gr));
       populateSubType(gr).entrySet().forEach(x -> {
            if(!graphToMapping.containsKey(x)
                    || ((graphToMapping.containsKey(x)) && graphToMapping.get(x).equals(NO_MAPPING)))
                graphToMapping.put(x.getKey(),x.getValue());});
        return graphToMapping;
    }

    private static String getClassName(String refactorTo) {
        return refactorTo.contains("<") ? refactorTo.substring(0, refactorTo.indexOf("<"))
                : refactorTo;

    }


}
