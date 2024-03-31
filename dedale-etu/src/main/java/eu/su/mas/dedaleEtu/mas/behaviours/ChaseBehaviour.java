package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation.MapAttribute;
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


public class ChaseBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
    private List<String> receivers;
    private List<Couple<String, Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>>> sendersInfos;
    private MapRepresentation myMap;
    //private SerializableSimpleGraph<String, MapAttribute> sg;
    



	/**
	 * Current knowledge of the agent regarding the environment
	 */
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
    myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);

    Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
    System.out.println(this.myAgent.getLocalName()+" -- myCurrentPosition is: "+myPosition);
    if (myPosition!=null){

        try {
            this.myAgent.doWait(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendersInfos = null;

        MessageTemplate msgTemplate=MessageTemplate.and(
			MessageTemplate.MatchProtocol("INFORMATIONS"),
			MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		List<ACLMessage> infosRecept=this.myAgent.receive(msgTemplate, receivers.size());

		if(infosRecept!=null){
            sendersInfos = new ArrayList<Couple<String, Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>>>();
			for(ACLMessage sender : infosRecept){
				if(sender!=null){
					System.out.println("I am "+myAgent.getName()+" and I received "+sender.getSender().getLocalName()+"'s INFORMATIONS");
                    try {
                        ChaseInfos chaseInfos = (ChaseInfos) sender.getContentObject();
                        sendersInfos.add(new Couple<String, Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>>(chaseInfos.getAgentId(), new Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>(chaseInfos.getNextPosition(), chaseInfos.getGolemPos())));
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
				}
			}
		}

        if(sendersInfos != null){
            System.out.println("I am "+this.myAgent.getLocalName()+" and I received "+sendersInfos.size()+" msg and the informations are: "+sendersInfos);
        }
        else{
            System.out.println("I am "+this.myAgent.getLocalName()+" and I didnt receive any information");
        }
        

        //List of observable from the agent's current position
        List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
        System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
        
        boolean isGolem = false;
        List<gsLocation> stenches = new ArrayList<gsLocation>();
         List<gsLocation> moves = new ArrayList<gsLocation>();
        gsLocation move = null;

        // CHASE

        for (Couple<Location,List<Couple<Observation,Integer>>> reachable : lobs){
            // If we detect a golem, we will chase it using the given direction node (see point 1))
            if(!reachable.getRight().isEmpty()){
                isGolem = true;
                stenches.add((gsLocation) reachable.getLeft());
            }
        }

        if (!isGolem){  // We don't smell anything

            if (sendersInfos != null){      // Someone smells something and tells me
                ArrayList<String> next_allies_pos = new ArrayList<String>();
                ArrayList<List<Couple<Location, List<Couple<Observation, Integer>>>>> lobs_allies = new ArrayList<List<Couple<Location,List<Couple<Observation,Integer>>>>>();
                for (int i=0; i<sendersInfos.size(); i++){      // More than one agent smells and tells me ? I have to sum those informations
                    Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>> infos = sendersInfos.get(i).getRight();
                    String next_pos_ally = infos.getLeft();
                    List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally = infos.getRight();
                    
                    next_allies_pos.add(next_pos_ally);
                    lobs_allies.add(lobs_ally);
                }

                for(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally : lobs_allies){
                    for (Couple<Location,List<Couple<Observation,Integer>>> reachable_from_ally : lobs_ally){
                        if (!reachable_from_ally.getRight().isEmpty()){
                            if(!next_allies_pos.contains(reachable_from_ally.getLeft().getLocationId())){
                                // My ally smells a stench but nobody goes for it
                                List<String> path = this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), reachable_from_ally.getLeft().getLocationId());
                                
                                System.out.println(this.myAgent.getLocalName()+" Path to " +path);
                                if (path!= null && path.size() > 0){
                                    move = new gsLocation(path.get(0));
                                    moves.add(move);
                                    // So my next move is the 1st node in the shortest path to this stench
                                    continue;
                                }
                                System.out.println("I am "+this.myAgent.getLocalName()+" Shortest path = null ! pour aller a "+reachable_from_ally.getLeft().getLocationId());
                            }
                        }
                    }
                }
                for(String next_pos_ally : next_allies_pos){
                    // I have to choose an adjacent node of the stench
                    Set<String> edges = this.myMap.getSerializableGraph().getEdges(next_pos_ally);
                    if (edges!= null){
                        for(String edge : edges){
                            if (edge != myPosition.getLocationId() && edges.size()>1){
                                List<String> path = this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), edge);
                                if(path!= null && path.size() > 0){
                                    move = new gsLocation(path.get(0));
                                    moves.add(move);
                                }
                            }                            
                        }
                    }
                }

                ((ExploreFSMAgent) this.myAgent).setLastVisitedNode(myPosition.getLocationId());

                move = moves.get(0);
                moves.remove(0);

                boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                while(!moved && moves.size()>0){      // If something is blocking me I'll try other good moves
                    gsLocation dead_move = move;
                    move = moves.get(0);
                    moves.remove(0);
                    System.out.println(this.myAgent.getLocalName()+" I can't move to " + dead_move +" I am trying to move to " + move);
                    moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                }

                Random r = new Random();
                while(!moved){  // Else, I'll do a random move
                    move = (gsLocation) lobs.get(1+r.nextInt(lobs.size()-1)).getLeft();
                    System.out.println(this.myAgent.getLocalName()+" I moved but it failed, I am trying a random move to " + move);
                    moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                }
            }
            else{   // I dont smell anything and nobody is calling me
                //Random move from the current position if no informations
                String lastVisitedNode = ((ExploreFSMAgent) this.myAgent).getLastVisitedNode();
                Random r = new Random();
                // int moveId=1+r.nextInt(lobs.size()-1);
                int moveId;

                if (lastVisitedNode == null) {
                    moveId = 1 + r.nextInt(lobs.size() - 1);
                } else {
                    // Select a random move different from the last visited nodeCERTAIN CASES, VERIFY IT");

                    System.out.println("I am "+myAgent.getName() +" and my last visited node is: " + lastVisitedNode);
                    moveId = 1 + r.nextInt(lobs.size() - 1);
                    while (lobs.get(moveId).getLeft().getLocationId() == lastVisitedNode && lobs.size() > 2){
                        moveId = 1 + r.nextInt(lobs.size() - 1);
                        System.out.println("I am "+myAgent.getName() + "and I want to move to my last visited node that is "+lastVisitedNode);
                    }
                }

                move = (gsLocation) lobs.get(moveId).getLeft();

                lastVisitedNode = myPosition.getLocationId();
                ((ExploreFSMAgent) this.myAgent).setLastVisitedNode(lastVisitedNode);

                boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                
                while(!moved){
                    System.out.println("I am "+myAgent.getName() + " and I tried to move to "+move+" but it failed");
                    moveId=1+r.nextInt(lobs.size()-1);
                    move = (gsLocation) lobs.get(moveId).getLeft();
                    moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                }
            }
        }
        else{   // There is a Stench near us and I have to chose my next move and send my infos
            if (sendersInfos != null){  // I received one or more messages, I have to choose my next move
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

                for(int i=stenches.size()-1; i>=0; i--){    // I have to go to the furthest stench because if i go to stenches.get(0) and I am on a stench, I will not move! 
                    gsLocation stench = stenches.get(i);
                    if (!next_allies_pos.contains(stench.getLocationId()) && !allies_pos.contains(stench.getLocationId())){
                        // I smell a stench and my allies are not moving to it and are not on it
                        moves.add(stench); 
                    }
                }

                for(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally : lobs_allies){
                    // I have to watch if I can move to the stench that my allies smelled
                    for (Couple<Location,List<Couple<Observation,Integer>>> reachable_from_ally : lobs_ally){
                        if (!reachable_from_ally.getRight().isEmpty()){
                            if(!next_allies_pos.contains(reachable_from_ally.getLeft().getLocationId()) && !allies_pos.contains(reachable_from_ally.getLeft().getLocationId())){
                                // If my ally smelled a stench and nobody is going to move to it
                                List<String> path = this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), reachable_from_ally.getLeft().getLocationId());
                                
                                if (path!= null && path.size() > 0){
                                    move = new gsLocation(path.get(0));
                                    moves.add(move);
                                }
                            }
                        }
                    }
                }
                for(gsLocation stench: stenches){
                    // I have to choose an adjacent node of my stench
                    Set<String> edges = this.myMap.getSerializableGraph().getEdges(stench.getLocationId());
                    if (edges!=null){
                        for(String edge : edges){
                            if (edge != myPosition.getLocationId() && edges.size()>1){
                                List<String> path = this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), edge);
                                if(path!= null && path.size() > 0){
                                    move = new gsLocation(path.get(0));
                                    moves.add(move);
                                }
                            }                            
                        }
                    }
                }
                moves.add((gsLocation) myPosition);
            }
            else{   // I didnt received any information so I just go to the stench I am smelling
                for (int i = 0; i < stenches.size(); i++){
                    moves.add(stenches.get(stenches.size()-1));
                }   
            }
            
            move = moves.get(0);
            // moves.remove(0);

            System.out.println("I am "+myAgent.getName()+" and I am sending my INFORMATIONS");

            ChaseInfos chaseInfos = new ChaseInfos(this.myAgent.getName(), move.getLocationId(), lobs);

            ACLMessage infos = new ACLMessage(ACLMessage.INFORM);
            infos.setProtocol("INFORMATIONS");
            infos.setSender(this.myAgent.getAID());
            for (String agentName : receivers) {
                infos.addReceiver(new AID(agentName,AID.ISLOCALNAME));
            }
            try {					
                infos.setContentObject(chaseInfos);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((AbstractDedaleAgent)this.myAgent).sendMessage(infos);

            ((ExploreFSMAgent) this.myAgent).setLastVisitedNode(myPosition.getLocationId());

            boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);

            // while(!moved && moves.size()>0){
            //     move = moves.get(0);
            //     moves.remove(0);
            //     moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
            //     System.out.println(this.myAgent.getLocalName()+" Is trying to move but failed, he is going to "+move);
            // }
        }
        
        System.out.println(this.myAgent.getLocalName()+" was at "+myPosition+" and moved to : "+ move.getLocationId());   
    }
}

@Override
public boolean done() {
    return true;
}
    
}