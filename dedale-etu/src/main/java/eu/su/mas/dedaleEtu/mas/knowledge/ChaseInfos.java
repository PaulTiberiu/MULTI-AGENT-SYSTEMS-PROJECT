package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.List;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;


public class ChaseInfos implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentId;
    private String nextPosition;
    private List<Couple<Location, List<Couple<Observation, Integer>>>> golemPos;

    public ChaseInfos(String Name, String nextPosition, List<Couple<Location, List<Couple<Observation, Integer>>>> golemPos) {
        this.agentId = Name;
        this.nextPosition = nextPosition;
        this.golemPos = golemPos;
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

    public List<Couple<Location, List<Couple<Observation, Integer>>>> getGolemPos() {
        return golemPos;
    }

    public void setGolemPos(List<Couple<Location, List<Couple<Observation, Integer>>>> golemPos) {
        this.golemPos = golemPos;
    }
}