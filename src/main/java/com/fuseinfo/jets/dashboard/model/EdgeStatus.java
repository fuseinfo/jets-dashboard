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

public class EdgeStatus {

	private String name;
	private Integer average;
	private Long total;
	
	public EdgeStatus() {
	}
	
	public EdgeStatus(String name, Long total) {
		this.name = name;
		this.average = 0;
		this.total = total;
	}
	
	public EdgeStatus(String name, Integer average, Long total) {
		this.name = name;
		this.average = average;
		this.total = total;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAverage() {
		return average == null ? 0 : average;
	}

	public void setAverage(Integer average) {
		this.average = average;
	}
	
	public Long getTotal() {
		return total == null ? 0 : total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}
	
}
