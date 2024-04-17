package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import java.util.Collections;




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

    private String leader;

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
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        exitValue = 0;

        this.myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);
        
        // I know the golem's position because I blocked him
        gsLocation golemPosition = ((ExploreFSMAgent) this.myAgent).getGolemPosition();

        MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("LEAVE"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgRecept=this.myAgent.receive(msgTemplate);

		if(msgRecept!=null){
            // If I receive a message to chase another golem, and I am not adjacent to the golem, I am passing to chaseBehaviour
            System.out.println("THEY TOLD ME TO LEAVE !!");
            exitValue = 1;
            ((ExploreFSMAgent) myAgent).setBlock(false);
            
            Set<String> edges = this.myMap.getSerializableGraph().getEdges(golemPosition.getLocationId());
            List<String> removed = new ArrayList<String>();
            if(edges!=null && edges.size()>0){
                for(String edge : edges){
                    removed.add(edge);
                    List<Edge> links = this.myMap.getNode(edge).edges().collect(Collectors.toList());
                    for(Edge link : links){
                        this.myMap.removeEdge(link);
                    }
                    this.myMap.removeNode(edge);
                }
            }
            removed.add(golemPosition.getLocationId());
            this.myMap.removeNode(golemPosition.getLocationId());
            System.out.println(myAgent.getLocalName()+" Nodes removed = "+removed);
            ((ExploreFSMAgent) this.myAgent).setGolemPosition(null);
            System.out.println(myAgent.getLocalName()+" ON A BLOQUE LE GOLEM GG JE PASSE EN CHASE POUR CHASSER UN AUTRE\n");
            try {
                Thread.sleep(1000000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        
        leader = myAgent.getLocalName();
        for (String agentName : receivers) {
            if(agentName.compareTo(leader) < 0) {
                leader = agentName;
            }
        }

        MessageTemplate received_path_template=MessageTemplate.and(
			MessageTemplate.MatchProtocol("PATH"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage received_path_leader=this.myAgent.receive(received_path_template);

        if(received_path_leader != null){
            try {
                PathInfo pathInfo = (PathInfo) received_path_leader.getContentObject();
                System.out.println(myAgent.getLocalName()+" received path from leader = "+pathInfo.getPath());
                ((ExploreFSMAgent) this.myAgent).setPathToCorner(pathInfo.getPath());
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
        }

        // I need to find the shortest path to a corner
        List<String> small_arity_nodes = this.myMap.getNodesWithSmallestArity();
        int length = Integer.MAX_VALUE;
        List<Couple<Location,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent)this.myAgent).observe();//myPosition

        System.out.println(myAgent.getLocalName()+" small_arity_nodes = "+small_arity_nodes);

        for (String node : small_arity_nodes) {
            // Check if golem is blocked already on a smallest arity node
            if(node.equals(golemPosition.getLocationId())) {
                System.out.println("Golem is already blocked on a smallest arity node");
                // If I am adjacent to the golem, send a message to the others to chase another golem
                
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

            if(leader.compareTo(myAgent.getLocalName())==0){
                List<String> shortest_path = this.myMap.getShortestPath(golemPosition.getLocationId(), node);
                // System.out.println(myAgent.getLocalName() + " shortest_path from "+golemPosition+" to " + node + " = "+shortest_path);
                // System.out.println(myAgent.getLocalName() + " length = "+shortest_path.size());
                if(length > shortest_path.size()) {
                    length = shortest_path.size();
                    ((ExploreFSMAgent) this.myAgent).setPathToCorner(shortest_path);
                }
            }
        }

        if(leader != null && leader.compareTo(this.myAgent.getLocalName())==0) {
            // I am the leader, I need to send to everybody my shortest path to a corner

            List<String> pathToCorner = ((ExploreFSMAgent) this.myAgent).getPathToCorner();

            System.out.println(myAgent.getLocalName()+" I am the leader and the shortest path is: "+pathToCorner);


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

        List<String> pathToCorner = ((ExploreFSMAgent) myAgent).getPathToCorner();

        if(pathToCorner!=null){

            // To chose the node to move to, we need to communicate to make sure we will block the golem on the next node
            // To do so, we will take a node at the edge of the next position in the path and we will check if everyone can move to an edge with only 1 move
            // If there is not enough edges to the next node, someone will have to follow us

            boolean searching = true;

            while(searching){
                try {
                    for(int i=0; i<receivers.size(); i++) {
                        myAgent.doWait(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                MessageTemplate msgT=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("EDGE"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage msgR=this.myAgent.receive(msgT);

                List<String> edges_occupied = new ArrayList<String>();
                List<String> senders = new ArrayList<String>();
                List<Boolean> ends = new ArrayList<Boolean>();

                while(msgR!=null){
                    try {
                        if(!senders.contains(msgR.getSender().getLocalName())){
                            System.out.println(myAgent.getLocalName()+" I received "+((PathInfo) msgR.getContentObject()).getPath()+" from "+msgR.getSender().getLocalName());
                            edges_occupied.add((((PathInfo) msgR.getContentObject()).getPath().get(0)));
                            if(((PathInfo) msgR.getContentObject()).getPath().get(1).compareTo("true")!=0){
                                ends.add(false);
                                ((ExploreFSMAgent) myAgent).setMovesToCorner(null);
                            }
                            else{
                                ends.add(true);
                            }
                            senders.add(msgR.getSender().getLocalName());
                        }
                        msgR=this.myAgent.receive(msgT);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }

                if(!ends.contains(false) && ((ExploreFSMAgent) myAgent).getMovesToCorner()==null){
                    ((ExploreFSMAgent) myAgent).setMovesToCorner(edges_occupied);
                    System.out.println(myAgent.getLocalName()+" I stock "+edges_occupied);
                }
                else if(!ends.contains(false) && ((ExploreFSMAgent) myAgent).getMovesToCorner()!=null){
                    List<String> movesToCorner = ((ExploreFSMAgent) myAgent).getMovesToCorner();
                    System.out.println(myAgent.getLocalName()+" I added my stock = "+ movesToCorner+" to edges_occupied = "+edges_occupied);
                    for(String m : movesToCorner){
                        if(!edges_occupied.contains(m)){
                            edges_occupied.add(m);
                        }
                    }
                }
                
                Collections.shuffle(edges_occupied);

                System.out.println(myAgent.getLocalName()+" edges_occupied = "+edges_occupied);

                List<Edge> es = this.myMap.getNode(pathToCorner.get(0)).edges().collect(Collectors.toList());
                List<String> ln = new ArrayList<String>();
                for(Edge e : es){
                    if(e.getNode0().getId().compareTo(pathToCorner.get(0))==0){
                        ln.add(e.getNode1().getId());
                    }
                    else{
                        ln.add(e.getNode0().getId());
                    }
                }

                List<String> reachable = new ArrayList<String>();   // Reachables around the next node in the path by only one move

                for(String n : ln){
                    for (Couple<Location, List<Couple<Observation, Integer>>> obs : lobs){
                        if (obs.getLeft().getLocationId().compareTo(n)==0){ // Reachable by only one move
                            reachable.add(n);
                        }
                    }
                }

                System.out.println(myAgent.getLocalName()+" reachable nodes around the next in the path = "+reachable);
                String move = null;

                if(reachable.size()==0){
                    System.out.println("\n\nJE PASSE EN CHASE MODE\n\n");
                }
                else if(reachable.size()==1) {
                    move = reachable.get(0);
                }
                else{
                    for(String reach : reachable){
                        if(edges_occupied.contains(reach)){
                            if(move == null){
                                move = reach;
                            }
                        }
                        else{
                            move = reach;
                        }
                    }
                }

                List<String> temp = new ArrayList<String>();
                boolean error = false; boolean done = false;

                edges_occupied.add(move);

                for(String m : edges_occupied){
                    if(temp.contains(m)){
                        error = true;
                    }
                    else{
                        temp.add(m);
                    }
                }
                
                if(edges_occupied.containsAll(ln) && !error){
                    done = true;
                }

                ends.removeAll(List.of(false));

                if(done == true && ends.size() == ln.size()-1){
                    searching = false;
                    ((ExploreFSMAgent) myAgent).setNextMove(new gsLocation(move));
                }

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setProtocol("EDGE");
                msg.setSender(this.myAgent.getAID());

                for (String agentName : receivers) {
                    msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                }
                try {
                    List<String> l = new ArrayList<String>();
                    l.add(move);
                    if(done==true){
                        l.add("true");
                    }else{
                        l.add("false");
                    }
                    msg.setContentObject(new PathInfo(l));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(myAgent.getLocalName()+" I am sending "+move+" to "+receivers);
                ((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
            }

            // If I am on the path of the golem, then I have to move to let him go to the corner
            // Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
            // String node = pathToCorner.get(0);
            // if (node.equals(myPosition.getLocationId())) {
            //     System.out.println(myAgent.getLocalName()+" I am on the path of the golem, I have to move");
            // }

            System.out.println("\n\n\n"+myAgent.getLocalName()+" I will move to "+((ExploreFSMAgent) myAgent).getNextMove().getLocationId()+"\n\n\n");
            try {
                Thread.sleep(1000000);
            } catch (Exception e) {
                
            }
        }

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