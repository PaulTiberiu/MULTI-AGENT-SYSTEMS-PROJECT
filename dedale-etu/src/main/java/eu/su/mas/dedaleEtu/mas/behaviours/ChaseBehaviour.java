package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.ChaseInfos;
import jade.core.behaviours.SimpleBehaviour;
import java.util.List;
import java.util.Random;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import java.io.IOException;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;




public class ChaseBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
    private List<String> receivers;
    private List<Couple<String, Couple<String, List<Couple<Location,List<Couple<Observation,Integer>>>>>>> sendersInfos;

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

        for (Couple<Location,List<Couple<Observation,Integer>>> reachable : lobs){
            // If we detect a golem, we will chase it
            if(!reachable.getRight().isEmpty()){
                isGolem = true;
                move = new gsLocation(reachable.getLeft().getLocationId());
            }
        }

        if (!isGolem){
            //Random move from the current position
            Random r = new Random();
            int moveId=1+r.nextInt(lobs.size()-1);

            boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());

            move = (gsLocation) lobs.get(moveId).getLeft();

            while(!moved){
                moveId=1+r.nextInt(lobs.size()-1);
                moved = ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
            }
        }
        else{

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