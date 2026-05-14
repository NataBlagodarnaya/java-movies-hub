package ru.practicum.moviehub.api;

public class ErrorResponse {
    private final String error;
    private final String details;

    public ErrorResponse(String error, String details) {
        this.error = error;
        this.details = details;
    }
}