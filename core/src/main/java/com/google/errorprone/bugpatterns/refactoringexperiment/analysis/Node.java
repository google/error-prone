package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;


/**
 * Created by ameya on 1/29/18.
 */
public class Node {

    private Identification id;
    private String refactorTo;
    private boolean visited;

    public Node(Identification id) {
        this.id = id;
    }

    public Node(Identification id, Identification owner) {
        this.id = id.toBuilder().setOwner(owner).build();
    }

    public Node(Identification id, String kind) {
        this.id = id.toBuilder().setKind(kind).build();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node)
            return sameID(this.id, ((Node) o).id);
        return false;
    }

    public boolean sameID(Identification id1, Identification id2) {
        if (!id1.getName().equals(id2.getName()) || !id1.getKind().equals(id2.getKind())
                || !id1.getType().equals(id2.getType()) || id1.hasOwner() ^ id2.hasOwner())
            return false;
        if (id1.hasOwner() && !sameID(id1.getOwner(), id2.getOwner()))
            return false;
        return true;
    }

    public boolean isSameAs(Identification n, String kind) {
        return equals(new Node(n, kind));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private String prettyPrintID(Identification id) {
        StringBuilder x = new StringBuilder();
        if (id.hasName())
            x.append("|" + id.getName() + "|");
        if (id.hasKind())
            x.append(id.getKind() + "|");
        if (id.hasType())
            x.append(id.getType() + "|");
        if (id.hasOwner())
            x.append(prettyPrintID(id.getOwner()) + "|");
        return x.toString();
    }

    @Override
    public String toString() {
        return prettyPrintID(this.id)
                + "|" + refactorTo();
    }

    public String getKind() {
        return id.getKind();
    }


    public String getName() {
        return id.getName();
    }

    public Identification getId() {
        return id;
    }

    public Identification getOwner() {
        return id.getOwner();
    }


    public String getType() {
        return id.getType();
    }


    public String refactorTo() {
        return refactorTo;
    }


    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void setRefactorTo(String s) {
        this.refactorTo = s;
    }
}
