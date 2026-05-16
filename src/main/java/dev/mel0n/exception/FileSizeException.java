package dev.mel0n.exception;

/**
 * When file is not pressent on data base
 */
public class FileSizeException extends RuntimeException {
    /**
     * Default constructor
     */
    public FileSizeException() {
        super("File diferent size local - web");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public FileSizeException(String message) {
        super(message);
    }
}
