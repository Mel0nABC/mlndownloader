package dev.mel0n.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.mel0n.exception.FileAlreadyDownloadederException;
import dev.mel0n.exception.MultipartMergeException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileAlreadyDownloadederException.class)
    public ResponseEntity<Map<String, Object>> fileAlreadyDownloadederException(Exception e) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    @ExceptionHandler(MultipartMergeException.class)
    public ResponseEntity<Map<String, Object>> multipartMergeException(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

}
