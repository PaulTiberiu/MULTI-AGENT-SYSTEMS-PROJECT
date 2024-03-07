package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.SimpleBehaviour;

public class StopBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
	/**
	 * Current knowledge of the agent regarding the environment
	 */
/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param receivers name of the agents to ping
 */
	public StopBehaviour(final AbstractDedaleAgent myagent) {
		super(myagent);
	}

@Override
public void action() {
    System.out.println(this.myAgent.getName()+" A FINI");
}

@Override
public boolean done() {
    return true;
}

    
}