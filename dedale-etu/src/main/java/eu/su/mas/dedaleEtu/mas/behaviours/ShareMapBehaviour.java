package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;

import jade.lang.acl.ACLMessage;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentInfo;


/**
 * The agent periodically share its map.
 * It blindly tries to send all its graph to its friend(s)  	
 * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.

 * @author hc
 *
 */
public class ShareMapBehaviour extends SimpleBehaviour {
	
	private MapRepresentation myMap;
	private List<String> receivers;
	//private Integer cmpt = 0;
	private String myNextNode;
	/**
	 * The agent periodically share its map.
	 * It blindly tries to send all its graph to its friend(s)  	
	 * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.

	 * @param a the agent
	 * @param period the periodicity of the behaviour (in ms)
	 * @param mymap (the map to share)
	 * @param receivers the list of agents to send the map to
	 */
	public ShareMapBehaviour(Agent a, List<String> receivers) {
		super(a);
		this.myMap = ((ExploreFSMAgent) a).getMap();
		this.receivers = receivers;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -568863390879327961L;

	@Override
	// MAYBE CHANGE THE ONTICK IN ORDER TO AVOID SENDING MESSAGES ALL THE TICKS // IT CANT BE BAD TO SEND THINGS AT 5 TICKS DISTANCE IN ORDER NOT TO GO TO THE SAME POINT
 	public void action() {
		myNextNode = null;
		this.myMap = ((ExploreFSMAgent) this.myAgent).getMap();
		System.out.println(myAgent.getName()+" JE SHARE MA MAP");
		// ENVOIE DES INFOS : NOM, PROCHAIN NOEUD, CARTE APRES AVOIR RECU UN ACK

		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();
		//1) remove the current node from openlist and add it to closedNodes.
		this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
		//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
		Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
		while(iter.hasNext()){
			Location accessibleNode=iter.next().getLeft();

			System.out.println(accessibleNode+" "+myAgent.getLocalName());//////////////////////////////

			boolean isNewNode=this.myMap.addNewNode(accessibleNode.getLocationId());
			System.out.println(isNewNode);//////////////////////////////////////////////////////////////

			//the node may exist, but not necessarily the edge
			if (myPosition.getLocationId()!=accessibleNode.getLocationId()) {
				this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
				// if (isNewNode) myNextNode=accessibleNode.getLocationId();
			}
		}
		if(myNextNode==null){
			System.out.println(myNextNode);/////////////////////////////////////////////////////////////
			myNextNode=this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
		}
		
		SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();

		AgentInfo agentInfo = new AgentInfo(((AbstractDedaleAgent)this.myAgent).getLocalName(), myNextNode, sg);

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("SHARE-TOPO-POS-ID");
		msg.setSender(this.myAgent.getAID());
		for (String agentName : receivers) {
			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
		try {					
			msg.setContentObject(agentInfo);
		} catch (IOException e) {
			e.printStackTrace();
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		((ExploreFSMAgent)this.myAgent).setMap(this.myMap);
	}

	@Override
	public boolean done() {
		return true;
	}

}