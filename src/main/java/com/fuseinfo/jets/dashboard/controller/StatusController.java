/*
 * Copyright (c) 2019 Fuseinfo Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package com.fuseinfo.jets.dashboard.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fuseinfo.jets.dashboard.model.ClientStatus;
import com.fuseinfo.jets.dashboard.model.EdgeStatus;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class StatusController {


	private final ObjectMapper mapper = new ObjectMapper();

	public StatusController() {
		File dataDir = new File("data");
		if (!dataDir.exists()) dataDir.mkdirs();
		File[] dirs = dataDir.listFiles(path -> path.isDirectory() && path.getName().charAt(0) != '.');
		for (File dir : dirs) {
			try {
				String dirName = dir.getName();
				File yamlFile = new File(dir, dirName + ".yaml");
				if (yamlFile.exists()) {
					DashboardStatus.masterMap.put(dirName, new ConcurrentHashMap<String, LinkedList<Entry<Long, EdgeStatus[]>>>());
				}
			} catch (Exception e) {

			}
		}
	}
	
	@GetMapping(path = "/list/app", produces = "application/json")
	public String[] listApps() {
		String[] result = new String[DashboardStatus.masterMap.size()];
		Iterator<String> iter = DashboardStatus.masterMap.keySet().iterator();
		for (int i = 0; i < result.length; i++) {
			result[i] = iter.next();
		}
		return result;
	}

	@GetMapping(path = "/status/{app}", produces = "application/json")
	public EdgeStatus[] getAppStatus(@PathVariable("app") String app) {
		Map<String, EdgeStatus> statusMap = new HashMap<>();
		Map<String, LinkedList<Entry<Long, EdgeStatus[]>>> appMap = DashboardStatus.masterMap.get(app);
		if (appMap == null)
			return new EdgeStatus[0];

		for (Entry<String, LinkedList<Entry<Long, EdgeStatus[]>>> appEntry : appMap.entrySet()) {
			long begin = System.currentTimeMillis() - 30000;
			LinkedList<Entry<Long, EdgeStatus[]>> ll = appEntry.getValue();
			Iterator<Entry<Long, EdgeStatus[]>> iter = ll.iterator();
			EdgeStatus[] status = null;
			Entry<Long, EdgeStatus[]> lastEntry = null;
			while (status == null && iter.hasNext()) {
				Entry<Long, EdgeStatus[]> entry = iter.next();
				Long thisTime = entry.getKey();
				EdgeStatus[] thisStatus = entry.getValue();
				if (thisTime > begin) {
					status = new EdgeStatus[thisStatus.length];
					Entry<Long, EdgeStatus[]> endEntry = ll.getLast();
					EdgeStatus[] endStatus = endEntry.getValue();
					if (lastEntry != null) {
						EdgeStatus[] lastStatus = lastEntry.getValue();
						Long lastTime = lastEntry.getKey();
						// (m - t1)/(t2-t1) (t2 - m)/(t2 - t1)
						// d1 + (d2 - d1)/ (t2 - t1) * (m - t1)
						// d2 - (d2 - d1)/ (t2 - t1) * (t2 - m)
						double rate2 = (begin - lastTime) * 1.0 / (thisTime - lastTime);
						double rate1 = 1 - rate2;
						// d1 + d2 * rate - d1 * rate = d1 * rate1 + d2 * rate2
						double interval = (endEntry.getKey() - begin) / 1000.0;
						for (int i = 0; i < endStatus.length; i++) {
							status[i] = new EdgeStatus(endStatus[i].getName(), endStatus[i].getTotal());
							status[i].setAverage((int) Math.round((endStatus[i].getTotal()
								- lastStatus[i].getTotal() * rate1 - thisStatus[i].getTotal() * rate2) / interval));
						}
					} else {
						double interval = (endEntry.getKey() - thisTime) / 1000.0;
						for (int i = 0; i < endStatus.length; i++) {
							status[i] = new EdgeStatus(endStatus[i].getName(), endStatus[i].getTotal());
							status[i].setAverage(
									(int) Math.round((endStatus[i].getTotal() - thisStatus[i].getTotal()) / interval));
						}
					}
				}
				lastEntry = entry;
			}
			if (status == null) {
				status = ll.getLast().getValue();
			}
			for (EdgeStatus edgeStatus : status) {
				String edgeName = edgeStatus.getName();
				EdgeStatus processed = statusMap.get(edgeName);
				if (processed == null) {
					processed = new EdgeStatus(edgeName, 0, 0L);
					statusMap.put(edgeName, processed);
				}
				processed.setAverage(processed.getAverage() + edgeStatus.getAverage());
				processed.setTotal(processed.getTotal() + edgeStatus.getTotal());
			}
		}
		EdgeStatus[] status = new EdgeStatus[statusMap.size()];
		Iterator<Entry<String, EdgeStatus>> iter = statusMap.entrySet().iterator();
		for (int i = 0; i < status.length; i++) {
			Entry<String, EdgeStatus> entry = iter.next();
			status[i] = entry.getValue();
		}
		return status;
	}

	@GetMapping(path = "/graph/{app}", produces = "application/json")
	public String getAppDAG(@PathVariable("app") String app) {
		return DashboardStatus.graphMap.getOrDefault(app, "");
	}

	@GetMapping(path = "/client/{app}", produces = "application/json")
	public ClientStatus[] getClients(@PathVariable("app") String app) {
		Map<String, LinkedList<Entry<Long, EdgeStatus[]>>> appMap = DashboardStatus.masterMap.get(app);
		if (appMap != null) {
			long time = System.currentTimeMillis();
			TreeSet<ClientStatus> result = new TreeSet<>();
			for (Entry<String, LinkedList<Entry<Long, EdgeStatus[]>>> entry : appMap.entrySet()) {
				long heartbeat = time - entry.getValue().getLast().getKey();
				if (heartbeat < 1800000) {
					ClientStatus status = new ClientStatus(entry.getKey(), heartbeat);
					result.add(status);
				}
			}
			return result.toArray(new ClientStatus[result.size()]);
		} else {
			return new ClientStatus[0];
		}
	}

	@GetMapping(path = "/nodeSchema/{app}/{node}", produces = "application/json")
	public String getNodeSchema(@PathVariable("app") String app, @PathVariable("node") String node) {
		Map<String, String> nodeSchemaMap = DashboardStatus.appSchemaMap.get(app);
		if (nodeSchemaMap != null) {
			return nodeSchemaMap.getOrDefault(node, "");
		} else {
			return "";
		}
	}

	@GetMapping(path = "/nodeDef/{app}/{node}", produces = "application/json")
	public String getNodeDef(@PathVariable("app") String app, @PathVariable("node") String node) {
		ObjectNode appNode = DashboardStatus.appDefMap.get(app);
		if (appNode != null) {
			JsonNode defNode = appNode.get(node);
			if (defNode != null) {
				return defNode.toString();
			}
		}
		return "";
	}
	
	@PostMapping(path = "/nodeDef/{app}/{node}", consumes = "application/json")
	public ResponseEntity<String> postNodeDef(@PathVariable("app") String app, @PathVariable("node") String node, 
			HttpServletRequest request) throws IOException {
		JsonNode jsonNode = mapper.readTree(request.getInputStream());
		Map<String, JsonNode> pendingNodesMap = DashboardStatus.pendingMap.get(app);
		if (pendingNodesMap == null) {
			pendingNodesMap = new ConcurrentHashMap<String, JsonNode>();
			DashboardStatus.pendingMap.put(app, pendingNodesMap);
		}
		pendingNodesMap.put(node, jsonNode);
		Map<String, Set<String>> evalNodesMap = DashboardStatus.evalMap.get(app);
		if (evalNodesMap == null) {
			evalNodesMap = new ConcurrentHashMap<>();
			DashboardStatus.evalMap.put(app, evalNodesMap);
		}
		Set<String> clients = new HashSet<>();
		long time = System.currentTimeMillis();
		for (Entry<String, LinkedList<Entry<Long, EdgeStatus[]>>> entry : DashboardStatus.masterMap.get(app).entrySet()) {
			long heartbeat = time - entry.getValue().getLast().getKey();
			if (heartbeat < 60000) {
				clients.add(entry.getKey());
			}
		}
		if (clients.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No active client to apply");
		}
		evalNodesMap.put(node, clients);
		
		long timeout = System.currentTimeMillis() + 90000;
		while (System.currentTimeMillis() < timeout) {
			if (!pendingNodesMap.containsKey(node)) {
				ObjectNode appRootNode = DashboardStatus.appDefMap.get(app);
				if (appRootNode != null) {
					JsonNode newNode = appRootNode.get(node);
					if (jsonNode.equals(newNode)) {
						return ResponseEntity.ok("Successfully applied changes to " + node + " of " + app);
					}
				}
				break;
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to apply change to node");
	}

}
