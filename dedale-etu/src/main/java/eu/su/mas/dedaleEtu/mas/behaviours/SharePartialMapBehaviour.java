package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

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
public class SharePartialMapBehaviour extends SimpleBehaviour {
	
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
	public SharePartialMapBehaviour(Agent a) {
		super(a);
		this.myMap = ((ExploreFSMAgent) a).getMap(true);
		//this.receivers = receivers;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -568863390879327961L;

	@Override
 	public void action() {
		// ((ExploreFSMAgent)this.myAgent).addIteration();
		myNextNode = ((ExploreFSMAgent)this.myAgent).getNextNode();
		this.myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);
		// ENVOIE DES INFOS : NOM, PROCHAIN NOEUD, CARTE APRES AVOIR RECU UN ACK

		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();

		//1) remove the current node from openlist and add it to closedNodes.
		this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
		
		//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
		Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
		while(iter.hasNext()){
			Location accessibleNode=iter.next().getLeft();
			if (myPosition.getLocationId()!=accessibleNode.getLocationId()) {
				this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
			}
		}
		if(myNextNode==null && this.myMap.hasOpenNode()){
			myNextNode=this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
		}
		
		// Plutot un get ack senders si on a plusieurs acks
		receivers = ((ExploreFSMAgent) this.myAgent).getAgentsTosend();	// Liste des ACKsenders	

		System.out.println("I am "+myAgent.getLocalName()+" and I am sending my map to "+receivers.size()+" people");
		for(String receiver:receivers){
			System.out.println("I am "+myAgent.getName()+" and I am sending my map to "+receiver);
			ArrayList<String> nodesToShare = ((ExploreFSMAgent)this.myAgent).getNodesToShare(receiver);
			MapRepresentation partialMap = ((ExploreFSMAgent)this.myAgent).getMap(true).getPartialMap(nodesToShare);
			System.out.println("I am "+myAgent.getName()+" and my NodesToShare to "+receiver+" are : "+nodesToShare);
			
			SerializableSimpleGraph<String, MapAttribute> sg = partialMap.getSerializableGraph();

			((ExploreFSMAgent)this.myAgent).addNodesShared(receiver, nodesToShare);
			((ExploreFSMAgent) this.myAgent).resetNodesToShare(receiver);

			AgentInfo agentInfo = new AgentInfo(((AbstractDedaleAgent)this.myAgent).getLocalName(),myNextNode,sg);
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("SHARE-TOPO-POS-ID");
			msg.setSender(this.myAgent.getAID());

			
			msg.addReceiver(new AID(receiver,AID.ISLOCALNAME));
			try {					
				msg.setContentObject(agentInfo);
			} catch (IOException e) {
				e.printStackTrace();
			}

			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			((ExploreFSMAgent)this.myAgent).setMap(this.myMap);
		}   
		((ExploreFSMAgent)this.myAgent).resetAgentsTosend();
	}

	@Override
	public boolean done() {
		return true;
	}

}