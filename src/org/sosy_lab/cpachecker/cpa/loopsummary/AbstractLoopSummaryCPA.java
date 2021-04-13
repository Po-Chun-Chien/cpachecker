// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.BaseStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.LoopAcceleration;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.NaiveLoopAcceleration;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation.LinearExtrapolationStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation.PolynomialExtrapolationStrategy;

@Options(prefix = "cpa.loopsummary")
public abstract class AbstractLoopSummaryCPA extends AbstractSingleWrapperCPA {

  private enum StrategiesEnum {
    BASE,
    LOOPACCELERATION,
    NAIVELOOPACCELERATION,
    POLYNOMIALEXTRAPOLATION,
    LINEAREXTRAPOLATION,
    CONSTANTEXTRAPOLATION
  }

  // TODO wie kann man die argumente angeben
  @Option(
      name = "strategies",
      secure = true,
      description =
          "Strategies to be used in the Summary. The order of the strategies marks in which order they are tried")
  private ArrayList<StrategiesEnum> strategies =
      new ArrayList<>(
          Arrays.asList(
              StrategiesEnum.CONSTANTEXTRAPOLATION,
              StrategiesEnum.LINEAREXTRAPOLATION,
              StrategiesEnum.POLYNOMIALEXTRAPOLATION,
              StrategiesEnum.NAIVELOOPACCELERATION,
              StrategiesEnum.LOOPACCELERATION,
              StrategiesEnum.BASE));

  private ArrayList<StrategyInterface> strategiesClass = new ArrayList<>();

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private final LoopSummaryCPAStatistics stats;

  @SuppressWarnings("unused")
  private CFA cfa;

  @SuppressWarnings("unused")
  private Specification specification;

  protected AbstractLoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this, AbstractLoopSummaryCPA.class);

    for (StrategiesEnum e : strategies) {
      switch (e) {
        case BASE:
          strategiesClass.add(new BaseStrategy(pLogger, pShutdownNotifier));
          break;
        case LOOPACCELERATION:
          strategiesClass.add(new LoopAcceleration(pLogger, pShutdownNotifier));
          break;
        case NAIVELOOPACCELERATION:
          strategiesClass.add(new NaiveLoopAcceleration(pLogger, pShutdownNotifier));
          break;
        case POLYNOMIALEXTRAPOLATION:
          strategiesClass.add(new PolynomialExtrapolationStrategy(pLogger, pShutdownNotifier));
          break;
        case LINEAREXTRAPOLATION:
          strategiesClass.add(new LinearExtrapolationStrategy(pLogger, pShutdownNotifier));
          break;
        case CONSTANTEXTRAPOLATION:
          strategiesClass.add(new ConstantExtrapolationStrategy(pLogger, pShutdownNotifier));
          break;
      }
    }

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    stats = new LoopSummaryCPAStatistics(pConfig, pLogger, this);
    cfa = pCfa;
    specification = pSpecification;
  }

  @Override
  protected ConfigurableProgramAnalysis getWrappedCpa() {
    // override for visibility
    // TODO There is an error when casting to ConfigurableProgramAnalysisWithLoopSummary like is
    // done in AbstractBAMCPA
    return super.getWrappedCpa();
  }

  public LogManager getLogger() {
    return logger;
  }

  LoopSummaryCPAStatistics getStatistics() {
    return stats;
  }

  ArrayList<StrategyInterface> getStrategies() {
    return strategiesClass;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }
}