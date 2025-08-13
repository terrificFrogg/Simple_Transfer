package org.simpletransfer.services;

import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.*;
import org.simpletransfer.utils.Util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Transfers data for sources whose credential type is Local.
 * */
public class SourceLocalTransfer implements Transfer {
    private final Logger logger;
    private final List<FolderMonitor> folderMonitors;
    private final Map<String, RemoteClient> remoteClients;
    private final List<Callable<Void>> callableList;
    private final ExecutorService executorService;
    private final RemoteClientFactory remoteClientFactory;

    private SourceLocalTransfer(Builder builder){
        folderMonitors = new ArrayList<>();
        remoteClients = new WeakHashMap<>();
        callableList = new ArrayList<>();
        this.executorService = builder.executorService;
        this.remoteClientFactory = builder.remoteClientFactory;
        this.logger = builder.logger;
    }

    public static class Builder{
        private Logger logger;
        private RemoteClientFactory remoteClientFactory;
        private ExecutorService executorService;

        public Builder withLogger(Logger logger){
            this.logger = logger;
            return this;
        }

        public Builder withRemoteClientFactory(RemoteClientFactory remoteClientFactory){
            this.remoteClientFactory = remoteClientFactory;
            return this;
        }

        public Builder withExecutorService(ExecutorService executorService){
            this.executorService = executorService;
            return this;
        }

        public SourceLocalTransfer build(){
            return new SourceLocalTransfer(this);
        }
    }

    @Override
    public void startTransfer(List<ConfigGroups> configGroups) {
        for (ConfigGroups configGroup : configGroups) {
            ServerConfig source = configGroup.source();
            if(source.credentials().type().equals(CredentialType.LOCAL)){
                logger.info("Transfer for {}", source.credentials().hostname());
                FolderMonitor folderMonitor = new FolderMonitor(source.folderPath());
                folderMonitors.add(folderMonitor);
                callableList.add(() -> {
                    folderMonitor.startMonitoring(sourcePath -> {
                        for (ServerConfig destination : configGroup.destinations()) {
                            Credentials credentials = destination.credentials();
                            switch (credentials.type()){
                                case FTP, FTPS, SFTP -> {
                                    try {
                                        RemoteClient remoteClient = remoteClients.computeIfAbsent(credentials.hostname(),
                                                _ -> remoteClientFactory.create(destination));
                                        remoteClient.connect();
                                        if(remoteClient.isConnected()){
                                            remoteClient.upload(sourcePath, destination.folderPath());
                                        }
                                    } catch (IOException e) {
                                        logger.error("Error on SourceLocalTransfer Upload for {}. Message: {}", credentials.hostname(), e.getMessage());
                                    }
                                }
                                case LOCAL -> Util.moveFile(sourcePath, destination
                                        .folderPath()
                                        .concat("\\")
                                        .concat(sourcePath.substring(sourcePath.lastIndexOf('\\') + 1)));
                            }
                        }
                    });

                    return null;
                });
            }
        }

        try {
            executorService.invokeAll(callableList);
        } catch (InterruptedException e) {
            logger.error("Error while transfer. Message: {}", e.getMessage());
        }
    }

    @Override
    public void stopTransfer() {
        folderMonitors.forEach(FolderMonitor::closeAll);
        remoteClients.forEach((_, v) -> {
            try {
                if(v.isConnected()) {
                    v.disconnect();
                }
            } catch (IOException e) {
                logger.error("Error while disconnecting. Message: {}", e.getMessage());
            }
        });

        executorService.shutdown();
    }
}