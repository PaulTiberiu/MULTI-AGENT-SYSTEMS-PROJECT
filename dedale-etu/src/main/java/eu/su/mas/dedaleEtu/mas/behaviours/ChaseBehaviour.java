package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.ChaseInfos;
import jade.core.behaviours.SimpleBehaviour;
import java.util.List;
import java.util.Random;


import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import dataStructures.serializableGraph.SerializableSimpleGraph;
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
    private SerializableSimpleGraph<String, MapAttribute> sg;



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

        //List of observable from the agent's current position
        List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
        System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);
        
        boolean isGolem = false;
        gsLocation move = null;
        
        // a faire : chasse

        // // 1) With our map, we will try to follow the same "direction" as the stench we observed.
        // // If direction changes, we make a random choice to a node containing stench.
        // myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);

        // //sg = myMap.getSerializableGraph();
        // SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sg = myMap.getSerializableGraph();

        // // For all the nodes in the map
        // for (SerializableNode<String, MapAttribute> n: this.sg.getAllNodes()){
        //     // Choose our direction in order to follow the stench
        //     // Search adjacent nodes of the node containing stench
		// }

        for (Couple<Location,List<Couple<Observation,Integer>>> reachable : lobs){
            // If we detect a golem, we will chase it using the given direction node (see point 1))
            if(!reachable.getRight().isEmpty()){
                isGolem = true;
                move = new gsLocation(reachable.getLeft().getLocationId());
            }
        }

        if (!isGolem){  // We don't smell anything

            if (sendersInfos != null){      // Someone smells something and tells me
                for (int i=0; i<sendersInfos.size(); i++){      // More than one smells and tells me ? (TO DO : We have to sum those infos)
                    Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>> infos = sendersInfos.get(i).getRight();
                    String next_pos_ally = infos.getLeft();
                    List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally = infos.getRight();

                    for (Couple<Location,List<Couple<Observation,Integer>>> reachable_from_ally : lobs_ally){
                        if (!reachable_from_ally.getRight().isEmpty()){
                            if (reachable_from_ally.getLeft().getLocationId().compareTo(next_pos_ally) != 0){
                                // My ally smells a stench but doesn't go for it
                                move = new gsLocation(this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), reachable_from_ally.getLeft().getLocationId()).get(0));
                                // So my next move is the 1st node in the shortest path to this stench
                                break;
                            }
                        }
                    }

                    if (move == null){  // There is no other stench apart the one that my ally is going for
                        // So I have to choose an adjacent node of the stench
                        Set<String> edges = this.myMap.getSerializableGraph().getEdges(next_pos_ally);
                        System.out.println("Edges of "+next_pos_ally+" : "+edges);
                        for(String edge : edges){
                            if (edge != lobs_ally.get(0).getLeft().getLocationId()){    // If my ally is not on this node
                                move = new gsLocation(this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), edge).get(0));
                            }
                            else if(move == null){  // If my ally is on this node but I don't have other choice
                                move = new gsLocation(this.myMap.getShortestPath(lobs.get(0).getLeft().getLocationId(), edge).get(0));
                            }
                        }
                    }
                }
                boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(move);
                while(!moved){      // If something is blocking me I'll just wait and retry
                    this.myAgent.doWait(1000);
                }
            }
            else{   // I dont smell anything and nobody is calling me
                //Random move from the current position if no informations
                Random r = new Random();
                int moveId=1+r.nextInt(lobs.size()-1);

                boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());

                move = (gsLocation) lobs.get(moveId).getLeft();

                while(!moved){
                    moveId=1+r.nextInt(lobs.size()-1);
                    moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
                }
            }
        }
        else{   // There is a Stench near us and I already chose my next move, I just have to send my infos

            // if (sendersInfos != null){
            //     for(int i=0; i<sendersInfos.size(); i++){
            //         Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>> infos = sendersInfos.get(i).getRight();
            //         String next_pos_ally = infos.getLeft();
            //         List<Couple<Location,List<Couple<Observation,Integer>>>> lobs_ally = infos.getRight();
                    
            //         if (move.getLocationId() == next_pos_ally || move.getLocationId() == lobs_ally.get(0).getLeft().getLocationId()){
            //             // If my next move is on my ally's next position or its actual position, I have to chose another move
                        
            //         }
            //     }
            // }
            
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

            ((AbstractDedaleAgent)this.myAgent).moveTo(move);   // que se passe t il si il essaye d aller sur le golem ? il va s'arreter et p.e le perdre ? a traiter
        }

        System.out.println(this.myAgent.getLocalName()+" was at "+myPosition+" and I moved to : "+ move.getLocationId());   
    }
}

@Override
public boolean done() {
    return true;
}
    
}