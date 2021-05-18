// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class LoopSummaryRefinementStrategy extends PredicateAbstractionRefinementStrategy {

  protected LoopSummaryRefinementStrategy(
      final Configuration config,
      final LogManager logger,
      final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr)
      throws InvalidConfigurationException {
    super(config, logger, pPredAbsMgr, pSolver);
  }

  @Override
  public boolean performRefinement(
      ARGReachedSet pReached,
      List<ARGState> abstractionStatesTrace,
      List<BooleanFormula> pInterpolants,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Doing Refinement");

    return super.performRefinement(
        pReached, abstractionStatesTrace, pInterpolants, pRepeatedCounterexample);
  }
}