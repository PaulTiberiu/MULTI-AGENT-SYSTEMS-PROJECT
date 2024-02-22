package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
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
public class ExploBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	private String myNextNode;

	// private List<String> list_agentNames;
	private int exitValue = 0;
	private int cmpt = 0;

/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap) {
		super(myagent);
		this.myMap=myMap;
		// this.list_agentNames=agentNames;
	}

	@Override
	public void action() {
		exitValue = 0;
		System.out.println("JE BOUGE cmpt = "+cmpt);
		if(this.myMap==null) {
			this.myMap= new MapRepresentation();
		}

		MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("PING"),
			MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		ACLMessage pingRecept=this.myAgent.receive(msgTemplate);

		if(pingRecept!=null){
			exitValue = 2;
		}

		else if(cmpt >= 3){
			exitValue = 1;
			cmpt = 0;
		}

		else{
			//0) Retrieve the current position
			Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

			if (myPosition!=null){
				//List of observable from the agent's current position
				List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

				/**
				 * Just added here to let you see what the agent is doing, otherwise he will be too quick
				 */
				try {
					this.myAgent.doWait(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}

				//1) remove the current node from openlist and add it to closedNodes.
				this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);

				//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
				Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
				while(iter.hasNext()){
					Location accessibleNode=iter.next().getLeft();
					boolean isNewNode=this.myMap.addNewNode(accessibleNode.getLocationId());
					//the node may exist, but not necessarily the edge
					if (myPosition.getLocationId()!=accessibleNode.getLocationId()) {
						this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
						if (isNewNode) myNextNode=accessibleNode.getLocationId();
					}
				}

				//3) while openNodes is not empty, continues.
				if (!this.myMap.hasOpenNode()){
					//Explo finished
					finished=true;
					System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
				}else{
					//4) select next move.
					if (myNextNode==null){
						myNextNode=this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
					}
					

					msgTemplate=MessageTemplate.and(
						MessageTemplate.MatchProtocol("SHARE-TOPO-POS-ID"),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM));
					ACLMessage infoRecept=this.myAgent.receive(msgTemplate);

					if(infoRecept!=null){
						System.out.println("J'AI RECU UNE MAP");
						AgentInfo msginfo = null;
						try {
							msginfo = (AgentInfo)infoRecept.getContentObject();
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

					cmpt++;
					boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(myNextNode));

					while(!moved){
						System.out.println("JE CHERCHE UN AUTRE NOEUD");
						for(Couple<Location, List<Couple<Observation, Integer>>> obs : lobs){
							if(!Objects.equals(obs.getLeft().getLocationId(), myNextNode)){
								if(!Objects.equals(obs.getLeft(), myPosition)){
									myNextNode = obs.getLeft().getLocationId();
									break;
								}
							}
							moved = ((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(myNextNode));
						}
					}
				}
			}
		}
		System.out.println(exitValue+" = EXITVALUE");
	}

	@Override
	public int onEnd(){
		return exitValue;
	}

	@Override
	public boolean done() {
		return finished;
	}

}