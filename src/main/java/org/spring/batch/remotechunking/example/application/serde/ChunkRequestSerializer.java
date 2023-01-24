package org.spring.batch.remotechunking.example.application.serde;

import org.apache.kafka.common.serialization.Serializer;
import org.springframework.batch.integration.chunk.ChunkRequest;
import org.springframework.util.SerializationUtils;

public class ChunkRequestSerializer implements Serializer<ChunkRequest> {
  @Override
  public byte[] serialize(String s, ChunkRequest chunkRequest) {
    if (chunkRequest == null) {
      return new byte[0];
    }
    return SerializationUtils.serialize(chunkRequest);
  }
}
