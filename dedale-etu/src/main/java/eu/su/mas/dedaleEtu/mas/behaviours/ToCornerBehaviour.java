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

        // if(((ExploreFSMAgent) myAgent).getToCornerTeam()==null){
        check(listG, myPosition);
        // }

        System.out.println(myAgent.getLocalName()+" Ok fine, my team in the ToCorner is "+((ExploreFSMAgent) myAgent).getToCornerTeam());

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
        
        
        if(((ExploreFSMAgent) myAgent).getToCornerTeam()==null) {
            System.out.println("I LEAVE HERE 7");
            return;
        }

        leader = myAgent.getLocalName();
        for (String agentName : ((ExploreFSMAgent) myAgent).getToCornerTeam()) {
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
            // return;
        }

        pathToCorner = ((ExploreFSMAgent) myAgent).getPathToCorner();

        if(pathToCorner!=null && pathToCorner.size()>0){

            // To chose the node to move to, we need to communicate to make sure we will block the golem on the next node
            // To do so, we will take a node at the edge of the next position in the path and we will check if everyone can move to an edge with only 1 move
            // If there is not enough edges to the next node, someone will have to follow us

            boolean searching = true;
            cmpt=0;

            while(searching){
                try {
                    for(int i=0; i<receivers.size(); i++) {
                        myAgent.doWait(500);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(cmpt>=20){
                    exitValue = 1;
                    System.out.println("I AM LEAVING HERE");
                    return; 
                }

                System.out.println(myAgent.getLocalName()+" cmpt = "+cmpt);

                MessageTemplate mess=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("STOP"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage messageRecept=this.myAgent.receive(mess);

                if(messageRecept!=null){
                    searching = false;

                    try {
                        @SuppressWarnings("unchecked")
                        ArrayList<Couple<String, String>> moves = (ArrayList<Couple<String, String>>) messageRecept.getContentObject();
                        System.out.println(myAgent.getLocalName() + " MOVES = " + moves);
                        for(Couple<String, String> m : moves){
                            if(m.getLeft().compareTo(myAgent.getLocalName())==0){
                                ((ExploreFSMAgent) myAgent).setNextMove(new gsLocation(m.getRight()));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }


                MessageTemplate msgT=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("EDGE"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage msgR=this.myAgent.receive(msgT);

                List<String> edges_occupied = new ArrayList<String>();
                List<String> senders = new ArrayList<String>();
                List<Boolean> ends = new ArrayList<Boolean>();
                ArrayList<Couple<String, String>> moves = new ArrayList<Couple<String, String>>();

                int cmptMsg = 0;

                while(msgR!=null){
                    myAgent.doWait(100);
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
                            cmptMsg++;
                             moves.add(new Couple<String, String>(msgR.getSender().getLocalName(), (((PathInfo) msgR.getContentObject()).getPath().get(0))));
                        }
                        msgR=this.myAgent.receive(msgT);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }

                if(cmptMsg<((ExploreFSMAgent)myAgent).getToCornerTeam().size()){
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if(cmptMsg==0){
                    ends.add(false);
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

                System.out.println(myAgent.getLocalName()+" ln = "+ln);

                List<String> reachable = new ArrayList<String>();   // Reachables around the next node in the path by only one move

                for(String n : ln){
                    for (Couple<Location, List<Couple<Observation, Integer>>> obs : lobs){
                        if (obs.getLeft().getLocationId().compareTo(n)==0){ // Reachable by only one move
                            reachable.add(n);
                        }
                    }
                }

                Collections.shuffle(reachable);                

                System.out.println(myAgent.getLocalName()+" reachable nodes around the next in the path = "+reachable+"\npath = "+pathToCorner);
                String move = null;

                if(reachable.size()==0){
                    exitValue = 1;
                    System.out.println("I AM LEAVING HERE 2");
                    return;
                }
                else if(ln.size()>0 && ln.size()<(((ExploreFSMAgent) myAgent).getToCornerTeam().size()+1) && reachable.size()<ln.size()){
                    for(String reach : reachable){
                        if(edges_occupied.contains(reach)){
                            if(move == null){
                                move = myPosition.getLocationId();
                            }
                        }
                        else{
                            move = reach;
                        }
                    }
                }
                else if(reachable.size()==1) {
                    move = reachable.get(0);
                }
                else{
                    for(String reach : reachable){
                        if(edges_occupied.contains(reach)){
                            if(myPosition.getLocationId().compareTo(pathToCorner.get(0))==0){
                                if(reach.compareTo(golemPosition.getLocationId())!=0){
                                    if(pathToCorner.size()>1 && reach.compareTo(pathToCorner.get(1))==0){
                                        move = null;
                                    }
                                    else{
                                        move = reach;
                                    }
                                }
                                else{
                                    if(move == null){
                                        move = reach;
                                    }
                                }
                            }
                            else{
                                if(move == null){
                                    move = reach;
                                }
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

                edges_occupied.remove(move);
                ends.removeAll(List.of(false));

                if(leader.compareTo(myAgent.getLocalName())==0){
                    System.out.println("Ends = "+ends);
                }

                if(done == true && ends.size() >= ln.size()-1 && leader.compareTo(myAgent.getLocalName())==0){  // Only the leader chose when we stop
                    searching = false;

                    ((ExploreFSMAgent) myAgent).setNextMove(new gsLocation(move));

                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    message.setProtocol("STOP");
                    message.setSender(this.myAgent.getAID());

                    for (String agentName : receivers) {
                        message.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                    }
                    try {
                        message.setContentObject(moves);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(myAgent.getLocalName()+" I am sending "+moves+" to "+receivers);
                    ((AbstractDedaleAgent)this.myAgent).sendMessage(message);
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
                System.out.println(myAgent.getLocalName()+" I am sending "+move+" "+done+" to "+receivers);
                ((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
                cmpt++;
            }

            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MessageTemplate msgT=MessageTemplate.and(
                MessageTemplate.MatchProtocol("EDGE"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msgR=this.myAgent.receive(msgT);

            while(msgR!=null){   // I have to delete the message or I will have fake infos for the next input
                msgR=this.myAgent.receive(msgT);
                System.out.println(myAgent.getLocalName()+" Deleting future fakes infos");
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
                    if(moved==false){
                        System.out.println(myAgent.getLocalName()+" J AI PAS REUSSI A ALLER SUR "+move);
                    }

                    MessageTemplate msgTemp=MessageTemplate.and(
                        MessageTemplate.MatchProtocol("MOVE"),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg=this.myAgent.receive(msgTemp);

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
                        MessageTemplate.MatchProtocol("MOVE"),
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
                        msg.setProtocol("MOVE");
                        msg.setSender(this.myAgent.getAID());

                        for (String agentName : receivers) {
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