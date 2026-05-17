/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.exception;

/**
 * When have some problem to write download files
 */
public class StorageException extends RuntimeException {
    /**
     * Default constructor
     */
    public StorageException() {
        super("File diferent size local - web");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public StorageException(String message) {
        super(message);
    }
}
