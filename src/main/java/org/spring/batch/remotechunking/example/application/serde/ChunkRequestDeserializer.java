package org.spring.batch.remotechunking.example.application.serde;

import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.batch.integration.chunk.ChunkRequest;
import org.springframework.util.SerializationUtils;

public class ChunkRequestDeserializer implements Deserializer<ChunkRequest> {
  @Override
  public ChunkRequest deserialize(String s, byte[] bytes) {
    return (ChunkRequest) SerializationUtils.deserialize(bytes);
  }
}
