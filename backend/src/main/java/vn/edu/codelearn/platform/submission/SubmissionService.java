package vn.edu.codelearn.platform.submission;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.codelearn.platform.common.ApiException;
import vn.edu.codelearn.platform.identity.CurrentUser;
import vn.edu.codelearn.platform.sandbox.SandboxLimitRepository;

@Service
class SubmissionService {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissionService.class);
    private static final String LANGUAGE = "python";

    private final CurrentUser currentUser;
    private final SubmissionRepository submissions;
    private final SandboxLimitRepository limits;
    private final SubmissionQueue queue;
    private final SubmissionRateLimiter rateLimiter;

    SubmissionService(
            CurrentUser currentUser,
            SubmissionRepository submissions,
            SandboxLimitRepository limits,
            SubmissionQueue queue,
            SubmissionRateLimiter rateLimiter) {
        this.currentUser = currentUser;
        this.submissions = submissions;
        this.limits = limits;
        this.queue = queue;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    SubmissionViews.Receipt submit(long exerciseId, SubmissionViews.CreateSubmission request) {
        long userId = currentUser.id();
        if (!submissions.publishedExerciseExists(exerciseId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND");
        }
        if (!LANGUAGE.equals(request.language())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LANGUAGE_NOT_SUPPORTED");
        }
        long maxCodeBytes = limits.get("code_size_max_bytes");
        if (request.sourceCode().getBytes(StandardCharsets.UTF_8).length > maxCodeBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "SOURCE_CODE_TOO_LARGE");
        }

        checkRateLimit(userId);

        submissions.lockAccountForSubmissions(userId);
        if (submissions.countPendingForUser(userId) >= limits.get("max_pending_per_user")) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_PENDING_SUBMISSIONS");
        }

        var receipt =
                submissions.insert(userId, exerciseId, request.language(), request.sourceCode());
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            queue.enqueue(receipt.submissionId());
                        } catch (DataAccessException exception) {
                            LOG.error(
                                    "Submission {} remains queued for worker recovery",
                                    receipt.submissionId(),
                                    exception);
                        }
                    }
                });
        return receipt;
    }

    private void checkRateLimit(long userId) {
        try {
            queue.checkConnection();
            if (!rateLimiter.allow(
                    userId,
                    limits.get("submit_rate_max"),
                    limits.get("submit_rate_window_seconds"))) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED");
            }
        } catch (DataAccessException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GRADER_UNAVAILABLE");
        }
    }

    @Transactional(readOnly = true)
    SubmissionViews.View get(long submissionId) {
        long userId = currentUser.id();
        var row =
                submissions
                        .findForUser(submissionId, userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND"));
        return submissions.toView(row);
    }

    @Transactional(readOnly = true)
    SubmissionViews.Detail getDetail(long submissionId) {
        long userId = currentUser.id();
        var row =
                submissions
                        .findDetailForUser(submissionId, userId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "SUBMISSION_NOT_FOUND"));
        return submissions.toDetail(row);
    }

    @Transactional(readOnly = true)
    SubmissionViews.Page history(long exerciseId, int page, int size) {
        long userId = currentUser.id();
        if (!submissions.publishedExerciseExists(exerciseId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EXERCISE_NOT_FOUND");
        }
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
        return new SubmissionViews.Page(
                submissions.findHistory(userId, exerciseId, page, size), page, size);
    }
}
