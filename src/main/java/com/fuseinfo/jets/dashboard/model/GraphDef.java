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

package com.fuseinfo.jets.dashboard.model;

public class GraphDef {

	private Node[] nodes;
	
	private Edge[] edges;
	
	public Node[] getNodes() {
		return nodes;
	}

	public void setNodes(Node[] nodes) {
		this.nodes = nodes;
	}

	public Edge[] getEdges() {
		return edges;
	}

	public void setEdges(Edge[] edges) {
		this.edges = edges;
	}

	public static class Node {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
		
	}
	
	public static class Edge {
		private String id;
		private String source;
		private String target;
		private String label;
		
		public String getId() {
			return id;
		}
		
		public void setId(String id) {
			this.id = id;
		}
		
		public String getSource() {
			return source;
		}
		
		public void setSource(String source) {
			this.source = source;
		}
		
		public String getTarget() {
			return target;
		}
		
		public void setTarget(String target) {
			this.target = target;
		}
		
		public String getLabel() {
			return label;
		}
		
		public void setLabel(String label) {
			this.label = label;
		}
		
	}
}
