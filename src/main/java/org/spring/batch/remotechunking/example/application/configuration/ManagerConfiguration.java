package org.spring.batch.remotechunking.example.application.configuration;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.spring.batch.remotechunking.example.application.inbound.ItemRequestDto;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilderFactory;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.xml.sax.SAXException;

@Profile("manager")
@Configuration
@EnableBatchProcessing
@EnableBatchIntegration
@RequiredArgsConstructor
@Slf4j
public class ManagerConfiguration {
  private final RemoteChunkingManagerStepBuilderFactory remoteChunkingManagerStepBuilderFactory;

  private final KafkaTemplate<String, ItemRequestDto> dettaglioKafkaTemplate;

  @Bean
  public TaskExecutor batchTaskScheduler() {
    return new ConcurrentTaskExecutor(
        Executors.newFixedThreadPool(5, new CustomizableThreadFactory("jobExecutor-")));
    //    ThreadPoolTaskExecutor threadPoolTaskScheduler = new ThreadPoolTaskExecutor();
    //    threadPoolTaskScheduler.setQueueCapacity(100);
    //    threadPoolTaskScheduler.setMaxPoolSize(5);
    //    threadPoolTaskScheduler.setCorePoolSize(5);
    //    threadPoolTaskScheduler.setThreadNamePrefix("jobExecutor-");
    //    threadPoolTaskScheduler.afterPropertiesSet();
    //    return threadPoolTaskScheduler;
  }

  @Bean
  public Job managerJob(JobRepository jobRepository, Step infoStepManager) {
    return new JobBuilder("Dett-Info-Manager-job", jobRepository)
        .incrementer(new RunIdIncrementer())
        .start(infoStepManager)
        .build();
  }

  @Bean
  @JobScope
  public TaskletStep infoStepManager(CustomDataReader customDataReader) {
    return this.remoteChunkingManagerStepBuilderFactory
        .get("Reader-Manager-Step")
        .<ItemRequestDto, ItemRequestDto>chunk(10)
        .reader(customDataReader)
        .outputChannel(outboundChannel())
        .inputChannel(inboundChannel())
        .build();
  }

  @Bean
  public ChunkMessageChannelItemWriter<ItemRequestDto> chunkMessageChannelItemWriter() {
    return new ChunkMessageChannelItemWriter<ItemRequestDto>();
  }

  @Bean
  public DirectChannel outboundChannel() {
    return new DirectChannel();
  }

  @Bean
  public QueueChannel inboundChannel() {
    return new QueueChannel();
  }

  @Bean
  public IntegrationFlow outboundFlow() {
    var producerMessageHandler =
        new KafkaProducerMessageHandler<String, ItemRequestDto>(dettaglioKafkaTemplate);
    producerMessageHandler.setTopicExpression(new LiteralExpression("dett-chunkRequests"));
    return IntegrationFlow.from(outboundChannel()).handle(producerMessageHandler).get();
  }

  @Bean
  public IntegrationFlow inboundFlow(ConsumerFactory<String, ItemRequestDto> consumerFactory) {

    consumerFactory.updateConfigs(
        Map.of(ConsumerConfig.GROUP_ID_CONFIG, "elab-manager-" + UUID.randomUUID()));

    return IntegrationFlow.from(
            Kafka.messageDrivenChannelAdapter(consumerFactory, "dett-chunkReplies"))
        .channel(inboundChannel())
        .get();
  }

  @Bean
  @JobScope
  public CustomDataReader customDataReader()
      throws ParserConfigurationException, IOException, SAXException,
          TransformerConfigurationException {
    return new CustomDataReader(new ClassPathResource("/data/data.xml"));
  }

  public static class CustomDataReader implements ItemReader<ItemRequestDto> {
    private List<ItemRequestDto> dettagli;
    // Leggo il file dei dati

    public CustomDataReader(ClassPathResource path)
        throws ParserConfigurationException, IOException, SAXException,
            TransformerConfigurationException {

      var documentBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();

      Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      var nodeList = documentBuilder.parse(path.getInputStream()).getElementsByTagName("id");

      dettagli =
          IntStream.range(0, nodeList.getLength())
              .mapToObj(nodeList::item)
              .map(
                  idNode -> {
                    try {

                      var ordineStampa =
                          Integer.valueOf(
                              idNode.getAttributes().getNamedItem("order").getNodeValue());

                      var sw = new StringWriter();
                      transformer.transform(new DOMSource(idNode), new StreamResult(sw));

                      return ItemRequestDto.builder()
                          .order(ordineStampa)
                          .data(sw.toString())
                          .build();

                    } catch (TransformerException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .collect(Collectors.toList());
    }

    public ItemRequestDto read() {
      if (!dettagli.isEmpty()) {
        return dettagli.remove(0);
      }
      return null;
    }
  }
}
