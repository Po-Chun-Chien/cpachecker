/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class TestCaseData {

  private int id;

  private List<TestCaseVariable> inputs;

  private List<TestCaseVariable> outputs;

  private List<String> coveredGoals;

  private List<String> coveredLabels;

  private String presenceCondition;

  private int errorPathLength;

  public TestCaseData() {
    id = -1;

    inputs = new ArrayList<>();

    outputs = new ArrayList<>();

    coveredGoals = Lists.newLinkedList();

    coveredLabels = Lists.newLinkedList();

    errorPathLength = -1;
  }

  public String getPresenceCondition() {
    return presenceCondition;
  }

  public void setPresenceCondition(String condition) {
    presenceCondition = condition;
  }

  public int getId() {
    return id;
  }

  public void setId(int pId) {
    id = pId;
  }

  public List<TestCaseVariable> getInputs() {
    return inputs;
  }

  public void setInputs(List<TestCaseVariable> pInputs) {
    inputs = pInputs;
  }

  public List<TestCaseVariable> getOutputs() {
    return outputs;
  }

  public void setOutputs(List<TestCaseVariable> pOutputs) {
    outputs = pOutputs;
  }

  public List<String> getCoveredGoals() {
    return coveredGoals;
  }

  public void setCoveredGoals(List<String> pCoveredGoals) {
    coveredGoals = pCoveredGoals;
  }

  public List<String> getCoveredLabels() {
    return coveredLabels;
  }

  public void setCoveredLabels(List<String> pCoveredLabels) {
    coveredLabels = pCoveredLabels;
  }

  public int getErrorPathLength() {
    return errorPathLength;
  }

  public void setErrorPathLength(int pErrorPathLength) {
    errorPathLength = pErrorPathLength;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append("TestCase ").append(id);
    if (presenceCondition != null && !presenceCondition.isEmpty()) {
      str.append(" with configurations ").append(presenceCondition);
    }

    str.append(":\n\n");

    str.append("\tinputs and outputs {\n");

    for (TestCaseVariable var : inputs) {
      str.append("\t\t-> ").append(var.getName()).append(" = ").append(var.getValue()).append("\n");
    }
    for (TestCaseVariable var : outputs) {
      str.append("\t\t<- ").append(var.getName()).append(" = ").append(var.getValue()).append("\n");
    }

    str.append("\t}");
    str.append("\n\n");

    str.append("\tCovered goals {\n");

    for (String g : coveredGoals) {
      str.append("\t\t").append(g).append("\n");
    }

    str.append("\t}\n\n");

    str.append("\tCovered labels {\n");
    str.append("\t\t");

    for (String label : coveredLabels) {
      str.append(label).append(", ");
    }

    str.delete(str.length() - 2, str.length());
    str.append("\n");
    str.append("\t}\n");
    str.append("\n");

    if (errorPathLength != -1) {
      str.append("\tErrorpath Length: ").append(errorPathLength).append("\n");
    }

    str.append("\n\n");

    return str.toString();
  }

}
