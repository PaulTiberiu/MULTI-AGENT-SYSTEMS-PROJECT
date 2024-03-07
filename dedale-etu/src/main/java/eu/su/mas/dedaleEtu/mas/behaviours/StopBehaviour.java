package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class StopBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
	private boolean finished = false;
    private List<String> receivers;
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