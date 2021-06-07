// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class SMGHasValueEdge implements SMGEdge {

  private final SMGValue value;
  private final CType type;
  private final BigInteger offset;

  public SMGHasValueEdge(
      SMGValue pValue,
      CType pType,
      BigInteger pOffset) {
    value = pValue;
    type = pType;
    offset = pOffset;
  }

  public SMGValue hasValue() {
    return value;
  }

  public CType getType() {
    return type;
  }

  @Override
  public BigInteger getOffset() {
    return offset;
  }
}