package org.simpletransfer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.FileInfo;
import org.simpletransfer.models.Parent;
import org.simpletransfer.models.RemoteClient;
import org.simpletransfer.models.ServerConfig;
import org.simpletransfer.services.clients.FtpsRemoteClient;
import org.simpletransfer.utils.ConfigParser;

import java.io.IOException;

public class Test {
    protected static final Logger logger = LogManager.getLogger();
    public static void main(String[] args) {
        Parent config = ConfigParser.getConfig("config.json");

        if(config != null && config.configCollection() != null){
            ServerConfig source = config.configCollection().getFirst().source();

            RemoteClient ftpsRemoteClient = new FtpsRemoteClient(source.credentials());
            try {
                ftpsRemoteClient.connect();
                //ftpsRemoteClient.download("C:\\Dev\\FolderMonitorTesting\\Source 1", source.folderPath());
                for (FileInfo listContent : ftpsRemoteClient.listContents(source.folderPath())) {
                    logger.info("Name: {}", listContent.name());
                }
                ftpsRemoteClient.disconnect();
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }
}
