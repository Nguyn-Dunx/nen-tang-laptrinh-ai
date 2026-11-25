package vn.edu.codelearn.platform.submission;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SubmissionController {

    private final SubmissionService submissions;

    SubmissionController(SubmissionService submissions) {
        this.submissions = submissions;
    }

    @PostMapping("/api/v1/exercises/{exerciseId}/submissions")
    ResponseEntity<SubmissionViews.Receipt> submit(
            @PathVariable long exerciseId,
            @Valid @RequestBody SubmissionViews.CreateSubmission request) {
        var receipt = submissions.submit(exerciseId, request);
        return ResponseEntity.accepted()
                .header(
                        HttpHeaders.LOCATION,
                        URI.create("/api/v1/submissions/" + receipt.submissionId()).toString())
                .body(receipt);
    }

    @GetMapping("/api/v1/submissions/{submissionId}")
    SubmissionViews.View get(@PathVariable long submissionId) {
        return submissions.get(submissionId);
    }

    @GetMapping("/api/v1/submissions/{submissionId}/detail")
    SubmissionViews.Detail detail(@PathVariable long submissionId) {
        return submissions.getDetail(submissionId);
    }

    @GetMapping("/api/v1/exercises/{exerciseId}/submissions")
    SubmissionViews.Page history(
            @PathVariable long exerciseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return submissions.history(exerciseId, page, size);
    }
}
