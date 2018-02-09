package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.COLUMN_SEPERATOR;

import com.google.common.base.Objects;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;


/**
 * Created by ameya on 1/29/18.
 */
public class Node {

    private String kind;
    private String name;
    private String owner;
    private String type;
    private boolean isRefactorable;
    private boolean visited;
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRefactorable() {
        return isRefactorable;
    }

    public void setRefactorable(boolean refactorable) {
        isRefactorable = refactorable;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }
}
