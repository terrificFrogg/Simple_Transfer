package org.simpletransfer.services;

import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.*;
import org.simpletransfer.utils.Util;

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
    private final int interval;
    private final TimeUnit timeUnit;
    private BiConsumer<List<String>, String> uploadedFilesConsumer;
    private final RemoteClientFactory remoteClientFactory;
    private final ScheduledExecutorService scheduler;

    private final List<RemoteClient> sourceRemoteClients = new ArrayList<>();
    private final Map<String, RemoteClient> destinationRemoteClients = new WeakHashMap<>();
    private final List<TransferTask> transferTasks = new ArrayList<>();

    private SourceFTPTransfer(Builder builder) {
        this.logger = builder.logger;
        this.baseInboundFolder = builder.baseInboundFolder;
        this.interval = builder.interval;
        this.timeUnit = builder.timeUnit == null ? TimeUnit.MINUTES : builder.timeUnit;
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
        }
        executeScheduledTasks();
    }

    private TransferTask getTransferTask(RemoteClient sourceRemoteClient, ServerConfig source, List<ServerConfig> destinations) {
        return () -> {
            try {
                if(!sourceRemoteClient.isConnected())
                    sourceRemoteClient.connect();

                if (sourceRemoteClient.isConnected()) {
                    String sourceStagingFolder = baseInboundFolder + "\\" + source.credentials().hostname();
                    sourceRemoteClient.download(sourceStagingFolder, source.folderPath());

                    for (ServerConfig destination : destinations) {
                        switch (destination.credentials().type()){
                            case SFTP, FTPS, FTP -> {
                                RemoteClient destinationRemoteClient = destinationRemoteClients.computeIfAbsent(destination.credentials().hostname(),
                                        _ -> remoteClientFactory.create(destination));

                                if(!destinationRemoteClient.isConnected())
                                    destinationRemoteClient.connect();

                                if (destinationRemoteClient.isConnected()) {
                                    destinationRemoteClient.upload(sourceStagingFolder, destination.folderPath());
                                }
                            }

                            case LOCAL -> Util.moveFile(sourceStagingFolder, destination.folderPath());
                        }
                    }
                }else{
                    logger.error("Not connected to {}", sourceRemoteClient.getHostName());
                }
            } catch (IOException e) {
                logger.error("Error while trying to transfer from {}. Message: {}", source.credentials().hostname(), e.getMessage());
            }
        };
    }

    private void executeScheduledTasks() {
        scheduler.scheduleAtFixedRate(()-> transferTasks.forEach(TransferTask::run), 0, interval, timeUnit);
    }

    @Override
    public void stopTransfer() {
        closeAllSourceRemoteClients(sourceRemoteClients);
        closeAllDestinationRemoteClients(destinationRemoteClients);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private void closeAllDestinationRemoteClients(Map<String, RemoteClient> destinationRemoteClients){
        destinationRemoteClients.forEach((_ , client) -> {
            try {
                client.disconnect();
                logger.info("Disconnected from {}? {}", client.getHostName(), client.isConnected());
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
                    logger.info("Disconnected from {}? {}", client.getHostName(), client.isConnected());
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
        private int interval = 5;
        private RemoteClientFactory remoteClientFactory;
        private ScheduledExecutorService scheduler;
        private TimeUnit timeUnit;

        public Builder withLogger(Logger logger){
            this.logger = logger;
            return this;
        }

        public Builder withTimeUnit(TimeUnit timeUnit){
            this.timeUnit = timeUnit;
            return this;
        }

        public Builder withBaseInboundFolder(String folder) {
            this.baseInboundFolder = folder;
            return this;
        }

        public Builder withInterval(int minutes) {
            this.interval = minutes;
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
            if (baseInboundFolder == null) {
                throw new IllegalStateException("Base inbound folder must be set.");
            }
            if (remoteClientFactory == null) {
                throw new IllegalStateException("RemoteClientFactory must be set.");
            }
            return new SourceFTPTransfer(this);
        }
    }
}