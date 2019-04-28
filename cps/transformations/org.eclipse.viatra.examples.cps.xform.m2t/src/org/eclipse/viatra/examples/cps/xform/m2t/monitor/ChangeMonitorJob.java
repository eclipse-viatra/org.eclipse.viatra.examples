/*******************************************************************************
 * Copyright (c) 2014-2016 Akos Horvath, Abel Hegedus, Marton Bur, Zoltan Ujhelyi, Robert Doczi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.examples.cps.xform.m2t.monitor;

import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.viatra.examples.cps.deployment.DeploymentApplication;
import org.eclipse.viatra.examples.cps.deployment.DeploymentElement;
import org.eclipse.viatra.examples.cps.deployment.DeploymentHost;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.transformation.evm.api.Activation;
import org.eclipse.viatra.transformation.evm.api.Context;
import org.eclipse.viatra.transformation.evm.specific.crud.CRUDActivationStateEnum;
import org.eclipse.viatra.transformation.evm.specific.job.StatelessJob;

import com.google.common.collect.Maps;

public class ChangeMonitorJob<Match extends IPatternMatch> extends StatelessJob<Match> {

	public static final String OUTDATED_ELEMENTS = "changedDeploymentElements";
	public static final String HOSTS = "deploymentHosts";
	public static final String APPLICATIONS = "deploymentApps";

	public ChangeMonitorJob(
			CRUDActivationStateEnum incQueryActivationStateEnum,
			Consumer<Match> matchProcessor) {
		super(incQueryActivationStateEnum, matchProcessor);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void execute(Activation<? extends Match> activation, Context context) {
		super.execute(activation, context);
		// For update jobs, store the old name
		if(getActivationState().equals(CRUDActivationStateEnum.UPDATED)){
			Map<DeploymentElement, String> map = (Map<DeploymentElement, String>) context.get(OUTDATED_ELEMENTS);
			if (map == null) {
				map = Maps.newHashMap();
				context.put(OUTDATED_ELEMENTS, map);
			}
			DeploymentElement changedElement = (DeploymentElement) activation.getAtom().get(0);
			store(changedElement, context);
		}
		
	}

	@SuppressWarnings("unchecked")
	private void store(DeploymentElement changedElement,Context context) {
		Map<DeploymentElement, String> map = (Map<DeploymentElement, String>) context.get(OUTDATED_ELEMENTS);
		// Store the old data in the values of the map
		if(changedElement instanceof DeploymentHost){
			map.put(changedElement, ((Map<DeploymentHost,String>)context.get(HOSTS)).get(changedElement));						
		}
		else if(changedElement instanceof DeploymentApplication){
			map.put(changedElement, ((Map<DeploymentApplication,String>)context.get(APPLICATIONS)).get(changedElement));						
		}
	}

	@Override
	protected void handleError(Activation<? extends Match> activation, Exception exception, Context context) {
		context.remove(OUTDATED_ELEMENTS);
		super.handleError(activation,exception,context);
	}

}
