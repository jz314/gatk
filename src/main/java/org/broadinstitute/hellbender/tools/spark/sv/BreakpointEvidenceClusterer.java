package org.broadinstitute.hellbender.tools.spark.sv;

import org.apache.commons.collections4.iterators.SingletonIterator;

import java.util.Collections;
import java.util.function.Function;
import java.util.Iterator;

/**
 * A class to examine a stream of BreakpointEvidence, and group it into Intervals.
 */
public final class BreakpointEvidenceClusterer implements Function<BreakpointEvidence, Iterator<BreakpointEvidence>> {
    private final int gapSize;
    private final PartitionEdges partitionEdges;
    private SVInterval curInterval;
    private int curWeight;
    private static final Iterator<BreakpointEvidence> EMPTY_ITERATOR = Collections.emptyIterator();

    public BreakpointEvidenceClusterer( final int gapSize, final PartitionEdges partitionEdges ) {
        this.gapSize = gapSize;
        this.partitionEdges = partitionEdges;
        this.curWeight = 0;
    }

    @Override
    public Iterator<BreakpointEvidence> apply( final BreakpointEvidence evidence ) {
        Iterator<BreakpointEvidence> result = EMPTY_ITERATOR;
        final SVInterval interval = evidence.getLocation();
        final int weight = evidence.getWeight();
        if ( curInterval == null ) {
            curInterval = interval;
            curWeight = weight;
        } else if ( !onEdge(interval) && !onEdge(curInterval) && curInterval.gapLen(interval) < gapSize ) {
            curInterval = curInterval.join(interval);
            curWeight += weight;
        } else {
            result = new SingletonIterator<>(new BreakpointEvidence(curInterval,curWeight));
            curInterval = interval;
            curWeight = weight;
        }
        return result;
    }

    private boolean onEdge( final SVInterval interval ) {
        return partitionEdges != null && partitionEdges.onEdge(interval);
    }
}
