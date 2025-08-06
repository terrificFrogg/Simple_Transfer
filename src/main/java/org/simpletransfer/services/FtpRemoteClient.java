package org.simpletransfer.services;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.Credentials;
import org.simpletransfer.models.RemoteClient;
import org.simpletransfer.utils.Util;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;

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

    @SuppressWarnings("LoggingSimilarMessage")
    @Override
    public void upload(Path localPath, Path remotePath) throws IOException {
        File localFile = localPath.toFile();
        if(localFile.isFile()){
            try(InputStream localFileStream = new FileInputStream(localPath.toString())){
                logger.info("[{}] Uploading {} to {}", credentials.hostname(), localPath, remotePath);
                ftpClient.storeFile(remotePath.resolve(localPath.getFileName()).toString(), localFileStream);
            }
        }else if(localFile.isDirectory()){
            for (File file : Objects.requireNonNull(localFile.listFiles())) {
                try(InputStream inputStream = new FileInputStream(file)){
                    logger.info("[{}] Uploading {} to {}", credentials.hostname(), localPath, remotePath);
                    ftpClient.storeFile(remotePath.resolve(file.getName()).toString(), inputStream);
                }
            }
        }
    }

    @Override
    public void download(Path localPath, Path remotePath) throws IOException {
        int downloadCount = 0;
        for(FTPFile ftpFile : ftpClient.listFiles()){
            OutputStream fos = new FileOutputStream(localPath.resolve(ftpFile.getName().trim()).toString());
            if(ftpClient.retrieveFile(remotePath.resolve(Path.of(ftpFile.getName().trim())).toString(), fos)){
                downloadCount++;
                fos.close();
                if(!ftpClient.deleteFile(remotePath.resolve(ftpFile.getName().trim()).toString())){
                    logger.error("Failed to delete {}", remotePath.resolve(ftpFile.getName().trim()).toString());
                }
            }else{
                fos.close();
                Util.deleteFile(localPath + "/" + ftpFile.getName().trim());
                logger.error("Failed to download {}", remotePath.resolve(ftpFile.getName().trim()).toString());
            }
            logger.info("Downloaded {} files from FTP {}", downloadCount, credentials.hostname());
        }
    }
}
