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
public class ToCornerBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	private int exitValue = 0;

/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 */
	public ToCornerBehaviour(final AbstractDedaleAgent myagent) {
		super(myagent);
	}

	@Override
	public void action() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);

        // I know the golem's position because I blocked him
        gsLocation golemPosition = ((ExploreFSMAgent) this.myAgent).getGolemPosition();

        // I need to find the shortest path to a corner
        List<String> small_arity_nodes = this.myMap.getNodesWithSmallestArity();
        int length = Integer.MAX_VALUE;

        System.out.println(myAgent.getLocalName()+" small_arity_nodes = "+small_arity_nodes);

        List<String> shortest_path = null;

        for (String node : small_arity_nodes) {
            shortest_path = this.myMap.getShortestPath(golemPosition.getLocationId(), node);
            System.out.println("shortest_path from "+golemPosition+" to " + node + " = "+shortest_path);
            if(length > shortest_path.size()) {
                length = shortest_path.size();
                ((ExploreFSMAgent) this.myAgent).setPathToCorner(shortest_path);
            }
        }

        System.out.println("Shortest path = "+ shortest_path);

        // Having the path to the corner, I need to push the golem there
        // So, I am going to leave him the possibility to move to the node that belongs to the shortest path


        // Set<String> g_edges = this.myMap.getSerializableGraph().getEdges(golemPosition.getLocationId());
        // if(g_edges!=null){
        //     // Tell my allies to block this edges
        // }
        
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