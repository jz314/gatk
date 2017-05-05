package org.broadinstitute.hellbender.tools.spark.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class FlatMapGluer<I,O> implements Iterator<O> {
    private final Function<I,Iterator<O>> flatMapFunc;
    private final Iterator<? extends I> inputIterator;
    private I sentinel;
    private Iterator<O> outputIterator;

    public FlatMapGluer( final Function<I,Iterator<O>> flatMapFunc,
                         final Iterator<? extends I> inputIterator ) {
        this.flatMapFunc = flatMapFunc;
        this.inputIterator = inputIterator;
        this.sentinel = null;
        this.outputIterator = Collections.emptyIterator();
    }

    public FlatMapGluer( final Function<I,Iterator<O>> flatMapFunc,
                         final Iterator<? extends I> inputIterator,
                         final I sentinel ) {
        this.flatMapFunc = flatMapFunc;
        this.inputIterator = inputIterator;
        this.sentinel = sentinel;
        this.outputIterator = Collections.emptyIterator();
    }

    @Override
    public boolean hasNext()
    {
        while ( !outputIterator.hasNext() ) {
            if ( inputIterator.hasNext() ) outputIterator = flatMapFunc.apply(inputIterator.next());
            else if ( sentinel != null ) {
                outputIterator = flatMapFunc.apply(sentinel);
                sentinel = null;
            }
            else return false;
        }
        return true;
    }

    @Override
    public O next()
    {
        if ( !hasNext() ) throw new NoSuchElementException("Iteration is exhausted.");
        return outputIterator.next();
    }

    public static <I,O> Iterator<O> applyMapFunc(final Function<I,Iterator<O>> flatMapFunc,
                                                 final Iterator<? extends I> inputIterator ) {
        return new FlatMapGluer<>(flatMapFunc,inputIterator,null);
    }

    public static <I,O> Iterator<O> applyMapFunc(final Function<I,Iterator<O>> flatMapFunc,
                                                 final Iterator<? extends I> inputIterator,
                                                 final I sentinel ) {
        return new FlatMapGluer<>(flatMapFunc,inputIterator,sentinel);
    }
}
