package org.simpletransfer.models;

import java.util.List;

public record Parent(List<ConfigGroups> configCollection, int intervalInMinutes, int taskTimeoutInSeconds) {}