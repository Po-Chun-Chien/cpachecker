// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockTransferRelation extends LocationTransferRelation {

  private ImmutableSet<CFAEdge> edges;
  private ImmutableSet<CFANode> nodes;
  private LocationStateFactory factory;

  /**
   * This transfer relation produces successors iff an edge between two nodes exists in the CFA
   * and it is part of the block
   * @param pFactory factory for location states
   */
  public BlockTransferRelation(LocationStateFactory pFactory) {
    super(pFactory);
    factory = pFactory;
  }

  public void init(BlockNode pBlockNode) {
    edges = validEdgesIn(pBlockNode);
    nodes = ImmutableSet.copyOf(pBlockNode.getNodesInBlock());
  }

  private ImmutableSet<CFAEdge> validEdgesIn(BlockNode pBlockNode) {
    ImmutableSet.Builder<CFAEdge> setBuilder = ImmutableSet.builder();
    Set<CFANode> nodesInBlock = pBlockNode.getNodesInBlock();
    for(CFANode node: nodesInBlock) {
      for(CFAEdge edge: CFAUtils.allLeavingEdges(node)) {
        if (nodesInBlock.contains(edge.getSuccessor())) {
          setBuilder.add(edge);
        }
      }
    }
    return setBuilder.build();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    checkNotNull(edges, "init method must be called before starting the analysis (edges == null)");
    CFANode node = ((LocationState) element).getLocationNode();

    if (Sets.intersection(ImmutableSet.copyOf(CFAUtils.allLeavingEdges(node)), edges).contains(cfaEdge)) {
      return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
    }

    return ImmutableSet.of();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element, Precision prec) throws CPATransferException {
    checkNotNull(nodes, "init method must be called before starting the analysis (nodes == null)");
    CFANode node = ((LocationState) element).getLocationNode();
    return CFAUtils.successorsOf(node).filter(n -> nodes.contains(n)).transform(n -> factory.getState(n)).toList();
  }

}