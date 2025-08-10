package org.simpletransfer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.ConfigGroups;
import org.simpletransfer.models.CredentialType;
import org.simpletransfer.models.Parent;
import org.simpletransfer.models.ServerConfig;
import org.simpletransfer.services.RemoteClientFactory;
import org.simpletransfer.services.SourceFTPTransfer;
import org.simpletransfer.services.SourceLocalTransfer;
import org.simpletransfer.utils.ConfigParser;
import org.simpletransfer.utils.Util;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class Main {
    public static void main(String[] args) {
        Main main = new Main();
    }
    private static final Logger logger = LogManager.getLogger();
    private static final String INBOUND_FOLDER = "Staging\\IN\\";
    private static final String INBOUND_FOLDER_ARCHIVE = "Staging\\IN_ARCHIVE\\";

    private final Parent config;

    public Main(){
        config = ConfigParser.getConfig("config.json");
        if(config != null && config.configCollection() != null){
            initChecks();

            BiConsumer<List<String>, String> fileMover = new BiConsumer<>() {
                @Override
                synchronized public void accept(List<String> fileList, String hostname) {
                    fileList.forEach(fileName -> Util.moveFile(INBOUND_FOLDER.concat(hostname).concat("\\").concat(fileName),
                            INBOUND_FOLDER_ARCHIVE.concat(hostname).concat("\\").concat(fileName)));
                }
            };

            SourceFTPTransfer sourceFTPTransfer = new SourceFTPTransfer
                    .Builder()
                    .withLogger(logger)
                    .withBaseInboundFolder(INBOUND_FOLDER)
                    .withBaseInboundFolderArchive(INBOUND_FOLDER_ARCHIVE)
                    .withIntervalInMinutes(10)
                    .withScheduler(Executors.newScheduledThreadPool(config.configCollection().size()))
                    .withRemoteClientFactory(new RemoteClientFactory(logger, fileMover, null, null, null))
                    .build();
            sourceFTPTransfer.startTransfer(config.configCollection());

            SourceLocalTransfer sourceLocalTransfer = new SourceLocalTransfer
                    .Builder()
                    .withLogger(logger)
                    .withRemoteClientFactory(new RemoteClientFactory(logger, fileMover, null, null, null))
                    .withExecutorService(Executors.newFixedThreadPool(config.configCollection().size()))
                    .build();
            sourceLocalTransfer.startTransfer(config.configCollection());
        }
    }

    private void initChecks(){
        //create directories using hostname for SFTP and FTP types
        for (ConfigGroups configGroups : config.configCollection()) {
            ServerConfig serverConfig = configGroups.source();
            if(serverConfig.credentials() != null){
                if(serverConfig.credentials().type().equals(CredentialType.SFTP)
                        || serverConfig.credentials().type().equals(CredentialType.FTP)){
                    Util.createDir(INBOUND_FOLDER, INBOUND_FOLDER_ARCHIVE, serverConfig.credentials().hostname());
                }
            }
        }
    }

}