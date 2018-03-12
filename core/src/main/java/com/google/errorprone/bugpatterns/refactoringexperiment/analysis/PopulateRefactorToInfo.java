package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.CLASS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.FIELD;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTERFACE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LOCAL_VARIABLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.METHOD_INVOCATION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.PARAMETER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Edges.PARAM_LAMBDA;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.SPECIALIZE_TO_PRIMITIVE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping.getMethodMappingFor;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Created by ameya on 2/28/18.
 */
public class PopulateRefactorToInfo {


    public static Predicate<Identification> varKind = n -> n.getKind().equals(PARAMETER) || n.getKind().equals(LOCAL_VARIABLE)
            || n.getKind().equals(FIELD);

    private static Predicate<Identification> typeKind = n -> n.getKind().equals(CLASS) || n.getKind().equals(INTERFACE);

    public static BiPredicate<Identification, ImmutableValueGraph<Identification, String>> mthdRet = (n, g) ->
            n.getKind().equals("METHOD") &&
                    g.successors(n).stream().anyMatch(a -> g.edgeValue(n, a).get().equals(Edges.RETURNS));

    public static BiPredicate<Identification, ImmutableValueGraph<Identification, String>> isSubType = (n, gr) ->
            !gr.successors(n).stream().anyMatch(y -> gr.edgeValue(n, y).get().equals(Edges.OF_TYPE));

    private static Optional<Identification> getRefactorInfo(Identification n, ImmutableValueGraph<Identification, String> gr) {
        return gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst();
    }

    public static final Map<Identification, String> getMapping(ImmutableValueGraph<Identification, String> gr) {
        Map<Identification, String> graphToMapping = new HashMap<>();
        populateObjeRef(gr,graphToMapping);
        populateSubType(gr,graphToMapping);
        return graphToMapping;
    }

    public static final Map<Identification, String> populateObjeRef(ImmutableValueGraph<Identification, String> gr, Map<Identification, String> graphToMapping) {
        gr.nodes().stream().filter(varKind).forEach(n -> graphToMapping.put(n, getRefactorInfo(n, gr).map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(varKind).filter(x -> !Strings.isNullOrEmpty(graphToMapping.get(x))).forEach(n -> propogateRefactorInfo(gr, graphToMapping, n));
        return graphToMapping;
    }

    public static Map<Identification, String> populateSubType(ImmutableValueGraph<Identification, String> gr, Map<Identification, String> graphToMapping) {
        gr.nodes().stream().filter(typeKind).forEach(n -> graphToMapping.put(n, getRefactorInfo(n, gr).map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(typeKind).filter(x -> !Strings.isNullOrEmpty(graphToMapping.get(x))).forEach(n -> {
            propogateRefactorInfo(gr, graphToMapping, n);
            getMethodInvocationsOfType(n.getType(), gr)
                    .forEach(m -> graphToMapping.putIfAbsent(m, getMethodMappingFor(getClassName(graphToMapping.get(n)), m.getName())));
        });
        return graphToMapping;
    }

    // populates the object references and sub_types with their dependents
    private static ImmutableSet<Identification> getMethodInvocationsOfType(String type, ImmutableValueGraph<Identification, String> gr) {
        return gr.nodes().stream().filter(m -> m.getKind().equals(METHOD_INVOCATION)).filter(x -> x.getOwner().getType().equals(type))
                .collect(collectingAndThen(toList(), ImmutableSet::copyOf));
    }

    private static void propogateRefactorInfo(ImmutableValueGraph<Identification, String> gr,
                                              Map<Identification, String> graphToMapping, Identification n) {
        Set<Identification> adjNodes = gr.adjacentNodes(n);
        for (Identification adj : adjNodes) {
            //successors
            if (gr.edgeValue(n, adj).isPresent()) {
                switch (gr.edgeValue(n, adj).get()) {
                    case Edges.METHOD_INVOKED:
                        graphToMapping.putIfAbsent(adj, getMethodMappingFor(getClassName(graphToMapping.get(n)), adj.getName()));
                        break;
                    case Edges.ASSIGNED_AS:
                        graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                    case Edges.ASSIGNED_TO:
                        graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                    case Edges.PASSED_AS_ARG_TO:
                        graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                    case Edges.OVERRIDES:
                        graphToMapping.putIfAbsent(adj, getMethodMappingFor(getClassName(graphToMapping.get(n)), adj.getName()));
                        gr.successors(adj).stream().filter(x -> gr.edgeValue(adj, x).get().contains(PARAM_LAMBDA)).forEach(
                                i -> graphToMapping.putIfAbsent(i, SPECIALIZE_TO_PRIMITIVE.get(i.getType())));
                        break;
                    default: {
                    }
                    break;
                }
            }
            //predecessor
            else {
                switch (gr.edgeValue(adj, n).get()) {
                    case Edges.ARG_PASSED:
                        if (!isSubType.test(adj, gr))
                            graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                    case Edges.ASSIGNED_TO:
                        graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                }
            }
        }
    }

    private static String getClassName(String refactorTo) {
        return refactorTo.contains("<") ? refactorTo.substring(0, refactorTo.indexOf("<"))
                : refactorTo;

    }


}
