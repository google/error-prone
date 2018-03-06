package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import com.google.auto.value.AutoValue;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;


/**
 * Created by ameya on 1/29/18.
 */
@AutoValue
abstract class Node {

    static Node create(Identification id) {return new AutoValue_Node(id);}

    abstract Identification getId();

}

