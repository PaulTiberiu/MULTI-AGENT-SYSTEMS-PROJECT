package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * @author hc
 *
 */
public class PingBehaviour extends SimpleBehaviour {

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
	public PingBehaviour(final AbstractDedaleAgent myagent, List<String> receivers) {
		super(myagent);
		this.receivers = receivers;
	}

	@Override
	public void action() {
        ACLMessage ping = new ACLMessage(ACLMessage.PROPOSE);
        ping.setProtocol("PING");
        ping.setSender(this.myAgent.getAID());
        for (String agentName : receivers) {
            ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));
        }
        try {					
            ping.setContentObject(this.myAgent.getLocalName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
	}

    @Override
	public boolean done() {
		return finished;
	}

}