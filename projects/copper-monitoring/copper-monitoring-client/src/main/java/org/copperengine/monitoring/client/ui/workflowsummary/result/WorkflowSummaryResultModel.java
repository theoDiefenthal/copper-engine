/*
 * Copyright 2002-2014 SCOOP Software GmbH
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
package org.copperengine.monitoring.client.ui.workflowsummary.result;

import javafx.beans.property.SimpleStringProperty;

import org.copperengine.monitoring.client.util.WorkflowVersion;
import org.copperengine.monitoring.core.model.WorkflowStateSummary;
import org.copperengine.monitoring.core.model.WorkflowSummary;

public class WorkflowSummaryResultModel {
    public final WorkflowVersion version;
    public final SimpleStringProperty totalCount;
    public final WorkflowStateSummary workflowStateSummary;

    public WorkflowSummaryResultModel(WorkflowSummary workflowSummary) {
        this.totalCount = new SimpleStringProperty("" + workflowSummary.getTotalCount());
        this.version = new WorkflowVersion(workflowSummary.getClassDescription());
        this.workflowStateSummary = workflowSummary.getStateSummary();

    }

}
