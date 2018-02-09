package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.COLUMN_SEPERATOR;

import com.google.common.base.Objects;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;


/**
 * Created by ameya on 1/29/18.
 */
public class Node {

    public String kind;
    public String name;
    public String owner;
    public String type;
    public boolean isRefactorable;
    public boolean visited;
    // only for var and methods

    public Node(Identification id) {
        this.name = id.getName();
        this.kind = id.getKind();
        this.owner = id.getOwner();
        this.type = id.getType();

    }

    public Node(Identification id, String owner) {
        this.name = id.getName();
        this.kind = id.getKind();
        this.owner = owner;
        this.type = id.getType();

    }

    public Node(String kind, Node n) {
        this.name = n.name;
        this.kind = kind;
        this.owner = n.owner;
        this.type = n.type;

    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node) {
            Node n = (Node) o;
            return this.kind.equals(n.kind) && this.owner.equals(n.owner)
                    && this.name.equals(n.name) && this.type.equals(n.type);
        }
        return false;
    }

    public boolean isSameAs(Identification id) {
        return this.kind.equals(id.getKind()) && this.owner.equals(id.getOwner())
                && this.name.equals(id.getName()) && this.type.equals(id.getType());
    }

    public boolean isSameAs(Node n, String kind) {
        return this.kind.equals(kind) && this.owner.equals(n.owner)
                && this.name.equals(n.name) && this.type.equals(n.type);
    }

    public boolean isSameAs(String owner, Identification id) {
        return this.kind.equals(id.getKind()) && this.owner.equals(owner)
                && this.name.equals(id.getName()) && this.type.equals(id.getType());
    }

    public boolean isSameAs(Identification id, String kind) {
        return this.kind.equals(kind) && this.owner.equals(id.getOwner())
                && this.name.equals(id.getName()) && this.type.equals(id.getType());
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(this.name, this.kind, this.type, this.owner);
    }

    @Override
    public String toString() {
        return this.name + COLUMN_SEPERATOR + kind + COLUMN_SEPERATOR + owner + COLUMN_SEPERATOR + type;
    }
}
