package com.datn.pc_store.common.exception;

/**
 * Ngoại lệ biểu thị xung đột trạng thái hoặc quy trình nghiệp vụ (HTTP 409 Conflict).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
