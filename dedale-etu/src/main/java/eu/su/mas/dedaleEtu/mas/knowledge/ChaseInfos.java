package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.List;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;


public class ChaseInfos implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agentId;
    private String nextPosition;
    private gsLocation golemPosition;
    private boolean block;
    private List<Couple<Location, List<Couple<Observation, Integer>>>> obs;

    public ChaseInfos(String Name, String nextPosition, gsLocation golemPosition, boolean block, List<Couple<Location, List<Couple<Observation, Integer>>>> obs) {
        this.agentId = Name;
        this.nextPosition = nextPosition;
        this.golemPosition = golemPosition;
        this.block = block;
        this.obs = obs;
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

    public gsLocation getGolemPosition() {
        return golemPosition;
    }

    public void setGolemPosition(gsLocation golemPosition) {
        this.golemPosition = golemPosition;
    }

    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    public List<Couple<Location, List<Couple<Observation, Integer>>>> getobs() {
        return obs;
    }

    public void setobs(List<Couple<Location, List<Couple<Observation, Integer>>>> obs) {
        this.obs = obs;
    }
}