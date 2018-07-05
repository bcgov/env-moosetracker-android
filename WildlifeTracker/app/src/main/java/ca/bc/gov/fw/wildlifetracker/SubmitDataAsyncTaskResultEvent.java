package ca.bc.gov.fw.wildlifetracker;

/**
 * Result of a SubmitDataAsyncTask.
 */
public class SubmitDataAsyncTaskResultEvent {

    public enum SubmitStatus {
        NotSubmitted(0),
        Success(1),
        Failed(2);

        private int code;

        SubmitStatus(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private String message_;
    private SubmitStatus status_;

    public SubmitDataAsyncTaskResultEvent(String message, SubmitStatus status) {
        message_ = message;
        status_ = status;
    }

    public String getMessage() {
        return message_;
    }

    public SubmitStatus getStatus() { return status_; }

}
