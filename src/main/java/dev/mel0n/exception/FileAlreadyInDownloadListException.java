package dev.mel0n.exception;

public class FileAlreadyInDownloadListException extends RuntimeException {
    /**
     * Default constructor
     */
    public FileAlreadyInDownloadListException() {
        super("File already in download list");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public FileAlreadyInDownloadListException(String message) {
        super("File already downloaded: " + message);
    }
}
