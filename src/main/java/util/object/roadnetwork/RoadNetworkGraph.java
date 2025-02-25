package util.object.roadnetwork;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;
import util.object.spatialobject.Rect;
import util.object.spatialobject.Segment;

import java.io.Serializable;
import java.util.*;

/**
 * A Road Network Graph object, based on OpenStreetMap (OSM) data model.
 *
 * @author uqdalves, Hellisk
 */
public class RoadNetworkGraph implements Serializable {
	
	private static final Logger LOG = Logger.getLogger(RoadNetworkGraph.class);
	private DistanceFunction distFunc;
	/**
	 * OSM primitives
	 */
	private List<RoadNode> nodeList = new ArrayList<>();
	private Map<String, RoadNode> id2NodeMap = new HashMap<>();
	private List<RoadWay> wayList = new ArrayList<>();
	private Map<String, RoadWay> id2WayMap = new HashMap<>();
	/**
	 * Map boundaries
	 */
	private boolean hasBoundary = false;
	private double minLat = Double.NEGATIVE_INFINITY, minLon = Double.NEGATIVE_INFINITY;
	private double maxLat = Double.POSITIVE_INFINITY, maxLon = Double.POSITIVE_INFINITY;
	
	private boolean isDirectedMap = true;    // false when it stores an undirected map, all roads has its reverse road
	
	private int maxVisitCount = 0;
	
	private boolean isCompactMap = false;    // A compact map does not have intersection whose degree is 2 (intermediate point in a road).
	// Otherwise it is a loose map whose road ways are all straight line (no intermediate point on the road).
	
	/**
	 * The current map will be used for map updateGoh if <tt>isUpdatable==true</tt>. The below variables are only useful for an updatable map.
	 */
	private boolean isUpdatable;
	
	/**
	 * The max absolute value of the road way ID, the id can be either positive or negative according to different directions.
	 */
	private long maxAbsWayID = 0;
	
	private long maxRoadNodeID = 0;
	
	private int maxMiniNodeID = 0;
	
	public RoadNetworkGraph(boolean updatable, DistanceFunction df) {
		this.isUpdatable = updatable;
		this.distFunc = df;
	}
	
	/**
	 * @return The list of nodes in this road network graph.
	 */
	public List<RoadNode> getNodes() {
		return nodeList;
	}
	
	public RoadNode getNode(int index) {
		return nodeList.get(index);
	}
	
	/**
	 * Reset the map by firstly setting the road node list.
	 *
	 * @param newNodeList The road node list representing the intersections or road ends.
	 */
	public void setNodes(List<RoadNode> newNodeList) {
		if (!this.wayList.isEmpty() || !this.id2WayMap.isEmpty())
			throw new IllegalCallerException("The setNodes() should not be called when there were road ways in the map.");
		this.clear();
		this.addNodes(newNodeList);
	}
	
	/**
	 * Reset the road network graph.
	 */
	private void clear() {
		this.nodeList.clear();
		this.id2NodeMap.clear();
		this.wayList.clear();
		this.id2WayMap.clear();
		this.maxAbsWayID = 0;
		this.maxRoadNodeID = 0;
		this.maxMiniNodeID = 0;
		this.maxVisitCount = 0;
		this.hasBoundary = false;
		this.minLat = Double.POSITIVE_INFINITY;
		this.minLon = Double.POSITIVE_INFINITY;
		this.maxLat = Double.NEGATIVE_INFINITY;
		this.maxLon = Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Adds the given node to this road network graph.
	 *
	 * @param node The road node to add.
	 */
	public void addNode(RoadNode node) {
		if (node != null) {
			if (!id2NodeMap.containsKey(node.getID()) || node.getID().equals("")) {
				node.clearConnectedWays();
				nodeList.add(node);
				id2NodeMap.put(node.getID(), node);
				updateBoundary(node);
				if (isUpdatable) {
					if (Long.parseLong(node.getID()) > maxRoadNodeID)
						maxRoadNodeID = Long.parseLong(node.getID());
				}
			} else LOG.error("Insert node to network failed. Node already exist: " + node.getID());
		}
	}
	
	private void removeNode(RoadNode node) {
		if (!this.id2NodeMap.containsKey(node.getID()))
			throw new IllegalArgumentException("The node " + node.getID() + " is not an intersection in the map.");
		if (node.getDegree() != 0)
			throw new IllegalArgumentException("The node to be removed is connected by some edges.");
		if (!this.nodeList.remove(node))
			throw new IllegalArgumentException("The node " + node.getID() + " is in the dictionary but not in the item list.");
		this.id2NodeMap.remove(node.getID());
	}
	
	/**
	 * Add all the nodes in the list to this road network graph.
	 *
	 * @param nodes The list of road nodes to add.
	 */
	public void addNodes(List<RoadNode> nodes) {
		if (nodes == null) {
			throw new NullPointerException("List of road nodes to add must not be null.");
		}
		for (RoadNode node : nodes) {
			if (!id2NodeMap.containsKey(node.getID())) {
				node.clearConnectedWays();
				nodeList.add(node);
				id2NodeMap.put(node.getID(), node);
				if (isUpdatable) {
					if (Long.parseLong(node.getID()) > maxRoadNodeID)
						maxRoadNodeID = Long.parseLong(node.getID());
				}
			} else LOG.error("Insert node to network failed. Node already exist: " + node.getID());
		}
		updateBoundary();
	}
	
	/**
	 * @return The list of Ways in this road network graph.
	 */
	public List<RoadWay> getWays() {
		return wayList;
	}
	
	public RoadWay getWay(int index) {
		return wayList.get(index);
	}
	
	/**
	 * Check if the boundary is preset.
	 *
	 * @return True if the boundary is preset
	 */
	public boolean hasBoundary() {
		return hasBoundary;
	}
	
	/**
	 * Check if the current map is directed. The default value is true.
	 *
	 * @return True if it is directed.
	 */
	public boolean isDirectedMap() {
		return this.isDirectedMap;
	}
	
	/**
	 * Get all nodes in the map, including intersections and mini nodes.
	 *
	 * @return both intersections and mini nodes
	 */
	public List<RoadNode> getAllTypeOfNodes() {
		if (!isCompactMap)
			return this.nodeList;    // no intermediate point in a loose map, return the node list directly.
		List<RoadNode> pointList = new ArrayList<>(this.getNodes());
		for (RoadWay w : this.getWays()) {
			for (RoadNode n : w.getNodes())
				if (!this.id2NodeMap.containsKey(n.getID()))
					pointList.add(n);
		}
		return pointList;
	}
	
	private void setDirectedMap(boolean directedMap) {
		isDirectedMap = directedMap;
	}
	
	/**
	 * Adds all the ways in the list to this road network graph.
	 *
	 * @param waysList The list of road ways to add.
	 */
	public void addWays(List<RoadWay> waysList) {
		if (waysList == null) {
			throw new NullPointerException("List of road ways to add must not be null.");
		}
		for (RoadWay way : waysList)
			addWay(way);
	}
	
	/**
	 * Set bounding box of the road network.
	 *
	 * @param minLon minimum longitude
	 * @param maxLon maximum longitude
	 * @param minLat minimum latitude
	 * @param maxLat maximum latitude
	 */
	public void setBoundary(double minLon, double maxLon, double minLat, double maxLat) {
		this.minLon = minLon;
		this.maxLon = maxLon;
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.hasBoundary = true;
	}
	
	/**
	 * Add the given Way to this road network graph. Make sure the endpoints of the road way should exist in the current node list
	 * unless it is a temp road.
	 *
	 * @param way The road way to add.
	 */
	public void addWay(RoadWay way) {
		if (!isCompactMap && way.getNodes().size() != 2) {
			LOG.info("A polyline road added to the current map, set as a compact map.");
			isCompactMap = true;
		}
		if (way != null && way.getNodes().size() > 1) {
			if (!id2WayMap.containsKey(way.getID())) {
				if (!id2NodeMap.containsKey(way.getFromNode().getID()) || !id2NodeMap.containsKey(way.getToNode().getID()))
					throw new IllegalArgumentException("The endpoints of the inserted road way do not exist in the current map: "
							+ way.getFromNode().getID() + "," + way.getToNode().getID());
				wayList.add(way);
				id2WayMap.put(way.getID(), way);
				way.getFromNode().addOutGoingWay(way);
				way.getToNode().addInComingWay(way);
				if (!isDirectedMap) {    // for undirected map, the road should be both incoming and outgoing adjacent road.
					way.getFromNode().addInComingWay(way);
					way.getToNode().addOutGoingWay(way);
				}
				if (this.maxVisitCount < way.getVisitCount())
					this.maxVisitCount = way.getVisitCount();
				for (RoadNode n : way.getNodes())
					updateBoundary(n);
				if (isUpdatable) {
					if (!way.getID().contains("temp_")) {
						maxAbsWayID = Math.max(Math.abs(Long.parseLong(way.getID())), maxAbsWayID);
						for (RoadNode n : way.getNodes()) {
							maxMiniNodeID = Math.max(Integer.parseInt(n.getID().substring(0, n.getID().length() - 1)), maxMiniNodeID);
						}
					} else    // temporary road way
						LOG.error("Temporary edges should not be included in the road map.");
				}
			} else
				throw new IllegalArgumentException("Road way already exist: " + way.getID());
		}
	}
	
	/**
	 * Reset the boundary to better represent the size.
	 */
	public void updateBoundary() {
		this.setMaxLon(Double.NEGATIVE_INFINITY);
		this.setMaxLat(Double.NEGATIVE_INFINITY);
		this.setMinLon(Double.POSITIVE_INFINITY);
		this.setMinLat(Double.POSITIVE_INFINITY);
		this.hasBoundary = false;
		for (RoadNode n : this.getAllTypeOfNodes())
			updateBoundary(n);
	}
	
	private void updateBoundary(RoadNode node) {
		// updateGoh the map boarder
		if (this.maxLon < node.lon()) {
			this.maxLon = node.lon();
		}
		if (this.minLon > node.lon()) {
			this.minLon = node.lon();
		}
		if (this.maxLat < node.lat()) {
			this.maxLat = node.lat();
		}
		if (this.minLat > node.lat()) {
			this.minLat = node.lat();
		}
		this.hasBoundary = true;
	}
	
	/**
	 * @return The minimum latitude value of this road map's boundary.
	 */
	public double getMinLat() {
		return minLat;
	}
	
	/**
	 * Set the minimum latitude value of this road map's boundary.
	 */
	public void setMinLat(double minLat) {
		this.minLat = minLat;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The minimum longitude value of this road map's boundary.
	 */
	public double getMinLon() {
		return minLon;
	}
	
	/**
	 * Set the minimum longitude value of this road map's boundary.
	 */
	public void setMinLon(double minLon) {
		this.minLon = minLon;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The maximum latitude value of this road map's boundary.
	 */
	public double getMaxLat() {
		return maxLat;
	}
	
	/**
	 * Set he maximum latitude value of this road map's boundary.
	 */
	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
		this.hasBoundary = true;
	}
	
	/**
	 * @return The maximum longitude value of this road map's boundary.
	 */
	public double getMaxLon() {
		return maxLon;
	}
	
	/**
	 * Set the maximum longitude value of this road map's boundary.
	 */
	public void setMaxLon(double maxLon) {
		this.maxLon = maxLon;
		this.hasBoundary = true;
	}
	
	public Rect getBoundary() {
		if (hasBoundary)
			return new Rect(minLon, minLat, maxLon, maxLat, distFunc);
		else {
			LOG.warn("The current map does not have boundary.");
			return new Rect(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, distFunc);
		}
	}
	
	public DistanceFunction getDistanceFunction() {
		return distFunc;
	}
	
	public void setDistanceFunction(DistanceFunction distFunc) {
		this.distFunc = distFunc;
	}
	
	/**
	 * Check whether this road network graph is empty.
	 *
	 * @return Returns true if this road network graph has no nodes.
	 */
	public boolean isEmpty() {
		return nodeList == null || nodeList.isEmpty();
	}
	
	public void removeRoadWayList(Collection<RoadWay> roadWayList) {
		List<RoadWay> removedWayList = new ArrayList<>();
		for (RoadWay way : roadWayList) {
			if (id2WayMap.containsKey(way.getID())) {
				id2WayMap.remove(way.getID());
				way.getFromNode().removeInComingWayFromList(way);
				way.getToNode().removeOutGoingWayFromList(way);
				if (!isDirectedMap()) {
					way.getFromNode().removeOutGoingWayFromList(way);
					way.getToNode().removeInComingWayFromList(way);
				}
			} else
				LOG.error("The road to be removed is not in the map: " + way.getID());
			removedWayList.add(way);
		}
		this.wayList.removeAll(removedWayList);
	}
	
	public int isolatedNodeRemoval() {
		int nodeSize = this.nodeList.size();
		for (Iterator<RoadNode> iterator = this.nodeList.iterator(); iterator.hasNext(); ) {
			RoadNode n = iterator.next();
			if (n.getDegree() == 0) {
				LOG.debug("Removed node ID: " + n.getID());
				iterator.remove();
				this.id2NodeMap.remove(n.getID());
			}
		}
		return nodeSize - this.nodeList.size();
	}
	
	public int getMaxVisitCount() {
		return maxVisitCount;
	}
	
	public void setMaxVisitCount(int maxVisitCount) {
		this.maxVisitCount = maxVisitCount;
	}
	
	public boolean containsWay(String id) {
		return this.id2WayMap.containsKey(id);
	}
	
	public RoadWay getWayByID(String id) {
		if (!containsWay(id))
			throw new IllegalArgumentException("The requested road way ID " + id + " is not in the map.");
		return id2WayMap.get(id);
	}
	
	public boolean containsNode(String id) {
		return this.id2NodeMap.containsKey(id);
	}
	
	public RoadNode getNodeByID(String id) {
		if (!containsNode(id))
			throw new IllegalArgumentException("The requested road node ID " + id + " is not in the map.");
		return id2NodeMap.get(id);
	}
	
	public void updateMaxVisitCount(int visitCount) {
		if (this.maxVisitCount < visitCount)
			this.maxVisitCount = visitCount;
	}
	
	public int getMaxMiniNodeID() {
		return maxMiniNodeID;
	}
	
	public long getMaxAbsWayID() {
		return maxAbsWayID;
	}
	
	public long getMaxRoadNodeID() {
		return maxRoadNodeID;
	}
	
	public void setUpdatable(boolean updatable) {
		if (!this.isUpdatable && updatable) {        // enable the map updateGoh, updateGoh the current value of the IDs
			for (RoadNode n : nodeList) {
				if (Long.parseLong(n.getID()) > maxRoadNodeID)
					maxRoadNodeID = Long.parseLong(n.getID());
			}
			for (RoadWay w : wayList) {
				maxAbsWayID = Math.max(Math.abs(Long.parseLong(w.getID())), maxAbsWayID);
				for (RoadNode n : w.getNodes()) {
					maxMiniNodeID = Math.max(Integer.parseInt(n.getID().substring(0, n.getID().length() - 1)), maxMiniNodeID);
				}
			}
		} else if (this.isUpdatable && !updatable) {    // disable the map updateGoh
			maxRoadNodeID = 0;
			maxAbsWayID = 0;
			maxMiniNodeID = 0;
		}
	}
	
	@Override
	public RoadNetworkGraph clone() {
		RoadNetworkGraph clone = new RoadNetworkGraph(isUpdatable, distFunc);
		Map<String, RoadNode> id2NodeMapping = new HashMap<>();
		for (RoadNode n : this.getNodes()) {
			RoadNode cloneNode = n.clone();
			cloneNode.clearConnectedWays();
			clone.addNode(cloneNode);
			id2NodeMapping.put(cloneNode.getID(), cloneNode);
		}
		for (RoadWay w : this.getWays()) {
			RoadWay cloneWay = new RoadWay(w.getID(), w.getDistanceFunction());
			if (!id2NodeMapping.containsKey(w.getNode(0).getID()) || !id2NodeMapping.containsKey(w.getNode(w.size() - 1).getID()))
				throw new IllegalArgumentException("The road way to be cloned " + w.getID() + " is not originally linked to the " +
						"intersections.");
			cloneWay.addNode(id2NodeMapping.get(w.getNode(0).getID()));
			for (int i = 1; i < w.getNodes().size() - 1; i++) {
				cloneWay.addNode(w.getNode(i).clone());
			}
			cloneWay.addNode(id2NodeMapping.get(w.getNode(w.size() - 1).getID()));
			clone.addWay(cloneWay);
		}
		clone.updateBoundary();
		if (clone.getMaxLon() != this.getMaxLon() || clone.getMinLon() != this.getMinLon() || clone.getMaxLat() != this.getMaxLat()
				|| clone.getMinLat() != this.getMinLat())
			LOG.warn("Clone result has different boundary as the original object.");
		if (clone.maxAbsWayID != this.maxAbsWayID || clone.maxMiniNodeID != this.maxMiniNodeID || clone.maxRoadNodeID != this.maxRoadNodeID)
			LOG.warn("Clone result has different count of roads.");
		return clone;
	}
	
	/**
	 * Convert a directed map to an undirected map.
	 *
	 * @return The undirected map.
	 */
	public RoadNetworkGraph toUndirectedMap() {
		RoadNetworkGraph tempMap = this.clone();
		int maxVisitCount = 0;
		List<RoadWay> reverseWayList = new ArrayList<>();
		List<RoadWay> remainingWayList = new ArrayList<>();
		Set<String> wayEndPointPositionSet = new HashSet<>();
		for (RoadWay w : tempMap.getWays()) {
			if (!w.getID().contains("-")) {    // reverse
				String endPointPosition =
						w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode().lat();
				String reverseEndPointPosition =
						w.getToNode().lon() + "_" + w.getToNode().lat() + "," + w.getFromNode().lon() + "_" + w.getFromNode().lat();
				if (wayEndPointPositionSet.contains(endPointPosition) || wayEndPointPositionSet.contains(reverseEndPointPosition)) {
					LOG.error("Multiple roads have the same endpoints: " + endPointPosition);
				} else {
					wayEndPointPositionSet.add(endPointPosition);
					maxVisitCount = Math.max(maxVisitCount, w.getVisitCount());
					remainingWayList.add(w);
				}
			} else
				reverseWayList.add(w);
		}
		
		// check if the removed roads have unique connection. Theoretically, they should all have reverse road included in the new map
		for (RoadWay w : reverseWayList) {
			String endPointPosition =
					w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode().lat();
			String reverseEndPointPosition =
					w.getFromNode().lon() + "_" + w.getFromNode().lat() + "," + w.getToNode().lon() + "_" + w.getToNode().lat();
			if (!wayEndPointPositionSet.contains(endPointPosition) && !wayEndPointPositionSet.contains(reverseEndPointPosition)) {
				LOG.error("Reverse road of " + w.getID() + " does not appear in the map.");
				if (tempMap.id2WayMap.containsKey(w.getID().substring(1))) {
					LOG.error("More interestingly, " + w.getID() + " has reverse road but is not included in the new map.");
				} else {
					w.setId(w.getID().substring(1));
					remainingWayList.add(w);
				}
			}
		}
		RoadNetworkGraph resultMap = new RoadNetworkGraph(false, this.distFunc);
		resultMap.setDirectedMap(false);
		List<RoadNode> nodeList = new ArrayList<>();
		for (RoadNode node : tempMap.getNodes()) {
			node.clearConnectedWays();
			nodeList.add(node);
		}
		resultMap.setNodes(nodeList);
		resultMap.addWays(remainingWayList);
		resultMap.updateBoundary();
		return resultMap;
	}
	
	public boolean isPlanarMap() {
		return nonPlanarNodeCount() == 0;
	}
	
	/**
	 * Calculate the total number of crosses happens for roads that do not have intersection. =0 means the map is planar.
	 *
	 * @return Count of potential intersections
	 */
	public int nonPlanarNodeCount() {
		RoadNetworkGraph currMap = this.clone();
		int count = 0;
		// TODO optimize the performance
		for (int i = 0; i < currMap.getWays().size(); i++) {
			RoadWay firstWay = currMap.getWay(i);
			for (int j = i + 1; j < currMap.getWays().size(); j++) {
				RoadWay secondWay = currMap.getWay(j);
				if (secondWay.getFromNode().toPoint().equals2D(firstWay.getToNode().toPoint())
						|| secondWay.getFromNode().toPoint().equals2D(firstWay.getFromNode().toPoint())
						|| secondWay.getToNode().toPoint().equals2D(firstWay.getFromNode().toPoint())
						|| secondWay.getToNode().toPoint().equals2D(firstWay.getToNode().toPoint())) {
					continue;
				}
				for (Segment firstEdge : firstWay.getEdges()) {
					for (Segment secondEdge : secondWay.getEdges()) {
						if (firstEdge.crosses(secondEdge.x1(), secondEdge.y1(), secondEdge.x2(), secondEdge.y2()))
							count++;
					}
				}
			}
		}
		return count;
	}
	
	/**
	 * Convert the current map to a planar map.
	 *
	 * @return The result planar map.
	 */
	// TODO Test the function
	public RoadNetworkGraph toPlanarMap() {
		RoadNetworkGraph tempMap = this.clone();
		List<RoadNode> newNodeList = new ArrayList<>();
		List<RoadWay> newWayList = new ArrayList<>();
		Set<RoadWay> removeWayList = new HashSet<>();
		Map<String, List<RoadWay>> removedID2ReplacedRoadList = new HashMap<>();    // for each split road, its id and the generated road
		// intersections
		Map<String, RoadNode> location2NewIntersectionMap = new HashMap<>();
		// TODO optimize the performance, same as the nonPlanarNodeCount()
		for (int i = 0; i < tempMap.getWays().size(); i++) {
			RoadWay firstWay = tempMap.getWay(i);
			for (int j = i + 1; j < tempMap.getWays().size(); j++) {
				RoadWay secondWay = tempMap.getWay(j);
				if (secondWay.getFromNode().toPoint().equals2D(firstWay.getToNode().toPoint())
						|| secondWay.getFromNode().toPoint().equals2D(firstWay.getFromNode().toPoint())
						|| secondWay.getToNode().toPoint().equals2D(firstWay.getFromNode().toPoint())
						|| secondWay.getToNode().toPoint().equals2D(firstWay.getToNode().toPoint())) {
					continue;
				}
				boolean isIntersected = false;
				for (Segment firstEdge : firstWay.getEdges()) {
					for (Segment secondEdge : secondWay.getEdges()) {
						if (firstEdge.crosses(secondEdge.x1(), secondEdge.y1(), secondEdge.x2(), secondEdge.y2())) {
							if (isIntersected)
								LOG.warn("The same road pair intersects more than once: " + firstWay.getID() + "," + secondWay.getID());
							Point intersection = firstEdge.getIntersection(secondEdge);
							String interSectLocation = intersection.x() + "_" + intersection.y();
							RoadNode intersectionNode;
							if (location2NewIntersectionMap.containsKey(interSectLocation))
								intersectionNode = location2NewIntersectionMap.get(interSectLocation);
							else {
								intersectionNode = new RoadNode(intersection.x() + intersection.y() + "", intersection.x(), intersection.y(),
										distFunc);
								newNodeList.add(intersectionNode);
								location2NewIntersectionMap.put(interSectLocation, intersectionNode);
							}
							
							// split the first road
							RoadWay candidateWay = null;    // the first road to be cut
							if (!removeWayList.contains(firstWay)) {    // the first time this road got cut
								candidateWay = firstWay;
								removeWayList.add(firstWay);
								removedID2ReplacedRoadList.put(firstWay.getID(), new ArrayList<>());
								List<RoadWay> splitFirstWayList = candidateWay.splitAtNode(intersectionNode, firstEdge);
								newWayList.addAll(splitFirstWayList);
								removedID2ReplacedRoadList.get(firstWay.getID()).addAll(splitFirstWayList);
							} else {
								if (!removedID2ReplacedRoadList.containsKey(firstWay.getID()))
									throw new IllegalArgumentException("Inconsistency between removedRoadWay and remove ID");
								boolean isActualRoadFound = false;    // the actual road to be cut, instead of firstWay, is found
								for (RoadWay way : removedID2ReplacedRoadList.get(firstWay.getID())) {
									List<RoadNode> nodes = way.getNodes();
									for (int index = 0; index < nodes.size(); index++) {
										RoadNode node = nodes.get(index);
										if (node.toPoint().equals2D(firstEdge.p1())) {
											if (index == 0 || index == nodes.size() - 1) {    // the end point of the current way is the
												// intersection, has been cut correctly.
												candidateWay = null;
											} else {
												candidateWay = way;
											}
											isActualRoadFound = true;
											break;
										}
									}
									if (isActualRoadFound)
										break;
								}
								if (!isActualRoadFound)
									throw new IllegalArgumentException("The actual sub road to be cut is not found:" + firstWay.getID());
								if (candidateWay != null) {    // new break happens
									newWayList.remove(candidateWay);
									removedID2ReplacedRoadList.get(firstWay.getID()).remove(candidateWay);
									List<RoadWay> splitFirstWayList = candidateWay.splitAtNode(intersectionNode, firstEdge);
									newWayList.addAll(splitFirstWayList);
									removedID2ReplacedRoadList.get(firstWay.getID()).addAll(splitFirstWayList);
								}
							}
							
							// split the second road
							candidateWay = null;    // the second road to be cut
							if (!removeWayList.contains(secondWay)) {    // the first time this road got cut
								candidateWay = secondWay;
								removeWayList.add(secondWay);
								removedID2ReplacedRoadList.put(secondWay.getID(), new ArrayList<>());
								List<RoadWay> splitSecondWayList = candidateWay.splitAtNode(intersectionNode, secondEdge);
								newWayList.addAll(splitSecondWayList);
								removedID2ReplacedRoadList.get(secondWay.getID()).addAll(splitSecondWayList);
							} else {
								if (!removedID2ReplacedRoadList.containsKey(secondWay.getID()))
									throw new IllegalArgumentException("Inconsistency between removedRoadWay and remove ID");
								boolean isActualRoadFound = false;    // the actual road to be cut, instead of secondWay, is found
								for (RoadWay way : removedID2ReplacedRoadList.get(secondWay.getID())) {
									List<RoadNode> nodes = way.getNodes();
									for (int index = 0; index < nodes.size(); index++) {
										RoadNode node = nodes.get(index);
										if (node.toPoint().equals2D(secondEdge.p1())) {
											if (index == 0 || index == nodes.size() - 1) {    // the end point of the current way is the
												// intersection, has been cut correctly.
												candidateWay = null;
											} else {
												candidateWay = way;
											}
											isActualRoadFound = true;
											break;
										}
									}
									if (isActualRoadFound)
										break;
								}
								if (!isActualRoadFound)
									throw new IllegalArgumentException("The actual sub road to be cut is not found:" + secondWay.getID());
								if (candidateWay != null) {    // new break happens
									newWayList.remove(candidateWay);
									removedID2ReplacedRoadList.get(secondWay.getID()).remove(candidateWay);
									List<RoadWay> splitSecondWayList = candidateWay.splitAtNode(intersectionNode, secondEdge);
									newWayList.addAll(splitSecondWayList);
									removedID2ReplacedRoadList.get(secondWay.getID()).addAll(splitSecondWayList);
								}
							}
							
							isIntersected = true;
						}
					}
				}
			}
		}
		tempMap.removeRoadWayList(removeWayList);
		tempMap.addNodes(newNodeList);
		tempMap.addWays(newWayList);
		return tempMap;
	}
	
	/**
	 * Convert a map to its compact form. A compact map does not have intersections whose degree is two. The roads whose degree is two are
	 * merged to one new road, the new road ID is the concatenation of previous roads with "," as separator, i.e.: id1,id2,id3
	 *
	 * @return The output compact map.
	 */
	public RoadNetworkGraph toCompactMap() {
		if (isCompactMap) {
			LOG.info("The current map is already a compact map, skip the toCompactMap() step.");
			return this;
		}
		boolean wasCompactMap = false;    // the original map was a compact map and we try to merge them back.
		int degree2NodeCount = 0;
		RoadNetworkGraph cloneMap = this.clone();
		List<RoadNode> removeNodeList = new ArrayList<>();
		for (RoadWay way : cloneMap.getWays()) {
			if (way.getID().contains("_S")) {    // check if the previous map was converted from a compact map
				wasCompactMap = true;
				break;
			}
		}
		for (RoadNode node : cloneMap.getAllTypeOfNodes()) {
			if (node.getDegree() == 2 && node.getInComingDegree() == node.getOutGoingDegree()) {
				degree2NodeCount++;
				RoadWay inComingWay = node.getInComingWayList().iterator().next();
				RoadWay outGoingWay = node.getOutGoingWayList().iterator().next();
				String mergedWayID;
				if (wasCompactMap) {
					// Merge the split roads back and try to recover their road ID.
					String inComingID = inComingWay.getID().split("_S")[0];
					String outGoingID = outGoingWay.getID().split("_S")[0];
					if (inComingID.equals(outGoingID)) {
						mergedWayID = inComingID;
					} else
						throw new IllegalArgumentException("Fail to merge two previously separated roads when compacting, ID conflict: "
								+ inComingWay + "," + outGoingWay);
				} else {
					mergedWayID = inComingWay.getID() + "," + outGoingWay.getID();
				}
				List<RoadNode> mergedNodeList = new ArrayList<>();
				List<RoadWay> removeWayList = new ArrayList<>();
				mergedNodeList.addAll(inComingWay.getNodes());
				mergedNodeList.addAll(outGoingWay.getNodes().subList(1, outGoingWay.getNodes().size()));
				node.removeInComingWayFromList(inComingWay);
				node.removeOutGoingWayFromList(outGoingWay);
				removeWayList.add(inComingWay);
				removeWayList.add(outGoingWay);
				cloneMap.removeRoadWayList(removeWayList);
				removeNodeList.add(node);
				RoadWay mergeWay = new RoadWay(mergedWayID, mergedNodeList, cloneMap.getDistanceFunction());
				cloneMap.addWay(mergeWay);
			} else if (node.getDegree() == 2) {
				LOG.warn("Current end point only contains incoming or outgoing roads: " + node.getInComingDegree() + "," + node.getOutGoingDegree());
			}
		}
		for (RoadNode node : removeNodeList) {
			cloneMap.removeNode(node);
		}
		
		// evaluate conversion result
		for (RoadWay way : cloneMap.getWays()) {
			if (way.getID().contains("_S"))
				throw new IllegalArgumentException("The current map still contains unmerged road after compact map conversion: " + way.getID());
		}
		for (RoadNode node : cloneMap.getNodes()) {
			if (node.getDegree() == 2 && node.getInComingDegree() == node.getOutGoingDegree())
				throw new IllegalArgumentException("The current map is still not compact after the compact conversion.");
		}
		cloneMap.isCompactMap = true;
		
		LOG.info("Finish compact map conversion, total number of node removed: " + degree2NodeCount + ". New map contains " + cloneMap.getNodes().size() + " nodes.");
		return cloneMap;
	}
	
	/**
	 * Convert a compact map to its loose form. The roads in a loose map are all straight lines, no polyline appears. New road ID is the
	 * combination
	 *
	 * @return The output simple map.
	 */
	public RoadNetworkGraph toLooseMap() {
		if (!isCompactMap) {
			LOG.info("The current map is already a loose map, skip the toLooseMap() step.");
			return this;
		}
		long startTime = System.currentTimeMillis();
		boolean wasLooseMap = false;    // the current map was a loose map and we try to separate it back with its original road ID
		
		RoadNetworkGraph cloneMap = this.clone();
		
		// check if it is a loose map
		for (RoadWay currWay : cloneMap.wayList) {
			if (currWay.getID().contains(",")) {
				wasLooseMap = true;    // the original map was a loose map
				break;
			}
		}
		List<RoadWay> removedWayList = new ArrayList<>();
		List<RoadWay> insertWayList = new ArrayList<>();
		for (RoadWay currWay : cloneMap.getWays()) {
			if (currWay.getNodes().size() > 2) {
				// the current road is a polyline, separate it
				String[] idList = currWay.getID().split(",");
				removedWayList.add(currWay);
				if (wasLooseMap) {
					// retrieve the previous road IDs
					if (idList.length != currWay.getNodes().size() - 1)
						throw new IllegalArgumentException("The current road to be separated during loose map conversion contains " +
								"inconsistent number of previous ids: " + (currWay.getNodes().size() - 1) + "," + idList.length);
				} else {
					if (idList.length != 1)
						throw new IllegalArgumentException("The current road to be separated during loose map conversion contains " +
								"complex road ID: " + currWay.getID());
				}
				for (int i = 1; i < currWay.getNodes().size(); i++) {
					List<RoadNode> insertWayEndNodeList = new ArrayList<>();
					RoadNode startNode = currWay.getNode(i - 1);
					RoadNode endNode = currWay.getNode(i);
					insertWayEndNodeList.add(startNode);
					insertWayEndNodeList.add(endNode);    // the last point already in the intersection list, do not add twice
					if (i != currWay.getNodes().size() - 1)
						cloneMap.addNode(endNode);
					RoadWay insertWay;
					if (wasLooseMap)
						insertWay = new RoadWay(idList[i - 1], insertWayEndNodeList, cloneMap.getDistanceFunction());
					else
						insertWay = new RoadWay(idList[0] + "_S" + (i - 1), insertWayEndNodeList, cloneMap.getDistanceFunction());
					insertWayList.add(insertWay);
				}
			}
		}
		cloneMap.removeRoadWayList(removedWayList);
		cloneMap.addWays(insertWayList);
		
		// evaluate conversion result
		for (RoadWay currWay : cloneMap.getWays()) {
			if (currWay.getNodes().size() != 2)
				throw new IllegalArgumentException("Some roads are still non-straight after the loose map conversion.");
			if (currWay.getID().contains(","))
				throw new IllegalArgumentException("Incorrect road ID after loose map conversion: " + currWay.getID());
		}
		if (this.getAllTypeOfNodes().size() != cloneMap.getNodes().size())
			throw new IllegalArgumentException("The number of nodes changes during the loose map conversion: "
					+ this.getAllTypeOfNodes().size() + "," + cloneMap.getNodes().size());
		isCompactMap = false;
		LOG.info("Finish loose map conversion, total number of roads affected: " + removedWayList.size() + ". Number of new way created: "
				+ insertWayList.size() + ", time spent: " + (System.currentTimeMillis() - startTime) / 1000 + "s.");
		return cloneMap;
	}
	
	/**
	 * Convert road IDs from loose map to compact map. Merge the road IDs that refers the same road.
	 *
	 * @param originalRoads The original road IDs whose format is "roadID_Sindex" e.g.: 123456_S1
	 * @return Route that only contains compact result.
	 */
	public static List<String> compactRoadID(Set<String> originalRoads) {
		List<String> resultList = new ArrayList<>();
		for (String road : originalRoads) {
			if (resultList.isEmpty() || !road.split("_")[0].equals(resultList.get(resultList.size() - 1)))
				resultList.add(road.split("_")[0]);
		}
		return resultList;
	}
}