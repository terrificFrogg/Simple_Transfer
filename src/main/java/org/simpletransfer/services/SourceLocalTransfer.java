package org.simpletransfer.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Transfers data for sources whose credential type is Local.
 * */
public class SourceLocalTransfer implements Transfer {
    protected static final Logger logger = LogManager.getLogger();
    private final List<FolderMonitor> folderMonitors;
    private final List<SftpRemoteClient> sftpRemoteClients;
    private final List<Callable<Void>> callableList;
    private ExecutorService executorService;

    public SourceLocalTransfer(){
        folderMonitors = new ArrayList<>();
        sftpRemoteClients = new ArrayList<>();
        callableList = new ArrayList<>();
    }

    @Override
    public void startTransfer(List<ConfigGroups> configGroups) {
        executorService = Executors.newFixedThreadPool(configGroups.size());
        for (ConfigGroups configGroup : configGroups) {
            ServerConfig source = configGroup.source();
            if(source.credentials().type().equals(CredentialType.LOCAL)){   //making sure we're dealing with local type
                FolderMonitor folderMonitor = new FolderMonitor(source.folderPath());
                folderMonitors.add(folderMonitor); //add to list to keep object alive
                callableList.add(() -> {

                    folderMonitor.startMonitoring(sourcePath -> {
                        for (ServerConfig destination : configGroup.destinations()) {
                            Credentials credentials = destination.credentials();
                            if(credentials.type().equals(CredentialType.SFTP)){
                                try {
                                    SftpRemoteClient sftpRemoteClient = new SftpRemoteClient(credentials);
                                    sftpRemoteClients.add(sftpRemoteClient); //add to list to keep object alive

                                    sftpRemoteClient.connect();
                                    if(sftpRemoteClient.isConnected()){
                                        sftpRemoteClient.upload(sourcePath, Path.of(destination.folderPath()));
                                    }
                                } catch (IOException e) {
                                    logger.error("Error on SourceLocalTransfer Upload for {}. Message: {}", credentials.hostname(), e.getMessage());
                                }
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
        sftpRemoteClients.forEach(a -> {
            try {
                if(a.isConnected()) {
                    a.disconnect();
                }
            } catch (IOException e) {
                logger.error("Error while disconnecting. Message: {}", e.getMessage());
            }
        });

        executorService.shutdown();
        executorService.close();
    }
}