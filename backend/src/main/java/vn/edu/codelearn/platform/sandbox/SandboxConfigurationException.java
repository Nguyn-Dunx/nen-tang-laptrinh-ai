package vn.edu.codelearn.platform.sandbox;

public class SandboxConfigurationException extends RuntimeException {

    public SandboxConfigurationException(String key) {
        super("Missing sandbox configuration: " + key);
    }
}
