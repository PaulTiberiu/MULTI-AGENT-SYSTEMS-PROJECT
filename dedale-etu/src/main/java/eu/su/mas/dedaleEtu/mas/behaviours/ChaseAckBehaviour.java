package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class ChaseAckBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
    private List<String> receivers;
	/**
	 * Current knowledge of the agent regarding the environment
	 */
/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param receivers name of the agents to ping
 */
	public ChaseAckBehaviour(final AbstractDedaleAgent myagent) {
		super(myagent);
	}

@Override
public void action() {
    // ((ExploreFSMAgent)this.myAgent).addIteration();
    receivers = ((ExploreFSMAgent)this.myAgent).getAgentsTosend();
    System.out.println("I am "+myAgent.getName()+" and I am sending an ACK to "+receivers.toString());
    ACLMessage ack = new ACLMessage(ACLMessage.INFORM);
    ack.setProtocol("ACK");
    ack.setSender(this.myAgent.getAID());
    for (String agentName : receivers) {
		ack.addReceiver(new AID(agentName,AID.ISLOCALNAME));
	}
    try {					
        ack.setContentObject(this.myAgent.getLocalName());
    } catch (IOException e) {
        e.printStackTrace();
    }
    ((AbstractDedaleAgent)this.myAgent).sendMessage(ack);
}

@Override
public boolean done() {
    return true;
}

    
}