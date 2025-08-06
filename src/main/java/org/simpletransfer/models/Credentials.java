package org.simpletransfer.models;

public record Credentials(CredentialType type, String hostname, int port, String username, String password) {}
