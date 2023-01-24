package org.spring.batch.remotechunking.example.application;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("manager")
@Component
@RequiredArgsConstructor
public class JobScheduler {
  private final Job managerJob;

  private final TaskExecutorJobLauncher jobLauncher;
  private final TaskExecutor batchTaskScheduler;

  @Scheduled(cron = "*/2 * * * * ?")
  public void perform() throws Exception {
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("jobId", String.valueOf(UUID.randomUUID()))
            .toJobParameters();

    jobLauncher.setTaskExecutor(batchTaskScheduler);

    jobLauncher.run(managerJob, jobParameters);
  }
}
