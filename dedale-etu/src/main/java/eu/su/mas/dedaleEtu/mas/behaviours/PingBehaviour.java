package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
    private List<String> receivers;
	private int exitValue = 0;
    
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
		// ((ExploreFSMAgent)this.myAgent).addIteration();
		exitValue = 0;
		System.out.println("I am "+myAgent.getName()+" and I am sending a PING");
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

		try {
			this.myAgent.doWait(100);
		} catch (Exception e) {
			e.printStackTrace();
		}

		MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("ACK"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage ackRecept=this.myAgent.receive(msgTemplate);

		while(ackRecept!=null){
			System.out.println("I am "+myAgent.getName()+" and I received an ACK from "+ackRecept.getSender().getLocalName());
			exitValue = 1;
			((ExploreFSMAgent)this.myAgent).addAgentsTosend(ackRecept.getSender().getLocalName());
			ackRecept = this.myAgent.receive(msgTemplate);
		}
	}

	public int onEnd(){
		return exitValue;
	}

    @Override
	public boolean done() {
		return true;
	}

}