/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

/**
 * Abstract class for the refinement strategy that should be used after a spurious
 * counterexample has been found and interpolants were computed.
 *
 * Instances of this interface get the path, the reached set, and the interpolants,
 * and shall update the ARG/the reached set accordingly.
 *
 * This class implements the general structure of refining a path with interpolants,
 * but delegates the actual updates to states, precisions, and ARG to its subclasses.
 */
public abstract class RefinementStrategy {

  private StatInt differentNontrivialInterpolants = new StatInt(StatKind.SUM, "Different non-trivial interpolants along paths");
  private StatInt equalNontrivialInterpolants = new StatInt(StatKind.SUM, "Equal non-trivial interpolants along paths");

  private StatInt truePathPrefixStates = new StatInt(StatKind.SUM, "Length (states) of path with itp 'true'");
  private StatInt nonTrivialPathStates = new StatInt(StatKind.SUM, "Length (states) of path with itp non-trivial itp");
  private StatInt falsePathSuffixStates = new StatInt(StatKind.SUM, "Length (states) of path with itp 'false'");

  private StatInt equalPrecisionsOnPaths = new StatInt(StatKind.SUM, "Equal precisions along paths");
  private StatInt differentPrecisionsOnPaths = new StatInt(StatKind.SUM, "Different precisions along paths");

  private StatInt numberOfAffectedStates = new StatInt(StatKind.SUM, "Number of affected states");
  private StatInt totalPathLengthToInfeasibility = new StatInt(StatKind.AVG, "Length of refined path (in blocks)");

  protected AbstractStatistics basicRefinementStatistics = new AbstractStatistics() {
    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      beginLeveledOutput(out)
        .put(totalPathLengthToInfeasibility)
        .put(numberOfAffectedStates)
        .put(truePathPrefixStates)
        .put(nonTrivialPathStates)
        .put(falsePathSuffixStates)
        .put(differentNontrivialInterpolants)
        .put(equalNontrivialInterpolants)
        .put(differentPrecisionsOnPaths)
        .put(equalPrecisionsOnPaths);
    }
  };

  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  public RefinementStrategy(BooleanFormulaManagerView pBfmgr, Solver pSolver) {
    bfmgr = pBfmgr;
    solver = pSolver;
  }

  public boolean needsInterpolants() {
    return true;
  }

  private void analyzePathPrecisions(ARGReachedSet argReached, List<ARGState> path) {
    int equalPrecisions = 0;
    int differentPrecisions = 0;

    UnmodifiableReachedSet reached = argReached.asReachedSet();
    PredicatePrecision lastPaPrec = null;
    for (ARGState state : path) {
      Precision prec = reached.getPrecision(state);
      PredicatePrecision paPrec = Precisions.extractPrecisionByType(prec, PredicatePrecision.class);
      if (lastPaPrec != null) {
        if (lastPaPrec.equals(paPrec)) {
          equalPrecisions++;
        } else {
          differentPrecisions++;
        }
      }
      lastPaPrec = paPrec;
    }

    equalPrecisionsOnPaths.setNextValue(equalPrecisions);
    differentPrecisionsOnPaths.setNextValue(differentPrecisions);
  }


  public void performRefinement(ARGReachedSet pReached, List<ARGState> path,
      List<BooleanFormula> pInterpolants, boolean pRepeatedCounterexample) throws CPAException {
    // Hook
    startRefinementOfPath();

    // The last state along the path is the target (error) state
    ARGState lastElement = path.get(path.size()-1);
    assert lastElement.isTarget();

    // Skip the last element of the path, itp is always false there
    path = path.subList(0, path.size()-1);
    assert pInterpolants.size() ==  path.size();

    List<ARGState> changedElements = new ArrayList<>();
    ARGState infeasiblePartOfART = lastElement;
    boolean previousItpWasTrue = true;

    // Statistics on the current refinement
    int truePrefixStates = 0;
    int nonTrivialStates = 0;
    int falseSuffixStates = 0;
    int differentNontrivialItps = 0;
    int equalNontrivialItps = 0;
    int pathLengthToInfeasibility = 0;

    BooleanFormula lastItp = null;

    // Traverse the path
    for (Pair<BooleanFormula, ARGState> interpolationPoint : Pair.zipList(pInterpolants, path)) {
      pathLengthToInfeasibility++;
      BooleanFormula itp = interpolationPoint.getFirst();
      ARGState w = interpolationPoint.getSecond();

      // ...
      if (bfmgr.isTrue(itp)) {
        // do nothing
        truePrefixStates++;
        previousItpWasTrue =  true;
        continue;
      }

      if (bfmgr.isFalse(itp)) {
        // we have reached the part of the path that is infeasible
        falseSuffixStates++;
        infeasiblePartOfART = w;
        if (previousItpWasTrue) {
          // If the previous itp was true, and the current one is false,
          // this means that the code block between them is in itself infeasible.
          // We can add this information to the cache to speed up later sat checks.
          PredicateAbstractState s = getPredicateState(w);
          BooleanFormula blockFormula = s.getAbstractionFormula().getBlockFormula().getFormula();
          solver.addUnsatisfiableFormulaToCache(blockFormula);
        }
        break;
      }

      // Compare non-trivial interpolants along path
      if (lastItp != null) {
        if (lastItp.equals(itp)) {
          equalNontrivialItps++;
        } else {
          differentNontrivialItps++;
        }
      }
      lastItp = itp;

      nonTrivialStates++;
      previousItpWasTrue = false;

      if (!performRefinementForState(itp, w)) {
        changedElements.add(w);
      }
    }
    if (infeasiblePartOfART == lastElement) {
      pathLengthToInfeasibility++;
    }
    numberOfAffectedStates.setNextValue(changedElements.size());

    if (changedElements.isEmpty() && pRepeatedCounterexample) {
      // TODO One cause for this exception is that the CPAAlgorithm sometimes
      // re-adds the parent of the error element to the waitlist, and thus the
      // error element would get re-discovered immediately again.
      // Currently the CPAAlgorithm does this only when there are siblings of
      // the target state, which should rarely happen.
      // We still need a better handling for this situation.
      throw new RefinementFailedException(RefinementFailedException.Reason.RepeatedCounterexample, null);
    }

    analyzePathPrecisions(pReached, path);

    // Hook
    finishRefinementOfPath(infeasiblePartOfART, changedElements, pReached, pRepeatedCounterexample);

    // Update global statistics
    truePathPrefixStates.setNextValue(truePrefixStates);
    nonTrivialPathStates.setNextValue(nonTrivialStates);
    falsePathSuffixStates.setNextValue(falseSuffixStates);
    differentNontrivialInterpolants.setNextValue(differentNontrivialItps);
    equalNontrivialInterpolants.setNextValue(equalNontrivialItps);
    totalPathLengthToInfeasibility.setNextValue(pathLengthToInfeasibility);

    assert !pReached.asReachedSet().contains(lastElement);
  }

  protected abstract void startRefinementOfPath();

  /**
   * Perform refinement on one state given the interpolant that was determined
   * by the solver for this state. This method is only called for states for
   * which there is a non-trivial interpolant (i.e., neither True nor False).
   * @param interpolant The interpolant.
   * @param state The state.
   * @return True if no refinement was necessary (this implies that refinement
   *          on all of the state's parents is also not necessary)
   */
  protected abstract boolean performRefinementForState(BooleanFormula interpolant, ARGState state);

  /**
   * Do any necessary work after one path has been refined.
   *
   * @param unreachableState The first state in the path which is infeasible (this identifies the path).
   * @param affectedStates The list of states that were affected by the refinement (ordered from root to target state).
   * @param reached The reached set.
   * @param repeatedCounterexample Whether the counterexample has been found before.
   * @throws CPAException
   */
  protected abstract void finishRefinementOfPath(final ARGState unreachableState,
      List<ARGState> affectedStates, ARGReachedSet reached,
      boolean repeatedCounterexample) throws CPAException;

  public abstract Statistics getStatistics();
}
