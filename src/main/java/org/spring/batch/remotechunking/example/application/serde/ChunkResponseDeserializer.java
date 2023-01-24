package org.spring.batch.remotechunking.example.application.serde;

import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.batch.integration.chunk.ChunkResponse;
import org.springframework.util.SerializationUtils;

public class ChunkResponseDeserializer implements Deserializer<ChunkResponse> {
  @Override
  public ChunkResponse deserialize(String s, byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    return (ChunkResponse) SerializationUtils.deserialize(bytes);
  }
}
