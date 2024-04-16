package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import dataStructures.serializableGraph.SerializableNode;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
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

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	private String myNextNode;

	// private List<String> list_agentNames;
	private int exitValue = 0;
	private int cmpt = 0;
	private boolean isFullMap = true;

/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploBehaviour(final AbstractDedaleAgent myagent) {
		super(myagent);
	}

	@Override
	public void action() {
		((ExploreFSMAgent)this.myAgent).addIteration();
		this.myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);
		exitValue = 0;
		myNextNode=null;
		if(this.myMap==null) {
			this.myMap = new MapRepresentation(isFullMap);
		}

		try {
			Thread.sleep(10);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("PING"),
			MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		ACLMessage pingRecept=this.myAgent.receive(msgTemplate);

		
		if (pingRecept!=null){
			while(pingRecept!=null){
				((ExploreFSMAgent)this.myAgent).addAgentsTosend(pingRecept.getSender().getLocalName());
				pingRecept=this.myAgent.receive(msgTemplate);
			}
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
				// System.out.println(lobs);

				//1) remove the current node from openlist and add it to closedNodes.
				this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
				ArrayList<String>list = new ArrayList<String>();
				list.add(myPosition.getLocationId());
				((ExploreFSMAgent) this.myAgent).addNodesToShare(list, null);

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
				//if (!this.myMap.hasOpenNode() || (((ExploreFSMAgent) this.myAgent).getIteration() >= 200 && )){
				if (!this.myMap.hasOpenNode() || ((ExploreFSMAgent) this.myAgent).getIteration() >= 300){
					//Explo finished
					System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done !");
					System.out.println(this.myMap.getClosedNodes());
					exitValue = 3;
					chase_mode();
				}else{
					//4) select next move.
					if (myNextNode==null){
						myNextNode=this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
						System.out.println("I am "+this.myAgent.getLocalName()+", my next node was null and now is "+myNextNode+", my position is "+myPosition);
					}
					
					

					msgTemplate=MessageTemplate.and(
						MessageTemplate.MatchProtocol("SHARE-TOPO-POS-ID"),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM));
					ACLMessage infoRecept=this.myAgent.receive(msgTemplate);

					if(infoRecept!=null){
						System.out.println("I am "+this.myAgent.getLocalName()+" and I received a partial map from "+infoRecept.getSender().getLocalName());
						AgentInfo msginfo = null;
						try {
							msginfo = (AgentInfo)infoRecept.getContentObject();
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
						
						SerializableSimpleGraph<String, MapAttribute> receivedMapSender = msginfo.getMap();
						String agentIdSender = msginfo.getAgentId();
						String nextPositionSender = msginfo.getNextPosition();
						this.myMap.mergeMap(receivedMapSender);

						Set<SerializableNode<String, MapAttribute>> all_nodes = msginfo.getMap().getAllNodes();

						ArrayList<String> nodeList = new ArrayList<>();
						for (SerializableNode<String, MapAttribute> node : all_nodes) {
								nodeList.add(node.getNodeId());
						}

						String except = infoRecept.getSender().getLocalName();
						((ExploreFSMAgent) this.myAgent).addNodesToShare(nodeList, except);

						// if (this.myNextNode!=null && nextPositionSender!=null && this.myNextNode.compareTo(nextPositionSender)==0){ // Noeud prioritaire, cas de conflit de next node avec un autre agent
						// 	if (agentIdSender.compareTo(this.myAgent.getLocalName())>0){
						// 		this.myMap.addNode(nextPositionSender, MapAttribute.closed);
						// 		List<String> path = this.myMap.getShortestPathToClosestOpenNode(((AbstractDedaleAgent)this.myAgent).getCurrentPosition().getLocationId());
						// 		if(path!=null && path.size() > 0)
						// 			myNextNode = path.get(0);
						// 		this.myMap.addNode(nextPositionSender, MapAttribute.open);
						// 	}
						// }
					}
					System.out.println("I am "+this.myAgent.getLocalName()+", my position is "+myPosition+" and I will try to move to "+myNextNode);
					cmpt++;
					boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(myNextNode));

					int c = 1;

					while(!moved){
						System.out.println("I am "+this.myAgent.getLocalName()+" and I am searching for another node");
						if (c >= lobs.size()){
							moved = ((AbstractDedaleAgent)this.myAgent).moveTo(myPosition);
							myNextNode = myPosition.getLocationId();
						}
						else{
							Couple<Location, List<Couple<Observation, Integer>>> obs = lobs.get(c);
							if(obs.getLeft().getLocationId().compareTo(myNextNode) != 0 && (obs.getLeft().getLocationId()).compareTo(myPosition.getLocationId()) != 0){
								myNextNode = obs.getLeft().getLocationId();
							}
							moved = ((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(myNextNode));
							c++;
						}
					}
					System.out.println("I am "+this.myAgent.getLocalName()+", my position is "+myPosition+" and I am moving to "+myNextNode);
				}
			}
		}
		((ExploreFSMAgent)this.myAgent).setMap(this.myMap);
	}

	@Override
	public int onEnd(){
		return exitValue;
	}

	@Override
	public boolean done() {
		return true;
	}

	public void chase_mode(){
		System.out.println(this.myAgent.getLocalName()+" ended exploration in "+((ExploreFSMAgent)this.myAgent).getIteration()+" iterations");

		String repertoireActuel = System.getProperty("user.dir");
		try (FileWriter writer = new FileWriter(repertoireActuel+"/resources/nbIterations.txt", true)) {
			writer.write(myAgent.getLocalName()+" : "+((ExploreFSMAgent)this.myAgent).getIteration()+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(this.myAgent.getLocalName()+" starts to chase golem");
	}	

}