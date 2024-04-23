package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.io.IOException;

import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
//import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;




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
public class BlockBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	//private MapRepresentation myMap;
	private int exitValue = 0;
    private List<String> receivers;
/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 */
	public BlockBehaviour(final AbstractDedaleAgent myagent, List<String> receivers) {
		super(myagent);
        this.receivers = receivers;
	}

	@Override
	public void action() {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        gsLocation golemPosition = ((ExploreFSMAgent) myAgent).getGolemPosition();
        System.out.println("I AM BLOCKED I SEND GOLEMPOS= "+golemPosition);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol("LEAVE");
        msg.setSender(this.myAgent.getAID());

        for (String agentName : receivers) {
            msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        }
        try {			
            // Chase another Golem		
            msg.setContentObject(golemPosition.getLocationId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
        
        try {
            Thread.sleep(9000);    // 10 seconds
        } catch (Exception e) {
            e.printStackTrace();
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