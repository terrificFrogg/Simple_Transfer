package org.simpletransfer.models;

import java.util.List;

public record ConfigGroups(ServerConfig source, List<ServerConfig> destinations) {}
