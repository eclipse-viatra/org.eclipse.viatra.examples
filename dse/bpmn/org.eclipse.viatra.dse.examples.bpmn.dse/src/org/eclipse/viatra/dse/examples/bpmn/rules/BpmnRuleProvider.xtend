/*******************************************************************************
 * Copyright (c) 2010-2015, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.dse.examples.bpmn.rules

import org.eclipse.viatra.dse.examples.bpmn.patterns.AllocateTaskToVariant
import org.eclipse.viatra.dse.examples.bpmn.patterns.CreateResource
import org.eclipse.viatra.dse.examples.bpmn.patterns.MakeParallel
import org.eclipse.viatra.dse.examples.bpmn.patterns.MakeSequential
import org.eclipse.viatra.dse.examples.bpmn.problems.BpmnProblems
import org.eclipse.viatra.dse.examples.bpmn.problems.SimplifiedBpmnBuilder
import org.eclipse.viatra.dse.examples.simplifiedbpmn.SimplifiedbpmnFactory
import org.eclipse.viatra.transformation.runtime.emf.rules.batch.BatchTransformationRule
import org.eclipse.viatra.transformation.runtime.emf.rules.batch.BatchTransformationRuleFactory

class BpmnRuleProvider {
    extension BatchTransformationRuleFactory factory = new BatchTransformationRuleFactory

    public BatchTransformationRule<?, ?> allocateRule
    public BatchTransformationRule<?, ?> createResourceRule
    public BatchTransformationRule<?, ?> makeParallelRule
    public BatchTransformationRule<?, ?> makeSequentialRule
    public BatchTransformationRule<?, ?> allocateRuleFilteredExample
    

    new() {
        allocateRule = createRule(AllocateTaskToVariant.instance())
            .name("AllocateTaskToVariantRule")
            .action[
                t.variant = RTV
            ]
            .build

        allocateRuleFilteredExample = createRule(AllocateTaskToVariant.instance())
            .name("FilteredAllocateTaskToVariantRule")
            .action[
                t.variant = RTV
            ]
            .filter[
                if (it.RTV.name.equals(BpmnProblems.NOSQL_MEDIUM) ||
                    it.RTV.name.equals(BpmnProblems.SQL_MEDIUM)) {
                    return false;
                }
                return true;
            ]
            .build

        createResourceRule = createRule(CreateResource.instance())
            .name("CreateResourceRule")
            .action[
                RTV.instances += SimplifiedbpmnFactory.eINSTANCE.createResourceInstance()
            ]
            .build

        makeParallelRule = createRule(MakeParallel.instance())
            .name("MakeParallelRule")
            .action[
                val builder = new SimplifiedBpmnBuilder(root)

                val divergingGateway = builder.createParallelGateway(t1, t2, true)
                val convergingGateway = builder.createParallelGateway(t1, t2, false)

                var flows = t1.inFlows
                while (!flows.empty) {
                    val flow = flows.get(0)
                    flow.target = divergingGateway
                }
                flows = t2.outFlows
                while (!flows.empty) {
                    val flow = flows.get(0)
                    flow.source = convergingGateway
                }

                val flow = t1.outFlows.get(0)
                root.sequenceFlows -= flow
                flow.target = null
                flow.source = null

                builder.createFlow(divergingGateway, t1)
                builder.createFlow(divergingGateway, t2)
                builder.createFlow(t1, convergingGateway)
                builder.createFlow(t2, convergingGateway)
            ]
            .build

        makeSequentialRule = createRule(MakeSequential.instance())
            .name("MakeSequentialRule")
            .action[
                var flows = t1.inFlows
                val divergingGateway = flows.get(0).getSource()
                root.parallelGateways.remove(divergingGateway)
                root.sequenceFlows.removeAll(flows)
                flows.clear()
                flows = t2.inFlows
                root.sequenceFlows.removeAll(flows)
                flows.clear()
                flows = t1.outFlows
                root.sequenceFlows.removeAll(flows)
                flows.clear()
                flows = t2.outFlows
                val convergingGateway = flows.get(0).target
                root.parallelGateways.remove(convergingGateway)
                root.sequenceFlows.removeAll(flows)
                flows.clear()

                flows = divergingGateway.inFlows
                while (!flows.empty) {
                    val flow = flows.get(0)
                    flow.target = t1
                }
                flows = convergingGateway.getOutFlows()
                while (!flows.empty) {
                    val flow = flows.get(0)
                    flow.source = t2
                }

                new SimplifiedBpmnBuilder(root).createFlow(t1, t2)
            ]
            .build
    }
}