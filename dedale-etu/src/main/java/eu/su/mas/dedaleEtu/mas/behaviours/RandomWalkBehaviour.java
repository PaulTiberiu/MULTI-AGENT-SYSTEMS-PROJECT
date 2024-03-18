package eu.su.mas.dedaleEtu.mas.behaviours;

//import java.io.IOException;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.TickerBehaviour;

/**************************************
 * 
 * 
 * 				BEHAVIOUR RandomWalk : Illustrates how an agent can interact with, and move in, the environment
 * 
 * 
 **************************************/


public class RandomWalkBehaviour extends TickerBehaviour{
	
	/**
	 * When an agent choose to move
	 *  
	 */
	private static final long serialVersionUID = 9088209402507795289L;

	public RandomWalkBehaviour (final AbstractDedaleAgent myagent) {
		super(myagent, 600);
	}

	@Override
	public void onTick() {
		//Example to retrieve the current position
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		System.out.println(this.myAgent.getLocalName()+" -- myCurrentPosition is: "+myPosition);
		if (myPosition!=null){
			//List of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
			System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);

			//Random move from the current position
			Random r= new Random();
			int moveId=1+r.nextInt(lobs.size()-1);

			boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());

			while(!moved){
				moveId=1+r.nextInt(lobs.size()-1);
                moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
			}
			System.out.println(this.myAgent.getLocalName()+" was at "+myPosition+" and I moved to : "+lobs.get(moveId).getLeft());
		}

	}

}