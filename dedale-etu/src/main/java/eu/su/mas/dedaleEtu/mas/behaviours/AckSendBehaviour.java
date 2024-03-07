package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class AckSendBehaviour extends SimpleBehaviour {
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
	public AckSendBehaviour(final AbstractDedaleAgent myagent, List<String> receivers) {
		super(myagent);
		this.receivers = receivers;
	}

@Override
public void action() {
    System.out.println(myAgent.getName()+" J ENVOIE UN ACK");
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