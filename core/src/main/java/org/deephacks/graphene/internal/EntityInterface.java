package org.deephacks.graphene.internal;

import org.deephacks.graphene.internal.serialization.KeySerialization.KeyWriter;
import org.deephacks.graphene.internal.serialization.ValueSerialization.ValueWriter;

import java.io.IOException;

public interface EntityInterface {

  public boolean isEmbedded();

  public byte[][] serialize(KeyWriter keyWriter, ValueWriter valueWriter, int schemaId) throws IOException;

  public byte[] serializeKey(KeyWriter keyWriter, int schemaId) throws IOException;

  public byte[] serializeValue(ValueWriter valueWriter) throws IOException;
}
