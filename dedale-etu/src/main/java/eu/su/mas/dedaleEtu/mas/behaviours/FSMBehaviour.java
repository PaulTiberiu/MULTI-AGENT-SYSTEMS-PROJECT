package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;


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
public class FSMBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	private String myNextNode;

	private List<String> receivers;
    private Integer cmpt = 0;

    private Behaviour pingBehaviour = new PingBehaviour((AbstractDedaleAgent) myAgent,this.receivers);
    private Behaviour exploBehaviour = new ExploCoopOptiBehaviour((AbstractDedaleAgent) myAgent,this.myMap,this.myNextNode,this.receivers);
    private Behaviour ackReceptBehaviour = new ACKReceptBehaviour((AbstractDedaleAgent) myAgent,this.myMap,this.myNextNode,this.receivers);

    private List<Behaviour> lb = new ArrayList<Behaviour>();
/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public FSMBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, String myNextNode,List<String> receivers) {
		super(myagent);
		this.myMap=myMap;
		this.receivers=receivers;	
		this.myNextNode = myNextNode;
	}

	@Override
	public void action() {

        lb.add(new PingReceptBehaviour((AbstractDedaleAgent) myAgent, this.myMap, this.myNextNode, this.receivers));
        lb.add(new InfoReceptBehaviour((AbstractDedaleAgent) myAgent, this.myMap, this.myNextNode));

        if(cmpt == -1){
            lb.remove(pingBehaviour);
            cmpt++;
        }

        else if(cmpt == 0){
            if(lb.contains(ackReceptBehaviour)){
                lb.remove(ackReceptBehaviour);
            }
            lb.add(exploBehaviour);
            cmpt++;
        }
        
        else if(cmpt >= 3){
            lb.remove(exploBehaviour);
            lb.add(pingBehaviour);
            lb.add(ackReceptBehaviour);
            cmpt = -2;
        }

        else{
            cmpt++;
        }

        myAgent.addBehaviour(new startMyBehaviours((AbstractDedaleAgent) myAgent,lb));
	}

    @Override
	public boolean done() {
		return finished;
	}

}