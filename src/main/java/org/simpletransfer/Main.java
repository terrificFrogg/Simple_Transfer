package org.simpletransfer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.ConfigGroups;
import org.simpletransfer.models.CredentialType;
import org.simpletransfer.models.Parent;
import org.simpletransfer.models.ServerConfig;
import org.simpletransfer.services.SourceFTPTransfer;
import org.simpletransfer.services.SourceLocalTransfer;
import org.simpletransfer.utils.ConfigParser;
import org.simpletransfer.utils.Util;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Main main = new Main();
    }
    private static final Logger logger = LogManager.getLogger();
    private static final Path INBOUND_FOLDER = Path.of("Staging\\IN\\");
    private static final Path INBOUND_FOLDER_ARCHIVE = Path.of("Staging\\IN_ARCHIVE\\");

    private final Parent config;

    public Main(){
        config = ConfigParser.getConfig("Config.json");
        initChecks();
        SourceFTPTransfer sourceFTPTransfer = new SourceFTPTransfer(INBOUND_FOLDER, INBOUND_FOLDER_ARCHIVE, config.intervalInMinutes());
        sourceFTPTransfer.startTransfer(config.configCollection());

        SourceLocalTransfer sourceLocalTransfer = new SourceLocalTransfer();
        sourceLocalTransfer.startTransfer(config.configCollection());
    }

    private void initChecks(){
        //create directories using hostname for SFTP and FTP types
        if(config != null && config.configCollection() != null){
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

}