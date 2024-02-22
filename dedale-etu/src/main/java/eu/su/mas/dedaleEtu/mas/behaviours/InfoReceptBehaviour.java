package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


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
public class InfoReceptBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;
	private boolean finished = false;
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
	public InfoReceptBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, String myNextNode) {
		super(myagent);
		this.myMap=myMap;	
		this.myNextNode = myNextNode;
	}

	@Override
	public void action() {
		MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("SHARE-TOPO-POS-ID"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived=this.myAgent.receive(msgTemplate);

		if(msgReceived!= null){
			AgentInfo msginfo = null;
			try {
				msginfo = (AgentInfo)msgReceived.getContentObject();
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			SerializableSimpleGraph<String, MapAttribute> receivedMapSender = msginfo.getMap();
			String agentIdSender = msginfo.getAgentId();
			String nextPositionSender = msginfo.getNextPosition(); //WHAT IF WE RECEIVE MULTIPLE? HOW DO WE STOCK IT?
			this.myMap.mergeMap(receivedMapSender);

			if (this.myNextNode == nextPositionSender){ // NOEUD PRIORITAIRE
				if (Integer.parseInt(agentIdSender) < Integer.parseInt(this.myAgent.getLocalName())){
					this.myMap.addNode(nextPositionSender, MapAttribute.closed);
					myNextNode = this.myMap.getShortestPathToClosestOpenNode(((AbstractDedaleAgent)this.myAgent).getCurrentPosition().getLocationId()).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
					this.myMap.addNode(nextPositionSender, MapAttribute.open);
				}
			}
		}
	}

    @Override
	public boolean done() {
		return finished;
	}

}