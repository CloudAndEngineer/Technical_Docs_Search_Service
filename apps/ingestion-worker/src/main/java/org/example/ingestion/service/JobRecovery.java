package org.example.ingestion.service;

import org.example.ingestion.repository.IngestionJobRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JobRecovery implements ApplicationRunner {

	private final IngestionJobRepository jobs;

	public JobRecovery(IngestionJobRepository jobs) {
		this.jobs = jobs;
	}

	@Override
	public void run(ApplicationArguments args) {
		jobs.failUnfinishedJobsAfterRestart();
	}
}
