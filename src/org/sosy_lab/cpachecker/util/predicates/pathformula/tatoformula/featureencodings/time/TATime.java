// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.time;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;

public interface TATime {
  BooleanFormula makeConditionFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableCondition pCondition);

  BooleanFormula makeClockVariablesDoNotChangeFormula(
      TaDeclaration pAutomaton, int pVariableIndexBefore, Iterable<TaVariable> pClocks);

  BooleanFormula makeResetToZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, Iterable<TaVariable> pClocks);

  BooleanFormula makeInitiallyZeroFormula(TaDeclaration pAutomaton, int pVariableIndex);

  BooleanFormula makeTimeUpdateFormula(TaDeclaration pAutomaton, int pIndexBefore);

  BooleanFormula makeTimeDoesNotAdvanceFormula(TaDeclaration pAutomaton, int pIndexBefore);

  FormulaType<?> getTimeFormulaType();
}