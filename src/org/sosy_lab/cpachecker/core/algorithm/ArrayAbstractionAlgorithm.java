// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.arrayabstraction.ArrayAbstraction;

/**
 * Algorithm for array abstraction by program translation.
 *
 * <p>A delegate analysis is run on the abstracted program which is represented by a transformed CFA
 * that is derived from the specified original CFA.
 */
@Options(prefix = "arrayAbstraction")
public final class ArrayAbstractionAlgorithm extends NestingAlgorithm {

  @Option(
      secure = true,
      name = "delegateAnalysis",
      description = "Configuration of the delegate analysis running on the translated program")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path delegateAnalysisConfigurationFile;

  @Option(
      secure = true,
      name = "checkCounterexamples",
      description =
          "Use a second delegate analysis run to check counterexamples on the original program that"
              + " contains (non-abstracted) arrays.")
  private boolean checkCounterexamples = false;

  @Option(
      secure = true,
      name = "cfa.export",
      description = "Whether to export the CFA with abstracted arrays as DOT file.")
  private boolean exportTranslatedCfa = true;

  @Option(
      secure = true,
      name = "cfa.file",
      description = "DOT file path for CFA with abstracted arrays.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportTranslatedCfaFile = Path.of("cfa-abstracted-arrays.dot");

  private final ShutdownManager shutdownManager;
  private final Collection<Statistics> stats;

  public ArrayAbstractionAlgorithm(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfiguration, pLogger, pShutdownNotifier, pSpecification, pCfa);

    shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    stats = new CopyOnWriteArrayList<>();

    pConfiguration.inject(this);
  }

  private CFA getOriginalCfa() {
    return cfa;
  }

  private AlgorithmStatus runDelegateAnalysis(
      CFA pCfa,
      ForwardingReachedSet pForwardingReachedSet,
      AggregatedReachedSets pAggregatedReached)
      throws InterruptedException, CPAEnabledAnalysisPropertyViolationException, CPAException {

    ImmutableSet<String> ignoreOptions =
        ImmutableSet.of("analysis.useArrayAbstraction", "arrayAbstraction.delegateAnalysis");

    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> delegate;
    try {
      delegate =
          createAlgorithm(
              delegateAnalysisConfigurationFile,
              pCfa,
              shutdownManager,
              pAggregatedReached,
              ignoreOptions,
              stats);
    } catch (IOException | InvalidConfigurationException ex) {
      logger.logUserException(Level.SEVERE, ex, "Could not create delegate algorithm");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    Algorithm algorithm = delegate.getFirst();
    ReachedSet reached = delegate.getThird();

    AlgorithmStatus status = algorithm.run(reached);

    while (reached.hasWaitingState()) {
      status = algorithm.run(reached);
    }

    pForwardingReachedSet.setDelegate(reached);

    return status;
  }

  private void exportTranslatedCfa(CFA pTranslatedCfa) {

    if (exportTranslatedCfa && exportTranslatedCfaFile != null && pTranslatedCfa != null) {
      try (Writer w = IO.openOutputFile(exportTranslatedCfaFile, Charset.defaultCharset())) {
        DOTBuilder.generateDOT(w, pTranslatedCfa);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write CFA to dot file");
      }
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;
    AggregatedReachedSets aggregatedReached = AggregatedReachedSets.singleton(pReachedSet);

    CFA transformedCfa = ArrayAbstraction.transformCfa(globalConfig, logger, getOriginalCfa());
    exportTranslatedCfa(transformedCfa);

    AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;

    if (transformedCfa != null) {
      status = runDelegateAnalysis(transformedCfa, forwardingReachedSet, aggregatedReached);
    }

    if (checkCounterexamples && forwardingReachedSet.wasTargetReached()) {
      status = runDelegateAnalysis(getOriginalCfa(), forwardingReachedSet, aggregatedReached);
    }

    return status;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.addAll(stats);
  }
}
