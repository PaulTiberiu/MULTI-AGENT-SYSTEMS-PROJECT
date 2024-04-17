package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import dataStructures.tuple.Couple;

import java.util.HashMap;

import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import eu.su.mas.dedaleEtu.mas.behaviours.AckSendBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.BlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ChaseAckBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploBehaviour;
import jade.core.behaviours.FSMBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareInfosBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SharePartialMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ToCornerBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ChaseBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import eu.su.mas.dedale.env.Location;

public class ExploreFSMAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	private MapRepresentation myMap;
	private Integer iteration = 0;
	private List<String> list_agentNames;
	private List<String> AgentsTosend;
	private HashMap<String, ArrayList<String>> nodesToShare; // key: agent name, value: list of IDs of the nodes to be shared next time we meet this agent
	private HashMap<String, ArrayList<String>> nodesShared;
	private String lastVisitedNode; // Field to store the last visited node
	private gsLocation nextMove;
	private gsLocation golemPosition;
	private boolean block = false;
	private List<String> pathToG;
	private List<String> pathToCorner;
	private List<Couple<String, Couple<String, List<Couple<Location, List<Couple<Observation, Integer>>>>>>> sendersInfos;
	private List<String> movesToCorner = null;
	
	/************************************************
	* 
	* State names
	* 
	************************************************/

	private static final String move = "move";
	private static final String ping = "ping";
	private static final String ackSend = "ackSend";
	private static final String shareMap = "shareMap";
	private static final String chase = "chase";
	private static final String chaseAck = "chaseAck";
	private static final String shareInfos = "shareInfos";
	private static final String toCorner = "toCorner";
	private static final String end = "end";

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();

		this.list_agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
			System.out.println("Moi : +"+this.getLocalName()+" a pour list_agents = "+list_agentNames);
		}

		List<Behaviour> lb=new ArrayList<Behaviour>();
	

		FSMBehaviour fsm = new FSMBehaviour(this);

		/************************************************
		 * 
		 * Define the different states and behaviours
		 * 
		 ************************************************/

		fsm.registerFirstState(new ExploBehaviour(this), move);
		fsm.registerState(new PingBehaviour(this, list_agentNames), ping);
		fsm.registerState(new AckSendBehaviour(this), ackSend);
		fsm.registerState(new SharePartialMapBehaviour(this), shareMap);
		fsm.registerState(new ChaseBehaviour(this, list_agentNames), chase);
		fsm.registerState(new ChaseAckBehaviour(this), chaseAck);
		fsm.registerState(new ShareInfosBehaviour(this), shareInfos);
		fsm.registerState(new ToCornerBehaviour(this, list_agentNames), toCorner);
		fsm.registerState(new BlockBehaviour(this, list_agentNames), end);

		/************************************************
		 * 
		 * Register the transitions
		 * 
		 ************************************************/

		fsm.registerTransition(move, move, 0);
		fsm.registerTransition(move, ping, 1);
		fsm.registerTransition(move, ackSend, 2);
		fsm.registerTransition(move, chase, 3);
		fsm.registerTransition(ping, move, 0);
		fsm.registerTransition(ping, shareMap, 1);
		fsm.registerDefaultTransition(ackSend, shareMap);
		fsm.registerDefaultTransition(shareMap, move);


		fsm.registerTransition(chase, chase, 0);
		fsm.registerTransition(chase, chaseAck, 1);
		fsm.registerTransition(chase, shareInfos, 2);
		fsm.registerTransition(chase, toCorner, 3);
		fsm.registerDefaultTransition(chaseAck, shareInfos);
		fsm.registerDefaultTransition(shareInfos, chase);
		fsm.registerTransition(toCorner, toCorner, 0);
		fsm.registerTransition(toCorner, chase, 1);
		fsm.registerTransition(toCorner, end, 2);
		fsm.registerDefaultTransition(end, end);

		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/

		lb.add(fsm);

		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 ***/
		
		
		addBehaviour(new startMyBehaviours(this,lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}

    public MapRepresentation getMap(boolean isFullMap) {
        if (isFullMap) {
            return this.myMap;
        } else {
            // Return the partial map
            ArrayList<String> nodesToShare = getNodesToShare(getLocalName());
            return this.myMap.getPartialMap(nodesToShare);
        }
    }

	public void setMap(MapRepresentation map) {
		this.myMap = map;
	}

	public void addIteration(){
		iteration++;
	}

	public Integer getIteration() {
        return iteration;
    }

	public ArrayList<String> getNodesToShare(String agentName){

		if (this.nodesToShare == null) {
			this.nodesToShare = new HashMap<String, ArrayList<String>>();
		}
		return this.nodesToShare.get(agentName);
	}

	public ArrayList<String> getSharedNodes(String agentName){

		if (this.nodesShared == null) {
			this.nodesShared = new HashMap<String, ArrayList<String>>();
		}
		return this.nodesShared.get(agentName);
	}

	public void addNodesToShare(ArrayList<String> nodes, String agentExcept){
		List<String> agentNames_except = new ArrayList<String>(list_agentNames);

		if (agentExcept != null){
			agentNames_except.remove(agentExcept);
		}
		
		for (String agentName : agentNames_except) {
			ArrayList<String> toshare_nodes = getNodesToShare(agentName);
			ArrayList<String> shared_nodes = getSharedNodes(agentName);
	
			ArrayList<String> existingNodes = nodesToShare.getOrDefault(agentName, new ArrayList<>());
	
			for (String n : nodes) {
				ArrayList<String> node = new ArrayList<String>();
				node.add(n);

				if (toshare_nodes != null && !toshare_nodes.contains(n) && (shared_nodes == null || !shared_nodes.contains(n))) {
					existingNodes.addAll(node);
				} else if (toshare_nodes == null && (shared_nodes == null || !shared_nodes.contains(n))) {
					existingNodes.addAll(node);
				}
			}
			nodesToShare.put(agentName, existingNodes);
		}
	}
	
    public String getLastVisitedNode() {
        return lastVisitedNode;
    }

    public void setLastVisitedNode(String lastVisitedNode) {
        this.lastVisitedNode = lastVisitedNode;
    }


	public void addNodesShared(String agentName, ArrayList<String> nodes){
		if (nodes == null){
			return;
		}

		ArrayList<String> shared_nodes = getSharedNodes(agentName);

		if (shared_nodes == null) {
			nodesShared.put(agentName, nodes);
			return;
		}

		for (int i = 0; i < nodes.size(); i++) {
			if (!shared_nodes.contains(nodes.get(i))){
				nodesShared.put(agentName, nodes);
			}
		}
	}

	// Method to reset the nodes to be shared with a specific agent
	public void resetNodesToShare(String agentName){
		if (this.nodesToShare != null) {
			this.nodesToShare.remove(agentName);
		}
	}


	// Getter for the ack senders
	public List<String> getAgentsTosend(){
		return this.AgentsTosend;
	}

	// Setter for the ack senders
	public void addAgentsTosend(String agentName){
		if (this.AgentsTosend == null) {
            this.AgentsTosend = new ArrayList<String>();
        }
		if(!this.AgentsTosend.contains(agentName)){
        	this.AgentsTosend.add(agentName);
		}
	}

	public void resetAgentsTosend(){
		this.AgentsTosend.clear();
	}

	public List<String> getAgentsNames(){
		return list_agentNames;
    }

	public void setNextMove(gsLocation nextMove){
		this.nextMove = nextMove;
	}

	public gsLocation getNextMove(){
		return this.nextMove;
	}

	public gsLocation getGolemPosition(){
		return this.golemPosition;
	}

	public void setGolemPosition(gsLocation golemPosition){
		this.golemPosition = golemPosition;
	}

	public List<String> getPathToG(){
		return this.pathToG;
	}

	public void setPathToG(List<String> pathToG){
		this.pathToG = pathToG;
	}

    public boolean isBlock() {
        return this.block;
    }

	public void setBlock(boolean block) {
        this.block = block;
    }
	
	public void setPathToCorner(List<String> shortest_path){
		this.pathToCorner = shortest_path;
	}

	public List<String> getPathToCorner(){
		return this.pathToCorner;
	}

	public List<Couple<String, Couple<String, List<Couple<Location, List<Couple<Observation, Integer>>>>>>> getSendersInfos(){
		return sendersInfos;
	}

	public void setSendersInfos(List<Couple<String,Couple<String,List<Couple<Location,List<Couple<Observation,Integer>>>>>>> sendersInfos){
		this.sendersInfos = sendersInfos;
	}

	public List<String> getMovesToCorner(){
		return this.movesToCorner;
	}

	public void setMovesToCorner(List<String> movesToCorner){
        this.movesToCorner = movesToCorner;
    }

}
