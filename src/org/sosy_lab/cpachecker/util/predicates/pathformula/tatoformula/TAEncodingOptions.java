// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import com.google.common.collect.Lists;
import java.util.List;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

@Options(prefix = "cpa.timedautomata")
public class TAEncodingOptions {
  public static enum TAEncodingExtensionType {
    SHALLOW_SYNC,
    INVARIANTS,
    TRANSITION_TYPES,
    TRANSITION_ACTIONS
  }

  @Option(
      secure = true,
      description =
          "Extensions to use for the encoding. Available options: SHALLOW_SYNC, INVARIANTS, ACTION_SYNC")
  public List<TAEncodingExtensionType> encodingExtensions =
      Lists.newArrayList(TAEncodingExtensionType.INVARIANTS);

  public static enum DiscreteEncodingType {
    LOCAL_ID,
    GLOBAL_ID,
    BOOLEAN_VAR
  }

  @Option(
      secure = true,
      description = "Formula representation of locations. Options are: BOOLEAN_VAR, LOCAL_VAR")
  public DiscreteEncodingType locationEncoding = DiscreteEncodingType.LOCAL_ID;

  @Option(
      secure = true,
      description =
          "Formula representation of actions. Options are: BOOLEAN_VAR, LOCAL_VAR, GLOBAL_VAR")
  public DiscreteEncodingType actionEncoding = DiscreteEncodingType.GLOBAL_ID;

  public static enum TimeEncodingType {
    GLOBAL_IMPLICIT,
    GLOBAL_EXPLICIT,
    LOCAL_EXPLICIT,
  }

  @Option(
      secure = true,
      description =
          "Formula representation of time. Options are: GLOBAL_IMPLICIT, GLOBAL_EXPLICIT, LOCAL_EXPLICIT")
  public TimeEncodingType timeEncoding = TimeEncodingType.GLOBAL_IMPLICIT;

  public static enum AutomatonEncodingType {
    TRANSITION_UNROLLING,
    LOCATION_UNROLLING,
    CONSTRAINT_UNROLLING
  }

  @Option(
      secure = true,
      description =
          "Type of unrolling of the formula. Options are: TRANSITION_UNROLLING, LOCATION_UNROLLING, CONSTRAINT_UNROLLING")
  public AutomatonEncodingType automatonEncodingType = AutomatonEncodingType.CONSTRAINT_UNROLLING;

  public static enum SpecialActionType {
    LOCAL,
    GLOBAL,
    NONE
  }

  @Option(
      secure = true,
      description = "Representation of a special idle action. Options are: LOCAL, GLOBAL, NONE")
  public SpecialActionType idleActionType = SpecialActionType.NONE;

  @Option(
      secure = true,
      description = "Representation of a special delay action. Options are: LOCAL, GLOBAL, NONE")
  public SpecialActionType delayActionType = SpecialActionType.NONE;

  @Option(
      secure = true,
      description =
          "Use actions to prevent delay transitions from being active in the same step as any other transition")
  public boolean actionDetachedDelay = false;

  @Option(
      secure = true,
      description =
          "Use actions to prevent idle transitions from being active in the same step as any other transition")
  public boolean actionDetachedIdle = true;

  @Option(secure = true, description = "Forbid distinct actions to appear in the same step")
  public boolean noTwoActions = true;

  public static enum InvariantType {
    LOCAL,
    GLOBAL
  }

  @Option(
      secure = true,
      description =
          "Whether to add the invariant to discrete/delay transition of globally to the whole step formula")
  public InvariantType invariantType = InvariantType.LOCAL;

  @Option(secure = true, description = "Delays of zero are allowed if true")
  public Boolean allowZeroDelay = true;
}