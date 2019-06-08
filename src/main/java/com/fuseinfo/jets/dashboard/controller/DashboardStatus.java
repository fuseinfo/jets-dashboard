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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fuseinfo.jets.dashboard.model.EdgeStatus;

public class DashboardStatus {
	static final Map<String, Map<String, LinkedList<Entry<Long, EdgeStatus[]>>>> masterMap = new ConcurrentHashMap<>();
	static final Map<String, ObjectNode> appDefMap = new ConcurrentHashMap<>();
	static final Map<String, String> graphMap = new ConcurrentHashMap<>();
	static final Map<String, Map<String, String>> appSchemaMap = new ConcurrentHashMap<>();
	static final Map<String, Map<String, JsonNode>> pendingMap = new ConcurrentHashMap<>();
	static final Map<String, Map<String, Set<String>>> evalMap = new ConcurrentHashMap<>();

	static final YAMLFactory factory = new YAMLFactory();
	static {
		factory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE);
		factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
		factory.disable(YAMLGenerator.Feature.SPLIT_LINES);
	}
	static ObjectMapper yamlMapper = new ObjectMapper(factory);
	
	static boolean saveAppDef(String appName, JsonNode defNode, boolean replace) {
		try {
			File appDir = new File("data/" + appName);
			if (!appDir.isDirectory()) {
				appDir.mkdirs();
			}
			File yamlFile = new File(appDir, appName + ".yaml");
			if (replace || !yamlFile.exists()) {
				FileOutputStream fos = new FileOutputStream(new File(appDir, appName + ".yaml"));
				SequenceWriter writer = yamlMapper.writerWithDefaultPrettyPrinter().writeValues(fos);
				writer.write(defNode);
				writer.close();
				return true;
			}
		} catch (IOException e) {
		}
		return false;
	}
}
