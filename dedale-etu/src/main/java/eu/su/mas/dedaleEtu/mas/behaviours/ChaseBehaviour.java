package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import jade.core.behaviours.SimpleBehaviour;
import java.util.List;
import java.util.Random;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;

public class ChaseBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
	/**
	 * Current knowledge of the agent regarding the environment
	 */
/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param receivers name of the agents to ping
 */
	public ChaseBehaviour(final AbstractDedaleAgent myagent) {
		super(myagent);
	}

@Override
public void action() {

    Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
    System.out.println(this.myAgent.getLocalName()+" -- myCurrentPosition is: "+myPosition);
    if (myPosition!=null){
        //List of observable from the agent's current position
        List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
        System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
        
        boolean isGolem = false;
        gsLocation move = null;
        
        for (Couple<Location,List<Couple<Observation,Integer>>> reachable : lobs){
            // If we detect a golem, we will chase it
            if(!reachable.getRight().isEmpty()){
                isGolem = true;
                move = new gsLocation(reachable.getLeft().getLocationId());
            }
        }


        try {
            this.myAgent.doWait(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!isGolem){
            //Random move from the current position
            Random r = new Random();
            int moveId=1+r.nextInt(lobs.size()-1);

            boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());

            move = (gsLocation) lobs.get(moveId).getLeft();

            while(!moved){
                moveId=1+r.nextInt(lobs.size()-1);
                moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
            }
        }
        else{
            ((AbstractDedaleAgent)this.myAgent).moveTo(move);
        }    

        System.out.println(this.myAgent.getLocalName()+" was at "+myPosition+" and I moved to : "+ move.getLocationId());
        
    }
}

@Override
public boolean done() {
    return true;
}
    
}