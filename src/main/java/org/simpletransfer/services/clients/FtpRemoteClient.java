package org.simpletransfer.services.clients;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
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
import java.util.function.BiConsumer;

public class FtpRemoteClient implements RemoteClient {
    private final Logger logger;
    private final FTPClient ftpClient;
    private final Credentials credentials;
    private final List<FileInfo> fileInfos;
    private final BiConsumer<List<String>, String> uploadedFilesConsumer;
    private final List<String> uploadedFiles;

    public FtpRemoteClient(FtpRemoteClient.Builder builder){
        this.logger = builder.logger;
        this.ftpClient = builder.ftpClient;
        this.credentials = builder.credentials;
        this.fileInfos = builder.fileInfos;
        this.uploadedFilesConsumer = builder.uploadedFilesConsumer;
        this.uploadedFiles = new ArrayList<>();
    }

    public static FtpRemoteClient.Builder builder(){
        return new FtpRemoteClient.Builder();
    }

    public static class Builder{
        private Credentials credentials;
        private FTPClient ftpClient;
        private List<FileInfo> fileInfos;
        private BiConsumer<List<String>, String> uploadedFilesConsumer;
        private Logger logger;

        public FtpRemoteClient.Builder withCredentials(Credentials credentials){
            this.credentials = credentials;
            return this;
        }

        public FtpRemoteClient.Builder withFTPClient(FTPClient ftpClient){
            this.ftpClient = ftpClient;
            return this;
        }

        public FtpRemoteClient.Builder withFileInfos(List<FileInfo> fileInfos){
            this.fileInfos = fileInfos;
            return this;
        }

        public FtpRemoteClient.Builder withConsumer(BiConsumer<List<String>, String> uploadedFilesConsumer){
            this.uploadedFilesConsumer = uploadedFilesConsumer;
            return this;
        }

        public FtpRemoteClient.Builder withLogger(Logger logger){
            this.logger = logger;
            return this;
        }

        public FtpRemoteClient build(){
            return new FtpRemoteClient(this);
        }
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
            uploadedFiles.clear();
            File localFile = new File(localPath);
            if(localFile.isFile()){
                try(InputStream localFileStream = new FileInputStream(localPath)){
                    logger.info("[{}] Uploading {} to {}", credentials.hostname(), localPath, remotePath);
                    ftpClient.storeFile(remotePath.concat("/").concat(localFile.getName()), localFileStream);
                    uploadedFiles.add(localFile.getName());
                }
            }else if(localFile.isDirectory()){
                for (File file : Objects.requireNonNull(localFile.listFiles())) {
                    try(InputStream inputStream = new FileInputStream(file)){
                        logger.info("[{}] Uploading {} to {}", credentials.hostname(), localPath, remotePath);
                        ftpClient.storeFile(remotePath.concat("/").concat(file.getName()), inputStream);
                        uploadedFiles.add(file.getName());
                    }
                }
            }
            uploadedFilesConsumer.accept(uploadedFiles, credentials.hostname());
        }
    }

    @Override
    public void download(String localPath, String remotePath) throws IOException {
        if(isConnected()){
            int downloadCount = 0;
            for(FTPFile ftpFile : ftpClient.listFiles(remotePath)){
                OutputStream fos = new FileOutputStream(localPath.concat("\\").concat(ftpFile.getName()));
                if(ftpClient.retrieveFile(remotePath.concat("/").concat(ftpFile.getName()), fos)){
                    downloadCount++;
                    fos.close();
                    delete(remotePath.concat("/").concat(ftpFile.getName()));
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
            if(!ftpClient.deleteFile(path)){
                logger.error("Failed to delete {}", path);
            }
        }
    }

    @Override
    public String getHostName() {
        return credentials.hostname();
    }
}
