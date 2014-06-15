package org.deephacks.graphene.internal;

import org.deephacks.graphene.internal.serialization.KeySerialization.KeyWriter;

import java.io.IOException;

public interface KeyInterface {

  public byte[] serializeKey(KeyWriter keyWriter, int schemaId) throws IOException;

}
