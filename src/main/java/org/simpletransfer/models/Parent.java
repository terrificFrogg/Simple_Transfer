package org.simpletransfer.models;

import java.util.List;
import java.util.concurrent.TimeUnit;

public record Parent(List<ConfigGroups> configCollection, int interval, TimeUnit timeUnit) {}