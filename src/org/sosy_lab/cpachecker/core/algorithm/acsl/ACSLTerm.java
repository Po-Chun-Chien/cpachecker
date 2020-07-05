package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public interface ACSLTerm {

  CExpression accept(ACSLToCExpressionVisitor visitor) throws UnrecognizedCodeException;

  /**
   * Returns a version of the term where each identifier is augmented with the ACSL builtin
   * predicate "\old" to signal to use the value from the pre-state when evaluating.
   */
  ACSLTerm useOldValues();
}
