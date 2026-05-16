/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.exception;

/**
 * When file is set downloaded
 */
public class FileAlreadyMerginException extends RuntimeException {
    /**
     * Default constructor
     */
    public FileAlreadyMerginException() {
        super("File not found exception");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public FileAlreadyMerginException(String message) {
        super(message);
    }
}
