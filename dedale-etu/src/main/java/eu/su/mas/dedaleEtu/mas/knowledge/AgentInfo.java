package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

import dataStructures.serializableGraph.SerializableSimpleGraph;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;


public class AgentInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentId;
    private String nextPosition;
    private SerializableSimpleGraph<String, MapAttribute> map;

    public AgentInfo(String Name, String nextPosition, SerializableSimpleGraph<String, MapAttribute> myMap) {
        this.agentId = Name;
        this.nextPosition = nextPosition;
        this.map = myMap;
    }

    // Getters and setters
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getNextPosition() {
        return nextPosition;
    }

    public void setNextPosition(String nextPosition) {
        this.nextPosition = nextPosition;
    }

    public SerializableSimpleGraph<String, MapAttribute> getMap() {
        return map;
    }

    public void setMap(SerializableSimpleGraph<String, MapAttribute> map) {
        this.map = map;
    }
}