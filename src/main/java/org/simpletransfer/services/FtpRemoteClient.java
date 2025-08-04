package org.simpletransfer.services;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.Credentials;
import org.simpletransfer.models.RemoteClient;

import java.io.*;

public class FtpRemoteClient implements RemoteClient {
    private static final Logger logger = LogManager.getLogger();
    private final Credentials credentials;
    private final FTPClient ftpClient;

    public FtpRemoteClient(Credentials credentials){
        this.credentials = credentials;
        this.ftpClient = new FTPClient();
    }

    @Override
    public void connect() throws IOException {
        ftpClient.connect(credentials.hostname(), credentials.port());
        ftpClient.login(credentials.username(), credentials.password());
        ftpClient.enterLocalPassiveMode();
        if(ftpClient.isConnected()){
            logger.info("Connected to {}", credentials.hostname());
        }else{
            logger.error("Failed to connect to {}", credentials.hostname());
        }
    }

    @Override
    public void disconnect() {
        if(ftpClient.isConnected()){
            try {
                ftpClient.disconnect();
                logger.info("Disconnected from {}", credentials.hostname());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return ftpClient.isConnected();
    }

    @Override
    public void upload(String localPath, String remotePath) throws IOException {
        try(InputStream localFileStream = new FileInputStream(localPath)){
            logger.info("[{}] Uploading {} to {}", credentials.hostname(), localPath, remotePath);
            ftpClient.storeFile(remotePath, localFileStream);
        }
    }

    @Override
    public void download(String localPath, String remotePath) throws IOException {
        int downloadCount = 0;
        for(FTPFile ftpFile : ftpClient.listFiles()){
            OutputStream fos = new FileOutputStream(localPath.concat("/").concat(ftpFile.getName().trim()));
            if(ftpClient.retrieveFile(remotePath.concat("/").concat(ftpFile.getName()), fos)){
                downloadCount++;
                fos.close();
                if(!ftpClient.deleteFile(remotePath.concat("/").concat(ftpFile.getName()))){
                    logger.error("Failed to delete {}", remotePath.concat("/").concat(ftpFile.getName()));
                }
            }else{
                fos.close();
                Util.deleteFile(localPath + "/" + ftpFile.getName().trim());
                logger.error("Failed to download {}", remotePath.concat("/").concat(ftpFile.getName()));
            }
            logger.info("Downloaded {} files from FTP {}", downloadCount, credentials.hostname());
        }
    }
}
