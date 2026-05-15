package com.safeher.ratingservice.exception;
public class DuplicateRatingException extends RuntimeException {
    public DuplicateRatingException(String msg) { super(msg); }
}
