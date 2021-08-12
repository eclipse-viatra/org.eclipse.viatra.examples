/*******************************************************************************
 * Copyright (c) 2014-2016 Akos Horvath, Abel Hegedus, Tamas Borbas, Marton Bur, Zoltan Ujhelyi, Robert Doczi, Daniel Segesdi, Peter Lunk, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.examples.cps.xform.m2m.launcher;

import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.CyberPhysicalSystemPackage;
import org.eclipse.viatra.examples.cps.deployment.DeploymentPackage;
import org.eclipse.viatra.examples.cps.traceability.TraceabilityPackage;
import org.eclipse.viatra.query.runtime.emf.types.EClassTransitiveInstancesKey;
import org.eclipse.viatra.query.runtime.emf.types.EStructuralFeatureInstancesKey;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchHints;
import org.eclipse.viatra.query.runtime.localsearch.planner.cost.IConstraintEvaluationContext;
import org.eclipse.viatra.query.runtime.localsearch.planner.cost.impl.StatisticsBasedConstraintCostFunction;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.matchers.util.Accuracy;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteBackendFactory;

import com.google.common.collect.Maps;

public enum TransformationType {
    BATCH_SIMPLE {
        public CPSTransformationWrapper getWrapper() {return new BatchSimple();}
    },
    BATCH_OPTIMIZED {
        public CPSTransformationWrapper getWrapper() {return new BatchOptimized();}
    },
    BATCH_VIATRA_QUERY_RETE {
    	    public CPSTransformationWrapper getWrapper() {
    		    QueryEvaluationHint hint = new QueryEvaluationHint(null, ReteBackendFactory.INSTANCE);
    		    return new BatchQueryOnly(hint, hint);
	    }
    },
    BATCH_VIATRA_QUERY_LOCAL_SEARCH {
        public CPSTransformationWrapper getWrapper() {
			QueryEvaluationHint hint = LocalSearchHints.getDefaultFlatten().build();
			EndOfTransformationCostFunction costFunction = new EndOfTransformationCostFunction(StatisticsBasedConstraintCostFunction.INVERSE_NAVIGATION_PENALTY_DEFAULT);
			QueryEvaluationHint traceHint = LocalSearchHints.getDefaultFlatten().setCostFunction(costFunction).build();
			return new BatchQueryLocalSearch(hint, traceHint);
	    }
    },
    BATCH_VIATRA_QUERY_LOCAL_SEARCH_GENERIC {
        public CPSTransformationWrapper getWrapper() {
            EndOfTransformationCostFunction costFunction = new EndOfTransformationCostFunction(StatisticsBasedConstraintCostFunction.INVERSE_NAVIGATION_PENALTY_GENERIC);
			QueryEvaluationHint hint = LocalSearchHints.getDefaultGeneric().setCostFunction(costFunction).build();
            return new BatchQueryLocalSearch(hint, hint);
        }
    },
    BATCH_VIATRA_QUERY_LOCAL_SEARCH_WO_INDEXER {
        public CPSTransformationWrapper getWrapper() {
            QueryEvaluationHint hint = LocalSearchHints.getDefaultNoBase().build();
			return new BatchQueryLocalSearch(hint, hint);
        }
    },
    BATCH_VIATRA_TRANSFORMATION {
        public CPSTransformationWrapper getWrapper() {return new BatchViatra();}

        @Override
        public boolean isDebuggable() {
            return true;
        }
    },
    INCR_VIATRA_QUERY_RESULT_TRACEABILITY {
        public CPSTransformationWrapper getWrapper() {return new QueryResultTraceability();}
    },
    INCR_VIATRA_EXPLICIT_TRACEABILITY {
        public CPSTransformationWrapper getWrapper() {return new ExplicitTraceability();}
    },
    INCR_VIATRA_AGGREGATED {
        public CPSTransformationWrapper getWrapper() {return new PartialBatch();}
    },
    INCR_VIATRA_TRANSFORMATION {
        public CPSTransformationWrapper getWrapper() {return new ViatraTransformation();}

        @Override
        public boolean isDebuggable() {
            return true;
        }
    
    },
    INCR_VIATRA_PUREGRATRA {
        public CPSTransformationWrapper getWrapper() {return new IncrPureGraTra();}
    };

    private final class EndOfTransformationCostFunction extends StatisticsBasedConstraintCostFunction {
        final Map<IInputKey, IInputKey> substitutions;

        public EndOfTransformationCostFunction(double inverseNavigationPenalty) {
            super(inverseNavigationPenalty);
            substitutions = Maps.newHashMap();
            substitutions.put(new EClassTransitiveInstancesKey(TraceabilityPackage.Literals.CPS2_DEPLOYMENT_TRACE), new EClassTransitiveInstancesKey(CyberPhysicalSystemPackage.Literals.IDENTIFIABLE));
            substitutions.put(new EClassTransitiveInstancesKey(DeploymentPackage.Literals.DEPLOYMENT_ELEMENT), new EClassTransitiveInstancesKey(CyberPhysicalSystemPackage.Literals.IDENTIFIABLE));
            substitutions.put(new EStructuralFeatureInstancesKey(TraceabilityPackage.Literals.CPS2_DEPLOYMENT_TRACE__CPS_ELEMENTS), new EClassTransitiveInstancesKey(CyberPhysicalSystemPackage.Literals.IDENTIFIABLE));
            substitutions.put(new EStructuralFeatureInstancesKey(TraceabilityPackage.Literals.CPS2_DEPLOYMENT_TRACE__DEPLOYMENT_ELEMENTS), new EClassTransitiveInstancesKey(CyberPhysicalSystemPackage.Literals.IDENTIFIABLE));
            substitutions.put(new EStructuralFeatureInstancesKey(TraceabilityPackage.Literals.CPS_TO_DEPLOYMENT__TRACES), new EClassTransitiveInstancesKey(CyberPhysicalSystemPackage.Literals.IDENTIFIABLE));
        }

        @Override
        public Optional<Long> projectionSize(IConstraintEvaluationContext input, IInputKey supplierKey, TupleMask groupMask, Accuracy requiredAccuracy) {
            if (supplierKey instanceof EClassTransitiveInstancesKey){
                EClass eclass = ((EClassTransitiveInstancesKey) supplierKey).getEmfKey();
                if (TraceabilityPackage.Literals.CPS_TO_DEPLOYMENT.equals(eclass)){
                    return Optional.of(1L);
                }
            }
            if (substitutions.containsKey(supplierKey)){
            	IInputKey substituteType = substitutions.get(supplierKey);
            	if (supplierKey.getArity() == substituteType.getArity()) 
            		return input.getRuntimeContext().estimateCardinality(substituteType, groupMask, requiredAccuracy);
            	// problem: the original and substitute types may have different arity, difficult to interpolate masks
				Optional<Long> totalCount = input.getRuntimeContext().estimateCardinality(substituteType, TupleMask.identity(substituteType.getArity()), requiredAccuracy);
				Optional<Long> existence = input.getRuntimeContext().estimateCardinality(substituteType, TupleMask.empty(substituteType.getArity()), requiredAccuracy);
				if (groupMask.getSize() == groupMask.sourceWidth) return totalCount;
				if (groupMask.getSize() == 0) return existence;
				return totalCount.flatMap((total) -> 
					existence.map((exists) -> 
						// approximate with linear map
						(groupMask.getSize() * (total - exists))/groupMask.sourceWidth + exists
					));
            }
            
        	return input.getRuntimeContext().estimateCardinality(supplierKey, groupMask, requiredAccuracy);
        }

    }

    public abstract CPSTransformationWrapper getWrapper();

    public boolean isDebuggable() {
        return false;
    }
}
