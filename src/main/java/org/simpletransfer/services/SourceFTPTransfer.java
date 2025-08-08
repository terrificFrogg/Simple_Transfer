package org.simpletransfer.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.*;
import org.simpletransfer.services.clients.FtpRemoteClient;
import org.simpletransfer.services.clients.SftpRemoteClient;
import org.simpletransfer.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Transfers data for sources whose credential type is FTP or SFTP.
 * */
public class SourceFTPTransfer implements Transfer {
    protected static final Logger logger = LogManager.getLogger();
    private final String baseInboundFolder;
    private final String baseInboundFolderArchive;
    private final int intervalInMinutes;
    private ScheduledExecutorService scheduler;
    private final List<RemoteClient> sourceRemoteClients;
    private final List<RemoteClient> destinationRemoteClients;
    private final List<TransferTask> transferTasks;

    public SourceFTPTransfer(String baseInboundFolder, String baseInboundFolderArchive, int intervalInMinutes){
        this.baseInboundFolder = baseInboundFolder;
        this.baseInboundFolderArchive = baseInboundFolderArchive;
        this.intervalInMinutes = intervalInMinutes;
        sourceRemoteClients = new ArrayList<>();
        transferTasks = new ArrayList<>();
        destinationRemoteClients = new ArrayList<>();
    }

    public SourceFTPTransfer (String inboundFolder, String inboundFolderArchive){
        this(inboundFolder, inboundFolderArchive, 5);
    }

    @Override
    public void startTransfer(List<ConfigGroups> configGroups) {
        scheduler = Executors.newScheduledThreadPool(configGroups.size());
        for (ConfigGroups configGroup : configGroups) {
            ServerConfig source = configGroup.source();
            RemoteClient sourceRemoteClient = null;
            switch (source.credentials().type()){
                case FTP -> sourceRemoteClient = new FtpRemoteClient(source.credentials());

                case SFTP -> sourceRemoteClient = new SftpRemoteClient(source.credentials());
            }
            sourceRemoteClients.add(sourceRemoteClient);
            TransferTask transferTask = getTransferTask(sourceRemoteClient, source, configGroup.destinations());
            transferTasks.add(transferTask);
            dataTransferScheduler(transferTask);
        }

    }

    private TransferTask getTransferTask(RemoteClient sourceRemoteClient, ServerConfig source, List<ServerConfig> destinations) {
        return () -> {
            try {
                sourceRemoteClient.connect();
                if(sourceRemoteClient.isConnected()){
                    String sourceFolder = baseInboundFolder.concat("\\").concat(source.credentials().hostname());
                    sourceRemoteClient.download(sourceFolder, source.folderPath());

                    for (ServerConfig destination : destinations) {
                        RemoteClient destinationRemoteClient = null;
                        switch (destination.credentials().type()){
                            case SFTP -> destinationRemoteClient = new SftpRemoteClient(destination.credentials());
                            case FTP -> destinationRemoteClient = new FtpRemoteClient(destination.credentials());
                        }

                        destinationRemoteClients.add(destinationRemoteClient);

                        if(destinationRemoteClient != null){
                            destinationRemoteClient.connect();
                            if(destinationRemoteClient.isConnected()){
                                destinationRemoteClient.upload(sourceFolder, destination.folderPath());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error while trying to download from {}. Error Message: {}", source.credentials().hostname(), e.getMessage());
            }
        };
    }

    private void dataTransferScheduler(TransferTask transferTask){
        scheduler.scheduleAtFixedRate(transferTask::run, 0, intervalInMinutes, TimeUnit.MINUTES);
    }


    @SuppressWarnings("LoggingSimilarMessage")
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

        sourceRemoteClients.forEach(source -> {
            if(source.isConnected()){
                try {
                    source.disconnect();
                } catch (IOException e) {
                    logger.error("Error while disconnecting. Message: {}", e.getMessage());
                }
            }
        });

        destinationRemoteClients.forEach(dest -> {
            if(dest.isConnected()){
                try {
                    dest.disconnect();
                } catch (IOException e) {
                    logger.error("Error while disconnecting. Message: {}", e.getMessage());
                }
            }
        });
    }
}