// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Class to represent a immutable bipartite symbolic memory graph. Manipulating methods return a
 * modified copy but do not modify a certain instance. Consists of (SMG-)objects, values, edges from
 * the objects to the values (has-value edges), edges from the values to objects (points-to edges)
 * and labelling functions (to get the kind, nesting level, size etc. of objects etc.)
 */
public class SMG {
  // TODO I don't like using utility implementations of the old SMG analysis
  private final PersistentSet<SMGObject> smgObjects;
  private final PersistentSet<SMGValue> smgValues;
  private final PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> hasValueEdges;
  private final PersistentMap<SMGValue, SMGPointsToEdge> pointsToEdges;

  private final SMGObject nullObject = SMGObject.nullInstance();

  /** Creates a new, empty SMG */
  public SMG() {
    pointsToEdges = PathCopyingPersistentTreeMap.of();
    hasValueEdges = PathCopyingPersistentTreeMap.of();
    smgValues = PersistentSet.of();
    smgObjects = PersistentSet.<SMGObject>of().addAndCopy(nullObject);
  }

  private SMG(
      PersistentSet<SMGObject> pSmgObjects,
      PersistentSet<SMGValue> pSmgValues,
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> pHasValueEdges,
      PersistentMap<SMGValue, SMGPointsToEdge> pPointsToEdges) {
    smgObjects = pSmgObjects;
    smgValues = pSmgValues;
    hasValueEdges = pHasValueEdges;
    pointsToEdges = pPointsToEdges;
  }

  /**
   * Creates a copy of the SMG an adds the given object.
   *
   * @param pObject - the object to be added
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddObject(SMGObject pObject) {
    return new SMG(smgObjects.addAndCopy(pObject), smgValues, hasValueEdges, pointsToEdges);
  }

  /**
   * Creates a copy of the SMG an adds the given value.
   *
   * @param pValue - the object to be added
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddValue(SMGValue pValue) {
    return new SMG(smgObjects, smgValues.addAndCopy(pValue), hasValueEdges, pointsToEdges);
  }

  /**
   * Creates a copy of the SMG an adds the given has value edge.
   *
   * @param edge - the edge to be added
   * @param source - the source object
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddHVEdge(SMGHasValueEdge edge, SMGObject source) {

    if (hasValueEdges.containsKey(source) && hasValueEdges.get(source).contains(edge)) {
      return this;
    }

    PersistentSet<SMGHasValueEdge> edges = hasValueEdges.getOrDefault(source, PersistentSet.of());
    edges = edges.addAndCopy(edge);
    return new SMG(smgObjects, smgValues, hasValueEdges.putAndCopy(source, edges), pointsToEdges);
  }

  /**
   * Creates a copy of the SMG an adds the given points to edge.
   *
   * @param edge - the edge to be added
   * @param source - the source value
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddPTEdge(SMGPointsToEdge edge, SMGValue source) {

    if (pointsToEdges.containsKey(source) && pointsToEdges.get(source).equals(edge)) {
      return this;
    }

    return new SMG(smgObjects, smgValues, hasValueEdges, pointsToEdges.putAndCopy(source, edge));
  }

  /**
   * Creates a copy of the SMG an adds the given has value edges.
   *
   * @param edges - the edges to be added
   * @param source - the source object
   * @return a modified copy of the SMG
   */
  public SMG copyAndSetHVEdges(PersistentSet<SMGHasValueEdge> edges, SMGObject source) {

    return new SMG(smgObjects, smgValues, hasValueEdges.putAndCopy(source, edges), pointsToEdges);
  }

  /**
   * Creates a copy of the SMG and adds the given points to edge.
   *
   * @param edge - the edge to be added
   * @param source - the source value
   * @return a modified copy of the SMG
   */
  public SMG copyAndSetPTEdges(SMGPointsToEdge edge, SMGValue source) {
    return new SMG(smgObjects, smgValues, hasValueEdges, pointsToEdges.putAndCopy(source, edge));
  }

  /**
   * Creates a copy of the SMG and replaces given object by a given new.
   *
   * @param pOldObject - the object to be replaced
   * @param pNewObject - the replacement
   * @return a modified copy
   */
  public SMG copyAndReplaceObject(SMGObject pOldObject, SMGObject pNewObject) {
    PersistentSet<SMGHasValueEdge> edges = hasValueEdges.get(pOldObject);
    // replace has value edges
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges =
        hasValueEdges.removeAndCopy(pOldObject).putAndCopy(pNewObject, edges);
    // replace points to edges
    PersistentMap<SMGValue, SMGPointsToEdge> newPointsToEdges = pointsToEdges;

    for (Map.Entry<SMGValue, SMGPointsToEdge> oldEntry : pointsToEdges.entrySet()) {
      if (pOldObject.equals(oldEntry.getValue().pointsTo())) {
        SMGPointsToEdge newEdge =
            new SMGPointsToEdge(pNewObject, oldEntry.getValue().getOffset(), oldEntry.getValue().targetSpecifier());
        newPointsToEdges = pointsToEdges.putAndCopy(oldEntry.getKey(), newEdge);
      }
    }

    //replace object
    PersistentSet<SMGObject> newObjects = smgObjects.removeAndCopy(pOldObject).addAndCopy(pNewObject);

    return new SMG(newObjects, smgValues, newHVEdges, newPointsToEdges);
  }

  /**
   * Returns the static null object.
   *
   * @return The null SMGObject.
   */
  public SMGObject getNullObject() {
    return nullObject;
  }

  /**
   * Returns all SMGObjects associated with this SMG in a set.
   *
   * @return The set of SMGObjects associated with this SMG.
   */
  public Set<SMGObject> getObjects() {
    return smgObjects;
  }

  /**
   * Returns all SMGValues associated with this SMG in a set.
   *
   * @return The set of SMGValues associated with this SMG.
   */
  public Set<SMGValue> getValues() {
    return smgValues;
  }

  /**
   * Returned the set of SMGHasValueEdges associated with the region that is specified by the
   * entered SMGObject. The region is an interval [object.offset, object.offset + object.size).
   *
   * @param pRegion SMGObject for whos region one wants the SMGHasValueEdges.
   * @return The set of SMGHasValueEdges associated with the region.
   */
  public Set<SMGHasValueEdge> getEdges(SMGObject pRegion) {
    return hasValueEdges.getOrDefault(pRegion, PersistentSet.of());
  }

  /**
   * Returns any SMGHasValueEdge associated with the entered SMGObject at the specified offset in an
   * Optional, or an empty Optional if no such SMGHasValueEdge exists.
   *
   * @param object SMGObject for which the SMGHasValueEdge are searched.
   * @param offset The offset that the SMGHasValueEdge has to have. May not be negative and must be
   *     in the region of the object.
   * @return Either an empty Optional if there is no such SMGHasValueEdge, or an Optional with some
   *     edge.
   */
  public Optional<SMGHasValueEdge> getHasValueEdgeByOffset(SMGObject object, BigInteger offset) {
    return hasValueEdges.get(object).stream().filter(o -> o.getOffset().equals(offset)).findAny();
  }

  /**
   * Returns any SMGHasValueEdge associated with the entered SMGObject at the specified offset and
   * size in an Optional, or an empty Optional if no such SMGHasValueEdge exists.
   *
   * @param object SMGObject for which the SMGHasValueEdge are searched.
   * @param offset The offset that the SMGHasValueEdge has to have. May not be negative and must be
   *     in the region of the object.
   * @param sizeInBits The size in bits that the SMGHasValueEdge has to have.
   * @return Either an empty Optional if there is no such SMGHasValueEdge, or an Optional with some
   *     edge.
   */
  public Optional<SMGHasValueEdge> getHasValueEdgeByOffsetAndSize(
      SMGObject object, BigInteger offset, BigInteger sizeInBits) {
    // TODO: Can there be more than one?
    return hasValueEdges
        .get(object)
        .stream()
        .filter(o -> o.getOffset().equals(offset) && o.getSizeInBits().equals(sizeInBits))
        .findAny();
  }

  /**
   * TODO: Check this method again once we can test the entire system! Why? Because in my opinion
   * one can interpret the specification of this method in 2 ways: 1. The field to be checked
   * (offset + size) has to be covered by a SINGLE nullObject. 2. The field to be checked has to be
   * covered by nullObjects (1 or multiple), such that it is covered entirely. (2. is the current
   * implementation)
   *
   * <p>This Method checks for the entered SMGObject if there exists SMGHasValueEdges such that the
   * field [offset; offset + size) is covered by nullObjects. Important: One may not take
   * SMGHasValueEdges into account which lay outside of the SMGObject! Else it would be possible to
   * read potentially invalid memory!
   *
   * @param object The SMGObject in which a field is to be checked for nullified blocks.
   * @param offset The offset (=start) of the field. Has to be inside of the object.
   * @param size The size in bits of the field. Has to be larger than the offset but still inside
   *     the field.
   * @return True if the field is indeed covered by nullified blocks. False else.
   */
  @SuppressWarnings("unused")
  private boolean isCoveredByNullifiedBlocks(SMGObject object, BigInteger offset, BigInteger size) {
    NavigableMap<BigInteger, BigInteger> nullEdgesRangeMap = getNullEdgesForObject(object);
    // We start at the beginning of the object itself, as the null edges may be larger than our
    // field.
    BigInteger currentMax = nullEdgesRangeMap.firstKey();
    // The first edge offset can't cover the entire field if it begins after the obj offset!
    if (currentMax.compareTo(offset) > 0) {
      return false;
    }
    BigInteger offsetPlusSize = offset.add(size);
    // TreeMaps keySet is ordered!
    for (Map.Entry<BigInteger, BigInteger> entry : nullEdgesRangeMap.entrySet()) {
      // The max encountered yet has to be bigger or eq to the next key.
      if (currentMax.compareTo(entry.getKey()) > 0) {
        return false;
      }
      currentMax = currentMax.max(entry.getValue());
      // If there are no gaps,
      // the max encountered has to be >= offset + size at some point.
      if (currentMax.compareTo(offsetPlusSize) >= 0) {
        return true;
      }
    }
    // The max encountered did not cover the entire field.
    return false;
  }

  /**
   * Returns the sorted Map<offset, max size> of SMGHasValueEdge of NullObjects that cover the
   * entered SMGObject somewhere. Only edges that do not exceed the boundries of the object are
   * used. It always defaults to the max size, such that no smaller size for a offset exists.
   * Example: <0, 16> and <0, 24> would result in <0, 24>.
   *
   * @param smgObject The SMGObject one wants to check for covering NullObjects.
   * @return TreeMap<offset, max size> of covering edges.
   */
  private ImmutableSortedMap<BigInteger, BigInteger> getNullEdgesForObject(SMGObject smgObject) {
    BigInteger offset = smgObject.getOffset();
    BigInteger offsetPlusSize = smgObject.getSize().add(offset);
    // Both inequalities have to hold, else one may read invalid memory outside of the object!
    // ObjectOffset <= HasValueEdgeOffset
    // HasValueEdgeOffset + HasValueEdgeSize <= ObjectOffset + ObjectSize
    return hasValueEdges
        .get(SMGObject.nullInstance())
        .stream()
        .filter(
            n ->
                offset.compareTo(n.getOffset()) <= 0
                    && offsetPlusSize.compareTo(n.getOffset().add(n.getSizeInBits())) >= 0)
        .collect(
            ImmutableSortedMap.toImmutableSortedMap(
                Comparator.naturalOrder(),
                SMGHasValueEdge::getOffset,
                SMGHasValueEdge::getSizeInBits,
                BigInteger::max));
  }

  /**
   * Returns a Set of all SMGDoublyLinkedListSegments of this SMG.
   *
   * @return The Set of all SMGDoublyLinkedListSegments.
   */
  public Set<SMGDoublyLinkedListSegment> getDLLs() {
    return smgObjects.stream()
        .filter(i -> i instanceof SMGDoublyLinkedListSegment)
        .map(i -> (SMGDoublyLinkedListSegment) i)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns all SMGHasValueEdges for this SMG in a Set.
   *
   * @return Set of all SMGHasValueEdges of this SMG.
   */
  public Set<SMGHasValueEdge> getHVEdges() {
    return hasValueEdges.values()
        .stream()
        .flatMap(Collection::stream)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns all SMGPointsToEdges for this SMG in a Collection.
   *
   * @return Collection of all SMGPointsToEdges of this SMG.
   */
  public Collection<SMGPointsToEdge> getPTEdges() {
    return pointsToEdges.values();
  }

  /**
   * Copies this SMG and returns the copy.
   *
   * @return A copy of this SMG.
   */
  public SMG copy() {
    return new SMG(smgObjects, smgValues, hasValueEdges, pointsToEdges);
  }

  /**
   * Returns the SMGPointsToEdge associated with the entered SMGValue.
   *
   * @param value The SMGValue for which the edge is to be returned.
   * @return The SMGPointsToEdge for the entered value.
   */
  public SMGPointsToEdge getPTEdge(SMGValue value) {
    // TODO: Is it guaranteed that there exists a edge for each value entered?
    // If it can be null, use a Optional.
    return pointsToEdges.get(value);
  }



}