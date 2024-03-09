package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

public class NodeSharingManager {

    // Dictionary to store the nodes to share and nodes shared already for each agent
    private HashMap<String, Map.Entry< HashMap<String, ArrayList<String>>,  HashMap<String, ArrayList<String>>>> sharingInfo;

    // Constructor
    public NodeSharingManager() {
        this.sharingInfo = new HashMap<>();
    }

    // Constructor with parameters
    public NodeSharingManager(String agentName, HashMap<String, ArrayList<String>> nodesToShare,HashMap<String, ArrayList<String>> nodesShared) {
        this.sharingInfo = new HashMap<>();
        updateSharingInfo(agentName, nodesToShare, nodesShared);
    }

    // Method to update the sharing information for an agent
    public void updateSharingInfo(String agentName, HashMap<String, ArrayList<String>> nodesToShare, HashMap<String, ArrayList<String>> nodesShared) {
        // Create a new entry for the agent
        Map.Entry<HashMap<String, ArrayList<String>>, HashMap<String, ArrayList<String>>> nodeEntry = new HashMap.SimpleEntry<>(nodesToShare, nodesShared);
        sharingInfo.put(agentName, nodeEntry);
    }

    // Method to get the nodes shared already for a specific agent
    public HashMap<String, ArrayList<String>> getSharedNodes(String agentName) {
        Map.Entry<HashMap<String, ArrayList<String>>, HashMap<String, ArrayList<String>>> nodeEntry = sharingInfo.get(agentName);
        if (nodeEntry != null) {
            // Return the nodes shared already for the agent
            return nodeEntry.getValue();
        } else {
            return new HashMap<>();
        }
    }

    // Method to get the nodes to share for a specific agent
    public HashMap<String, ArrayList<String>> getNodesToShare(String agentName) {
        Map.Entry<HashMap<String, ArrayList<String>>, HashMap<String, ArrayList<String>>> nodeEntry = sharingInfo.get(agentName);
        if (nodeEntry != null) {
            // Return the nodes to share for the agent
            return nodeEntry.getKey();
        } else {
            return new HashMap<>();
        }
    }
}