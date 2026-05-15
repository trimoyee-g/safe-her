package com.safeher.authservice.exception;
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) { super(message); }
}
