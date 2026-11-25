package vn.edu.codelearn.platform.submission;

interface SubmissionQueue {

    void enqueue(long submissionId);

    Long dequeue();

    void checkConnection();
}
