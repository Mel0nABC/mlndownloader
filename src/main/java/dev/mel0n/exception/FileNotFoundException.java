/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.exception;

/**
 * When file is not pressent on data base
 */
public class FileNotFoundException extends RuntimeException {
    /**
     * Default constructor
     */
    public FileNotFoundException() {
        super("File not found exception");
    }

    /**
     * Constructor with specific message
     * 
     * @param message specific message String
     */
    public FileNotFoundException(String message) {
        super(message);
    }
}
