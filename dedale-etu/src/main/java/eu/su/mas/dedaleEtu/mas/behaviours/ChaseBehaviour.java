package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.ChaseInfos;
import jade.core.behaviours.SimpleBehaviour;
import java.util.List;
import java.util.Random;


import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
//import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import java.io.IOException;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;


public class ChaseBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
    private List<String> receivers;
    private List<Couple<String, Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>>> sendersInfos;
    private MapRepresentation myMap;
    private int exitValue = 0;
    private gsLocation golemPosition = null;
    private boolean block = false;
    //private SerializableSimpleGraph<String, MapAttribute> sg;

    /**
     * 
     * @param myagent reference to the agent we are adding this behaviour to
     * @param receivers name of the agents to ping
     */
        public ChaseBehaviour(final AbstractDedaleAgent myagent, List<String> receivers) {
            super(myagent);
            this.receivers = receivers;
        }

    @Override
    public void action() {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
        golemPosition = null;

        myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);
        exitValue = 0;

        Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
        System.out.println(this.myAgent.getLocalName()+" -- myCurrentPosition is: "+myPosition);

        MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("PING"),
			MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		ACLMessage pingRecept=this.myAgent.receive(msgTemplate);

		while(pingRecept!=null){
			((ExploreFSMAgent)this.myAgent).addAgentsTosend(pingRecept.getSender().getLocalName());
			pingRecept=this.myAgent.receive(msgTemplate);
		    exitValue = 1;      // Go to chaseAck
        }

        if (myPosition!=null){

            msgTemplate=MessageTemplate.and(
                MessageTemplate.MatchProtocol("INFORMATIONS"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage infosRecept=this.myAgent.receive(msgTemplate);

            sendersInfos = new ArrayList<Couple<String, Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>>>();
            ArrayList<gsLocation> golemPositions = new ArrayList<gsLocation>();

            while(infosRecept!=null){
                System.out.println("I am "+myAgent.getName()+" and I received "+infosRecept.getSender().getLocalName()+"'s INFORMATIONS");
                try {
                    ChaseInfos chaseInfos = (ChaseInfos) infosRecept.getContentObject();
                    sendersInfos.add(new Couple<String, Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>>(chaseInfos.getAgentId(), new Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>(chaseInfos.getNextPosition(), chaseInfos.getobs())));
                    // LISTE DE GOLEM POSITIONS PUIS TRAITER QUELLE EST LA BONNE VIA ALLIES POS ETC
                    golemPositions.add(chaseInfos.getGolemPosition());
                    if(block == false){
                        block = chaseInfos.isBlock();
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                infosRecept=this.myAgent.receive(msgTemplate);
            }

            if(sendersInfos.size()>0){
                ArrayList<String> allies_pos = new ArrayList<String>();
                for(int i=0; i<sendersInfos.size(); i++){
                    allies_pos.add(sendersInfos.get(i).getRight().getRight().get(0).getLeft().getLocationId());
                }
                for(gsLocation golemP : golemPositions){
                    if(golemP!=null && golemPosition==null && !allies_pos.contains(golemP.getLocationId()) && !((gsLocation) myPosition).equals(golemP)){
                        golemPosition = golemP;
                    }
                }
            }
            

            if(sendersInfos.size()>0){
                System.out.println("I am "+this.myAgent.getLocalName()+" and I received "+sendersInfos.size()+" msg and the informations are: "+sendersInfos +"\ngolem postion = "+golemPosition);
            }
            else{
                System.out.println("I am "+this.myAgent.getLocalName()+" and I didnt receive any information");
            }
            
            if(golemPosition==null){
                golemPosition = ((ExploreFSMAgent) myAgent).getGolemPosition();
                ((ExploreFSMAgent) myAgent).setGolemPosition(golemPosition);
                System.out.println("I remember the position of the golem he is at : "+golemPosition);
            }
            else{
                ((ExploreFSMAgent) myAgent).setGolemPosition(golemPosition);
            }

            if(block == true && ((ExploreFSMAgent) myAgent).isBlock()==true){  // We blocked the golem ! FAUT IL METTRE UN && OU UN || ?
                ((ExploreFSMAgent) myAgent).setBlock(block);
                exitValue = 3;
                return;
            }

            //List of observable from the agent's current position
            List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
            System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
            
            boolean isGolem = false;
            List<gsLocation> stenches = new ArrayList<gsLocation>();
            List<gsLocation> moves = new ArrayList<gsLocation>();
            gsLocation move = null;
            List<String> pathToG = ((ExploreFSMAgent) myAgent).getPathToG();

            Random random = new Random();

            // CHASE

            boolean p = false;

            for (Couple<Location,List<Couple<Observation,Integer>>> reachable : lobs){
                // If we detect a golem, we will chase it using the given direction node (see point 1))
                if(!reachable.getRight().isEmpty()){
                    isGolem = true;
                    stenches.add((gsLocation) reachable.getLeft());
                }

                if(pathToG!=null && reachable.getLeft().equals((Location)(new gsLocation(pathToG.get(0))))){    // If the path is not reachable, then we cant follow it
                    p = true;   // The path is reachable
                }
            }

            if (p == false){    // If the path is not reachable : we dont follow it
                pathToG = null;
                ((ExploreFSMAgent) myAgent).setPathToG(pathToG);
            }

            Collections.shuffle(stenches);

            if (!isGolem){  // I don't smell anything

                if (sendersInfos.size()>0){      // Someone smells something and tells me
                    ArrayList<String> next_allies_pos = new ArrayList<String>();
                    ArrayList<List<Couple<Location, List<Couple<Observation, Integer>>>>> lobs_allies = new ArrayList<List<Couple<Location,List<Couple<Observation,Integer>>>>>();
                    for (int i=0; i<sendersInfos.size(); i++){      // More than one agent smells and tells me ? I have to sum those informations
                        Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>> infos = sendersInfos.get(i).getRight();
                        String next_pos_ally = infos.getLeft();
                        List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally = infos.getRight();
                        
                        next_allies_pos.add(next_pos_ally);
                        lobs_allies.add(lobs_ally);
                    }

                    ArrayList<String> allies_pos = new ArrayList<String>();
                    for(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally : lobs_allies){      // Get position of my allies
                        allies_pos.add(lobs_ally.get(0).getLeft().getLocationId());
                    }

                    if (golemPosition != null && allies_pos.contains(golemPosition.getLocationId())){
                        golemPosition = null;
                    }

                    if (golemPosition != null){

                        Set<String> edges_g = this.myMap.getSerializableGraph().getEdges(golemPosition.getLocationId());
                        List<String> edges_golem = new ArrayList<String>(edges_g);
                        Collections.shuffle(edges_golem);
                        if(edges_golem != null){
                            System.out.println(this.myAgent.getLocalName()+" I dont smell and I know where is the golem so edges_golem = "+edges_golem);
                            for(String edge : edges_golem){
                                if (edge.equals(myPosition.getLocationId()) && edges_golem.size()>1 && !next_allies_pos.contains(edge)){
                                    List<String> path = this.myMap.getShortestPathWithoutPassing(lobs.get(0).getLeft().getLocationId(), edge, allies_pos);
                                    System.out.println(myAgent.getLocalName()+" My path to the golem edge is = "+path);
                                    if(path!= null && path.size() > 0){
                                        move = new gsLocation(path.get(0));
                                        moves.add(move);
                                        path.remove(0);
                                        if (path.size()>0){
                                            ((ExploreFSMAgent) myAgent).setPathToG(path);
                                        }
                                        else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                                    }
                                }                            
                            }
                        }
                    }

                    if (pathToG!=null){
                        move = new gsLocation(pathToG.get(0));
                        moves.add(move);
                        pathToG.remove(0);
                        if (pathToG.size()>0){
                            ((ExploreFSMAgent) myAgent).setPathToG(pathToG);
                        }
                        else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                    }

                    for(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally : lobs_allies){
                        for (Couple<Location,List<Couple<Observation,Integer>>> reachable_from_ally : lobs_ally){
                            if (!reachable_from_ally.getRight().isEmpty()){
                                if(!next_allies_pos.contains(reachable_from_ally.getLeft().getLocationId())){
                                    // My ally smells a stench but nobody goes for it
                                    List<String> path = this.myMap.getShortestPathWithoutPassing(lobs.get(0).getLeft().getLocationId(), reachable_from_ally.getLeft().getLocationId(), allies_pos);

                                    System.out.println(this.myAgent.getLocalName()+" Path to " +path + " to go to "+ reachable_from_ally.getLeft().getLocationId());
                                    if (path!= null && path.size() > 0){
                                        move = new gsLocation(path.get(0));
                                        moves.add(move);
                                        // So my next move is the 1st node in the shortest path to this stench
                                        path.remove(0);
                                        if (path.size()>0){
                                            ((ExploreFSMAgent) myAgent).setPathToG(path);
                                        }
                                        else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                                        continue;
                                    }
                                    System.out.println("I am "+this.myAgent.getLocalName()+" Shortest path = null ! pour aller a "+reachable_from_ally.getLeft().getLocationId());
                                }
                            }
                        }
                    }
                    for(String next_pos_ally : next_allies_pos){
                        // I have to choose an adjacent node of the stench
                        Set<String> e = this.myMap.getSerializableGraph().getEdges(next_pos_ally);
                        List<String> edges = new ArrayList<String>(e);
                        Collections.shuffle(edges);
                        if (edges!= null){
                            for(String edge : edges){
                                if (!edge.equals(myPosition.getLocationId()) && edges.size()>1 && edge != null){
                                    //List<String> path = this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), edge);
                                    System.out.println(myAgent.getLocalName()+" I want to check the path to edge = "+edge);
                                    List<String> path = this.myMap.getShortestPathWithoutPassing(lobs.get(0).getLeft().getLocationId(), edge, allies_pos);
                                    if(path!= null && path.size() > 0){
                                        move = new gsLocation(path.get(0));
                                        moves.add(move);
                                        path.remove(0);
                                        if (path.size()>0){
                                            ((ExploreFSMAgent) myAgent).setPathToG(path);
                                        }
                                        else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                                    }
                                }                            
                            }
                        }
                    }

                    ((ExploreFSMAgent) this.myAgent).setLastVisitedNode(myPosition.getLocationId());

                    if (moves.size()>0){
                        move = moves.get(0);
                        moves.remove(0);
                    }
                    else{
                        move = (gsLocation) myPosition;
                    }                

                    ((ExploreFSMAgent) this.myAgent).setNextMove(move);

                    boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                    while(!moved && moves.size()>0){      // If something is blocking me I'll try other good moves
                        gsLocation dead_move = move;
                        move = moves.get(0);
                        moves.remove(0);
                        ((ExploreFSMAgent) this.myAgent).setNextMove(move);
                        System.out.println(this.myAgent.getLocalName()+" I can't move to " + dead_move +" I am trying to move to " + move);
                        moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                    }

                    Random r = new Random();

                    int c = 1;
                    
                    while(!moved){
                        if (c >= lobs.size()){
                            move = (gsLocation)myPosition;
                            ((ExploreFSMAgent) this.myAgent).setNextMove(move);
							moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
						}
						else{
                            System.out.println(this.myAgent.getLocalName()+" I moved but it failed, I am trying a random move to " + move);
                            int moveId=1+r.nextInt(lobs.size()-1);
                            move = (gsLocation) lobs.get(moveId).getLeft();
                            lobs.remove(moveId);
                            ((ExploreFSMAgent) this.myAgent).setNextMove(move);
                            moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                            c++;
                        }
                    }
                }
                else{   // I dont smell anything and nobody is calling me

                    if(pathToG!=null){
                        move = new gsLocation(pathToG.get(0));
                        moves.add(move);
                        pathToG.remove(0);
                        if (pathToG.size()>0){
                            ((ExploreFSMAgent) myAgent).setPathToG(pathToG);
                        }
                        else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                    }

                    //Random move from the current position if no informations
                    String lastVisitedNode = ((ExploreFSMAgent) this.myAgent).getLastVisitedNode();
                    Random r = new Random();
                    // int moveId=1+r.nextInt(lobs.size()-1);
                    int moveId;

                    if (lastVisitedNode == null) {
                        moveId = 1 + r.nextInt(lobs.size() - 1);
                    } else {
                        // Select a random move different from the last visited node
                        moveId = 1 + r.nextInt(lobs.size() - 1);
                        while (lobs.get(moveId).getLeft().getLocationId().equals(lastVisitedNode) && lobs.size() > 2){
                            moveId = 1 + r.nextInt(lobs.size() - 1);
                        }
                    }

                    move = (gsLocation) lobs.get(moveId).getLeft();
                    moves.add(move);

                    move = moves.get(0);

                    lastVisitedNode = myPosition.getLocationId();
                    ((ExploreFSMAgent) this.myAgent).setLastVisitedNode(lastVisitedNode);
                    ((ExploreFSMAgent) this.myAgent).setNextMove(move);

                    boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                    
                    int c = 1;
                    while(!moved){
                        if (c >= lobs.size()){
                            move = (gsLocation)myPosition;
                            ((ExploreFSMAgent) this.myAgent).setNextMove(move);
							moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
						}
						else{
                            System.out.println("I am "+myAgent.getName() + " and I tried to move to "+move+" but it failed");
                            moveId=1+r.nextInt(lobs.size()-1);
                            move = (gsLocation) lobs.get(moveId).getLeft();
                            lobs.remove(moveId);
                            ((ExploreFSMAgent) this.myAgent).setNextMove(move);
                            moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                            c++;
                        }
                    }
                }
            }
            else{   // There is a Stench near me and I have to chose my next move and send my infos

                System.out.println("I am "+myAgent.getName()+" and I am sending a PING");
                ACLMessage ping = new ACLMessage(ACLMessage.PROPOSE);
                ping.setProtocol("PING");
                ping.setSender(this.myAgent.getAID());
                for (String agentName : receivers) {
                    ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));
                }
                try {					
                    ping.setContentObject(this.myAgent.getLocalName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ((AbstractDedaleAgent)this.myAgent).sendMessage(ping);

                try {
                    Thread.sleep(300);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                msgTemplate=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("ACK"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                ACLMessage ackRecept=this.myAgent.receive(msgTemplate);

                while(ackRecept!=null){
                    System.out.println("I am "+myAgent.getName()+" and I received an ACK from "+ackRecept.getSender().getLocalName());
                    ((ExploreFSMAgent)this.myAgent).addAgentsTosend(ackRecept.getSender().getLocalName());
                    ackRecept = this.myAgent.receive(msgTemplate);
                    if (exitValue != 1){
                        exitValue = 2;  // Go to ShareInfos
                    }
                }


                if (sendersInfos.size()>0){  // I received one or more messages, I have to choose my next move
                    ArrayList<String> next_allies_pos = new ArrayList<String>();
                    ArrayList<List<Couple<Location, List<Couple<Observation, Integer>>>>> lobs_allies = new ArrayList<List<Couple<Location,List<Couple<Observation,Integer>>>>>();
                    for (int i=0; i<sendersInfos.size(); i++){      // More than one agent smells and tells me ? I have to sum those informations
                        Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>> infos = sendersInfos.get(i).getRight();
                        String next_pos_ally = infos.getLeft();
                        List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally = infos.getRight();
                        
                        next_allies_pos.add(next_pos_ally);
                        lobs_allies.add(lobs_ally);
                    }

                    ArrayList<String> allies_pos = new ArrayList<String>();
                    for(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally : lobs_allies){      // Get position of my allies
                        allies_pos.add(lobs_ally.get(0).getLeft().getLocationId());
                    }

                    if (golemPosition != null && allies_pos.contains(golemPosition.getLocationId())){
                        golemPosition = null;
                    }

                    // DO WE HAVE BLOCKED THE GOLEM ?

                    if(golemPosition!=null){
                        Set<String> g_edges = this.myMap.getSerializableGraph().getEdges(golemPosition.getLocationId());
                        if(g_edges!=null){
                            boolean block = false;
                            for (String g_edge : g_edges){
                                if (!g_edge.equals(myPosition.getLocationId()) && !allies_pos.contains(g_edge)){
                                    block = false;
                                    break;
                                }
                                else{
                                    block = true;
                                }
                            }
                            if (block == true){
                                System.out.println("\n\n\n ---------------------- WE BLOCKED THE GOLEM -------------------\n\n\n");
                                try{
                                    Thread.sleep(1000);
                                }catch(InterruptedException e){
                                    e.printStackTrace();
                                }
                                ((ExploreFSMAgent) myAgent).setBlock(block);
                            }
                        }
                    }

                    if (golemPosition != null){
                        System.out.println(myAgent.getName()+" Je connais la position du golem = "+golemPosition);
                        for(Couple<Location, List<Couple<Observation, Integer>>> lobs_position : lobs){
                            if (golemPosition.getLocationId().equals(lobs_position.getLeft().getLocationId())){
                                move = (gsLocation) golemPosition;
                                System.out.println(myAgent.getLocalName()+" The golem is in front of me so I move into him");
                                break;
                            }
                        }

                        if(pathToG!=null && move==null){
                            move = new gsLocation(pathToG.get(0));
                            pathToG.remove(0);
                            if (pathToG.size()>0){
                                ((ExploreFSMAgent) myAgent).setPathToG(pathToG);
                            }
                            else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                        }

                        if(move==null){
                            Set<String> edges_g = this.myMap.getSerializableGraph().getEdges(golemPosition.getLocationId());
                            List<String> edges_golem = new ArrayList<String>(edges_g);
                            Collections.shuffle(edges_golem);
                            System.out.println(myAgent.getLocalName()+" edge : "+edges_golem + " are the edges around the GOLEM : "+golemPosition.getLocationId());
                            if(edges_golem != null){
                                for(String edge : edges_golem){
                                    System.out.println(myAgent.getLocalName()+" edge : "+edge + " is around the GOLEM");
                                    if (!edge.equals(myPosition.getLocationId()) && edges_golem.size()>1 && !next_allies_pos.contains(edge)){
                                        List<String> path = this.myMap.getShortestPathWithoutPassing(lobs.get(0).getLeft().getLocationId(), edge, allies_pos);
                                        System.out.println(myAgent.getLocalName()+" I got a PATH to the GOLEM = "+path);
                                        if(path!= null && path.size() > 0){
                                            move = new gsLocation(path.get(0));
                                            path.remove(0);
                                            if (path.size()>0){
                                                ((ExploreFSMAgent) myAgent).setPathToG(path);
                                            }
                                            else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                                            break;
                                        }
                                    }                            
                                }
                            }
                        }
                    }

                    // Follow the path

                    if(pathToG!=null && move==null){
                        move = new gsLocation(pathToG.get(0));
                        pathToG.remove(0);
                        if (pathToG.size()>0){
                            ((ExploreFSMAgent) myAgent).setPathToG(pathToG);
                        }
                        else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                    }

                    if(move==null){
                        List<gsLocation> stenches_copy = new ArrayList<gsLocation>();
                        stenches_copy.addAll(stenches);
                        for(int i=random.nextInt(0,stenches_copy.size()); stenches_copy.size()>1; i=random.nextInt(0,stenches_copy.size())){    // I have to go to the furthest stench because if i go to stenches.get(0) and I am on a stench, I will not move! 
                            gsLocation stench = stenches_copy.get(i);
                            if (!next_allies_pos.contains(stench.getLocationId()) && !allies_pos.contains(stench.getLocationId()) && !stench.getLocationId().equals(((ExploreFSMAgent) myAgent).getLastVisitedNode())){
                                // I smell a stench and my allies are not moving to it and are not on it
                                System.out.println(myAgent.getLocalName()+" I chosed randomly this stench : "+stench.getLocationId());
                                move = stench;
                                break;
                            }
                            stenches_copy.remove(i);
                        }
                        if(move==null){
                            move = stenches_copy.get(0);        
                        }
                    }
                    
                    if(move==null){
                        for(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally : lobs_allies){
                            // I have to watch if I can move to the stench that my allies smelled
                            for (Couple<Location,List<Couple<Observation,Integer>>> reachable_from_ally : lobs_ally){
                                if (!reachable_from_ally.getRight().isEmpty()){
                                    if(!next_allies_pos.contains(reachable_from_ally.getLeft().getLocationId()) && !allies_pos.contains(reachable_from_ally.getLeft().getLocationId())){
                                        // If my ally smelled a stench and nobody is going to move to it
                                        List<String> path = this.myMap.getShortestPathWithoutPassing(lobs.get(0).getLeft().getLocationId(), reachable_from_ally.getLeft().getLocationId(), allies_pos);

                                        if (path!= null && path.size() > 0){
                                            System.out.println(myAgent.getLocalName()+" found a path = "+path+" to a stench that an ally smelled"+reachable_from_ally.getLeft().getLocationId());
                                            move = new gsLocation(path.get(0));
                                            path.remove(0);
                                            if (path.size()>0){
                                                ((ExploreFSMAgent) myAgent).setPathToG(path);
                                            }
                                            else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if(move==null){
                        for(gsLocation stench: stenches){
                            // I have to choose an adjacent node of my stench
                            Set<String> e = this.myMap.getSerializableGraph().getEdges(stench.getLocationId());
                            List<String> edges = new ArrayList<String>(e);
                            Collections.shuffle(edges);
                            if (edges!=null){
                                for(String edge : edges){
                                    if (!edge.equals(myPosition.getLocationId()) && edges.size()>1){
                                        List<String> path = this.myMap.getShortestPathWithoutPassing(lobs.get(0).getLeft().getLocationId(), edge, allies_pos);
                                        if(path!= null && path.size() > 0){
                                            System.out.println(myAgent.getLocalName()+" found a path = "+path+" to an edge of my stench "+ edge);
                                            move = new gsLocation(path.get(0));
                                            path.remove(0);
                                            if (path.size()>0){
                                                ((ExploreFSMAgent) myAgent).setPathToG(path);
                                            }
                                            else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                                            break;
                                        }
                                    }                            
                                }
                            }
                        }
                    }
                }
                else{   // I didnt received any information so I just go to the stench I am smelling

                    if(pathToG!=null){
                        move = new gsLocation(pathToG.get(0));
                        pathToG.remove(0);
                        if (pathToG.size()>0){
                            ((ExploreFSMAgent) myAgent).setPathToG(pathToG);
                        }
                        else{((ExploreFSMAgent) myAgent).setPathToG(null);}
                    }

                    List<gsLocation> stenches_copy = new ArrayList<gsLocation>();
                    stenches_copy.addAll(stenches);
                    for(int i=random.nextInt(0,stenches_copy.size()); stenches_copy.size()>1; i=random.nextInt(0,stenches_copy.size())){
                        gsLocation stench = stenches_copy.get(i);
                        if(((ExploreFSMAgent)myAgent).getLastVisitedNode() == null || !((ExploreFSMAgent)myAgent).getLastVisitedNode().equals(stench.getLocationId())){
                            System.out.println(myAgent.getLocalName()+" I chosed randomly this stench : "+stench.getLocationId());
                            move = stench;
                        }
                        stenches_copy.remove(i);
                    }
                    if(move == null){
                        move = stenches_copy.get(0);
                    }
                }

                if (move==null){
                    move = (gsLocation) myPosition;
                }

                ((ExploreFSMAgent) this.myAgent).setLastVisitedNode(myPosition.getLocationId());

                ((ExploreFSMAgent) this.myAgent).setNextMove(move);

                boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);   // If I don't move it means it is the golem

                if (!moved){
                    ((ExploreFSMAgent) this.myAgent).setNextMove((gsLocation) myPosition);
                    System.out.println(this.myAgent.getLocalName()+" found the GOLEM !!!!!!!!!!!!!!!! IN FRONT OF ME IN POSITION "+move);
                    ((ExploreFSMAgent) this.myAgent).setGolemPosition(move);
                    move = (gsLocation) myPosition;
                }
                else{
                    ((ExploreFSMAgent) this.myAgent).setGolemPosition(null);
                }
            }

            System.out.println(this.myAgent.getLocalName()+" was at "+myPosition+" and moved to : "+ move.getLocationId());   
        }
    
    }

    @Override
    public boolean done() {
        return true;
    }

	@Override
	public int onEnd(){
		return exitValue;
	}

    
}