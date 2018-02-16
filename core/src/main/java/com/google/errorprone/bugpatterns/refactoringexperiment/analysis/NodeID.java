package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Owner;

/**
 * Created by ameya on 2/16/18.
 */
public class NodeID {

    private String kind;
    private String name;
    private NodeID owner;
    private String type;

    public Identification getIdentification(){
        Identification.Builder build = Identification.newBuilder().setName(this.name)
                .setKind(this.kind);
        if(type!=null)
            build.setType(this.type);
        if(owner!=null)
            build.setOwner(Owner.newBuilder().setId(owner.getIdentification()));
        return build.build();
    }


    public NodeID(Identification id) {
        this.name = id.getName();
        this.kind = id.getKind();
        if(id.getType()!=null)
            this.type = id.getType();
        if(id.hasOwner())
            this.owner = new NodeID(id.getOwner().getId());
    }

    public NodeID(Identification id, String kind) {
        this.name = id.getName();
        this.kind = kind;
        if(id.getType()!=null)
            this.type = id.getType();
        if(id.getOwner()!=null)
            this.owner = new NodeID(id.getOwner().getId());
    }

    public NodeID(Identification id, Identification owner) {
        this.name = id.getName();
        this.kind = id.getKind();
        if(id.hasType())
            this.type = id.getType();
        this.owner = new NodeID(owner);
    }

    public NodeID(NodeID id, String kind) {
        this.name = id.getName();
        this.kind = kind;
        if(id.getType()!=null)
            this.type = id.getType();
        if(id.getOwner()!=null)
            this.owner = id.getOwner();
    }


    @Override
    public boolean equals(Object o) {
        if(o instanceof  NodeID){
            NodeID id = (NodeID) o;
            if(this.name.equals(id.getName())   &&  this.kind.equals(id.getKind()))
                if((this.type == null && id.getType() == null )|| this.type.equals(id.getType()))
                    if((this.owner == null && id.getOwner() == null )|| this.owner.equals(id.getOwner()))
                        return true;
        }
        return false;
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

    public NodeID getOwner() {
        return owner;
    }

    public void setOwner(NodeID owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString(){
        return name + "|" + kind + "| "  + type!=null?type:"" + "||" +  owner!=null?owner.toString():"";
    }


    public boolean isSameAs(Identification id) {
        NodeID temp =new NodeID(id);
        return equals(temp);
    }

    public boolean isSameAs(Identification n, String kind) {
        NodeID temp = new NodeID(n,kind);
        return  equals(temp);

    }

    public boolean isSameAs(Identification id, Identification owner) {
        NodeID temp = new NodeID(id,owner);
        return equals(temp);
    }

    public boolean isSameAs(NodeID id, String kind) {
        NodeID temp = new NodeID(id,kind);
        return equals(temp);
    }
}
