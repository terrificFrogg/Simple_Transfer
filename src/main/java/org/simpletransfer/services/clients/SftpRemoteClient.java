package org.simpletransfer.services.clients;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.Credentials;
import org.simpletransfer.models.FileInfo;
import org.simpletransfer.models.FileType;
import org.simpletransfer.models.RemoteClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SftpRemoteClient implements RemoteClient {
    private final Logger logger;
    private final Credentials credentials;
    private final SSHClient sshClient;
    private final List<FileInfo> fileInfos;
    private final BiConsumer<List<String>, String> uploadedFilesConsumer;
    private final List<String> uploadedFiles;

    public SftpRemoteClient(Builder builder){
        this.logger = builder.logger;
        this.credentials = builder.credentials;
        this.sshClient = builder.sshClient;
        this.fileInfos = builder.fileInfos;
        this.uploadedFilesConsumer = builder.uploadedFilesConsumer;
        this.uploadedFiles = new ArrayList<>();
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        private Logger logger;
        private Credentials credentials;
        private SSHClient sshClient;
        private List<FileInfo> fileInfos;
        private BiConsumer<List<String>, String> uploadedFilesConsumer;


        public Builder(Logger logger, Credentials credentials, SSHClient sshClient, List<FileInfo> fileInfos, BiConsumer<List<String>, String> uploadedFilesConsumer) {
            this.logger = logger;
            this.credentials = credentials;
            this.sshClient = sshClient;
            this.fileInfos = fileInfos;
            this.uploadedFilesConsumer = uploadedFilesConsumer;
        }

        public Builder(){}

        public Builder withLogger(Logger logger){
            this.logger = logger;
            return this;
        }

        public Builder withCredentials(Credentials credentials){
            this.credentials = credentials;
            return this;
        }

        public Builder withSSHClient(SSHClient sshClient){
            this.sshClient = sshClient;
            return this;
        }

        public Builder withFileInfos(List<FileInfo> fileInfos){
            this.fileInfos = fileInfos;
            return this;
        }

        public Builder withConsumer(BiConsumer<List<String>, String> uploadedFilesConsumer){
            this.uploadedFilesConsumer = uploadedFilesConsumer;
            return this;
        }

        public SftpRemoteClient build(){
            return new SftpRemoteClient(this);
        }
    }

    @Override
    public void connect() throws IOException {
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(credentials.hostname(), credentials.port());
        sshClient.authPassword(credentials.username(), credentials.password());
    }

    @Override
    public void disconnect() throws IOException {
        if(sshClient.isConnected()){
            sshClient.disconnect();
        }
    }

    @Override
    public boolean isConnected() {
        return sshClient.isConnected();
    }

    @SuppressWarnings("LoggingSimilarMessage")
    @Override
    public void upload(String localPath, String remotePath) throws IOException {
        if(isConnected()){
            try(SFTPClient sftpClient = sshClient.newSFTPClient()){
                uploadedFiles.clear();
                File localFile = new File(localPath);
                if(localFile.isFile()){
                    sftpClient.put(localFile.getAbsolutePath(), remotePath.concat("/").concat(localFile.getName()));
                    logger.info("[{}] Uploaded '{}'", credentials.hostname(), localFile.getName());
                    uploadedFiles.add(localFile.getName());
                }else if(localFile.isDirectory()){
                    for (File file : Objects.requireNonNull(localFile.listFiles())) {
                        sftpClient.put(file.getAbsolutePath(), remotePath.concat("/").concat(file.getName()));
                        logger.info("[{}] Uploaded '{}'", credentials.hostname(), file.getName());
                        uploadedFiles.add(localFile.getName());
                    }
                }
                if(!uploadedFiles.isEmpty())
                    uploadedFilesConsumer.accept(uploadedFiles, credentials.hostname());
            }
        }
    }

    @Override
    public void download(String localPath, String remotePath) throws IOException {
        if(isConnected()){
            try(SFTPClient sftpClient = sshClient.newSFTPClient()){
                for (RemoteResourceInfo resourceInfo : sftpClient.ls(remotePath)) {
                    sftpClient.get(resourceInfo.getPath(), localPath);
                    sftpClient.rm(resourceInfo.getPath());
                    logger.info("Downloaded {} from {}", resourceInfo.getName(), credentials.hostname());
                }
            }
        }
    }

    @Override
    public void createDirectory(String directoryPath) throws IOException {
        if(isConnected()){
            try(SFTPClient sftpClient = sshClient.newSFTPClient()){
                sftpClient.mkdir(directoryPath);
            }
        }
    }

    @Override
    public List<FileInfo> listContents(String path) throws IOException {
        if(isConnected()){
            fileInfos.clear();
            try(SFTPClient sftpClient = sshClient.newSFTPClient()){
                for (RemoteResourceInfo resourceInfo : sftpClient.ls(path)) {
                    if(resourceInfo.isRegularFile()){
                        fileInfos.add(new FileInfo(resourceInfo.getName(), resourceInfo.getPath(), FileType.FILE));
                    }else if(resourceInfo.isDirectory()){
                        fileInfos.add(new FileInfo(resourceInfo.getName(), resourceInfo.getPath(), FileType.DIRECTORY));
                    }
                }
            }
        }else{
            logger.error("Not connected to {}", credentials.hostname());
        }
        return fileInfos;
    }

    @Override
    public void delete(String path) throws IOException {
        if(isConnected()){
            try(SFTPClient sftpClient = sshClient.newSFTPClient()){
                sftpClient.rm(path);
            }
        }
    }


}
