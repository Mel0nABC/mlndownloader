package dev.mel0n.exception;

public class FileAlreadyDownloadedException extends RuntimeException {
    /**
     * Default constructor
     */
    public FileAlreadyDownloadedException() {
        super("File already downloaded");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public FileAlreadyDownloadedException(String message) {
        super("File already downloaded: " + message);
    }
}
