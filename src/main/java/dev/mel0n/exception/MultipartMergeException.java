package dev.mel0n.exception;

public class MultipartMergeException extends RuntimeException {
    /**
     * Default constructor
     */
    public MultipartMergeException() {
        super("Multipart merge exception");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public MultipartMergeException(String message) {
        super(message);
    }
}
