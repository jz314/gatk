package org.broadinstitute.hellbender.tools.spark.sv;

public class PartitionEdges {
    private final int beginningContigID;
    private final int beginningPosition;
    private final int endingContigID;
    private final int endingPosition;

    public PartitionEdges( final int partitionIdx,
                           final ReadMetadata readMetadata,
                           final int edgeWidth ) {
        if ( partitionIdx == 0 ) {
            beginningContigID = ReadMetadata.PartitionBounds.UNMAPPED;
            beginningPosition = -1;
        } else {
            final ReadMetadata.PartitionBounds bounds = readMetadata.getPartitionBounds(partitionIdx - 1);
            beginningContigID = bounds.getLastContigID();
            if ( beginningContigID == ReadMetadata.PartitionBounds.UNMAPPED ) {
                beginningPosition = -1;
            } else {
                beginningPosition = bounds.getLastStart() + edgeWidth;
            }
        }
        if (partitionIdx == readMetadata.getNPartitions() - 1) {
            endingContigID = ReadMetadata.PartitionBounds.UNMAPPED;
            endingPosition = Integer.MAX_VALUE;
        } else {
            final ReadMetadata.PartitionBounds bounds = readMetadata.getPartitionBounds(partitionIdx + 1);
            endingContigID = bounds.getFirstContigID();
            if ( endingContigID == ReadMetadata.PartitionBounds.UNMAPPED ) {
                endingPosition = Integer.MAX_VALUE;
            } else {
                endingPosition = bounds.getFirstStart() - edgeWidth;
            }
        }
    }

    public boolean onEdge( final SVInterval interval ) {
        final int contigID = interval.getContig();
        final int position = interval.getStart();
        return (contigID == beginningContigID && position < beginningPosition) ||
                (contigID == endingContigID && position >= endingPosition);
    }
}
