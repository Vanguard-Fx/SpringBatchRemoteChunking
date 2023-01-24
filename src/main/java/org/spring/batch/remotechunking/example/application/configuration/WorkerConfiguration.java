package org.spring.batch.remotechunking.example.application.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.batch.remotechunking.example.application.WorkerService;
import org.spring.batch.remotechunking.example.application.inbound.ItemRequestDto;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Profile("worker")
@EnableBatchProcessing
@EnableBatchIntegration
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WorkerConfiguration {

  private final RemoteChunkingWorkerBuilder<ItemRequestDto, ItemRequestDto>
      remoteChunkingWorkerBuilder;

  private final KafkaTemplate<String, ItemRequestDto> dettaglioKafkaTemplate;

  private final WorkerService workerService;

  @Bean
  public IntegrationFlow workerStep() {
    return this.remoteChunkingWorkerBuilder
        .inputChannel(inboundChannel())
        .outputChannel(outboundChannel())
        .itemProcessor(workerService::doJob)
        .itemWriter(items -> log.info("item writing: {}", items.size()))
        .build();
  }

  @Bean
  public QueueChannel inboundChannel() {
    return new QueueChannel();
  }

  @Bean
  public IntegrationFlow inboundFlow(ConsumerFactory<String, ItemRequestDto> consumerFactory) {
    return IntegrationFlow.from(
            Kafka.messageDrivenChannelAdapter(consumerFactory, "dett-chunkRequests"))
        .channel(inboundChannel())
        .get();
  }

  @Bean
  public DirectChannel outboundChannel() {
    return new DirectChannel();
  }

  @Bean
  public IntegrationFlow outboundFlow() {
    var producerMessageHandler =
        new KafkaProducerMessageHandler<String, ItemRequestDto>(dettaglioKafkaTemplate);
    producerMessageHandler.setTopicExpression(new LiteralExpression("dett-chunkReplies"));
    return IntegrationFlow.from(outboundChannel()).handle(producerMessageHandler).get();
  }
}
