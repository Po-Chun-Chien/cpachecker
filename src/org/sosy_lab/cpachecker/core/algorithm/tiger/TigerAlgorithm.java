/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.MainCPAStatistics;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.ClusteringCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.ClusteredElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ARTReuse;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.PrecisionCallback;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorkerRunnable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WorklistEntryComparator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.WrapperUtil;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.bdd.BDDCPA;
import org.sosy_lab.cpachecker.cpa.bdd.BDDState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

@Options(prefix = "tiger")
public class TigerAlgorithm implements Algorithm, PrecisionCallback<PredicatePrecision>, StatisticsProvider, Statistics {

  public static String originalMainFunction = null;

  @Option(name = "fqlQuery", description = "Coverage criterion given as an FQL query")
  private String fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE; // default is basic block coverage

  @Option(name = "optimizeGoalAutomata", description = "Optimize the test goal automata")
  private boolean optimizeGoalAutomata = true;

  @Option(name = "printARGperGoal", description = "Print the ARG for each test goal")
  private boolean printARGperGoal = false;

  @Option(name = "checkCoverage", description = "Checks whether a test case for one goal covers another test goal")
  private boolean checkCoverage = true;

  @Option(name = "reuseARG", description = "Reuse ARG across test goals")
  private boolean reuseARG = true;

  @Option(name = "reusePredicates", description = "Reuse predicates across modifications of an ARG.")
  private boolean reusePredicates = true;

  @Option(name = "testsuiteFile", description = "Filename for output of generated test suite")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testsuiteFile = Paths.get("testsuite.txt");

  @Option(name = "useInfeasibilityPropagation", description = "Map information on infeasibility of one test goal to other test goals.")
  private boolean useInfeasibilityPropagation = false;

  enum TimeoutStrategy {
    SKIP_AFTER_TIMEOUT,
    RETRY_AFTER_TIMEOUT
  }

  @Option(name = "timeoutStrategy", description = "How to proceed with timed-out goals if some time remains after processing all other goals.")
  private TimeoutStrategy timeoutStrategy = TimeoutStrategy.SKIP_AFTER_TIMEOUT;

  @Option(name = "limitsPerGoal.time.cpu.increment", description = "Value for which timeout gets incremented if timed-out goals are re-processed.")
  private int timeoutIncrement = 0;

  /*@Option(name = "globalCoverageCheckBeforeTimeout", description = "Perform a coverage check on all remaining coverage goals before the global time out happens.")
  private boolean globalCoverageCheckBeforeTimeout = false;

  @Option(name = "timeForGlobalCoverageCheck", description = "Time budget for coverage check before global time out.")
  private String timeForGlobalCoverageCheck = "0s";*/

  @Option(name = "limitsPerGoal.time.cpu", description = "Time limit per test goal in seconds (-1 for infinity).")
  private long cpuTimelimitPerGoal = -1;

  @Option(name = "inverseOrder", description = "Inverses the order of test goals each time a new round of re-processing of timed-out goals begins.")
  private boolean inverseOrder = true;

  @Option(name = "useOrder", description = "Enforce the original order each time a new round of re-processing of timed-out goals begins.")
  private boolean useOrder = true;

  @Option(name = "algorithmConfigurationFile", description = "Configuration file for internal cpa algorithm.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path algorithmConfigurationFile = Paths.get("config/tiger-internal-algorithm.properties");

  private LogManager logger;
  private StartupConfig startupConfig;

  private ConfigurableProgramAnalysis cpa;
  private CFA cfa;

  private CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private FQLSpecification fqlSpecification;

  private Wrapper wrapper;
  private GuardedEdgeLabel mAlphaLabel;
  private GuardedEdgeLabel mOmegaLabel;
  private InverseGuardedEdgeLabel mInverseAlphaLabel;

  private TestSuite testsuite;
  private ReachedSet reachedSet = null;
  private ReachedSet outsideReachedSet = null;

  private PredicatePrecision reusedPrecision = null;

  private int statistics_numberOfTestGoals;
  private int statistics_numberOfProcessedTestGoals = 0;
  private int statistics_numberOfMCCalls = 0;

  private Prediction[] lGoalPrediction;

  private String programDenotation;
  private MainCPAStatistics stats;

  NamedRegionManager bddCpaNamedRegionManager = null;

  public TigerAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, ShutdownNotifier pShutdownNotifier,
      CFA pCfa, Configuration pConfig, LogManager pLogger, String programDenotation, @Nullable final MainCPAStatistics stats) throws InvalidConfigurationException {

    this.programDenotation = programDenotation;
    this.stats = stats;

    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    startupConfig.getConfig().inject(this);

    logger = pLogger;

    cpa = pCpa;
    cfa = pCfa;

    if (cpa instanceof WrapperCPA) {
      BDDCPA bddcpa = ((WrapperCPA)cpa).retrieveWrappedCpa(BDDCPA.class);
      if (bddcpa != null) {
        bddCpaNamedRegionManager = bddcpa.getManager();
      } else {
        throw new InvalidConfigurationException("CPAtiger-variability-aware started without BDDCPA. We need BDDCPA!");
      }
    } else if (cpa instanceof BDDCPA) {
      bddCpaNamedRegionManager = ((BDDCPA)cpa).getManager();
    }

    testsuite = new TestSuite(bddCpaNamedRegionManager);


    assert originalMainFunction != null;
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(pCfa.getFunctionHead(originalMainFunction));


    wrapper = new Wrapper(pCfa, originalMainFunction);

    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(wrapper.getOmegaEdge()));


    // get internal representation of FQL query
    logger.logf(Level.INFO, "FQL query string: %s", fqlQuery);

    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(fqlQuery);

    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());

    // TODO fix this restriction
    if (fqlSpecification.hasPassingClause()) {
      logger.logf(Level.SEVERE, "No PASSING clauses supported at the moment!");

      throw new InvalidConfigurationException("No PASSING clauses supported at the moment!");
    }

    // TODO fix this restriction
    if (fqlSpecification.hasPredicate()) {
      logger.logf(Level.SEVERE, "No predicates in FQL queries supported at the moment!");

      throw new InvalidConfigurationException("No predicates in FQL queries supported at the moment!");
    }
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
  PredicatedAnalysisPropertyViolationException {

    // we empty pReachedSet to stop complaints of an incomplete analysis
    // Problem: pReachedSet does not match the internal CPA structure!
    logger.logf(Level.INFO, "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");
    outsideReachedSet = pReachedSet;
    outsideReachedSet.clear();


    // Optimization: Infeasibility propagation
    Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation;

    if (useInfeasibilityPropagation) {
      lInfeasibilityPropagation = InfeasibilityPropagation.canApplyInfeasibilityPropagation(fqlSpecification);
    }
    else {
      lInfeasibilityPropagation = Pair.of(Boolean.FALSE, null);
    }

    LinkedList<ElementaryCoveragePattern> goalPatterns;

    if (lInfeasibilityPropagation.getFirst()) {
      goalPatterns = extractTestGoalPatterns_InfeasibilityPropagation(fqlSpecification, lInfeasibilityPropagation.getSecond());

      lGoalPrediction = new Prediction[statistics_numberOfTestGoals];

      for (int i = 0; i < statistics_numberOfTestGoals; i++) {
        lGoalPrediction[i] = Prediction.UNKNOWN;
      }
    }
    else {
      // (ii) translate query into set of test goals
      // I didn't move this operation to the constructor since it is a potentially expensive operation.
      goalPatterns = extractTestGoalPatterns(fqlSpecification);
      // each test goal needs to be covered in all (if possible) products.
      // Therefore we add a "todo" presence-condition TRUE to each test goal
      // it is the "maximum" set of products for which we try to cover this goal (could be useful to limit this set if we have feature models?)
      lGoalPrediction = null;
    }
    LinkedList<Pair<ElementaryCoveragePattern, Region>> goalsToCover = new LinkedList<>();
    for (int i = 0; i < goalPatterns.size(); i++) {
      goalsToCover.add(Pair.of(goalPatterns.get(i), bddCpaNamedRegionManager.makeTrue()));
    }

    // (iii) do test generation for test goals ...
    boolean wasSound = true;
    if (!testGeneration(goalsToCover, lInfeasibilityPropagation)) {
      logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
      wasSound = false;
    }

    assert(goalsToCover.isEmpty());
    /* removed by andreas
    // write generated test suite and mapping to file system
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testsuiteFile), "utf-8"))) {
      writer.write(testsuite.toString());
      writer.close();
    } catch (IOException e){
      throw new RuntimeException(e);
    }
     */
    return wasSound;
  }

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns_InfeasibilityPropagation(FQLSpecification pFQLQuery, LinkedList<Edges> pEdges) {
    logger.logf(Level.INFO, "Extracting test goals.");

    CFANode lInitialNode = this.mAlphaLabel.getEdgeSet().iterator().next().getSuccessor();
    ClusteringCoverageSpecificationTranslator lTranslator = new ClusteringCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator, pEdges, lInitialNode);

    ElementaryCoveragePattern[] lGoalPatterns = lTranslator.createElementaryCoveragePatternsAndClusters();
    statistics_numberOfTestGoals = lGoalPatterns.length;

    logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);

    LinkedList<ElementaryCoveragePattern> goalPatterns = new LinkedList<>();

    for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
      goalPatterns.add(lGoalPatterns[lGoalIndex]);
    }

    return goalPatterns;
  }

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns(FQLSpecification pFQLQuery) {
    logger.logf(Level.INFO, "Extracting test goals.");


    // TODO check for (temporarily) unsupported features

    // TODO enable use of infeasibility propagation


    IncrementalCoverageSpecificationTranslator lTranslator = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);

    statistics_numberOfTestGoals = lTranslator.getNumberOfTestGoals(pFQLQuery.getCoverageSpecification());
    logger.logf(Level.INFO, "Number of test goals: %d", statistics_numberOfTestGoals);

    Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(pFQLQuery.getCoverageSpecification());
    LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();

    for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
      lGoalPatterns.add(lGoalIterator.next());
    }

    return lGoalPatterns;
  }

  private boolean isCovered(int goalIndex, Goal lGoal) {
    Region remainingPCforGoalCoverage=lGoal.getPresenceCondition();
    boolean isFullyCovered=false;
    for (TestCase testcase : testsuite.getTestCases()) {
      ThreeValuedAnswer isCovered = TigerAlgorithm.accepts(lGoal.getAutomaton(), testcase.getPath().asEdgesList());

      if (isCovered.equals(ThreeValuedAnswer.UNKNOWN)) {
        logger.logf(Level.WARNING, "Coverage check for goal %d could not be performed in a precise way!", goalIndex);
        continue;
      }
      else if (isCovered.equals(ThreeValuedAnswer.REJECT)) {
        continue;
      }

      // the critical edge of the goal might be covered by a different presence condition
      CFAEdge criticalEdge = lGoal.getCriticalEdge();
      Region coveringRegion = testcase.getRegion(); // TODO remove this assignment

      Iterator<Pair<ARGState, CFAEdge>> it = testcase.getPath().descendingIterator();

      while (it.hasNext()) {
        Pair<ARGState, CFAEdge> p = it.next();

        if (p.getSecond().equals(criticalEdge)) {
          BDDState state = getWrappedBDDState(p.getFirst());
          coveringRegion = state.getRegion();

          break;
        }
      }

      if (!bddCpaNamedRegionManager.makeAnd(lGoal.getPresenceCondition(), coveringRegion).isFalse()) { // configurations in testGoalPCtoCover and testcase.pc have a non-empty intersection
        // test goal is already (at least for some PCs) covered by an existing test case
        // remove those PCs from todo
        remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeAnd(remainingPCforGoalCoverage, bddCpaNamedRegionManager.makeNot(coveringRegion));
        lGoal.setPresenceCondition(remainingPCforGoalCoverage);

        testsuite.addTestCase(testcase, lGoal);

        if (remainingPCforGoalCoverage.isFalse()) {
          logger.logf(Level.INFO, "Test goal %d is already fully covered by an existing test case.", goalIndex);
          isFullyCovered = true;
          break;
        } else {
          logger.logf(Level.INFO, "Test goal %d is already partly covered by an existing test case.", goalIndex , " Remaining PC: ", bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
        }
      }
    }
    return isFullyCovered;
  }

  private boolean testGeneration(LinkedList<Pair<ElementaryCoveragePattern,Region>> pTestGoalPatterns, Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation) throws CPAException, InterruptedException {
    boolean wasSound = true;

    int goalIndex = 0;
    int numberOfTestGoals = pTestGoalPatterns.size();

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> previousAutomaton = null;

    while (!pTestGoalPatterns.isEmpty()) {
      statistics_numberOfProcessedTestGoals++;

      Pair<ElementaryCoveragePattern,Region> lTestGoalPatternWithRegion = pTestGoalPatterns.poll();
      ElementaryCoveragePattern lTestGoalPattern = lTestGoalPatternWithRegion.getFirst();
      // the condition identifying configurations that we want to cover (gets reduced in due process until only an infeasible/non-coverable condition remains)
      Region remainingPCforGoalCoverage = lTestGoalPatternWithRegion.getSecond();

      goalIndex++;

      Boolean stop = false;

      while (!stop && !remainingPCforGoalCoverage.isFalse()) {
        logger.logf(Level.INFO, "Processing test goal %d of %d for PC %s.", goalIndex, numberOfTestGoals, bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));

        Goal lGoal = constructGoal(goalIndex, lTestGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,  optimizeGoalAutomata, remainingPCforGoalCoverage);

        if (lGoalPrediction != null && lGoalPrediction[goalIndex - 1] == Prediction.INFEASIBLE) {
          // GoalPrediction does not use the target presence condition (remainingPCforGoalCoverage)
          // I think this is OK (any infeasible goal will be even more infeasible when restricted with a certain pc)
          // TODO: remainingPCforGoalCoverage could perhaps be used to improve precision of the prediction?
          logger.logf(Level.INFO, "This goal is predicted as infeasible!");
          testsuite.addInfeasibleGoal(lGoal, remainingPCforGoalCoverage);
          stop = true;
          continue;
        }

        NondeterministicFiniteAutomaton<GuardedEdgeLabel> currentAutomaton = lGoal.getAutomaton();

        if (ARTReuse.isDegeneratedAutomaton(currentAutomaton)) {
          // current goal is for sure infeasible
          logger.logf(Level.INFO, "Test goal infeasible.");
          testsuite.addInfeasibleGoal(lGoal, remainingPCforGoalCoverage);
          if (lGoalPrediction != null) {
            lGoalPrediction[goalIndex - 1] = Prediction.INFEASIBLE;
          }
          // update PC coverage todo
          if (bddCpaNamedRegionManager.entails(testsuite.getInfeasibleGoals().get(lGoal), remainingPCforGoalCoverage)) {
            // 1st condition: this goal is infeasible for some constraint
            // 2nd condition: remainingPCforGoalCoverage is part of this constraint (implied by this constraint)
            logger.logf(Level.WARNING, "Goal %d is infeasible for remaining PC %s !", goalIndex, bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
            remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeFalse();
          } else {
            // now we need to cover all remaining configurations
            // the remaining configs are represented by the negation of the already covered pcs (in conjunction with the previous testGoalPCtoCover)
            remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeAnd(remainingPCforGoalCoverage, bddCpaNamedRegionManager.makeNot(getGoalCoverage(lGoal)));
            logger.logf(Level.WARNING, "Covered some PCs for Goal %d. Remaining PC %s !", goalIndex, bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
          }

          stop = true;
          continue; // we do not want to modify the ARG for the degenerated automaton to keep more reachability information
        }

        if (checkCoverage && isCovered(goalIndex, lGoal)) {
          if (lGoalPrediction != null) {
            lGoalPrediction[goalIndex - 1] = Prediction.FEASIBLE;
          }

          remainingPCforGoalCoverage = lGoal.getPresenceCondition();

          continue;
        }

        // goal is uncovered so far; run CPAchecker to cover it
        ReachabilityAnalysisResult result = runReachabilityAnalysis(goalIndex, lGoal, previousAutomaton, pInfeasibilityPropagation, remainingPCforGoalCoverage);
        if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
          logger.logf(Level.WARNING, "Analysis run was unsound!");
          wasSound = false;
        }
        previousAutomaton = currentAutomaton;

        if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
          stop = true;
          continue;
        }

        // update PC coverage todo
        if (testsuite.isInfeasible(lGoal) &&
            bddCpaNamedRegionManager.entails(testsuite.getInfeasibleGoals().get(lGoal), remainingPCforGoalCoverage)) {
          // 1st condition: this goal is infeasible for some constraint
          // 2nd condition: remainingPCforGoalCoverage is part of this constraint (implied by this constraint)
          logger.logf(Level.WARNING, "Goal %d is infeasible for remaining PC %s !", goalIndex, bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
          remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeFalse();
          // remainingPCforGoalCoverage := FALSE ensures that the while loop exits and the next goal is processed.
        } else {
          // now we need to cover all remaining configurations
          // the remaining configs are represented by the negation of the already covered pcs (in conjunction with the previous testGoalPCtoCover)
          remainingPCforGoalCoverage = bddCpaNamedRegionManager.makeAnd(remainingPCforGoalCoverage, bddCpaNamedRegionManager.makeNot(getGoalCoverage(lGoal)));
          logger.logf(Level.WARNING, "Covered some PCs for Goal %d. Remaining PC %s !", goalIndex, bddCpaNamedRegionManager.dumpRegion(remainingPCforGoalCoverage));
        }
      }
    }

    // reprocess timed-out goals
    if (testsuite.getTimedOutGoals().isEmpty()) {
      System.out.println("There were no timed out goals.");
    } else {
      if (! timeoutStrategy.equals(TimeoutStrategy.RETRY_AFTER_TIMEOUT)) {
        System.out.println("There were timed out goals but retry after timeout strategy is disabled.");
      } else {
        // retry timed-out goals
        // TODO move to upper loop
        Map<Goal, Integer> coverageCheckOpt = new HashMap<>();

        //int previousNumberOfTestCases = 0;
        //int previousPreviousNumberOfTestCases = testsuite.getNumberOfTestCases();

        boolean order = true;

        do {
          if (timeoutIncrement > 0) {
            long oldCPUTimeLimitPerGoal = cpuTimelimitPerGoal;
            cpuTimelimitPerGoal += timeoutIncrement;
            logger.logf(Level.INFO, "Incremented timeout from %d to %d seconds.", oldCPUTimeLimitPerGoal, cpuTimelimitPerGoal);
          }

          Collection<Entry<Integer, Pair<Goal,Region>>> set;

          if (useOrder) {
            if (inverseOrder) {
              order = !order;
            }

            // keep original order of goals (or inverse of it)
            if (order) {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_RESPECTING_COMPARATOR);
            }
            else {
              set = new TreeSet<>(WorklistEntryComparator.ORDER_INVERTING_COMPARATOR);
            }

            set.addAll(testsuite.getTimedOutGoals().entrySet());
          }
          else {
            set = new LinkedList<>();
            set.addAll(testsuite.getTimedOutGoals().entrySet());
          }

          testsuite.getTimedOutGoals().clear();

          for (Entry<Integer, Pair<Goal,Region>> entry : set) {
            goalIndex = entry.getKey();
            Goal lGoal = entry.getValue().getFirst();
            Region lRegion = entry.getValue().getSecond();
            logger.logf(Level.INFO, "Processing test goal %d of %d.", goalIndex, numberOfTestGoals);

            if (lGoalPrediction != null && lGoalPrediction[goalIndex - 1] == Prediction.INFEASIBLE) {
              logger.logf(Level.INFO, "This goal is predicted as infeasible!");

              testsuite.addInfeasibleGoal(lGoal, lRegion);

              continue;
            }

            // TODO optimization: do not check for coverage if no new testcases were generated.
            if (checkCoverage) {
              if (coverageCheckOpt.containsKey(lGoal)) {
                if (coverageCheckOpt.get(lGoal) < testsuite.getNumberOfTestCases()) {
                  if (isCovered(goalIndex, lGoal)) {
                    continue;
                  }
                  else {
                    // TODO optimization: only add if goal times out!
                    coverageCheckOpt.put(lGoal, testsuite.getNumberOfTestCases());
                  }
                }
              }
            }

            /*if (checkCoverage && (previousNumberOfTestCases < testsuite.getNumberOfTestCases()) && isCovered(goalIndex, lGoal)) {
              continue;
            }*/
            ReachabilityAnalysisResult result = runReachabilityAnalysis(goalIndex, lGoal, previousAutomaton, pInfeasibilityPropagation, lRegion);
            if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
              logger.logf(Level.WARNING, "Analysis run was unsound!");
              wasSound = false;
            }
            previousAutomaton = lGoal.getAutomaton();
          }
        } while (testsuite.hasTimedoutTestGoals());
      }
    }
    return wasSound;
  }

  /**
   * Summarizes the presence conditions of tests in testsuite that cover the parameter testgoal.
   */
  private Region getGoalCoverage(Goal pGoal) {
    Region totalCoverage = bddCpaNamedRegionManager.makeFalse();
    for (Entry<TestCase, List<Goal>> entry : testsuite.getMapping().entrySet()) {
      if (entry.getValue().contains(pGoal)) {
        assert entry.getKey().getRegion()!=null;
        totalCoverage = bddCpaNamedRegionManager.makeOr(totalCoverage, entry.getKey().getRegion());
      }
    }
    return totalCoverage;
  }

  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEDOUT
  }
  private ReachabilityAnalysisResult runReachabilityAnalysis(int goalIndex, Goal pGoal, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousGoalAutomaton, Pair<Boolean, LinkedList<Edges>> pInfeasibilityPropagation, Region remainingPCforGoalCoverage) throws CPAException, InterruptedException {
    GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(pGoal.getAutomaton());

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(1);//(2);

    /*if (pPassingCPA != null) {
      lAutomatonCPAs.add(pPassingCPA);
    }*/

    lAutomatonCPAs.add(lAutomatonCPA);

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    // TODO what is the more efficient order for the CPAs? Can we substitute a placeholder CPA? or inject an automaton in to an automaton CPA?
    //int lProductAutomatonIndex = lComponentAnalyses.size();
    int lProductAutomatonIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    // TODO experiment
    if (cpa instanceof ARGCPA) {
      ARGCPA argcpa = (ARGCPA)cpa;
      CompositeCPA compositeCPA = argcpa.retrieveWrappedCpa(CompositeCPA.class);

      if (compositeCPA != null) {
        lComponentAnalyses.addAll(compositeCPA.getWrappedCPAs());
      }
      else {
        throw new RuntimeException("Unsupported subcpa!");
      }
    }
    else if (cpa instanceof CompositeCPA) {
      CompositeCPA compositeCPA = (CompositeCPA)cpa;
      lComponentAnalyses.addAll(compositeCPA.getWrappedCPAs());
    }
    else {
      lComponentAnalyses.add(cpa);
    }

    ARGCPA lARTCPA;
    try {
      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(startupConfig.getConfig());
      lCPAFactory.setLogger(logger);
      lCPAFactory.set(cfa, CFA.class);

      ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.set(cfa, CFA.class);
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(startupConfig.getConfig());
      lARTCPAFactory.setLogger(logger);

      lARTCPA = (ARGCPA)lARTCPAFactory.createInstance();
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    if (reuseARG && (reachedSet != null)) {
      ARTReuse.modifyReachedSet(reachedSet, cfa.getMainFunction(), lARTCPA, lProductAutomatonIndex, pPreviousGoalAutomaton, pGoal.getAutomaton());

      // reusedPrecision == null indicates that there is no PredicateCPA
      if (reusePredicates && reusedPrecision != null) {
        for (AbstractState lWaitlistElement : reachedSet.getWaitlist()) {
          Precision lOldPrecision = reachedSet.getPrecision(lWaitlistElement);
          Precision lNewPrecision = Precisions.replaceByType(lOldPrecision, reusedPrecision, PredicatePrecision.class);

          reachedSet.updatePrecision(lWaitlistElement, lNewPrecision);
        }
      }
    }
    else {
      reachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.BFS); // TODO why does TOPSORT not exist anymore?

      AbstractState lInitialElement = lARTCPA.getInitialState(cfa.getMainFunction());
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(cfa.getMainFunction());

      reachedSet.add(lInitialElement, lInitialPrecision);

      outsideReachedSet.add(lInitialElement, lInitialPrecision);

      if (reusePredicates) {
        // initialize reused predicate precision
        PredicateCPA predicateCPA = lARTCPA.retrieveWrappedCpa(PredicateCPA.class);

        if (predicateCPA != null) {
          reusedPrecision = (PredicatePrecision)predicateCPA.getInitialPrecision(cfa.getMainFunction());
        }
        else {
          logger.logf(Level.INFO, "No predicate CPA available to reuse predicates!");
        }
      }
    }

    ShutdownNotifier algNotifier = ShutdownNotifier.createWithParent(startupConfig.getShutdownNotifier());

    startupConfig.getConfig();

    Algorithm algorithm;

    try {
      Configuration internalConfiguration = Configuration.builder().loadFromFile(algorithmConfigurationFile).build();

      CoreComponentsFactory factory = new CoreComponentsFactory(internalConfiguration, logger, algNotifier);

      algorithm = factory.createAlgorithm(lARTCPA, programDenotation, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm)algorithm;

        Refiner refiner = cegarAlg.getRefiner();
        if (refiner instanceof PredicateCPARefiner) {
          PredicateCPARefiner predicateRefiner = (PredicateCPARefiner)refiner;

          if (reusePredicates) {
            RefinementStrategy strategy = predicateRefiner.getRefinementStrategy();
            assert(strategy instanceof PredicateAbstractionRefinementStrategy);

            PredicateAbstractionRefinementStrategy refinementStrategy = (PredicateAbstractionRefinementStrategy)strategy;
            refinementStrategy.setPrecisionCallback(this);
          }
        }

        ARGStatistics lARTStatistics;
        try {
          lARTStatistics = new ARGStatistics(internalConfiguration, lARTCPA);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = new HashSet<>();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }
      // inject goal Presence Condition in BDDCPA
      BDDCPA bddcpa = null;
      if (cpa instanceof WrapperCPA) {
        // must be non-null, otherwise Exception in constructor of this class
        bddcpa = ((WrapperCPA)cpa).retrieveWrappedCpa(BDDCPA.class);
      } else if (cpa instanceof BDDCPA) {
        bddcpa = (BDDCPA)cpa;
      }
      bddcpa.getTransferRelation().setGlobalConstraint(remainingPCforGoalCoverage);

    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    boolean analysisWasSound = false;
    boolean hasTimedOut = false;

    statistics_numberOfMCCalls++;

    // start CPAchecker to find a path to the test goal
    if (cpuTimelimitPerGoal < 0) {
      // run algorithm without time limit
      analysisWasSound = algorithm.run(reachedSet);
    }
    else {
      // run algorithm with time limit
      WorkerRunnable workerRunnable = new WorkerRunnable(algorithm, reachedSet, cpuTimelimitPerGoal, algNotifier);

      Thread workerThread = new Thread(workerRunnable);

      workerThread.start();
      workerThread.join();

      if (workerRunnable.throwableWasCaught()) {
        throw new RuntimeException(workerRunnable.getCaughtThrowable());
      }
      else {
        analysisWasSound = workerRunnable.analysisWasSound();

        if (workerRunnable.hasTimeout()) {
          logger.logf(Level.INFO, "Test goal timed out!");

          testsuite.addTimedOutGoal(goalIndex, pGoal, remainingPCforGoalCoverage);

          hasTimedOut = true;
        }
      }
    }

    if (printARGperGoal) {
      Path argFile = Paths.get("output", "ARG_goal_" + goalIndex + ".dot");

      try (Writer w = Files.openOutputFile(argFile)) {
        ARGUtils.writeARGAsDot(w, (ARGState) reachedSet.getFirstState());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (hasTimedOut) {
      return ReachabilityAnalysisResult.TIMEDOUT;
    }
    else {
      // TODO check whether a last state might remain from an earlier run and a reuse of the ARG
      AbstractState lastState = reachedSet.getLastState();

      if (lastState != null) {
        if (AbstractStates.isTargetState(lastState)) {
          // we consider the test goal as feasible

          logger.logf(Level.INFO, "Test goal is feasible.");

          // TODO add missing soundness checks!

          if (lGoalPrediction != null) {
            lGoalPrediction[goalIndex - 1] = Prediction.FEASIBLE;
          }


          // TODO identify abstract states
          CFAEdge criticalEdge = pGoal.getCriticalEdge();


          // can we obtain a counterexample to check coverage for other test goals?
          Map<ARGState, CounterexampleInfo> counterexamples = lARTCPA.getCounterexamples();

          if (counterexamples.isEmpty()) {
            logger.logf(Level.INFO, "Counterexample is not available.");

            ARGPath trace = new ARGPath();
            //LinkedList<CFAEdge> trace = new LinkedList<>();

            // Try to reconstruct a trace in the ARG
            ARGState argState = AbstractStates.extractStateByType(lastState, ARGState.class);

            Collection<ARGState> parents;
            parents = argState.getParents();

            Region testCaseCriticalStateRegion = null;
            while (!parents.isEmpty()) {
              //assert (parents.size() == 1);
              /*if (parents.size() != 1) {
                throw new RuntimeException();
              }*/

              ARGState parent = null;

              for (ARGState tmp_parent : parents) {
                parent = tmp_parent;
                break; // we just choose some parent
              }

              CFAEdge edge = parent.getEdgeToChild(argState);
              trace.addFirst(Pair.of(argState, edge));

              if (edge.equals(criticalEdge)) {
                testCaseCriticalStateRegion = getRegionFromWrappedBDDstate(argState);
              }

              argState = parent;
              parents = argState.getParents();
            }

            // TODO we need a different way to obtain input values
            List<BigInteger> inputValues = new ArrayList<>();

            Region testCaseFinalRegion = getRegionFromWrappedBDDstate(lastState);
            logger.logf(Level.INFO, " generated test case with " + (testCaseCriticalStateRegion==null ?"(final)":"(critical)") + " PC " + bddCpaNamedRegionManager.dumpRegion((testCaseCriticalStateRegion==null ?testCaseFinalRegion:testCaseCriticalStateRegion)));

            TestCase testcase = new TestCase(inputValues,
                (testCaseCriticalStateRegion==null ? testCaseFinalRegion : testCaseCriticalStateRegion), // use region from critical state if available and final region otherwise
                trace, bddCpaNamedRegionManager);
            testsuite.addTestCase(testcase, pGoal);
          }
          else {
            // test goal is feasible
            logger.logf(Level.INFO, "Counterexample is available.");

            assert counterexamples.size() == 1;

            for (Map.Entry<ARGState, CounterexampleInfo> lEntry : counterexamples.entrySet()) {
              //ARGState state = lEntry.getKey();
              CounterexampleInfo cex = lEntry.getValue();

              if (cex.isSpurious()) {
                logger.logf(Level.WARNING, "Counterexample is spurious!");
              }
              else {
                Model model = cex.getTargetPathModel();

                Comparator<Map.Entry<Model.AssignableTerm, Object>> comp = new Comparator<Map.Entry<Model.AssignableTerm, Object>>() {

                  @Override
                  public int compare(Entry<AssignableTerm, Object> pArg0, Entry<AssignableTerm, Object> pArg1) {
                    assert pArg0.getKey().getName().equals(pArg1.getKey().getName());
                    assert pArg0.getKey() instanceof Model.Variable;
                    assert pArg1.getKey() instanceof Model.Variable;

                    Model.Variable v0 = (Model.Variable)pArg0.getKey();
                    Model.Variable v1 = (Model.Variable)pArg1.getKey();

                    return (v0.getSSAIndex() - v1.getSSAIndex());
                  }

                };

                TreeSet<Map.Entry<Model.AssignableTerm, Object>> inputs = new TreeSet<>(comp);

                for (Map.Entry<Model.AssignableTerm, Object> e : model.entrySet()) {
                  if (e.getKey() instanceof Model.Variable) {
                    Model.Variable v = (Model.Variable)e.getKey();

                    if (v.getName().equals(WrapperUtil.CPAtiger_INPUT + "::__retval__")) {
                      inputs.add(e);
                    }
                  }
                }

                List<BigInteger> inputValues = new ArrayList<>(inputs.size());

                for (Map.Entry<Model.AssignableTerm, Object> e : inputs) {
                  assert e.getValue() instanceof BigInteger;
                  inputValues.add((BigInteger)e.getValue());
                }


                // determine regions for coverage goals reached earlier during execution of the test case
                Region testCaseCriticalStateRegion = null;
                int abstract_state_index = 0;
                for (Pair<ARGState, CFAEdge> stateEdgePair : cex.getTargetPath()) {
                  if (stateEdgePair.getSecond().equals(criticalEdge)) {
                    if (abstract_state_index >= cex.getTargetPath().size() - 1) {
                      throw new RuntimeException("Wrong abstract state index!");
                    }

                    Pair<ARGState, CFAEdge> successor = cex.getTargetPath().get(abstract_state_index + 1);

                    testCaseCriticalStateRegion = getRegionFromWrappedBDDstate(successor.getFirst());
                  }
                  abstract_state_index++;
                }
                Region testCaseFinalRegion = getRegionFromWrappedBDDstate(reachedSet.getLastState());
                logger.logf(Level.INFO, " generated test case with " + (testCaseCriticalStateRegion==null ?"(final)":"(critical)") + " PC " + bddCpaNamedRegionManager.dumpRegion((testCaseCriticalStateRegion==null ?testCaseFinalRegion:testCaseCriticalStateRegion)));
                TestCase testcase = new TestCase(inputValues,
                    (testCaseCriticalStateRegion==null ? testCaseFinalRegion : testCaseCriticalStateRegion), // use region from critical state if available and final region otherwise
                    cex.getTargetPath(), bddCpaNamedRegionManager);
                testsuite.addTestCase(testcase, pGoal);
              }
            }
          }
        }
        else {
          // we consider the test goals is infeasible

          logger.logf(Level.INFO, "Test goal infeasible.");

          if (lGoalPrediction != null) {
            lGoalPrediction[goalIndex - 1] = Prediction.INFEASIBLE;
          }

          testsuite.addInfeasibleGoal(pGoal, remainingPCforGoalCoverage);
          // TODO add missing soundness checks!

          if (pInfeasibilityPropagation.getFirst()) {
            logger.logf(Level.INFO, "Do infeasibility propagation!");

            HashSet<CFAEdge> lTargetEdges = new HashSet<>();

            ClusteredElementaryCoveragePattern lClusteredPattern = (ClusteredElementaryCoveragePattern)pGoal.getPattern();

            ListIterator<ClusteredElementaryCoveragePattern> lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

            int lTmpIndex = goalIndex - 1; // caution lIndex starts at 0

            while (lRemainingPatterns.hasNext()) {
              Prediction lPrediction = lGoalPrediction[lTmpIndex];

              ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

              if (lPrediction.equals(Prediction.UNKNOWN)) {
                lTargetEdges.add(lRemainingPattern.getLastSingletonCFAEdge());
              }

              lTmpIndex++;
            }

            Collection<CFAEdge> lFoundEdges = InfeasibilityPropagation.dfs2(lClusteredPattern.getCFANode(), lClusteredPattern.getLastSingletonCFAEdge(), lTargetEdges);

            lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

            lTmpIndex = goalIndex - 1;

            while (lRemainingPatterns.hasNext()) {
              Prediction lPrediction = lGoalPrediction[lTmpIndex];

              ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

              if (lPrediction.equals(Prediction.UNKNOWN)) {
                if (!lFoundEdges.contains(lRemainingPattern.getLastSingletonCFAEdge())) {
                  //mFeasibilityInformation.setStatus(lTmpIndex+1, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);
                  // TODO remove ???

                  lGoalPrediction[lTmpIndex] = Prediction.INFEASIBLE;
                }
              }

              lTmpIndex++;
            }
          }
        }
      }
      else {
        throw new RuntimeException("We need a last state to determine the feasibility of the test goal!");
      }
    }

    return ReachabilityAnalysisResult.SOUND;
  }

  private Region getRegionFromWrappedBDDstate(AbstractState pAbstractState) {
    BDDState wrappedBDDState = getWrappedBDDState(pAbstractState);
    if (wrappedBDDState== null) {
      throw new RuntimeException("Did not find a BDD state component in a state!");
    }
    Region bddStateRegion = wrappedBDDState.getRegion();
    assert wrappedBDDState.getNamedRegionManager() == bddCpaNamedRegionManager;
    return bddStateRegion;
  }

  BDDState getWrappedBDDState(AbstractState inState) {
    if (inState instanceof BDDState) {
      return (BDDState)inState;
    } else if (inState instanceof AbstractWrapperState) {
      for (AbstractState subState : ((AbstractWrapperState)inState).getWrappedStates()) {
        if (subState instanceof BDDState) {
          return (BDDState)subState;
        } else if (subState instanceof AbstractWrapperState) {
          BDDState res = getWrappedBDDState(subState);
          if (res != null) {
            return res;
          }
        }
      }
    }
    return null;
  }

  public static ThreeValuedAnswer accepts(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, List<CFAEdge> pCFAPath) {
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();

    lCurrentStates.add(pAutomaton.getInitialState());

    boolean lHasPredicates = false;

    for (CFAEdge lCFAEdge : pCFAPath) {
      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (pAutomaton.getFinalStates().contains(lCurrentState)) {
          return ThreeValuedAnswer.ACCEPT;
        }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lCurrentState)) {
          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          if (lLabel.hasGuards()) {
            lHasPredicates = true;
          }
          else {
            if (lLabel.contains(lCFAEdge)) {
              lNextStates.add(lOutgoingEdge.getTarget());
            }
          }
        }
      }

      lCurrentStates.clear();

      Set<NondeterministicFiniteAutomaton.State> lTmp = lCurrentStates;
      lCurrentStates = lNextStates;
      lNextStates = lTmp;
    }

    for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
      // Automaton accepts as soon as it sees a final state (implicit self-loop)
      if (pAutomaton.getFinalStates().contains(lCurrentState)) {
        return ThreeValuedAnswer.ACCEPT;
      }
    }

    if (lHasPredicates) {
      return ThreeValuedAnswer.UNKNOWN;
    }
    else {
      return ThreeValuedAnswer.REJECT;
    }
  }

  /**
   * Constructs a test goal from the given pattern.
   * @param pGoalPattern
   * @param pAlphaLabel
   * @param pInverseAlphaLabel
   * @param pOmegaLabel
   * @param pUseAutomatonOptimization
   * @return
   */
  private Goal constructGoal(int pIndex, ElementaryCoveragePattern pGoalPattern, GuardedEdgeLabel pAlphaLabel,
      GuardedEdgeLabel pInverseAlphaLabel, GuardedLabel pOmegaLabel, boolean pUseAutomatonOptimization, Region pTestGoalPCtoCover) {

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton = ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, pUseAutomatonOptimization);

    Goal lGoal = new Goal(pIndex, pGoalPattern, automaton, pTestGoalPCtoCover);

    return lGoal;
  }

  @Override
  public PredicatePrecision getPrecision() {
    return reusedPrecision;
  }

  @Override
  public void setPrecision(PredicatePrecision pNewPrec) {
    reusedPrecision = pNewPrec;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    // TODO Print information about feasible, infeasible, timed-out, and unprocessed test goals.

    if (testsuiteFile != null) {
      // write generated test suite and mapping to file system
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testsuiteFile.toFile()), "utf-8"))) {
        writer.write(testsuite.toString());
        writer.close();
      } catch (IOException e){
        throw new RuntimeException(e);
      }
    }

    int numberOfTimedoutTestGoals = statistics_numberOfProcessedTestGoals - (testsuite.getNumberOfFeasibleTestGoals() + testsuite.getNumberOfInfeasibleTestGoals());

    pOut.println("Number of test goals:                              " + statistics_numberOfTestGoals);
    pOut.println("Number of processed test goals:                    " + statistics_numberOfProcessedTestGoals);
    pOut.println("Number of feasible test goals:                     " + testsuite.getNumberOfFeasibleTestGoals());
    pOut.println("Number of infeasible test goals:                   " + testsuite.getNumberOfInfeasibleTestGoals());
    //pOut.println("Number of timedout test goals:                     " + testsuite.getNumberOfTimedoutTestGoals());
    pOut.println("Number of timedout test goals:                     " + numberOfTimedoutTestGoals);
    pOut.println("Number of MC calls:                                " + statistics_numberOfMCCalls);

    if (statistics_numberOfProcessedTestGoals > testsuite.getNumberOfFeasibleTestGoals() + testsuite.getNumberOfInfeasibleTestGoals() + testsuite.getNumberOfTimedoutTestGoals()) {
      pOut.println("Timeout occured during processing of a test goal!");
    }
  }

  @Override
  public String getName() {
    return "TigerAlgorithm (variability aware)";
  }

}
