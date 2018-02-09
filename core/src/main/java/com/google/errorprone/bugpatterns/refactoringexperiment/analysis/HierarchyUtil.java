package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ameya on 1/31/18.
 */
public class HierarchyUtil {

    private static List<Set<MethodDeclaration>> methodsGroupedByHierarchy = new ArrayList<>();

    public static List<MethodDeclaration> methodsAffectedHierarchy(Identification id, List<MethodDeclaration> mthdDecls) {
        if (methodsGroupedByHierarchy.isEmpty())
            methodsGroupedByHierarchy = getMthdDclHierarchy(mthdDecls);
        return methodsGroupedByHierarchy.stream().filter(x -> x.stream().anyMatch(y -> y.getId().equals(id)))
                .map(y -> y.stream().filter(m -> !m.getId().equals(id)).collect(Collectors.toList())).findFirst().get();
    }

    //TODO: make this better
    private static List<Set<MethodDeclaration>> getMthdDclHierarchy(List<MethodDeclaration> mthdDecls) {
        List<Set<MethodDeclaration>> lists = new ArrayList<>();
        for (MethodDeclaration m : mthdDecls) {
            if (m.hasSuperMethodIn()) {
                MethodDeclaration superMthd = getSuperMethodId(m, mthdDecls);
                Optional<Set<MethodDeclaration>> l = hierarchyListContains(lists, superMthd, m);
                if (l.isPresent()) {
                    if (l.get().contains(m))
                        l.get().add(superMthd);
                    l.get().add(m);
                } else {
                    Set<MethodDeclaration> set = new HashSet<>();
                    set.add(superMthd);
                    set.add(m);
                    lists.add(set);
                }
            }
        }
        return lists;
    }

    private static Optional<Set<MethodDeclaration>> hierarchyListContains(List<Set<MethodDeclaration>> lists, MethodDeclaration superMthd, MethodDeclaration mthd) {
        return lists.stream().filter(l -> l.contains(superMthd) || l.contains(mthd)).findFirst();
    }

    //TODO Exception handling
    private static MethodDeclaration getSuperMethodId(MethodDeclaration
                                                              md, List<MethodDeclaration> mthDcl) {

        try {
            return mthDcl.stream().filter(m -> m.getId().getName().equals(md.getId().getName())
                    && m.getId().getOwner().equals(md.getSuperMethodIn()) && m.getId().getKind().equals(md.getId().getKind())
                    && m.getId().getType().equals(md.getId().getType())).findFirst().map(Function.identity()).orElseThrow(() -> new Exception());
        } catch (Exception e) {
            System.out.println("Super method not found!");
            return null;
        }
    }


}

