package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ARG_PASSED;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ASSIGNED_AS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_ASSIGNED_TO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_METHOD_INVOKED;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_OF_TYPE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_OVERRIDES;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_PARAM_LAMBDA;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.EDGE_PASSED_AS_ARG_TO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.METHOD_INVOCATION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ConstructGraph.isTypeKind;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ConstructGraph.isVarKind;
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

/**
 * Created by ameya on 2/28/18.
 */
public final class PopulateRefactorToInfo {



    public static BiPredicate<Identification, ImmutableValueGraph<Identification, String>> isSubType = (n, gr) ->
            gr.successors(n).stream().anyMatch(y -> gr.edgeValue(n, y).get().equals(EDGE_OF_TYPE));

    private static final Optional<Identification> getRefactorInfo(Identification n, ImmutableValueGraph<Identification, String> gr) {
        return gr.successors(n).stream().filter(x -> gr.edgeValue(n, x).get().equals(REFACTOR_INFO)).findFirst();
    }

    public static Map<Identification, String> getMapping(ImmutableValueGraph<Identification, String> gr) {
        Map<Identification, String> graphToMapping = new HashMap<>();
        populateObjeRef(gr,graphToMapping);
        populateSubType(gr,graphToMapping);
        return graphToMapping;
    }

    public static final Map<Identification, String> populateObjeRef(ImmutableValueGraph<Identification, String> gr, Map<Identification, String> graphToMapping) {

        gr.nodes().stream().filter(isVarKind).filter(x ->!isSubType.test(x,gr)).forEach(n -> graphToMapping.put(n, getRefactorInfo(n, gr).map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(isVarKind).filter(x -> !Strings.isNullOrEmpty(graphToMapping.get(x))).forEach(n -> propogateRefactorInfo(gr, graphToMapping, n));
        return graphToMapping;
    }

    private static Map<Identification, String> populateSubType(ImmutableValueGraph<Identification, String> gr, Map<Identification, String> graphToMapping) {
        gr.nodes().stream().filter(isTypeKind).forEach(n -> graphToMapping.put(n, getRefactorInfo(n, gr).map(x -> x.getType()).orElse("")));
        gr.nodes().stream().filter(isTypeKind).filter(x -> !Strings.isNullOrEmpty(graphToMapping.get(x))).forEach(n -> {
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
                    case EDGE_ASSIGNED_TO:
                    case EDGE_METHOD_INVOKED:
                        graphToMapping.putIfAbsent(adj, getMethodMappingFor(getClassName(graphToMapping.get(n)), adj.getName()));
                        break;
                    case EDGE_ASSIGNED_AS:
                    case EDGE_ARG_PASSED:
                        if (!isSubType.test(adj, gr))
                            graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                    case EDGE_PASSED_AS_ARG_TO:
                        graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                    case EDGE_OVERRIDES:
                        graphToMapping.putIfAbsent(adj, getMethodMappingFor(getClassName(graphToMapping.get(n)), adj.getName()));
                        gr.successors(adj).stream().filter(x -> gr.edgeValue(adj, x).get().contains(EDGE_PARAM_LAMBDA)).forEach(
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
                    case EDGE_ARG_PASSED:
                        if (!isSubType.test(adj, gr))
                            graphToMapping.putIfAbsent(adj, graphToMapping.get(n));
                        break;
                    case EDGE_ASSIGNED_TO:
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
