package org.deephacks.graphene;

import com.sleepycat.je.ForeignConstraintException;
import org.deephacks.graphene.internal.RowKey;

public class ForeignKeyConstraintException extends RuntimeException {

    public ForeignKeyConstraintException(String message) {
        super(message);
    }

    public ForeignKeyConstraintException(ForeignConstraintException cause) {
        super("Primary " + new RowKey(cause.getPrimaryKey().getData()) + " Secondary "
                + new RowKey(cause.getSecondaryKey().getData()), cause);
    }
}
