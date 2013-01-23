/*
 * Copyright 2002-2012 SCOOP Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.scoopgmbh.copper.monitor.adapter.model;

import java.io.Serializable;

public class WorkflowSummery implements Serializable {
	private static final long serialVersionUID = 4867510351238162279L;
	
	private String alias;
	private int totalcount;
	private WorkflowStateSummery stateSummery;
	private WorkflowClassVersionInfo classDescription;
	
	public WorkflowSummery() {
		super();
	}

	public WorkflowSummery(String alias, int totalcount, WorkflowClassVersionInfo classDescription, WorkflowStateSummery stateSummery) {
		super();
		this.alias = alias;
		this.totalcount = totalcount;
		this.stateSummery = stateSummery;
		this.classDescription = classDescription;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public int getTotalcount() {
		return totalcount;
	}

	public void setTotalcount(int totalcount) {
		this.totalcount = totalcount;
	}

	public WorkflowStateSummery getStateSummery() {
		return stateSummery;
	}

	public void setStateSummery(WorkflowStateSummery stateSummery) {
		this.stateSummery = stateSummery;
	}

	public WorkflowClassVersionInfo getClassDescription() {
		return classDescription;
	}

	public void setClassDescription(WorkflowClassVersionInfo classDescription) {
		this.classDescription = classDescription;
	}


	


	
	
	
	
}
