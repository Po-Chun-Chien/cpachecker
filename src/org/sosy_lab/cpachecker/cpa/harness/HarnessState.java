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
package org.sosy_lab.cpachecker.cpa.harness;

import com.google.common.collect.ImmutableSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.harness.ComparableFunctionDeclaration;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
public class HarnessState implements AbstractState {

  private final PersistentMap<String, MemoryLocation> pointerVariableAssignments;
  private final Partition<MemoryLocation> locationEqualityAssumptions;
  private final PersistentList<MemoryLocation> orderedExternallyKnownLocations;
  private final PersistentMap<String, PersistentList<MemoryLocation>> externFunctionCalls;

  public HarnessState() {
    pointerVariableAssignments = PathCopyingPersistentTreeMap.of();
    locationEqualityAssumptions = new Partition<>();
    orderedExternallyKnownLocations = PersistentLinkedList.of();
    externFunctionCalls = PathCopyingPersistentTreeMap.of();
  }

  public HarnessState(
      PersistentMap<String, MemoryLocation> pPointerVariableAssignments,
      Partition<MemoryLocation> pLocationEqualityAssumptions,
      PersistentList<MemoryLocation> pOrderedExternallyKnownLocations,
      PersistentMap<String, PersistentList<MemoryLocation>> pExternFunctionCalls) {
    pointerVariableAssignments = pPointerVariableAssignments;
    locationEqualityAssumptions = pLocationEqualityAssumptions;
    orderedExternallyKnownLocations = pOrderedExternallyKnownLocations;
    externFunctionCalls = pExternFunctionCalls;
  }

  public HarnessState
      addFunctionCall(CExpression pFunctionNameExpression, MemoryLocation pLocation) {
    if (pFunctionNameExpression instanceof CIdExpression) {
      CIdExpression functionIdExpression = (CIdExpression) pFunctionNameExpression;
      CFunctionDeclaration functionDeclaration =
          (CFunctionDeclaration) functionIdExpression.getDeclaration();
      String functionIdentifier = functionDeclaration.getQualifiedName();
      PersistentList<MemoryLocation> currentFunctionCallsList =
          externFunctionCalls.get(functionIdentifier);
      PersistentList<MemoryLocation> newFunctionCallsList =
          currentFunctionCallsList.with(pLocation);
      PersistentMap<String, PersistentList<MemoryLocation>> newExternFunctionCalls =
          externFunctionCalls.putAndCopy(functionIdentifier, newFunctionCallsList);
      HarnessState newState =
          new HarnessState(
              pointerVariableAssignments,
              locationEqualityAssumptions,
              orderedExternallyKnownLocations,
              newExternFunctionCalls);
      return newState;
    }
    return this;
  }

  private MemoryLocation getLocationFromIdExpression(CIdExpression pExpression) {
    return memoryLocations.stream()
        .filter(m -> m.getIdentifier() == pExpression.getName())
        .findFirst()
        .orElse(new MemoryLocation(pExpression));
  }

  public HarnessState
      addPointerVariableAssignment(CLeftHandSide pLeftHandSide, CRightHandSide pRightHandSide) {
    if (pLeftHandSide instanceof CIdExpression) {
      CIdExpression lhsIdExpression = (CIdExpression) pLeftHandSide;
      CSimpleDeclaration lhsDeclaration = lhsIdExpression.getDeclaration();
      String lhsIdentifier = lhsDeclaration.getQualifiedName();
    } else {
      CLeftHandSideVisitor visitor = newLeftHandSideVisitor() {
    }
    return new HarnessState(
        newLocations,
        pointers,
        externallyKnownLocations,
        externFunctionCalls).addPointsToInformation(pAssigneeLocation, assignedLocation);
  }


  public HarnessState addMemoryLocation(CVariableDeclaration pDeclaration) {
    String variableName = pDeclaration.getName();
    MemoryLocation newLocation = new MemoryLocation(variableName);
    ImmutableSet.Builder<MemoryLocation> memoryLocationsBuilder = ImmutableSet.builder();
    memoryLocationsBuilder.addAll(memoryLocations);
    memoryLocationsBuilder.add(newLocation);
    ImmutableSet<MemoryLocation> newLocations = memoryLocationsBuilder.build();
    return new HarnessState(
        newLocations,
        pointers,
        externallyKnownLocations,
        externFunctionCalls);
  }

  public HarnessState addMemoryLocation(MemoryLocation pMemoryLocation) {
    ImmutableSet.Builder<MemoryLocation> memoryLocationsBuilder = ImmutableSet.builder();
    memoryLocationsBuilder.addAll(memoryLocations);
    memoryLocationsBuilder.add(pMemoryLocation);
    ImmutableSet<MemoryLocation> newLocations = memoryLocationsBuilder.build();
    return new HarnessState(
        newLocations,
        pointers,
        externallyKnownLocations,
        externFunctionCalls);
  }

  public HarnessState
      addPointsToInformation(MemoryLocation pAssigneeLocation, MemoryLocation pAssignedLocation) {
    PointerState newPointers = pointers.addPointer(pAssigneeLocation, pAssignedLocation);
    return new HarnessState(
        memoryLocations,
        newPointers,
        externallyKnownLocations,
        externFunctionCalls);
  }

  public HarnessState addExternallyKnownPointer(MemoryLocation pPointerLocation) {
    PersistentList<MemoryLocation> newExternallyKnownPointers =
        externallyKnownLocations.with(pPointerLocation);
    return new HarnessState(
        memoryLocations,
        pointers,
        newExternallyKnownPointers,
        externFunctionCalls);
  }

  private boolean isAddressOperation(CExpression pExpression) {
    if (pExpression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression) pExpression;
      UnaryOperator operator = unaryExpression.getOperator();
      return operator.getOperator() == "&";
    }
    return false;
  }



  public HarnessState addPointerDeclaration(CVariableDeclaration pVariableDeclaration) {
    String identifier = pVariableDeclaration.getName();
    MemoryLocation pointerLocation = new MemoryLocation(identifier);
    MemoryLocation pointerTarget = new MemoryLocation();
    PointerState newPointers =
        pointers.addPointer(pointerLocation, pointerTarget);
    return new HarnessState(
        memoryLocations,
        newPointers,
        externallyKnownLocations,
        externFunctionCalls);
  }

  public HarnessState
      addExternFunctionCallNoPointerParam(ComparableFunctionDeclaration pFunctionDeclaration) {
    ExternFunctionCallsState newExternFunctionCallsState =
        externFunctionCalls.addExternFunctionCall(pFunctionDeclaration);
    return new HarnessState(
        memoryLocations,
        pointers,
        externallyKnownLocations,
        newExternFunctionCallsState);
  }

  public HarnessState addExternFunctionCallWithPointerParams(CFunctionCallExpression pExpression) {
   /* List<MemoryLocation> pointerParams =
        pExpression.getParameterExpressions()
            .stream()
            .filter(param -> param.getExpressionType().getCanonicalType() instanceof CPointerType)
            .map(param -> (CIdExpression) param)
            .map(param -> param.getName())
            .map(param -> pointers.fromIdentifier(param).get())
            .filter(param -> (param != null))
            .collect(Collectors.toList());*/
    /*
     * PersistentList<MemoryLocation> newExternallyKnownPointers =
     * externallyKnownPointers.withAll(pExpression.getParameterExpressions()); return new
     * HarnessState( pointers, newExternallyKnownPointers, externFunctionCalls);
     */
    return this;
  }

  public HarnessState
      addPointsToInformation(CExpression pLeftHandSide, MemoryLocation pLocation) {
    // TODO: handle cases where leftHandSide is not an IdExpression, e.g. a[3] = malloc()
    CIdExpression idExpression = (CIdExpression) pLeftHandSide;
    String lhsName = idExpression.getName();
    MemoryLocation lhsLocation = pointers.fromIdentifier(lhsName);
    PointerState newPointers = pointers.addPointerIfNotExists(lhsLocation, pLocation);
    HarnessState newState =
        new HarnessState(
            memoryLocations,
            newPointers,
            externallyKnownLocations,
            externFunctionCalls);
    return newState;
  }

  public HarnessState merge(CExpression pOperand1, CExpression pOperand2) {
    final MemoryLocation firstPointerValue = pointers.getValue(pOperand1);
    final MemoryLocation secondPointerValue = pointers.getValue(pOperand2);

    final PointerState newPointers = pointers.merge(firstPointerValue, secondPointerValue);
    final ExternFunctionCallsState newExternFunctionCallsState =
        externFunctionCalls.merge(firstPointerValue, secondPointerValue);

    return new HarnessState(
        memoryLocations,
        newPointers,
        externallyKnownLocations,
        newExternFunctionCallsState);
  }

  public List<Integer> getIndices(ComparableFunctionDeclaration pFunctionName) {
    List<MemoryLocation> locations = externFunctionCalls.getCalls(pFunctionName);
    List<Integer> result =
        locations.stream().map(location -> getIndex(location)).collect(Collectors.toList());
    return result;
  }

  public Set<ComparableFunctionDeclaration> getFunctionsWithIndices() {
    Set<ComparableFunctionDeclaration> functionCallsKeys = externFunctionCalls.getKeys();
    return functionCallsKeys;
  }

  private int getIndex(MemoryLocation pLocation) {
    int result = externallyKnownLocations.indexOf(pLocation);
    if (result == -1) {
      result = 0;
    }
    return result;
  }

  public MemoryLocation getLocationFromInitializer(CInitializer pInitializer) {
    return pointers.getLocationFromInitializer(pInitializer);
  }

  public int getExternPointersArrayLength() {
    // off by one for case "alloc_complexexpression"
    return externallyKnownLocations.size() + 1;
  }

  public HarnessState updatePointerTarget(CAssignment pAssignment) {
    return this;
  }

  public HarnessState
      addExternallyKnownLocations(List<CExpression> pFunctionParameters) {
    // TODO: handle case that arguments are not given by id expressions, like foo(&i);
    List<String> locationIdentifiers =
        pFunctionParameters.stream()
            .map(cExpression -> (CIdExpression) cExpression)
            .map(idExpression -> idExpression.getName())
            .collect(Collectors.toList());
    List<MemoryLocation> pointerArgumentKeys =
        locationIdentifiers.stream().map(identifier -> pointers.fromIdentifier(identifier)).collect(
            Collectors.toList());
    List<MemoryLocation> pointerArgumentValues =
        pointerArgumentKeys.stream().map(location -> pointers.getTarget(location)).collect(
            Collectors.toList());
    List<MemoryLocation> newExternallyKnownPointers = new LinkedList<>(externallyKnownLocations);
    newExternallyKnownPointers.addAll(pointerArgumentValues);
    PersistentList<MemoryLocation> persistentNewExternallyKnownPointers =
        PersistentLinkedList.copyOf(newExternallyKnownPointers);
    HarnessState newState =
        new HarnessState(
            memoryLocations,
            pointers,
            persistentNewExternallyKnownPointers,
            externFunctionCalls);
    return newState;
  }

  public HarnessState addMemoryLocations(List<CDeclaration> pPointerMembers) {
    // TODO Auto-generated method stub
    return null;
  }
}
