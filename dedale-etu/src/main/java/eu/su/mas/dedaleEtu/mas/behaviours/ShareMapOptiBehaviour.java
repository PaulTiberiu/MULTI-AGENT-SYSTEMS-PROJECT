package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import eu.su.mas.dedaleEtu.mas.knowledge.AgentInfo;


/**
 * The agent periodically share its map.
 * It blindly tries to send all its graph to its friend(s)  	
 * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.

 * @author hc
 *
 */
public class ShareMapOptiBehaviour extends TickerBehaviour{
	
	private MapRepresentation myMap;
	private List<String> receivers;
	private Integer cmpt = 0;
	private String myNextNode;

	/**
	 * The agent periodically share its map.
	 * It blindly tries to send all its graph to its friend(s)  	
	 * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.

	 * @param a the agent
	 * @param period the periodicity of the behaviour (in ms)
	 * @param mymap (the map to share)
	 * @param receivers the list of agents to send the map to
	 */
	public ShareMapOptiBehaviour(Agent a, long period,MapRepresentation mymap, String myNextNode, List<String> receivers) {
		super(a, period);
		this.myMap=mymap;
		this.receivers=receivers;	
		this.myNextNode = myNextNode;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -568863390879327961L;

	@Override
	// MAYBE CHANGE THE ONTICK IN ORDER TO AVOID SENDING MESSAGES ALL THE TICKS // IT CANT BE BAD TO SEND THINGS AT 5 TICKS DISTANCE IN ORDER NOT TO GO TO THE SAME POINT
	protected void onTick() {
		
		cmpt++;

		if(cmpt == 3){
			ACLMessage ping = new ACLMessage(ACLMessage.PROPOSE);
			ping.setProtocol("PING");
			ping.setSender(this.myAgent.getAID());
			for (String agentName : receivers) {
				ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));		// ICI PRBLM DE RANGE A FAIRE !!!
			}
			try {					
				ping.setContentObject(this.myAgent.getLocalName());
			} catch (IOException e) {
				e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
			// this.stop = true; NE PAS AVANCER


			//if(ACK){
				// ENVOIE DES INFOS : NOM, PROCHAIN NOEUD, CARTE APRES AVOIR RECU UN ACK
				SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();

				AgentInfo agentInfo = new AgentInfo(((AbstractDedaleAgent)this.myAgent).getLocalName(), this.myNextNode, sg);

				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setProtocol("SHARE-TOPO-POS-ID");
				msg.setSender(this.myAgent.getAID());
				for (String agentName : receivers) {
					msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
				}
				try {					
					msg.setContentObject(agentInfo);
				} catch (IOException e) {
					e.printStackTrace();
				}
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			//}

			cmpt = 0;
		}
	
	}

}