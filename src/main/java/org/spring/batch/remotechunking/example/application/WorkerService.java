package org.spring.batch.remotechunking.example.application;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.batch.remotechunking.example.application.inbound.ItemRequestDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerService {
  public ItemRequestDto doJob(ItemRequestDto requestDto) {
    var xxx = requestDto.getOrder();

    log.info("item processing: {}", xxx);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return ItemRequestDto.builder().id(UUID.randomUUID()).build();
  }
}
