/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.layoutmgr;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.properties.Constants;
import org.apache.fop.area.*;

import java.util.ArrayList;
import java.util.List;

/**
 * LayoutManager for an fo:flow object.
 * Its parent LM is the PageLayoutManager.
 * This LM is responsible for getting columns of the appropriate size
 * and filling them with block-level areas generated by its children.
 */
public class FlowLayoutManager extends BlockStackingLayoutManager {

    ArrayList blockBreaks = new ArrayList();      

    /** Array of areas currently being filled stored by area class */
    private BlockParent[] currentAreas = new BlockParent[Area.CLASS_MAX];

    /**
     * This is the top level layout manager.
     * It is created by the PageSequence FO.
     */
    public FlowLayoutManager(FObj fobj) {
        super(fobj);
    }

    public BreakPoss getNextBreakPoss(LayoutContext context,
                                      Position prevLineBP) {

        BPLayoutManager curLM ; // currently active LM
        MinOptMax stackSize = new MinOptMax();

        while ((curLM = getChildLM()) != null) {
            // Make break positions and return page break
            // Set up a LayoutContext
            MinOptMax bpd = context.getStackLimit();
            BreakPoss bp;

            LayoutContext childLC = new LayoutContext(0);
            boolean breakPage = false;
            childLC.setStackLimit(MinOptMax.subtract(bpd, stackSize));
            childLC.setRefIPD(context.getRefIPD());

            if (!curLM.isFinished()) {
                if ((bp = curLM.getNextBreakPoss(childLC, null)) != null) {
                    stackSize.add(bp.getStackingSize());
                    blockBreaks.add(bp);
                    // set stackLimit for remaining space
                    childLC.setStackLimit(MinOptMax.subtract(bpd, stackSize));

                    if(bp.isForcedBreak()) {
                        breakPage = true;
                        break;
                    }
                }
            }

            // check the stack bpd and if greater than available
            // height then go to the last best break and return
            // break position
            if(stackSize.min > context.getStackLimit().opt) {
                breakPage = true;
            }
            if(breakPage) {
                return new BreakPoss(
			     new LeafPosition(this, blockBreaks.size() - 1));
            }
        }
        setFinished(true);
        if(blockBreaks.size() > 0) {
            return new BreakPoss(
                             new LeafPosition(this, blockBreaks.size() - 1));
        }
        return null;
    }

    public void addAreas(PositionIterator parentIter, LayoutContext layoutContext) {

        BPLayoutManager childLM ;
        int iStartPos = 0;
        LayoutContext lc = new LayoutContext(0);
        while (parentIter.hasNext()) {
            LeafPosition lfp = (LeafPosition) parentIter.next();
            // Add the block areas to Area
            PositionIterator breakPosIter =
              new BreakPossPosIter(blockBreaks, iStartPos,
                                   lfp.getLeafPos() + 1);
            iStartPos = lfp.getLeafPos() + 1; 
            while ((childLM = breakPosIter.getNextChildLM()) != null) {
                childLM.addAreas(breakPosIter, lc);
            }
        }

        flush();
        // clear the breaks for the page to start for the next page
        blockBreaks.clear();
    }


    /**
     * Add child area to a the correct container, depending on its
     * area class. A Flow can fill at most one area container of any class
     * at any one time. The actual work is done by BlockStackingLM.
     */
    public boolean addChild(Area childArea) {
        return addChildToArea(childArea,
                              this.currentAreas[childArea.getAreaClass()]);
    }

    public Area getParentArea(Area childArea) {
        // Get an area from the Page
        BlockParent parentArea =
          (BlockParent) parentLM.getParentArea(childArea);
        this.currentAreas[parentArea.getAreaClass()] = parentArea;
        setCurrentArea(parentArea);
        return parentArea;
    }

    public void resetPosition(Position resetPos) {
        if (resetPos == null) {
            reset(null);
        }
    }
}

