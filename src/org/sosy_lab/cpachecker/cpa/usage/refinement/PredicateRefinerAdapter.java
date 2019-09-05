/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.predicate.BAMBlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class PredicateRefinerAdapter extends GenericSinglePathRefiner {
  ARGBasedRefiner refiner;
  LogManager logger;

  private final UsageStatisticsRefinementStrategy strategy;
  private ARGReachedSet ARGReached;

  // Statistics
  private StatTimer externalRefinement = new StatTimer("Time for predicate refinement");
  private StatCounter solverFailures = new StatCounter("Solver failures");
  // Number of refined and repeated paths are calculated in generic refiner
  private StatCounter numberOfBAMupdates = new StatCounter("Number of BAM updates");

  public PredicateRefinerAdapter(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> wrapper,
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger)
      throws InvalidConfigurationException {
    super(wrapper);

    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(BAMPredicateRefiner.class.getSimpleName() + " could not find the PredicateCPA");
    }
    logger = pLogger;

    @SuppressWarnings("resource")
    BAMPredicateCPA bamPredicateCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(BAMPredicateCPA.class);
    PredicateCPA predicateCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(PredicateCPA.class);

    boolean withBAM = bamPredicateCpa != null;
    predicateCpa = (withBAM) ? bamPredicateCpa : predicateCpa;
    assert predicateCpa != null;

    BlockFormulaStrategy blockFormulaStrategy;

    if (withBAM) {
      strategy =
          new UsageStatisticsRefinementStrategy(
              bamPredicateCpa.getConfiguration(),
              logger,
              bamPredicateCpa.getSolver(),
              bamPredicateCpa.getPredicateManager(),
              s -> ((BackwardARGState) s).getARGState());
      PathFormulaManager pfmgr = predicateCpa.getPathFormulaManager();
      blockFormulaStrategy = new BAMBlockFormulaStrategy(pfmgr);
    } else {
      strategy =
          new UsageStatisticsRefinementStrategy(
              predicateCpa.getConfiguration(),
              logger,
              predicateCpa.getSolver(),
              predicateCpa.getPredicateManager(),
              s -> s);
      blockFormulaStrategy = new BlockFormulaStrategy();
    }

    refiner = new PredicateCPARefinerFactory(pCpa)
        .setBlockFormulaStrategy(blockFormulaStrategy)
            .create(strategy);
  }

  @Override
  public RefinementResult call(ExtendedARGPath pInput) throws CPAException, InterruptedException {
    RefinementResult result;

    /*
     * if (!totalARGCleaning) {
     * subtreesRemover.addStateForRemoving((ARGState)target.getKeyState()); for (ARGState state :
     * strategy.lastAffectedStates) { subtreesRemover.addStateForRemoving(state); } }
     */
    try {
      externalRefinement.start();
      CounterexampleInfo cex = refiner.performRefinementForPath(ARGReached, pInput);
      externalRefinement.stop();

      if (!cex.isSpurious()) {
        result = RefinementResult.createTrue();
      } else {
        result = RefinementResult.createFalse();
        List<ARGState> affectedStates = strategy.getLastAffectedStates();
        if (!affectedStates.isEmpty()) {
          // it may be, if there are no valuable interpolants: ..., true, false, ...
          result.addInfo(PredicateRefinerAdapter.class, affectedStates);
          PredicatePrecision lastPrecision = strategy.getLastPrecision();
          assert (lastPrecision != null);
          result.addPrecision(lastPrecision);
        }
      }

    } catch (IllegalStateException e) {
      // msat_solver return -1 <=> unknown
      // consider its as true;
      logger.log(Level.WARNING, "Solver exception: " + e.getMessage());
      solverFailures.inc();
      externalRefinement.stop();
      result = RefinementResult.createTrue();
    } catch (RefinementFailedException e) {
      logger.log(Level.WARNING, "Path is repeated, BAM is looped");
      pInput.getUsageInfo().setAsLooped();
      externalRefinement.stop();
      result = RefinementResult.createTrue();
    }
    return result;
  }

  @Override
  protected void handleUpdateSignal(Class<? extends RefinementInterface> pCallerClass, Object pData) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      if (pData instanceof ReachedSet) {
        //Updating new reached set
        updateReachedSet((ReachedSet)pData);
      }
    }
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> pCallerClass) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      ARGReached = null;
      strategy.flush();
    }
  }

  @Override
  protected void printAdditionalStatistics(StatisticsWriter pOut) {
    pOut.put(externalRefinement)
        .put(solverFailures)
        .put(numberOfBAMupdates);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStats) {
    if (refiner instanceof StatisticsProvider) {
      ((StatisticsProvider)refiner).collectStatistics(pStats);
    }
    super.collectStatistics(pStats);
  }

  private void updateReachedSet(ReachedSet pReached) {
    ARGReached = new ARGReachedSet(pReached);
  }

  protected static class UsageStatisticsRefinementStrategy
      extends PredicateAbstractionRefinementStrategy {

    private List<ARGState> lastAffectedStates = new ArrayList<>();
    private PredicatePrecision lastAddedPrecision;
    private Function<ARGState, ARGState> transformer;

    public UsageStatisticsRefinementStrategy(
        final Configuration config,
        final LogManager logger,
        final Solver pSolver,
        final PredicateAbstractionManager pPredAbsMgr, Function<ARGState, ARGState> pTransformer) throws InvalidConfigurationException {
      super(config, logger, pPredAbsMgr, pSolver);
      transformer = pTransformer;
    }

    @Override
    protected void finishRefinementOfPath(
            ARGState pUnreachableState,
            List<ARGState> pAffectedStates,
            ARGReachedSet pReached,
            List<ARGState> abstractionStatesTrace,
            boolean pRepeatedCounterexample)
        throws CPAException, InterruptedException {

      super.finishRefinementOfPath(
          pUnreachableState,
          pAffectedStates,
          pReached,
          abstractionStatesTrace,
          pRepeatedCounterexample);

        lastAffectedStates.clear();
        from(pAffectedStates)
          .transform(transformer)
            .forEach(lastAffectedStates::add);
    }

    @Override
    protected PredicatePrecision addPredicatesToPrecision(PredicatePrecision basePrecision) {
      PredicatePrecision newPrecision = super.addPredicatesToPrecision(basePrecision);
      lastAddedPrecision = (PredicatePrecision) newPrecision.subtract(basePrecision);
      return newPrecision;
    }

    @Override
    protected void updateARG(PredicatePrecision pNewPrecision, ARGState pRefinementRoot, ARGReachedSet pReached) throws InterruptedException {
      //Do not update ARG for race analysis
    }

    @Override
    public List<ARGState> filterAbstractionStates(ARGPath pPath) {
      List<ARGState> result =
          from(pPath.asStatesList())
              .skip(1)
              .filter(PredicateAbstractState.CONTAINS_ABSTRACTION_STATE)
              .toList();

      if (pPath.getLastState() != result.get(result.size() - 1)) {
        List<ARGState> newResult = new ArrayList<>(result);
        newResult.add(pPath.getLastState());
        return newResult;
      }
      return result;
    }

    public List<ARGState> getLastAffectedStates() {
      return lastAffectedStates;
    }

    public PredicatePrecision getLastPrecision() {
      return lastAddedPrecision;
    }

    public void flush() {
      lastAddedPrecision = null;
      lastAffectedStates.clear();
    }
  }
}
