package org.simpletransfer.models;

import java.util.List;

public record Parent(List<ServerConfig> configCollection, int intervalInMinutes, int taskTimeoutInSeconds) {}