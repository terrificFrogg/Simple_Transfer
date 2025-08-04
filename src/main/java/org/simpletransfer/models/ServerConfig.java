package org.simpletransfer.models;

public record ServerConfig(Credentials credentials, String remoteFolderName) {}