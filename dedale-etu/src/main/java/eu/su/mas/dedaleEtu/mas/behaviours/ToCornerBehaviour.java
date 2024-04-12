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
import eu.su.mas.dedaleEtu.mas.knowledge.PathInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.ChaseInfos;

import jade.core.AID;
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
public class ToCornerBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	private int exitValue = 0;
    private List<String> receivers;



/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 */
	public ToCornerBehaviour(final AbstractDedaleAgent myagent, List<String> receivers) {
		super(myagent);
        this.receivers = receivers;
	}

	@Override
	public void action() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        exitValue = 0;
        myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);

        // I know the golem's position because I blocked him
        gsLocation golemPosition = ((ExploreFSMAgent) this.myAgent).getGolemPosition();

        MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("LEAVE"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgRecept=this.myAgent.receive(msgTemplate);

		if(msgRecept!=null){
            // If I receive a message to chase another golem, and I am not adjacent to the golem, I am passing to chaseBehaviour
            exitValue = 1;
            ((ExploreFSMAgent) myAgent).setBlock(false);
            
            Set<String> edges = this.myMap.getSerializableGraph().getEdges(golemPosition.getLocationId());
            List<String> removed = new ArrayList<String>();
            for(String edge : edges){
                removed.add(edge);
                this.myMap.removeNode(edge);
            }
            removed.add(golemPosition.getLocationId());
            this.myMap.removeNode(golemPosition.getLocationId());
            System.out.println("Nodes removed = "+removed);
            return;
        }

        // I need to find the shortest path to a corner
        List<String> small_arity_nodes = this.myMap.getNodesWithSmallestArity();
        int length = Integer.MAX_VALUE;

        System.out.println(myAgent.getLocalName()+" small_arity_nodes = "+small_arity_nodes);

        List<String> shortest_path = null;

        for (String node : small_arity_nodes) {
            // Check if golem is blocked already on a smallest arity node
            if(node.equals(golemPosition.getLocationId())) {
                System.out.println("Golem is already blocked on a smallest arity node");
                // If I am adjacent to the golem, send a message to the others to chase another golem
                List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
                System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);

                // Then, if I am adjacent to the Golem, pass to BlockBehaviour
                for (Couple<Location,List<Couple<Observation,Integer>>> reachable : lobs){
                    String nodeId=reachable.getLeft().getLocationId();
                    if (nodeId.equals(golemPosition.getLocationId())) {
                        exitValue = 2; // Passing to BlockBehaviour
                        System.out.println("-------------------- "+myAgent.getLocalName()+" I blocked the golem on the lowest arity node of the graph (as far as I know) --------------------");
                        return;
                    }
                }
            }

            shortest_path = this.myMap.getShortestPath(golemPosition.getLocationId(), node);
            System.out.println(myAgent.getLocalName() + " shortest_path from "+golemPosition+" to " + node + " = "+shortest_path);
            System.out.println(myAgent.getLocalName() + " length = "+shortest_path.size());
            if(length > shortest_path.size()) {
                length = shortest_path.size();
                ((ExploreFSMAgent) this.myAgent).setPathToCorner(shortest_path);
            }

        }
        
        System.out.println(myAgent.getLocalName() + " finally chose the shortest path = "+ ((ExploreFSMAgent) this.myAgent).getPathToCorner());

        // 1. We need to choose a leader that will choose the shortest path to the corner, then will send it to the others

        //1.2 Receive the message containing the leader
        MessageTemplate msgTemp=MessageTemplate.and(
            MessageTemplate.MatchProtocol("LEADER"),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage recept_leader=this.myAgent.receive(msgTemp);

        String receivedLeader = null;
        if(recept_leader != null){
            try {
                receivedLeader = (String) recept_leader.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
        }

        if(receivedLeader != null && receivedLeader.equals(this.myAgent.getLocalName())) {
            System.out.println(myAgent.getLocalName()+" I am the leader");
            // I am the leader, I need to send to everybody my shortest path to a corner

            List<String> pathToCorner = ((ExploreFSMAgent) this.myAgent).getPathToCorner();

            ACLMessage msg_path = new ACLMessage(ACLMessage.INFORM);
            msg_path.setProtocol("PATH");
            msg_path.setSender(this.myAgent.getAID());

            for (String agentName : receivers) {
                msg_path.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            try {
                PathInfo pathInfo = new PathInfo(pathToCorner);

                msg_path.setContentObject(pathInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((AbstractDedaleAgent)this.myAgent).sendMessage(msg_path);
        }

        // 1.1 Choosing the leader based on the shortest name and sending the ping
        String my_name = this.myAgent.getLocalName();
        String leader = null;
        for (String agentName : receivers) {
            if(agentName.compareTo(my_name) < 0) {
                leader = agentName;
            }
        }

        ACLMessage msg_leader = new ACLMessage(ACLMessage.INFORM);
        msg_leader.setProtocol("LEADER");
        msg_leader.setSender(this.myAgent.getAID());

        for (String agentName : receivers) {
            msg_leader.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        }
        try {			
            msg_leader.setContentObject(leader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ((AbstractDedaleAgent)this.myAgent).sendMessage(msg_leader);

	}

	@Override
	public int onEnd(){
		return exitValue;
	}

	@Override
	public boolean done() {
		return true;
	}

}