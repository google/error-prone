package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import com.google.common.base.Objects;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;


/**
 * Created by ameya on 1/29/18.
 */
public class Node {

    private NodeID id;
    private String refactorTo;
    private boolean visited;
    // only for var and methods

    public Node(Identification id) {
        this.id = new NodeID(id);
    }

    public Node(Identification id, Identification owner) {
        this.id = new NodeID(id,owner);
    }

    public Node( Node n, String kind) {
        this.id = new NodeID(n.id,kind);
    }

    public Node(Identification id,String kind) {
        this.id = new NodeID(id,kind);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node) {
            Node n = (Node) o;
            return this.id.equals(n.id);
        }
        return false;
    }

    public boolean isSameAs(Identification id) {
        return this.id.isSameAs(id);
    }

    public boolean isSameAs(Identification n, String kind) {
        return this.id.isSameAs(n,kind);
    }

    public boolean isSameAs(Identification id, Identification owner) {
        return this.id.isSameAs(id,owner);
    }

    public boolean isSameAs(Node n, String kind) {
        return this.id.isSameAs(n.id,kind);
    }

//    public boolean isSameAs(Identification id, String kind) {
//        return this.kind.equals(kind) && this.owner.equals(id.getOwner())
//                && this.name.equals(id.getName()) && this.type.equals(id.getType());
//    }


    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public String toString() {
        return this.getName() + "|" + this.getKind() + "| " + this.getType() + "|" +
                this.getOwner().getName()+ "| " + this.getOwner().getType()+ "| " + this.getOwner().getKind();
    }

    public String getKind() {
        return id.getKind();
    }


    public String getName() {
        return id.getName();
    }

    public NodeID getId(){
        return id;
    }
    public NodeID getOwner() {
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

    public void setRefactorTo(String s){
        this.refactorTo = s;
    }
}
