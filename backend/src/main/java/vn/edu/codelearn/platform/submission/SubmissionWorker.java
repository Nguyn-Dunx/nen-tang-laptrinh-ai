package vn.edu.codelearn.platform.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.codelearn.platform.sandbox.SandboxConfigurationException;
import vn.edu.codelearn.platform.sandbox.SandboxExecutionException;
import vn.edu.codelearn.platform.sandbox.SandboxLimitRepository;

@Component
@Profile("worker")
class SubmissionWorker {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissionWorker.class);

    private final SubmissionQueue queue;
    private final GradingRepository grading;
    private final GradingService grader;
    private final SandboxLimitRepository limits;

    SubmissionWorker(
            SubmissionQueue queue,
            GradingRepository grading,
            GradingService grader,
            SandboxLimitRepository limits) {
        this.queue = queue;
        this.grading = grading;
        this.grader = grader;
        this.limits = limits;
    }

    @Scheduled(fixedDelayString = "${sandbox.worker.poll-interval-ms}")
    void processNext() {
        Long submissionId = dequeue();
        if (submissionId == null) {
            submissionId = grading.nextQueuedSubmissionId().orElse(null);
        }
        if (submissionId == null) {
            return;
        }
        grading.claim(submissionId).ifPresent(this::grade);
    }

    @Scheduled(fixedDelayString = "${sandbox.worker.recovery-interval-ms}")
    void recoverStuckSubmissions() {
        long threshold = limits.get("stuck_timeout_seconds");
        long maxAttempts = limits.get("max_attempts");
        long retryDelay = limits.get("retry_backoff_seconds");
        int batchSize = Math.toIntExact(limits.get("recovery_batch_size"));
        for (var stuck : grading.findStuck(threshold, batchSize)) {
            if (grading.recoverStuck(stuck, maxAttempts, retryDelay)) {
                enqueueForRecovery(stuck.id());
            }
        }
    }

    private Long dequeue() {
        try {
            return queue.dequeue();
        } catch (DataAccessException exception) {
            LOG.error("Redis queue unavailable; checking PostgreSQL for queued work", exception);
            return null;
        }
    }

    private void grade(SubmissionViews.WorkerSubmission submission) {
        try {
            var result = grader.grade(submission);
            if (grading.complete(submission, result)) {
                LOG.info(
                        "Submission {} completed with verdict {}",
                        submission.id(),
                        result.verdict());
            }
        } catch (SandboxExecutionException | SandboxConfigurationException exception) {
            retryOrFail(submission, exception);
        }
    }

    private void retryOrFail(
            SubmissionViews.WorkerSubmission submission, RuntimeException failure) {
        long maxAttempts = limits.get("max_attempts");
        long backoff = limits.get("retry_backoff_seconds");
        long delay = Math.multiplyExact(backoff, submission.attempt());
        LOG.warn(
                "Sandbox grading failed for submission {} on attempt {}: {}",
                submission.id(),
                submission.attempt(),
                failure.getMessage());
        if (grading.retryOrFail(submission, maxAttempts, delay)) {
            enqueueForRecovery(submission.id());
        }
    }

    private void enqueueForRecovery(long submissionId) {
        try {
            queue.enqueue(submissionId);
        } catch (DataAccessException exception) {
            LOG.error(
                    "Submission {} remains queued for PostgreSQL recovery",
                    submissionId,
                    exception);
        }
    }
}
