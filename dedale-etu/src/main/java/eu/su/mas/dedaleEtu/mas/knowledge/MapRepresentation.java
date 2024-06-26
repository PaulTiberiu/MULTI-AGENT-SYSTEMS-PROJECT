package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
//import java.util.stream.Stream;
import java.util.stream.Stream;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;


/**
 * This simple topology representation only deals with the graph, not its content.</br>
 * The knowledge representation is not well written (at all), it is just given as a minimal example.</br>
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 * 
 * @author hc
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 * @author hc
	 *
	 */

	public enum MapAttribute {	
		agent,open,closed;

	}

	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle= "node {"+"fill-color: black;"+" size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.agent {"+"fill-color: forestgreen;"+"}";
	private String nodeStyle_agent = "node.open {"+"fill-color: blue;"+"}";
	private String nodeStyle=defaultNodeStyle+nodeStyle_agent+nodeStyle_open;

	private Graph g; //data structure non serializable
	private Viewer viewer; //ref to the display,  non serializable
	private Integer nbEdges;//used to generate the edges ids

	private SerializableSimpleGraph<String, MapAttribute> sg;//used as a temporary dataStructure during migration


	public MapRepresentation(boolean isFullMap) {
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);

		if(isFullMap){
			// Platform.runLater(() -> {
			// 	openGui();
			// });
		}
		//this.viewer = this.g.display();
		this.nbEdges=0;
	}

	/**
	 * Add or replace a node and its attribute 
	 * @param id unique identifier of the node
	 * @param mapAttribute attribute to process
	 */
	public synchronized void addNode(String id,MapAttribute mapAttribute){
		Node n;
		if (this.g.getNode(id)==null){
			n=this.g.addNode(id);
		}else{
			n=this.g.getNode(id);
		}
		n.clearAttributes();
		n.setAttribute("ui.class", mapAttribute.toString());
		n.setAttribute("ui.label",id);
	}

	/**
	 * Add a node to the graph. Do nothing if the node already exists.
	 * If new, it is labeled as open (non-visited)
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized boolean addNewNode(String id) {
		if (this.g.getNode(id)==null){
			addNode(id,MapAttribute.open);
			return true;
		}
		return false;
	}

	/**
	 * Add an undirect edge if not already existing.
	 * @param idNode1 unique identifier of node1
	 * @param idNode2 unique identifier of node2
	 */
	public synchronized void addEdge(String idNode1,String idNode2){
		this.nbEdges++;
		try {
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		}catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		}catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch(ElementNotFoundException e3){

		}
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
	 * 
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow, null if the targeted node is not currently reachable
	 */
	public synchronized List<String> getShortestPath(String idFrom,String idTo){
		List<String> shortestPath=new ArrayList<String>();
		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		List<Node> path=dijkstra.getPath(g.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		if (shortestPath.isEmpty()) {//The openNode is not currently reachable
			return null;
		}else {
			shortestPath.remove(0);//remove the current position
		}
		return shortestPath;
	}
	
	public synchronized List<String> getShortestPathWithoutPassing(String idFrom, String idTo, List<String> nodesToAvoid) {
		List<String> shortestPath = new ArrayList<>();

		if(idFrom == null || idTo == null || g.getNode(idFrom) == null || g.getNode(idTo) == null){
			System.out.println("\n\nERREUR ERREUR ERREUR ERREUR ERREUR ERREUR ERREUR ERREUR ERREUR IDFROM = "+idFrom+" IDTO = "+idTo+" g.getNode(idFrom) = "+g.getNode(idFrom)+" g.getNode(idTo) = "+g.getNode(idTo)+"\n\n");
			return null;
		}
	
		if(nodesToAvoid.contains(idFrom)){
			nodesToAvoid.remove(idFrom);
		}
		if(nodesToAvoid.contains(idTo)){
			nodesToAvoid.remove(idTo);
		}

		Stream<Edge> edges_stream = null;
		List<Couple<String, Couple<Node, Node>>> edges = new ArrayList<Couple<String, Couple<Node, Node>>>(); 
		List<Node> removedNodes = new ArrayList<>();

		// Remove nodes to avoid temporarily
		if (nodesToAvoid.size()>0){
			for (String nodeId : nodesToAvoid) {
				Node n = g.getNode(nodeId);
				if(n != null && !n.getId().equals(idFrom) && !n.getId().equals(idTo)){
					edges_stream = n.edges();
					List<Edge> edges_list = edges_stream.collect(Collectors.toList());
					if (edges_list.size()>0){
						for(Edge edge : edges_list) {
							Edge e = g.getEdge(edge.getId());
							if (e!=null) {
								edges.add(new Couple<String, Couple<Node, Node>>(edge.getId(), new Couple<Node, Node>(edge.getNode0(), edge.getNode1())));
								g.removeEdge(e);
							}
						}
						removedNodes.add(n);
						g.removeNode(n);
					}
				}
			}
		}
	
		Dijkstra dijkstra = new Dijkstra();
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();
		List<Node> path = dijkstra.getPath(g.getNode(idTo)).getNodePath();
		Iterator<Node> iter = path.iterator();
		while (iter.hasNext()) {
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
	
		// Restore removed nodes
		for (Node removedNode : removedNodes) {
			Node node = g.getNode(removedNode.getId());
        	if (node == null) {
				g.addNode(removedNode.getId());
				removedNode = g.getNode(removedNode.getId());
				removedNode.clearAttributes();
				removedNode.setAttribute("ui.class", MapAttribute.closed.toString());
				removedNode.setAttribute("ui.label",removedNode.getId());
			}
		}

		// Restore removed edges
		for(Couple<String, Couple<Node, Node>> edge : edges){
			if(g.getEdge(edge.getLeft())==null){
				g.addEdge(edge.getLeft(), edge.getRight().getLeft().getId(), edge.getRight().getRight().getId());
			}
		}
	
		if (shortestPath.isEmpty()) {
			return null; // The destination is not currently reachable
		} else {
			shortestPath.remove(0); // Remove the current position
		}
	
		return shortestPath;
	}
	

	public List<String> getShortestPathToClosestOpenNode(String myPosition) {
		//1) Get all openNodes
		List<String> opennodes=getOpenNodes();

		//2) select the closest one
		List<Couple<String,Integer>> lc=
				opennodes.stream()
				.map(on -> (getShortestPath(myPosition,on)!=null)? new Couple<String, Integer>(on,getShortestPath(myPosition,on).size()): new Couple<String, Integer>(on,Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
				.collect(Collectors.toList());

		Optional<Couple<String,Integer>> closest=lc.stream().min(Comparator.comparing(Couple::getRight));
		//3) Compute shorterPath

		return getShortestPath(myPosition,closest.get().getLeft());
	}

	public List<String> getShortestPathToClosestOpenNodeWithoutPassing(String myPosition, String nodeToAvoid){
		Stream<Edge> edges_stream = g.getNode(nodeToAvoid).edges();
		List<Edge> edges_list = edges_stream.collect(Collectors.toList());
		List<Couple<String, Couple<Node, Node>>> edges = new ArrayList<Couple<String, Couple<Node, Node>>>();
		if (edges_list.size()>0){
			for(Edge edge : edges_list) {
				Edge e = g.getEdge(edge.getId());
				if (e!=null) {
					edges.add(new Couple<String, Couple<Node, Node>>(edge.getId(), new Couple<Node, Node>(edge.getNode0(), edge.getNode1())));
					g.removeEdge(e);
				}
			}
		}
		g.removeNode(nodeToAvoid);

		//1) Get all openNodes
		List<String> opennodes=getOpenNodes();

		//2) select the closest one
		List<Couple<String,Integer>> lc=
				opennodes.stream()
				.map(on -> (getShortestPath(myPosition,on)!=null)? new Couple<String, Integer>(on,getShortestPath(myPosition,on).size()): new Couple<String, Integer>(on,Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
				.collect(Collectors.toList());

		Optional<Couple<String,Integer>> closest=lc.stream().min(Comparator.comparing(Couple::getRight));
		//3) Compute shorterPath


		Node node = g.getNode(nodeToAvoid);
		if (node == null) {
			g.addNode(nodeToAvoid);
			node = g.getNode(nodeToAvoid);
			node.clearAttributes();
			node.setAttribute("ui.class", MapAttribute.open.toString());
			node.setAttribute("ui.label",nodeToAvoid);
		}

		for(Couple<String, Couple<Node, Node>> edge : edges){
			if(g.getEdge(edge.getLeft())==null){
				g.addEdge(edge.getLeft(),edge.getRight().getLeft().getId(), edge.getRight().getRight().getId());
			}
		}

		try {
			closest.get();
		} catch (Exception e) {
			return null;
		}
		
		return getShortestPath(myPosition,closest.get().getLeft());
	}


	public List<String> getOpenNodes(){
		return this.g.nodes()
				.filter(x ->x .getAttribute("ui.class")==MapAttribute.open.toString()) 
				.map(Node::getId)
				.collect(Collectors.toList());
	}

	public List<String> getClosedNodes(){
		return this.g.nodes()
				.filter(x ->x .getAttribute("ui.class")==MapAttribute.closed.toString()) 
				.map(Node::getId)
				.collect(Collectors.toList());
	}

	/**
	 * Before the migration we kill all non serializable components and store their data in a serializable form
	 */
	public void prepareMigration(){
		serializeGraphTopology();

		closeGui();

		this.g=null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private void serializeGraphTopology() {
		this.sg= new SerializableSimpleGraph<String,MapAttribute>();
		Iterator<Node> iter=this.g.iterator();
		while(iter.hasNext()){
			Node n=iter.next();
			sg.addNode(n.getId(),MapAttribute.valueOf((String)n.getAttribute("ui.class")));
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}	
	}


	public synchronized SerializableSimpleGraph<String,MapAttribute> getSerializableGraph(){
		serializeGraphTopology();
		return this.sg;
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public synchronized void loadSavedData(){

		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);

		openGui();

		Integer nbEd=0;
		for (SerializableNode<String, MapAttribute> n: this.sg.getAllNodes()){
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
			for(String s:this.sg.getEdges(n.getNodeId())){
				this.g.addEdge(nbEd.toString(),n.getNodeId(),s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream components
	 */
	private synchronized void closeGui() {
		//once the graph is saved, clear non serializable components
		if (this.viewer!=null){
			//Platform.runLater(() -> {
			try{
				this.viewer.close();
			}catch(NullPointerException e){
				System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			//});
			this.viewer=null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private synchronized void openGui() {
		this.viewer =new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);//GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		g.display();
	}

	public void mergeMap(SerializableSimpleGraph<String, MapAttribute> sgreceived) {
		//System.out.println("You should decide what you want to save and how");
		//System.out.println("We currently blindy add the topology");

		for (SerializableNode<String, MapAttribute> n: sgreceived.getAllNodes()){
			//System.out.println(n);
			boolean alreadyIn =false;
			//1 Add the node
			Node newnode=null;
			try {
				newnode=this.g.addNode(n.getNodeId());
			}	catch(IdAlreadyInUseException e) {
				alreadyIn=true;
				//System.out.println("Already in"+n.getNodeId());
			}
			if (!alreadyIn) {
				newnode.setAttribute("ui.label", newnode.getId());
				newnode.setAttribute("ui.class", n.getNodeContent().toString());
			}else{
				newnode=this.g.getNode(n.getNodeId());
				//3 check its attribute. If it is below the one received, update it.
				if (((String) newnode.getAttribute("ui.class"))==MapAttribute.closed.toString() || n.getNodeContent().toString()==MapAttribute.closed.toString()) {
					newnode.setAttribute("ui.class",MapAttribute.closed.toString());
				}
			}
		}

		//4 now that all nodes are added, we can add edges
		for (SerializableNode<String, MapAttribute> n: sgreceived.getAllNodes()){
			for(String s:sgreceived.getEdges(n.getNodeId())){
				addEdge(n.getNodeId(),s);
			}
		}
		//System.out.println("Merge done");
	}



	public MapRepresentation getPartialMap(ArrayList<String> nodesToShare) {
		//System.out.println("NodesToShare = "+nodesToShare);
		MapRepresentation partialMap = new MapRepresentation(false);
		if (nodesToShare == null) {
			return partialMap;
		}
		ArrayList<String> nodesToBeSent = new ArrayList<String>();
		// Adding the nodes
		for (String nodeId: nodesToShare) {
			Node oldNode = this.g.getNode(nodeId);
			Node newNode = partialMap.g.addNode(nodeId);
			nodesToBeSent.add(nodeId);
			for (Object attribute : oldNode.attributeKeys().toArray()) {
				newNode.setAttribute((String) attribute, oldNode.getAttribute((String) attribute));
			}
		}
		// Adding the edges
		for (String nodeId: nodesToShare) {
			Node n = this.g.getNode(nodeId);
			for (Object edge: n.edges().toArray()) {
				String node0 = ((Edge) edge).getNode0().getId();
				String node1 = ((Edge) edge).getNode1().getId();
				if (!nodesToBeSent.contains(node0)) {
					Node oldNode = this.g.getNode(node0);
					Node newNode = partialMap.g.addNode(node0);
					nodesToBeSent.add(node0);
					for (Object attribute : oldNode.attributeKeys().toArray()) {
						newNode.setAttribute((String) attribute, oldNode.getAttribute((String) attribute));
					}
				}
				if (!nodesToBeSent.contains(node1)) {
					Node oldNode = this.g.getNode(node1);
					Node newNode = partialMap.g.addNode(node1);
					nodesToBeSent.add(node1);
					for (Object attribute : oldNode.attributeKeys().toArray()) {
						newNode.setAttribute((String) attribute, oldNode.getAttribute((String) attribute));
					}
				}
				partialMap.addEdge(node0, node1);
			}
		}

		return partialMap;
	}


	public List<String> getNodesWithSmallestArity() {
        int minDegree = Integer.MAX_VALUE;
        List<String> nodesWithSmallestArity = new ArrayList<>();

        for (Node node : this.g) {
            int degree = node.getDegree();
            if (degree >= 1 && degree < minDegree) {
                minDegree = degree;
                nodesWithSmallestArity.clear();
                nodesWithSmallestArity.add(node.getId());
            } else if (degree == minDegree) {
                nodesWithSmallestArity.add(node.getId());
            }
        }

		return nodesWithSmallestArity;
	}


	public List<String> getMidPath(String myPos){
		Integer len = 0;
		List<Node> nnodes = g.nodes().collect(Collectors.toList());
		List<String> nodes = new ArrayList<String>();
		for (Node node : nnodes){
			nodes.add(node.getId());
		}
		List<String> path = new ArrayList<String>();
		List<String> longest_path = new ArrayList<String>();
		for (String node : nodes){
			path = getShortestPath(myPos, node);
			if (path!=null && path.size() > len){
				len = path.size();
				longest_path = path;
			}
		}
		if(longest_path.size()>1){
			String node = longest_path.get(longest_path.size()/2);
			System.out.println(longest_path.size()/2+" node to go "+node);
			return getShortestPath(myPos, node);
		}
		else{
			return null;
		}
	}


	// Partial Graph of nodes containing stench
	// public MapRepresentation getStenchMap(ArrayList<String> StenchNodes){
	// 	MapRepresentation stenchMap = new MapRepresentation(false);
	// 	if (StenchNodes == null) {
	// 		return stenchMap;
	// 	}
		
	// 	// Adding the nodes
	// 	for (String nodeId: StenchNodes) {
	// 		stenchMap.g.addNode(nodeId);
	// 	}
	// 	// Adding the edges
	// 	for (String nodeId: StenchNodes) {
	// 		Node n = this.g.getNode(nodeId);
	// 		for (Object edge: n.edges().toArray()) {
	// 			String node0 = ((Edge) edge).getNode0().getId();
	// 			String node1 = ((Edge) edge).getNode1().getId();
	// 			stenchMap.addEdge(node0, node1);
	// 		}	
	// 	}
	// 	return stenchMap;    
	// }

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph 
	 */
	public boolean hasOpenNode() {
		return (this.g.nodes()
				.filter(n -> n.getAttribute("ui.class")==MapAttribute.open.toString())
				.findAny()).isPresent();
	}

	public Node removeNode(String node){
		return this.g.removeNode(node);
	}

	public Node getNode(String node){
		return this.g.getNode(node);
	}

	public void removeEdge(Edge edge){
		this.g.removeEdge(edge);
	}

	public Edge getEdge(String left) {
		return this.g.getEdge(left);
	}

}