package org.simpletransfer.models;

public record Credentials(String type, String hostname, int port, String username, String password) {}
