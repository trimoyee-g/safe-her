package com.safeher.ratingservice.exception;

public class RatingNotFoundException extends RuntimeException {
    public RatingNotFoundException(String msg) { super(msg); }
}
