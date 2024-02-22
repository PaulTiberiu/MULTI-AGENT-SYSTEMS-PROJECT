package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import eu.su.mas.dedaleEtu.mas.behaviours.AckSendBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploBehaviour;
import jade.core.behaviours.FSMBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.behaviours.Behaviour;


public class ExploreFSMAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	private MapRepresentation myMap;
	
	/************************************************
	* 
	* State names
	* 
	************************************************/

	private static final String move = "move";
	private static final String ping = "ping";
	private static final String ackSend = "ackSend";
	private static final String shareMap = "shareMap";

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
		
		List<String> list_agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}

		List<Behaviour> lb=new ArrayList<Behaviour>();
	

		FSMBehaviour fsm = new FSMBehaviour(this);

		/************************************************
		 * 
		 * Define the different states and behaviours
		 * 
		 ************************************************/

		fsm.registerFirstState(new ExploBehaviour(this, myMap), move);
		fsm.registerState(new PingBehaviour(this, list_agentNames), ping);
		fsm.registerState(new AckSendBehaviour(this, list_agentNames), ackSend);
		fsm.registerState(new ShareMapBehaviour(this, myMap, list_agentNames), shareMap);

		/************************************************
		 * 
		 * Register the transitions
		 * 
		 ************************************************/

		fsm.registerTransition(move, move, 0);
		fsm.registerTransition(move, ping, 1);
		fsm.registerTransition(move, ackSend, 2);
		fsm.registerTransition(ping, move, 0);
		fsm.registerTransition(ping, shareMap, 1);
		fsm.registerDefaultTransition(ackSend, shareMap);
		fsm.registerDefaultTransition(shareMap, move);

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

}
