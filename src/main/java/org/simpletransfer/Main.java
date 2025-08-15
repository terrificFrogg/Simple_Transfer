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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class Main {
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
                    fileList.forEach(fileName -> {
                        String in = INBOUND_FOLDER.concat(hostname).concat("\\").concat(fileName);
                        String archive = INBOUND_FOLDER_ARCHIVE.concat(hostname).concat("\\").concat(fileName);

                        //Local type transfers bypass the staging folder. Their folders won't exist at this point.
                        //This is basically to prevent an exception being thrown for them.
                        //I just can't be bothered to prevent this from happening in the first place
                        if(Files.exists(Path.of(in)) && Files.exists(Path.of(archive))){
                            Util.moveFile(in, archive);
                        }else{
                            logger.info("Skipped '{}' because it doesn't exist", in);
                        }
                    });
                }
            };

            List<ConfigGroups> remoteSourceConfigGroups = new ArrayList<>();
            List<ConfigGroups> localSourceConfigGroups = new ArrayList<>();
            SourceFTPTransfer sourceFTPTransfer;
            SourceLocalTransfer sourceLocalTransfer;

            for (ConfigGroups configGroups : config.configCollection()) {
                switch (configGroups.source().credentials().type()){
                    case FTP, FTPS, SFTP -> {
                        remoteSourceConfigGroups.add(configGroups);
                    }

                    case LOCAL -> {
                        localSourceConfigGroups.add(configGroups);
                    }
                }
            }

            if(!remoteSourceConfigGroups.isEmpty()){
                sourceFTPTransfer = new SourceFTPTransfer
                        .Builder()
                        .withLogger(logger)
                        .withBaseInboundFolder(INBOUND_FOLDER)
                        .withInterval(config.interval())
                        .withTimeUnit(config.timeUnit())
                        .withScheduler(Executors.newScheduledThreadPool(remoteSourceConfigGroups.size()))
                        .withRemoteClientFactory(new RemoteClientFactory(logger, fileMover, null, null, null))
                        .build();
                sourceFTPTransfer.startTransfer(remoteSourceConfigGroups);
            } else {
                sourceFTPTransfer = null;
            }

            if(!localSourceConfigGroups.isEmpty()){
                sourceLocalTransfer = new SourceLocalTransfer
                        .Builder()
                        .withLogger(logger)
                        .withRemoteClientFactory(new RemoteClientFactory(logger, fileMover, null, null, null))
                        .withExecutorService(Executors.newFixedThreadPool(localSourceConfigGroups.size()))
                        .build();
                sourceLocalTransfer.startTransfer(config.configCollection());
            } else {
                sourceLocalTransfer = null;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if(sourceFTPTransfer != null){
                    sourceFTPTransfer.stopTransfer();
                }

                if(sourceLocalTransfer != null){
                    sourceLocalTransfer.stopTransfer();
                }
            }));
        }
    }

    private void initChecks(){
        //create directories using hostname
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
    public static void main(String[] args) {
        Main _ = new Main();
    }
}