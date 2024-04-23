package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.ArrayList;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.PathInfo;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;




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
        exitValue = 0;

        List<String> pathToCorner = ((ExploreFSMAgent) this.myAgent).getPathToCorner();
        this.myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);
        Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
       
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

            try {
                ((ExploreFSMAgent)myAgent).setGolemPosition(new gsLocation((String) msgRecept.getContentObject()));
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            golemPosition = ((ExploreFSMAgent)myAgent).getGolemPosition();

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
            return;
        }

        List<Edge> ed = this.myMap.getNode(golemPosition.getLocationId()).edges().collect(Collectors.toList());
        List<String> listG = new ArrayList<String>();
        for(Edge e : ed){
            if(e.getNode0().getId().compareTo(golemPosition.getLocationId())==0){
                listG.add(e.getNode1().getId());
            }
            else{
                listG.add(e.getNode0().getId());
            }
        }

        listG.remove(myPosition.getLocationId());
        int cmpt = 0;

        check(listG, myPosition);

        List<String> team = ((ExploreFSMAgent) myAgent).getToCornerTeam();

        System.out.println(myAgent.getLocalName()+" Ok fine, my team in the ToCorner is "+team);

        try {
            Thread.sleep(500); // We wait for everyone to finish the treatment of the messages
        } catch (Exception e) {
            e.printStackTrace();
        }

        MessageTemplate msgHere=MessageTemplate.and(
            MessageTemplate.MatchProtocol("HERE"),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msgRH=this.myAgent.receive(msgHere);

        while(msgRH!=null){   // I have to delete the message or I will have fake infos for the next input
            msgRH=this.myAgent.receive(msgHere);
            System.out.println(myAgent.getLocalName()+" Deleting here infos");
        }
        
        
        if(team==null) {
            System.out.println("I LEAVE HERE 7");
            return;
        }

        leader = myAgent.getLocalName();
        for (String agentName : team) {
            if(agentName.compareTo(leader) < 0) {
                leader = agentName;
            }
        }    

        if(myAgent.getLocalName().compareTo(leader)!=0){
            try {
                myAgent.doWait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        MessageTemplate received_path_template=MessageTemplate.and(
			MessageTemplate.MatchProtocol("PATH"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage received_path_leader=this.myAgent.receive(received_path_template);
        
        if(received_path_leader != null && pathToCorner == null){
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
        List<Couple<Location,List<Couple<Observation,Integer>>>> remove = new ArrayList<Couple<Location,List<Couple<Observation,Integer>>>>();
        for(Couple<Location,List<Couple<Observation,Integer>>> o : lobs){
            Node n = this.myMap.getNode(o.getLeft().getLocationId());
            if(n == null){
                System.out.println("ON REGARDE UN NOEUD SUPPRIME");
                remove.add(o);
            }
        }
        lobs.removeAll(remove);
        System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);

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

            if(leader.compareTo(myAgent.getLocalName())==0 && pathToCorner==null){
                List<String> shortest_path = this.myMap.getShortestPath(golemPosition.getLocationId(), node);
                // System.out.println(myAgent.getLocalName() + " shortest_path from "+golemPosition+" to " + node + " = "+shortest_path);
                // System.out.println(myAgent.getLocalName() + " length = "+shortest_path.size());
                if(length > shortest_path.size()) {
                    length = shortest_path.size();
                    ((ExploreFSMAgent) this.myAgent).setPathToCorner(shortest_path);
                }
            }
        }

        if(leader != null && leader.compareTo(this.myAgent.getLocalName())==0 && pathToCorner==null) {
            // I am the leader, I need to send to everybody my shortest path to a corner

            pathToCorner = ((ExploreFSMAgent) this.myAgent).getPathToCorner();

            System.out.println(myAgent.getLocalName()+" I am the leader and the shortest path is: "+pathToCorner);


            ACLMessage msg_path = new ACLMessage(ACLMessage.INFORM);
            msg_path.setProtocol("PATH");
            msg_path.setSender(this.myAgent.getAID());

            for (String agentName : team) {
                msg_path.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            try {
                PathInfo pathInfo = new PathInfo(pathToCorner);

                msg_path.setContentObject(pathInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((AbstractDedaleAgent)this.myAgent).sendMessage(msg_path);
            // return;
        }

        pathToCorner = ((ExploreFSMAgent) myAgent).getPathToCorner();

        if(pathToCorner!=null && pathToCorner.size()>0){

            // To chose the node to move to, we need to communicate to make sure we will block the golem on the next node
            // To do so, we will take a node at the edge of the next position in the path and we will check if everyone can move to an edge with only 1 move
            // If there is not enough edges to the next node, someone will have to follow us

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

            // Leader receive other agents informations

            if (leader.compareTo(this.myAgent.getLocalName())!=0){
                List<String> l = new ArrayList<String>();

                l.add(myPosition.getLocationId());
                l.addAll(reachable);

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setProtocol("EDGE");
                msg.setSender(this.myAgent.getAID());
                msg.addReceiver(new AID(leader, AID.ISLOCALNAME));
                
                try {
                    msg.setContentObject(new PathInfo(l));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(myAgent.getLocalName()+" I am sending my postion "+myPosition+" and my reachables "+reachable+" to "+leader);
                ((AbstractDedaleAgent)this.myAgent).sendMessage(msg);

                MessageTemplate mess=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("MOVE"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage messageRecept=this.myAgent.receive(mess);

                while(messageRecept == null){
                    try {
                        myAgent.doWait(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    messageRecept=this.myAgent.receive(mess);
                }

                // Catch the message and the move sent by the leader
                String move;
                try {
                    move = (String) messageRecept.getContentObject();
                    if(move == null){
                        ((ExploreFSMAgent)myAgent).setNextMove((gsLocation) myPosition);
                    }
                    else{
                        ((ExploreFSMAgent)myAgent).setNextMove(new gsLocation(move));
                    }
                } catch (UnreadableException e1) {
                    e1.printStackTrace();
                }
            }
            else{
                try {
                    for(int i=0; i<team.size(); i++) {
                        myAgent.doWait(1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                List<Couple<String,List<String>>> reachables = new ArrayList<Couple<String,List<String>>>();
                List<Couple<String,String>> positions = new ArrayList<Couple<String,String>>();

                int nbmsg = 0;

                MessageTemplate msgT=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("EDGE"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage msgR=this.myAgent.receive(msgT);

                while(nbmsg<team.size()){
                    try {
                        myAgent.doWait(1000);   
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(msgR!=null){
                        System.out.println(myAgent.getLocalName()+" I got a message from "+msgR.getSender().getLocalName());
                        try {
                            PathInfo pathInfo = (PathInfo) msgR.getContentObject();
                            String pos = pathInfo.getPath().get(0);
                            List<String> reach = pathInfo.getPath();
                            reach.remove(0);
                            reachables.add(new Couple<String, List<String>>(msgR.getSender().getLocalName(), reach));
                            positions.add(new Couple<String, String>(msgR.getSender().getLocalName(), pos));
                            System.out.println(myAgent.getLocalName()+" I got his position = "+pos+" and its reach = "+reach);
                            nbmsg++;
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                    msgR=this.myAgent.receive(msgT);
                }

                // Calcul the moves

                reachables.add(new Couple<String, List<String>>(myAgent.getLocalName(), reachable));
                positions.add(new Couple<String, String>(myAgent.getLocalName(), myPosition.getLocationId()));

                List<Couple<String,String>> moves = new ArrayList<Couple<String,String>>(); // list of (name,nextmove)
                List<Couple<String, String>> toRemove = new ArrayList<Couple<String, String>>();

                List<Couple<String, List<String>>> reachables_ordered = new ArrayList<Couple<String,List<String>>>();

                String agent_in_path = null;

                for(Couple<String, String> c : positions){
                    if(c.getRight().compareTo(pathToCorner.get(0))==0){
                        agent_in_path = c.getLeft();
                    }
                }

                for(Couple<String, List<String>> couple : reachables){
                    if(agent_in_path!=null && couple.getLeft().compareTo(agent_in_path)==0){
                        reachables_ordered.add(0,couple);
                    }
                    else{
                        reachables_ordered.add(couple);
                    }
                }

                for(Couple<String, List<String>> couple : reachables_ordered){  // reachable_ordered start by the agent in the pathToCorner
                    List<String> reach = couple.getRight();
                    String name = couple.getLeft();
                    boolean m = false;

                    String pos = null;
                    for(Couple<String, String> c : positions){
                        if(c.getLeft().compareTo(name)==0){
                            pos = c.getRight();
                        }
                    }

                    List<String> other_reachables = new ArrayList<String>();
                    for(Couple<String, List<String>> pairs : reachables){
                        if(!pairs.equals(couple)){
                            other_reachables.addAll(pairs.getRight());
                        }
                    }

                    if(reach.size()==0){
                        moves.add(new Couple<String, String>(name, pos));
                    }   
                    else if(reach.size()==1){   // I follow the golem
                        moves.add(new Couple<String, String>(name, reach.get(0)));
                    }
                    else if(pos.compareTo(pathToCorner.get(0))==0){   // If he is in the path of the golem he needs to move to the next node in the path
                        for(String r : reach){
                            if(!other_reachables.contains(r)){
                                if(pathToCorner.size()>1 && pathToCorner.get(1).compareTo(r)==0){                                        
                                    Node node = this.myMap.getNode(r);
                                    Node goal = this.myMap.getNode(pathToCorner.get(pathToCorner.size()-1));
                                    if(node.getDegree()>2 || goal.getDegree()>1){    // Not a corridor
                                        moves.add(new Couple<String, String>(name, r));
                                        m = true;
                                    }
                                }
                                else{
                                    moves.add(new Couple<String, String>(name, r));
                                    m = true;
                                }
                            }
                        }
                        if(m==false){   // Wall or Corner
                            for(String r : reach){
                                if(r.compareTo(golemPosition.getLocationId())!=0){
                                    if(pathToCorner.size()>1 && pathToCorner.get(1).compareTo(r)!=0){   // Not on the path of the golem
                                        moves.add(new Couple<String, String>(name, r));
                                        m = true;
                                    }
                                    else if(pathToCorner.size()==1){    // In the corner
                                        moves.add(new Couple<String, String>(name, r));
                                        m = true;
                                    }
                                }
                            }
                        }
                    }
                    else{   // I am an edge blocker
                        for(String r : reach){
                            if(!other_reachables.contains(r)){
                                if(m == true){
                                    for(Couple<String, String> c : moves){
                                        if(c.getLeft().compareTo(name)==0){
                                            toRemove.add(c);
                                        }
                                    }
                                }
                                moves.add(new Couple<String, String>(name, r));
                                m = true;
                            }
                        }
                        if(m == false){
                            for(String re : reach){
                                if(re.compareTo(golemPosition.getLocationId())!=0){
                                    if(m == true){
                                        for(Couple<String, String> c : moves){
                                            if(c.getLeft().compareTo(name)==0){
                                                toRemove.add(c);
                                            }
                                        }
                                    }
                                    moves.add(new Couple<String, String>(name, re));
                                    m = true;
                                }
                                else if(m == false){
                                    moves.add(new Couple<String, String>(name, re));
                                    m = true;
                                }
                            }                        
                        }
                    }
                }

                // Remove the marked duplicates from the moves list
                System.out.println(moves);
                System.out.println("TO REMOVE ="+toRemove.toString());
                moves.removeAll(toRemove);

                // Send the moves

                System.out.println(moves);

                for(String receiver : team){
                    String move = null;
                    for(Couple<String, String> c : moves){
                        System.out.println(c);
                        if(c.getLeft().compareTo(receiver)==0){
                            move = c.getRight();
                        }
                    }

                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    message.setProtocol("MOVE");
                    message.setSender(this.myAgent.getAID());

                    message.addReceiver(new AID(receiver, AID.ISLOCALNAME));
                    
                    try {
                        message.setContentObject(move);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    ((AbstractDedaleAgent)this.myAgent).sendMessage(message);

                    System.out.println(myAgent.getLocalName()+" I send to "+receiver+" his next move = "+move);
                }

                String move = null;
                for(Couple<String, String> c : moves){
                    if(c.getLeft()==myAgent.getLocalName()){
                        move = c.getRight();
                    }
                }

                if(move == null){
                    ((ExploreFSMAgent)myAgent).setNextMove((gsLocation) myPosition);
                }
                else{
                    ((ExploreFSMAgent)myAgent).setNextMove(new gsLocation(move));
                }
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            gsLocation move = ((ExploreFSMAgent) myAgent).getNextMove();

            System.out.println("\n\n\n"+myAgent.getLocalName()+" I will move to "+move.getLocationId()+"\n\n\n");

            boolean moved = false;
            cmpt = 0;

            while(!moved){
                System.out.println(myAgent.getLocalName()+" my position = "+myPosition+" golemPosition = "+golemPosition);

                //If I am on the path of the golem, then I have to move to let him go to the corner
                if (myPosition.getLocationId().equals(pathToCorner.get(0))) {
                    System.out.println(myAgent.getLocalName()+" I am on the path of the golem, I have to move");
                    moved = ((AbstractDedaleAgent) myAgent).moveTo(move);
                    while(moved==false && cmpt >= 25){
                        try {
                            myAgent.doWait(500);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println(myAgent.getLocalName()+" J AI PAS REUSSI A ALLER SUR "+move);
                        moved = ((AbstractDedaleAgent) myAgent).moveTo(move);
                        cmpt++;
                    }

                    MessageTemplate msgTemp=MessageTemplate.and(
                        MessageTemplate.MatchProtocol("GO"),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg=this.myAgent.receive(msgTemp);

                    cmpt=0;
                    while(msg==null){
                        try {
                            myAgent.doWait(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        msg=this.myAgent.receive(msgTemp);

                        if(cmpt >= 25){
                            exitValue = 1;
                            System.out.println("I AM LEAVING HERE 3");
                            return;
                        }
                        cmpt++;
                    }
                }

                // I am not on the path of the golem and I dont go into him, so I'll have to block him, I am an edge blocker
                else if (!move.equals(golemPosition)){
                    System.out.println(myAgent.getLocalName()+" I am an edge blocker, I am waiting for a msg");
                    try {
                        myAgent.doWait(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    MessageTemplate msgTemp=MessageTemplate.and(
                        MessageTemplate.MatchProtocol("GO"),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg=this.myAgent.receive(msgTemp);

                    if(msg!=null){
                        System.out.println(myAgent.getLocalName()+" I received a message I'll move");
                        moved = ((AbstractDedaleAgent) myAgent).moveTo(move);
                        if(moved == false){
                            System.out.println("\n\n"+myAgent.getLocalName()+" Have been really slow "+"\n\n");
                        }
                    }
                    else{
                        if(cmpt>=100){
                            exitValue = 1;
                            System.out.println("I AM LEAVING HERE 4");
                            return;
                        }
                        cmpt++;
                    }
                }

                // I am on the path of the golem and I go into him so I will notify the edges blocker to move !
                else{
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        
                    }
                    System.out.println(myAgent.getLocalName()+" I am trying to move to te golem, if it works, it means he moved");
                    moved = ((AbstractDedaleAgent) myAgent).moveTo(move);
                    if(moved){
                        System.out.println(myAgent.getLocalName()+" I notify the edges blocker to move");
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setProtocol("GO");
                        msg.setSender(this.myAgent.getAID());

                        for (String agentName : team) {
                            msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                        }
                        ((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
                    }
                    else{
                        if(cmpt>=200){
                            exitValue = 1;
                            System.out.println("I AM LEAVING HERE 5");
                            return;
                        }
                        cmpt++;
                    }
                }
            }

            System.out.println(myAgent.getLocalName()+" I moved to "+move);
            ((ExploreFSMAgent)myAgent).setGolemPosition(new gsLocation(pathToCorner.get(0)));
            pathToCorner.remove(0);
            ((ExploreFSMAgent)myAgent).setPathToCorner(pathToCorner);

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                
            }
        }
        else{
            System.out.println(myAgent.getLocalName()+" PathToCorner = "+pathToCorner);
        }
    }

    private void check(List<String> listG, Location myPosition){
        MessageTemplate mTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("HERE"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage mRecept=this.myAgent.receive(mTemplate);

        List<String> team = new ArrayList<String>();
        int cmpt = 0;
 
        while(listG.size()>0){
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(myAgent.getLocalName()+" Is everyone in the ToCornerBehaviour ?");
            
            mRecept=this.myAgent.receive(mTemplate);
            while(mRecept!=null){
                try {
                    if(listG.contains(((Location)mRecept.getContentObject()).getLocationId())){
                        if(listG.remove(((Location)mRecept.getContentObject()).getLocationId())){
                            team.add(mRecept.getSender().getLocalName());
                            System.out.println(myAgent.getLocalName()+" I recept HERE from "+mRecept.getSender().getLocalName());
                        }
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                mRecept=this.myAgent.receive(mTemplate);
            }

            if(cmpt >= 25){
                System.out.println(myAgent.getLocalName()+" I think we are not enough in the ToCornerBehaviour");
                exitValue = 1;
                System.out.println("I AM LEAVING HERE 6");
                ((ExploreFSMAgent)myAgent).setToCornerTeam(null);
                return;
            }

            cmpt++;

            ACLMessage msgHere = new ACLMessage(ACLMessage.INFORM);
            msgHere.setProtocol("HERE");
            msgHere.setSender(this.myAgent.getAID());

            for (String agentName : receivers) {
                msgHere.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            }
            try {
                msgHere.setContentObject(myPosition);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((AbstractDedaleAgent)this.myAgent).sendMessage(msgHere);

            System.out.println(myAgent.getLocalName()+" I send HERE to "+receivers);
        }

        ((ExploreFSMAgent)myAgent).setToCornerTeam(team);
    }

	@Override
	public int onEnd(){

        if(exitValue == 1){
            System.out.println(myAgent.getLocalName()+" I go back to the Chase");
            ((ExploreFSMAgent)myAgent).setToCornerTeam(null);
            ((ExploreFSMAgent)myAgent).setPathToCorner(null);
            ((ExploreFSMAgent)myAgent).setMovesToCorner(null);

            for(int i=0; i<50; i++){   // we delete all the messages from the msg queue
                this.myAgent.receive();
            }
        }

		return exitValue;
	}

	@Override
	public boolean done() {
		return true;
	}

}