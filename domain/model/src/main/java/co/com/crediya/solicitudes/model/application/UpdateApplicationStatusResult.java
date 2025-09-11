package co.com.crediya.solicitudes.model.application;

public record UpdateApplicationStatusResult(
    Application application,
    String previousStateName,
    String newStateName
) {}
