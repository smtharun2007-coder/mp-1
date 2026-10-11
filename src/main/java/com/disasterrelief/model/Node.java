package com.disasterrelief.model;

/**
 * Represents a topological vertex in the disaster relief graph.
 * Total 14 nodes: 10 regions (1-10), 2 depots (11-12), 2 shelters (13-14).
 */
public class Node {
    public enum NodeType {
        REGION,
        DEPOT,
        SHELTER
    }

    private int id;
    private String name;
    private NodeType type;

    public Node() {}

    public Node(int id, String name, NodeType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
