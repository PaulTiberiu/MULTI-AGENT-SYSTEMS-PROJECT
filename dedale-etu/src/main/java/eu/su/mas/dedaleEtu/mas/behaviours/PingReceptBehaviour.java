package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
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
public class PingReceptBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;
	private boolean finished = false;
	private List<String> receivers;
    private MapRepresentation myMap;
	private String myNextNode;
	/**
	 * Current knowledge of the agent regarding the environment
	 */
/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param receivers name of the agents to ping
 */
	public PingReceptBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, String myNextNode, List<String> receivers) {
		super(myagent);
		this.receivers = receivers;
		this.myMap=myMap;	
		this.myNextNode = myNextNode;
	}

	@Override
	public void action() {

		MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("PING"),
			MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		ACLMessage msgReceived=this.myAgent.receive(msgTemplate);

		if(msgReceived!= null){
			// ENVOIE D'UN ACK APRES AVOIR RECU UN PING

			ACLMessage ack = new ACLMessage(ACLMessage.INFORM);
			ack.setProtocol("ACK");
			ack.setSender(this.myAgent.getAID());
			ack.addReceiver(msgReceived.getSender());
			try {					
				ack.setContentObject(this.myAgent.getLocalName());
			} catch (IOException e) {
				e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(ack);

			// ENVOIE DES INFOS : NOM, PROCHAIN NOEUD, CARTE APRES AVOIR RECU UN ACK
			myAgent.addBehaviour(new ShareMapOptiBehaviour((AbstractDedaleAgent) myAgent,this.myMap,this.myNextNode,this.receivers));
		}
	}

    @Override
	public boolean done() {
		return finished;
	}

}