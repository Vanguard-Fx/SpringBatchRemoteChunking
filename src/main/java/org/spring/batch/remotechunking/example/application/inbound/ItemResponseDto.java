package org.spring.batch.remotechunking.example.application.inbound;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ItemResponseDto implements Serializable {
  UUID id;
  Integer order;
  Exception errore;
}
