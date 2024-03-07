package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.FileWriter;
import java.io.IOException;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreFSMAgent;
import jade.core.behaviours.SimpleBehaviour;

public class StopBehaviour extends SimpleBehaviour {
    private static final long serialVersionUID = 8567689731496787661L;
	/**
	 * Current knowledge of the agent regarding the environment
	 */
/**
 * 
 * @param myagent reference to the agent we are adding this behaviour to
 * @param receivers name of the agents to ping
 */
	public StopBehaviour(final AbstractDedaleAgent myagent) {
		super(myagent);
	}

@Override
public void action() {
    System.out.println(this.myAgent.getName()+" ended exploration in "+((ExploreFSMAgent)this.myAgent).getIteration()+" iterations");

    String repertoireActuel = System.getProperty("user.dir");
    try (FileWriter writer = new FileWriter(repertoireActuel+"/resources/nbIterations.txt", true)) {
        // Écrivez la valeur dans le fichier
        writer.write(myAgent.getName()+" : "+((ExploreFSMAgent)this.myAgent).getIteration()+"\n");
    } catch (IOException e) {
        e.printStackTrace();
    }
}

@Override
public boolean done() {
    return true;
}

    
}