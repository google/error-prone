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


    //Here the logic is:
    // 1. get all methods connected by hierarchy in a list of list : methodsGroupedByHierarchy
    //          Iterate over all methods,
    //                  if it has supermethod, get the corresponding method decl of super method
    //                  Check if any list contains either the methd or super method are in any hierarchy list
    //                              IF yes, Add the whichever (mthd/supr mthd) is not present to the list
    //                              ELSE create a new list with mthd and supermthd as members and add it to methodsGroupedByHierarchy
    // 2. Return the list containing the method queried. Remove the queried method from the returned list for convinince.
    private static List<Set<MethodDeclaration>> methodsGroupedByHierarchy = new ArrayList<>();

    public static List<MethodDeclaration> methodsAffectedHierarchy(Identification id, List<MethodDeclaration> mthdDecls) {
        if (methodsGroupedByHierarchy.isEmpty())
            getMthdDclHierarchy(mthdDecls);
        return methodsGroupedByHierarchy.stream().filter(x -> x.stream().anyMatch(y -> y.getId().equals(id)))
                .map(y -> y.stream().filter(m -> !m.getId().equals(id)).collect(Collectors.toList())).findFirst().get();
    }

    //TODO: make this better
    private static void getMthdDclHierarchy(List<MethodDeclaration> mthdDecls) {
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
                    methodsGroupedByHierarchy.add(set);
                }
            }
        }
    }

    private static Optional<Set<MethodDeclaration>> hierarchyListContains(List<Set<MethodDeclaration>> lists, MethodDeclaration superMthd, MethodDeclaration mthd) {
        return lists.stream().filter(l -> l.contains(superMthd) || l.contains(mthd)).findFirst();
    }

    //TODO Exception handling
    private static MethodDeclaration getSuperMethodId(MethodDeclaration md, List<MethodDeclaration> mthDcl) {
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

