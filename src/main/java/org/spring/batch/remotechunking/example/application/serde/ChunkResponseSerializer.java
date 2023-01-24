package org.spring.batch.remotechunking.example.application.serde;

import org.apache.kafka.common.serialization.Serializer;
import org.springframework.batch.integration.chunk.ChunkResponse;
import org.springframework.util.SerializationUtils;

public class ChunkResponseSerializer implements Serializer<ChunkResponse> {
  @Override
  public byte[] serialize(String s, ChunkResponse chunkResponse) {
    return SerializationUtils.serialize(chunkResponse);
  }
}
