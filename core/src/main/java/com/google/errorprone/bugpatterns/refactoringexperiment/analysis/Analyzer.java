package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.FIELD;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LOCAL_VARIABLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.PARAMETER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.REFACTOR_INFO;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.WRAPPER_CLASSES;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.errorprone.bugpatterns.refactoringexperiment.Constants;
import com.google.errorprone.bugpatterns.refactoringexperiment.ProtoBuffPersist;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.RefactorInfoOuterClass.RefactorInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ameya on 1/26/18.
 */
public class Analyzer {


    public static Predicate<MutableValueGraph<Node, String>> PRE_CONDITION_1 = graph ->
            !graph.edges().stream().filter(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).equals(Relationships.arg_passed))
                    .map(endpt -> endpt.nodeV()).anyMatch(v -> !(v.getKind().equals(Constants.lambdaExpr)|| v.getKind().equals(PARAMETER) || v.getKind().equals(LOCAL_VARIABLE) || v.getKind().equals(FIELD)));

    public static Predicate<MutableValueGraph<Node, String>> PRE_CONDITION_2 = graph ->
            !graph.edges().stream().filter(endpt -> (graph.edgeValue(endpt.nodeU(), endpt.nodeV()).equals(Relationships.assigned_as)||  graph.edgeValue(endpt.nodeU(), endpt.nodeV()).equals(Relationships.initialized_as)))
                    .anyMatch(endpt -> (!endpt.nodeV().getKind().equals(Constants.lambdaExpr)));

    public static Predicate<MutableValueGraph<Node, String>> PRE_CONDITION_3 = graph ->
            !graph.edges().stream().filter(endpt -> graph.edgeValue(endpt.nodeU(), endpt.nodeV()).equals(Relationships.passed_to_non_editable))
                    .anyMatch(endpt -> (endpt.nodeV().getKind().equals(Constants.PARAMETER) || endpt.nodeU().getKind().equals(Constants.PARAMETER)));

    public static void main(String args[]) throws Exception {

        MutableValueGraph<Node, String> graphOfGraphs = CreateGraph.create();
        List<Node> params = graphOfGraphs.nodes().stream().filter(x -> x.getKind().equals(PARAMETER)).collect(Collectors.toList());
        List<MutableValueGraph<Node, String>> subGraphs = new ArrayList<>();
        for (Node n : params) {
            Set<Node> reachables = Graphs.reachableNodes(graphOfGraphs.asGraph(), n);
            n.setVisited(true);
            reachables.stream().forEach(x -> x.setVisited(true));
            subGraphs.add(Graphs.inducedSubgraph(graphOfGraphs, reachables));
        }

        subGraphs.stream().filter(PRE_CONDITION_1).filter(PRE_CONDITION_2).filter(PRE_CONDITION_3).forEach(g -> {
            List<String> refactorTo = g.nodes().stream().filter(x -> x.getKind().equals(PARAMETER) || x.getKind().equals(LOCAL_VARIABLE) || x.getKind().equals(FIELD))
                    .map(x -> x.refactorTo()).collect(Collectors.toList());

//            boolean anyMappingInconsistency = refactorTo.stream().anyMatch(x -> !x.equals(refactorTo.get(0)));
//            if(!anyMappingInconsistency) {

            //Entry<String, List<String>> refactInfo = getRefactorFromInfo(param.getType());
            //String refactorTo = Mapping.getMappedTypeFor(refactInfo.getKey(), refactInfo.getValue().get(0), refactInfo.getValue().get(1));
            System.out.println(refactorTo.get(0));
            g.nodes().stream().map(n -> mapToRefactorObj(n, refactorTo.get(0))).forEach(r -> ProtoBuffPersist.write(r, REFACTOR_INFO));
            g.nodes().stream().forEach(System.out::println);
            System.out.println("**");
//            }else {
//                System.out.println("appingInconsistency");
//            }
        });
    }

    private static RefactorInfo.Builder mapToRefactorObj(Node n, String refactorTo) {
        RefactorInfo.Builder ri = RefactorInfo.newBuilder();
        return ri.setName(n.getName())
                .setKind(n.getKind())
                .setOwner(n.getOwner())
                .setType(n.getType())
                .setRefactorTo(refactorTo);
    }

    //TODO: A better way to extract this information
    public static Entry<String, List<String>> getRefactorFromInfo(String type) {
        String className = type.substring(0, type.indexOf("<"));
        List<String> typeParam = new ArrayList<>();
        Optional<String> elem1isWrapper = WRAPPER_CLASSES.stream().filter(x -> type.indexOf(x) == type.indexOf("<") + 1).findFirst();
        Optional<String> elem2isWrapper = WRAPPER_CLASSES.stream().filter(x -> type.lastIndexOf(x) == type.lastIndexOf(",") +1).findFirst();
        if (elem1isWrapper.isPresent() && elem2isWrapper.isPresent()) {
            typeParam.add(elem1isWrapper.get());
            typeParam.add(elem2isWrapper.get());
        }else if(!elem1isWrapper.isPresent() && elem2isWrapper.isPresent()){
            String sub = type.replace(elem2isWrapper.get(),"").replace(className,"");
            typeParam.add(sub.substring(sub.indexOf("<")+1,sub.indexOf(",")));
            typeParam.add(elem2isWrapper.get());
        }
        else if(elem1isWrapper.isPresent() && !elem2isWrapper.isPresent()){
            typeParam.add(elem1isWrapper.get());
            String sub = type.replace(elem1isWrapper.get(),"").replace(className,"");
            typeParam.add(sub.substring(sub.indexOf(",")+1,sub.indexOf("<")));
            typeParam.add(elem2isWrapper.get());
        }

        return Maps.immutableEntry(className, typeParam);
    }
}

