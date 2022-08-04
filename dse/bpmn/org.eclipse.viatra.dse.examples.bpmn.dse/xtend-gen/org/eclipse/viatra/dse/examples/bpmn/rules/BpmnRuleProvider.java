/**
 * Copyright (c) 2010-2015, Andras Szabolcs Nagy, Abel Hegedus, Akos Horvath, Zoltan Ujhelyi and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.dse.examples.bpmn.rules;

import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.viatra.dse.examples.bpmn.patterns.AllocateTaskToVariant;
import org.eclipse.viatra.dse.examples.bpmn.patterns.CreateResource;
import org.eclipse.viatra.dse.examples.bpmn.patterns.MakeParallel;
import org.eclipse.viatra.dse.examples.bpmn.patterns.MakeSequential;
import org.eclipse.viatra.dse.examples.bpmn.problems.BpmnProblems;
import org.eclipse.viatra.dse.examples.bpmn.problems.SimplifiedBpmnBuilder;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.BaseElement;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.ParallelGateway;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.ResourceInstance;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.SequenceFlow;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.SimplifiedBPMN;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.SimplifiedbpmnFactory;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.Task;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;
import org.eclipse.viatra.transformation.runtime.emf.rules.batch.BatchTransformationRule;
import org.eclipse.viatra.transformation.runtime.emf.rules.batch.BatchTransformationRuleFactory;
import org.eclipse.xtext.xbase.lib.Extension;

@SuppressWarnings("all")
public class BpmnRuleProvider {
  @Extension
  private BatchTransformationRuleFactory factory = new BatchTransformationRuleFactory();

  public BatchTransformationRule<?, ?> allocateRule;

  public BatchTransformationRule<?, ?> createResourceRule;

  public BatchTransformationRule<?, ?> makeParallelRule;

  public BatchTransformationRule<?, ?> makeSequentialRule;

  public BatchTransformationRule<?, ?> allocateRuleFilteredExample;

  public BpmnRuleProvider() {
    final Consumer<AllocateTaskToVariant.Match> _function = (AllocateTaskToVariant.Match it) -> {
      Task _t = it.getT();
      _t.setVariant(it.getRTV());
    };
    this.allocateRule = this.factory.<AllocateTaskToVariant.Match, AllocateTaskToVariant.Matcher>createRule(AllocateTaskToVariant.instance()).name("AllocateTaskToVariantRule").action(_function).build();
    final Consumer<AllocateTaskToVariant.Match> _function_1 = (AllocateTaskToVariant.Match it) -> {
      Task _t = it.getT();
      _t.setVariant(it.getRTV());
    };
    final EventFilter<AllocateTaskToVariant.Match> _function_2 = (AllocateTaskToVariant.Match it) -> {
      if ((it.getRTV().getName().equals(BpmnProblems.NOSQL_MEDIUM) || 
        it.getRTV().getName().equals(BpmnProblems.SQL_MEDIUM))) {
        return false;
      }
      return true;
    };
    this.allocateRuleFilteredExample = this.factory.<AllocateTaskToVariant.Match, AllocateTaskToVariant.Matcher>createRule(AllocateTaskToVariant.instance()).name("FilteredAllocateTaskToVariantRule").action(_function_1).filter(_function_2).build();
    final Consumer<CreateResource.Match> _function_3 = (CreateResource.Match it) -> {
      EList<ResourceInstance> _instances = it.getRTV().getInstances();
      ResourceInstance _createResourceInstance = SimplifiedbpmnFactory.eINSTANCE.createResourceInstance();
      _instances.add(_createResourceInstance);
    };
    this.createResourceRule = this.factory.<CreateResource.Match, CreateResource.Matcher>createRule(CreateResource.instance()).name("CreateResourceRule").action(_function_3).build();
    final Consumer<MakeParallel.Match> _function_4 = (MakeParallel.Match it) -> {
      SimplifiedBPMN _root = it.getRoot();
      final SimplifiedBpmnBuilder builder = new SimplifiedBpmnBuilder(_root);
      final ParallelGateway divergingGateway = builder.createParallelGateway(it.getT1(), it.getT2(), true);
      final ParallelGateway convergingGateway = builder.createParallelGateway(it.getT1(), it.getT2(), false);
      EList<SequenceFlow> flows = it.getT1().getInFlows();
      while ((!flows.isEmpty())) {
        {
          final SequenceFlow flow = flows.get(0);
          flow.setTarget(divergingGateway);
        }
      }
      flows = it.getT2().getOutFlows();
      while ((!flows.isEmpty())) {
        {
          final SequenceFlow flow = flows.get(0);
          flow.setSource(convergingGateway);
        }
      }
      final SequenceFlow flow = it.getT1().getOutFlows().get(0);
      EList<SequenceFlow> _sequenceFlows = it.getRoot().getSequenceFlows();
      _sequenceFlows.remove(flow);
      flow.setTarget(null);
      flow.setSource(null);
      builder.createFlow(divergingGateway, it.getT1());
      builder.createFlow(divergingGateway, it.getT2());
      builder.createFlow(it.getT1(), convergingGateway);
      builder.createFlow(it.getT2(), convergingGateway);
    };
    this.makeParallelRule = this.factory.<MakeParallel.Match, MakeParallel.Matcher>createRule(MakeParallel.instance()).name("MakeParallelRule").action(_function_4).build();
    final Consumer<MakeSequential.Match> _function_5 = (MakeSequential.Match it) -> {
      EList<SequenceFlow> flows = it.getT1().getInFlows();
      final BaseElement divergingGateway = flows.get(0).getSource();
      it.getRoot().getParallelGateways().remove(divergingGateway);
      it.getRoot().getSequenceFlows().removeAll(flows);
      flows.clear();
      flows = it.getT2().getInFlows();
      it.getRoot().getSequenceFlows().removeAll(flows);
      flows.clear();
      flows = it.getT1().getOutFlows();
      it.getRoot().getSequenceFlows().removeAll(flows);
      flows.clear();
      flows = it.getT2().getOutFlows();
      final BaseElement convergingGateway = flows.get(0).getTarget();
      it.getRoot().getParallelGateways().remove(convergingGateway);
      it.getRoot().getSequenceFlows().removeAll(flows);
      flows.clear();
      flows = divergingGateway.getInFlows();
      while ((!flows.isEmpty())) {
        {
          final SequenceFlow flow = flows.get(0);
          flow.setTarget(it.getT1());
        }
      }
      flows = convergingGateway.getOutFlows();
      while ((!flows.isEmpty())) {
        {
          final SequenceFlow flow = flows.get(0);
          flow.setSource(it.getT2());
        }
      }
      SimplifiedBPMN _root = it.getRoot();
      new SimplifiedBpmnBuilder(_root).createFlow(it.getT1(), it.getT2());
    };
    this.makeSequentialRule = this.factory.<MakeSequential.Match, MakeSequential.Matcher>createRule(MakeSequential.instance()).name("MakeSequentialRule").action(_function_5).build();
  }
}
