/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.sl;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SLState implements AbstractState, Targetable, AbstractQueryableState {

  private static final String HAS_INVALID_DEREFS = "has-invalid-derefs";
  private static final String HAS_LEAKS = "has-leaks";
  private static final String HAS_INVALID_FREES = "has-invalid-frees";

  public enum SLStateError {
    INVALID_DEREF,
    INVALID_FREE,
    MEMORY_LEAK,
    NONE;
  }


  private PathFormula pathFormula;
  private final Map<Formula, Formula> heap;
  private final Map<Formula, Formula> stack;

  private final Map<Formula, BigInteger> allocationSizes;
  private final Set<CSimpleDeclaration> inScopePtrs;
  private final Set<String> declarations;

  private final Set<SLStateError> errors = new HashSet<>();


  public SLState(
      PathFormula pPathFormula,
      Map<Formula, Formula> pHeap,
      Map<Formula, Formula> pStack,
      Map<Formula, BigInteger> pAllocationSizes,
      Set<CSimpleDeclaration> pInScopePtrs,
      Set<String> pDeclarations,
      SLStateError pError) {
    pathFormula = pPathFormula;
    heap = pHeap;
    stack = pStack;
    allocationSizes = pAllocationSizes;
    inScopePtrs = pInScopePtrs;
    declarations = pDeclarations;
    if (pError != null) {
      errors.add(pError);
    }

  }

  public SLState(PathFormula pStore) {
    this(
        pStore,
        new HashMap<>(),
        new HashMap<>(),
        new HashMap<>(),
        new HashSet<>(),
        new HashSet<>(),
        null);
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public String toString() {
    return "Errors:   "
        + errors
        + "\nFormula:  "
        + pathFormula
        + "\nHeap:     "
        + heap
        + "\nStack:    "
        + stack;
  }

  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  public Map<Formula, Formula> getStack() {
    return stack;
  }

  @Override
  public boolean isTarget() {
    return !errors.isEmpty();
  }

  public void addError(SLStateError pError) {
    if (pError != null) {
      errors.add(pError);
    }
  }

  @Override
  @Nonnull
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    Set<Property> res = new HashSet<>();
    errors.stream().forEach(e -> res.add(NamedProperty.create(e.name())));
    return ImmutableSet.copyOf(res);
  }

  @Override
  public String getCPAName() {
    return "SLCPA";
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {
    switch (property) {
      case HAS_LEAKS:
        return errors.contains(SLStateError.MEMORY_LEAK);

      case HAS_INVALID_DEREFS:
        return errors.contains(SLStateError.INVALID_DEREF);

      case HAS_INVALID_FREES:
        return errors.contains(SLStateError.INVALID_FREE);

      default:
        throw new InvalidQueryException("Query '" + property + "' is invalid.");
    }
  }

  public @Nullable SLState copyWithoutErrors() {
    PathFormula newFormula =
        new PathFormula(
            pathFormula.getFormula(),
            pathFormula.getSsa(),
            pathFormula.getPointerTargetSet(),
            pathFormula.getLength());
    SLState s =
        new SLState(
        newFormula,
        new HashMap(heap),
        new HashMap(stack),
        new HashMap(allocationSizes),
        new HashSet(inScopePtrs),
        new HashSet(declarations),
        null);
    return s;
  }

  public Map<Formula, BigInteger> getAllocationSizes() {
    return allocationSizes;
  }

  public Set<String> getDeclarations() {
    return declarations;
  }

  public Set<CSimpleDeclaration> getInScopePtrs() {
    return inScopePtrs;
  }

  public void setPathFormula(PathFormula pFormula) {
    pathFormula = pFormula;
  }
}