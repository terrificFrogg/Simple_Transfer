package org.simpletransfer.services.clients;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.Credentials;
import org.simpletransfer.models.FileInfo;
import org.simpletransfer.models.FileType;
import org.simpletransfer.models.RemoteClient;
import org.simpletransfer.utils.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FtpRemoteClient implements RemoteClient {
    private static final Logger logger = LogManager.getLogger();
    private final Credentials credentials;
    private final FTPClient ftpClient;
    private final List<FileInfo> fileInfos;

    public FtpRemoteClient(Credentials credentials){
        this.credentials = credentials;
        this.ftpClient = new FTPClient();
        this.fileInfos = new ArrayList<>();
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
    public void upload(String localPath, String remotePath) throws IOException {
        if(isConnected()){
            File localFile = new File(localPath);
            if(localFile.isFile()){
                try(InputStream localFileStream = new FileInputStream(localPath)){
                    logger.info("[{}] Uploading {} to {}", credentials.hostname(), localPath, remotePath);
                    ftpClient.storeFile(remotePath.concat("\\").concat(localFile.getName()), localFileStream);
                }
            }else if(localFile.isDirectory()){
                for (File file : Objects.requireNonNull(localFile.listFiles())) {
                    try(InputStream inputStream = new FileInputStream(file)){
                        logger.info("[{}] Uploading {} to {}", credentials.hostname(), localPath, remotePath);
                        ftpClient.storeFile(remotePath.concat("\\").concat(file.getName()), inputStream);
                    }
                }
            }
        }
    }

    @Override
    public void download(String localPath, String remotePath) throws IOException {
        if(isConnected()){
            int downloadCount = 0;
            for(FTPFile ftpFile : ftpClient.listFiles()){
                OutputStream fos = new FileOutputStream(localPath.concat("\\").concat(ftpFile.getName()));
                if(ftpClient.retrieveFile(ftpFile.getName(), fos)){
                    downloadCount++;
                    fos.close();
                    if(!ftpClient.deleteFile(ftpFile.getName())){
                        logger.error("Failed to delete {}", ftpFile.getName());
                    }
                }else{
                    fos.close();
                    Util.deleteFile(localPath + "/" + ftpFile.getName().trim());
                    logger.error("Failed to download {}", ftpFile.getName());
                }
                logger.info("Downloaded {} files from FTP {}", downloadCount, credentials.hostname());
            }
        }
    }

    @Override
    public void createDirectory(String directoryPath) throws IOException {
        if(isConnected()){
            ftpClient.makeDirectory(directoryPath);
        }
    }

    @Override
    public List<FileInfo> listContents(String path) throws IOException {
        if(isConnected()){
            fileInfos.clear();
            for (FTPFile ftpFile : ftpClient.listFiles(path)) {
                FileType fileType = null;
                if(ftpFile.isFile()){
                    fileType = FileType.FILE;
                }else if(ftpFile.isDirectory()){
                    fileType = FileType.DIRECTORY;
                }
                fileInfos.add(new FileInfo(ftpFile.getName(), ftpFile.getLink(), fileType));
            }
        }else{
            logger.error("Not connected to {}", credentials.hostname());
        }
        return fileInfos;
    }

    @Override
    public void delete(String path) throws IOException {
        if(isConnected()){
            ftpClient.deleteFile(path);
        }
    }


}
