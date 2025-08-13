package org.simpletransfer.services;

import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Transfers data for sources whose credential type is FTP, SFTP, or FTPS.
 */
@SuppressWarnings("LoggingSimilarMessage")
public class SourceFTPTransfer implements Transfer {
    private final Logger logger;
    private final String baseInboundFolder;
    private final String baseInboundFolderArchive;
    private final int intervalInMinutes;
    private BiConsumer<List<String>, String> uploadedFilesConsumer;
    private final RemoteClientFactory remoteClientFactory;
    private final ScheduledExecutorService scheduler;

    private final List<RemoteClient> sourceRemoteClients = new ArrayList<>();
    private final Map<String, RemoteClient> destinationRemoteClients = new WeakHashMap<>();
    private final List<TransferTask> transferTasks = new ArrayList<>();

    private SourceFTPTransfer(Builder builder) {
        this.logger = builder.logger;
        this.baseInboundFolder = builder.baseInboundFolder;
        this.baseInboundFolderArchive = builder.baseInboundFolderArchive;
        this.intervalInMinutes = builder.intervalInMinutes;
        this.remoteClientFactory = builder.remoteClientFactory;
        this.scheduler = builder.scheduler != null ? builder.scheduler : Executors.newScheduledThreadPool(4);
    }

    @Override
    public void startTransfer(List<ConfigGroups> configGroups) {
        for (ConfigGroups configGroup : configGroups) {
            RemoteClient sourceRemoteClient = remoteClientFactory.create(configGroup.source());
            sourceRemoteClients.add(sourceRemoteClient);

            TransferTask transferTask = getTransferTask(sourceRemoteClient, configGroup.source(), configGroup.destinations());
            transferTasks.add(transferTask);
            scheduleTask(transferTask);
        }
    }

    private TransferTask getTransferTask(RemoteClient sourceRemoteClient, ServerConfig source, List<ServerConfig> destinations) {
        return () -> {
            try {
                sourceRemoteClient.connect();
                if (sourceRemoteClient.isConnected()) {
                    String sourceFolder = baseInboundFolder + "\\" + source.credentials().hostname();
                    sourceRemoteClient.download(sourceFolder, source.folderPath());

                    for (ServerConfig destination : destinations) {
                        RemoteClient destinationRemoteClient = destinationRemoteClients.computeIfAbsent(destination.credentials().hostname(),
                                _ -> remoteClientFactory.create(destination));

                        destinationRemoteClient.connect();
                        if (destinationRemoteClient.isConnected()) {
                            destinationRemoteClient.upload(sourceFolder, destination.folderPath());
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error while trying to transfer from {}. Message: {}", source.credentials().hostname(), e.getMessage());
            }
        };
    }

    private void scheduleTask(TransferTask transferTask) {
        scheduler.scheduleAtFixedRate(transferTask::run, 0, intervalInMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void stopTransfer() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        closeAllSourceRemoteClients(sourceRemoteClients);
        closeAllDestinationRemoteClients(destinationRemoteClients);
    }

    private void closeAllDestinationRemoteClients(Map<String, RemoteClient> destinationRemoteClients){
        destinationRemoteClients.forEach((_ , v) -> {
            try {
                v.disconnect();
            } catch (IOException e) {
                logger.error("Error while disconnecting. Message: {}", e.getMessage());
            }
        });
    }

    private void closeAllSourceRemoteClients(List<RemoteClient> clients) {
        for (RemoteClient client : clients) {
            if (client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException e) {
                    logger.error("Error while disconnecting. Message: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Builder for SourceFTPTransfer
     */
    public static class Builder {
        private Logger logger;
        private String baseInboundFolder;
        private String baseInboundFolderArchive;
        private int intervalInMinutes = 5;
        private RemoteClientFactory remoteClientFactory;
        private ScheduledExecutorService scheduler;

        public Builder withLogger(Logger logger){
            this.logger = logger;
            return this;
        }

        public Builder withBaseInboundFolder(String folder) {
            this.baseInboundFolder = folder;
            return this;
        }

        public Builder withBaseInboundFolderArchive(String folderArchive) {
            this.baseInboundFolderArchive = folderArchive;
            return this;
        }

        public Builder withIntervalInMinutes(int minutes) {
            this.intervalInMinutes = minutes;
            return this;
        }

        public Builder withRemoteClientFactory(RemoteClientFactory factory) {
            this.remoteClientFactory = factory;
            return this;
        }

        public Builder withScheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public SourceFTPTransfer build() {
            if (baseInboundFolder == null || baseInboundFolderArchive == null) {
                throw new IllegalStateException("Base inbound folder and archive folder must be set.");
            }
            if (remoteClientFactory == null) {
                throw new IllegalStateException("RemoteClientFactory must be set.");
            }
            return new SourceFTPTransfer(this);
        }
    }
}