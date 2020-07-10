/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class ForwardPreConditionRanking implements FaultRanking {

  private final TraceFormula traceFormula;
  private final FormulaContext context;

  public ForwardPreConditionRanking(TraceFormula pTraceFormula, FormulaContext pContext){
    traceFormula = pTraceFormula;
    context = pContext;
  }

  /**
   * Tell the user which initial variable assignment lead to an error.
   * This is not an actual ranking.
   * @param result The result of any FaultLocalizationAlgorithm
   * @return Faults ranked by identity
   */
  @Override
  public List<Fault> rank(Set<Fault> result) {
    // check if alternative precondition was used
    List<Fault> rankedList = new ArrayList<>(result);
    if(!traceFormula.getPreCondition().toString().contains("_VERIFIER_nondet_")){
      return rankedList;
    }

    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    Set<BooleanFormula> preconditions = bmgr.toConjunctionArgs(traceFormula.getPreCondition(), true);

    Map<String, String> mapFormulaToValue = new HashMap<>();
    List<String> assignments = new ArrayList<>();

    for (BooleanFormula precondition : preconditions) {
      String formulaString = precondition.toString();
      formulaString = formulaString.replaceAll("\\(", "").replaceAll("\\)", "");
      List<String> operatorAndOperands = Splitter.on("` ").splitToList(formulaString);
      if(operatorAndOperands.size() != 2) {
        return rankedList;
      }
      String withoutOperator = operatorAndOperands.get(1);
      List<String> operands = Splitter.on(" ").splitToList(withoutOperator);
      if(operands.size() != 2){
        return rankedList;
      }
      if (operands.get(0).contains("__VERIFIER_nondet_") || (operands.get(0).contains("::") && operands.get(0).contains("@"))){
        if((operands.get(0).contains("::") && operands.get(0).contains("@"))){
          assignments.add(Splitter.on("@").splitToList(operands.get(0)).get(0) + " = " + operands.get(1));
        } else {
          mapFormulaToValue.put(operands.get(0), operands.get(1));
        }
      } else {
        if((operands.get(1).contains("::") && operands.get(1).contains("@"))){
          assignments.add(Splitter.on("@").splitToList(operands.get(1)).get(0) + " = " + operands.get(0));
        } else {
          mapFormulaToValue.put(operands.get(1), operands.get(0));
        }
      }
    }

    String hint = "The program fails for the initial variable assignment ";

    for(Entry<String, String> entry: mapFormulaToValue.entrySet()){
      for (int i = 0 ; i < traceFormula.getAtoms().size(); i++) {
        BooleanFormula atom = traceFormula.getAtom(i);
        if(atom.toString().contains(entry.getKey())){
          atom = context.getSolver().getFormulaManager().uninstantiate(atom);
          String assignment = context.getConverter().convert(atom.toString().replaceAll(entry.getKey(), entry.getValue()));
          assignments.add(assignment);
        }
      }
    }

    String allAssignments = String.join(", ", assignments);
    for (Fault fault : rankedList) {
      fault.addInfo(FaultInfo.hint(hint + allAssignments));
    }

    return rankedList;
  }
}