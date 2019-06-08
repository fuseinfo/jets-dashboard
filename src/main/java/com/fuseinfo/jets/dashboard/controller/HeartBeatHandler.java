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

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fuseinfo.jets.dashboard.model.EdgeStatus;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


public class HeartBeatHandler extends TextWebSocketHandler {
	private final ObjectMapper mapper = new ObjectMapper();
	//private final Queue<String> evaluationLog = new ConcurrentLinkedQueue<>();
	
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {		
		try {
			JsonNode rootNode = mapper.readTree(message.getPayload());
			String appName = rootNode.get("app").asText();
			String client = rootNode.get("client").asText();
			
			Map<String, LinkedList<Entry<Long, EdgeStatus[]>>> appMap = DashboardStatus.masterMap.get(appName);
			if (appMap == null) {
				appMap = new ConcurrentHashMap<>();
				DashboardStatus.masterMap.put(appName, appMap);
			}
			LinkedList<Entry<Long, EdgeStatus[]>> statusList = appMap.get(client);
			if (statusList == null) {
				statusList = new LinkedList<>();
				appMap.put(client, statusList);
			}
			JsonNode status = rootNode.get("status");
			if (status != null) {
				int size = status.size();
				EdgeStatus[] edgeStatus = new EdgeStatus[size];
				for (int i = 0; i < size; i++) {
					JsonNode item = status.get(i);
					edgeStatus[i] = new EdgeStatus(item.get("name").asText(), item.get("total").asLong());
				}
				statusList.add(new SimpleImmutableEntry<>(System.currentTimeMillis(), edgeStatus));
				if (statusList.size() > 20)
					statusList.removeFirst();
			}
			
			if (!DashboardStatus.appDefMap.containsKey(appName)) {
				JsonNode defNode = rootNode.get("def");
				if (defNode != null) {
					DashboardStatus.appDefMap.put(appName, (ObjectNode) defNode);
					DashboardStatus.saveAppDef(appName, defNode, false);
				}
			}
				
			if (!DashboardStatus.graphMap.containsKey(appName)) {
				JsonNode graphNode = rootNode.get("graph");
				if (graphNode != null) {
					DashboardStatus.graphMap.put(appName, graphNode.toString());
				}
			}
				
			if (!DashboardStatus.appSchemaMap.containsKey(appName)) {
				JsonNode schemasNode = rootNode.get("schemas");
				if (schemasNode != null) {
					final Map<String, String> nodeSchemaMap = new HashMap<>();
					DashboardStatus.appSchemaMap.put(appName, nodeSchemaMap);
					schemasNode.iterator().forEachRemaining(schemaNode -> {
						String nodeName = schemaNode.get("name").asText();
						String schema = schemaNode.get("schema").asText();
						nodeSchemaMap.put(nodeName, schema);
					});
				}
			}
			Map<String, JsonNode> pendingNodesMap = DashboardStatus.pendingMap.get(appName);
			if (pendingNodesMap != null && pendingNodesMap.size() > 0) {
				final Map<String, Object> sessionAttrs = session.getAttributes();
				final ObjectNode resultNode = mapper.createObjectNode();
				final ObjectNode pendingNode = mapper.createObjectNode();
				pendingNodesMap.entrySet().iterator().forEachRemaining(entry -> {
					String nodeName = entry.getKey();
					JsonNode pendingValue = entry.getValue();
					Integer hashValue = new Integer(pendingValue.hashCode());
					if (!hashValue.equals(sessionAttrs.get(nodeName))) {
						pendingNode.set(entry.getKey(), entry.getValue());
					}
				});
				if (pendingNode.size() > 0) {
					resultNode.set("pending", pendingNode);
					session.sendMessage(new TextMessage(resultNode.toString()));					
				}
			}
			
			JsonNode evalNode = rootNode.get("eval");
			if (evalNode != null && evalNode.isArray()) {
				Map<String, Set<String>> evalNodeMap = DashboardStatus.evalMap.get(appName);
				Iterator<JsonNode> iterator = evalNode.elements();
				while (iterator.hasNext()) {
					JsonNode testNode = iterator.next();
					String nodeName = testNode.get("node").asText();
					boolean testResult = testNode.get("isSuccess").asBoolean();
					if (!testResult) {
						// doesn't work
						pendingNodesMap.remove(nodeName);
						evalNodeMap.remove(nodeName);
					} else {
						Set<String> clientSet = evalNodeMap.get(nodeName);
						// success on this client
						clientSet.remove(client);
						JsonNode jsonNode = pendingNodesMap.get(nodeName);
						if (clientSet.isEmpty() && jsonNode != null) {
							// success on all clients
							ObjectNode appRootNode = DashboardStatus.appDefMap.get(appName);
							appRootNode.set(nodeName, jsonNode);
							DashboardStatus.saveAppDef(appName, rootNode, true);
							pendingNodesMap.remove(nodeName);
						}
					}
					System.out.println(new Date().toString() + "- " + appName + " : " + nodeName + (testResult?" success":" failed"));
					//evaluationLog.add(new Date().toString() + "- " + appName + " : " + nodeName + (testResult?" success":" failed"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
