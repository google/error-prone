package com.google.errorprone;

/**
 * An exception that indicates that validation failed.
 */
public class ValidationException extends Exception {
  public ValidationException(String message) {
    super(message);
  }
}
