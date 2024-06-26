package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.ChaseInfos;
import jade.core.behaviours.SimpleBehaviour;
import java.util.List;
import org.graphstream.graph.Node;
import java.util.ArrayList;




import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
//import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import java.io.IOException;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;


public class ShareInfosBehaviour extends SimpleBehaviour{
    private static final long serialVersionUID = 8567689731496787661L;
    private List<String> receivers;
    private MapRepresentation myMap;

    /**
     * 
     * @param myagent reference to the agent we are adding this behaviour to
     * @param receivers name of the agents to ping
     */
    public ShareInfosBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent);
    }

    @Override
    public void action() {
        System.out.println("I am "+myAgent.getName()+" and I am sending my INFORMATIONS");
        myMap = ((ExploreFSMAgent) this.myAgent).getMap(true);

        List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
        List<Couple<Location,List<Couple<Observation,Integer>>>> toRemove = new ArrayList<Couple<Location,List<Couple<Observation,Integer>>>>();
        for(Couple<Location,List<Couple<Observation,Integer>>> o : lobs){
            Node n = this.myMap.getNode(o.getLeft().getLocationId());
            if(n == null){
                // System.out.println("ON REGARDE UN NOEUD SUPPRIME");
                toRemove.add(o);
            }
        }
        lobs.removeAll(toRemove);
    
        gsLocation move = ((ExploreFSMAgent) myAgent).getNextMove();

        gsLocation golemPosition = ((ExploreFSMAgent) myAgent).getGolemPosition();

        boolean block = ((ExploreFSMAgent) this.myAgent).isBlock();

        ChaseInfos chaseInfos = new ChaseInfos(this.myAgent.getName(), move.getLocationId(), golemPosition, block, lobs);

        receivers = ((ExploreFSMAgent) this.myAgent).getAgentsTosend();	// Liste des ACKsenders	
        System.out.println("I am "+myAgent.getLocalName()+" and I am sending my ShareInfos to "+receivers.size()+" people");

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
        ((ExploreFSMAgent) this.myAgent).resetAgentsTosend();
    }

    @Override
    public boolean done() {
        return true;
    }

}