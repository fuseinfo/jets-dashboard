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

public class ClientStatus implements Comparable<ClientStatus> {
	private String name;
	private Long heartbeat;
	
	public ClientStatus() {
	}
	
	public ClientStatus(String name, Long heartbeat) {
		this.name = name;
		this.heartbeat = heartbeat;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getHeartbeat() {
		return heartbeat;
	}
	public void setHeartbeat(Long heartbeat) {
		this.heartbeat = heartbeat;
	}

	@Override
	public int compareTo(ClientStatus o) {
		return name.compareTo(o.name);
	}
	
}
