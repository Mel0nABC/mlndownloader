package dev.mel0n.exception;

public class FileAlreadyDownloadederException extends RuntimeException {
    /**
     * Default constructor
     */
    public FileAlreadyDownloadederException() {
        super("File already downloaded");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public FileAlreadyDownloadederException(String message) {
        super(message);
    }
}
