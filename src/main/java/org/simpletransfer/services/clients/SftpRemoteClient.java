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

public class SftpRemoteClient implements RemoteClient {
    protected static final Logger logger = LogManager.getLogger();
    private final Credentials credentials;
    private final SSHClient sshClient;
    private final List<FileInfo> fileInfos;

    public SftpRemoteClient(Credentials credentials){
        this.credentials = credentials;
        this.sshClient = new SSHClient();
        this.fileInfos = new ArrayList<>();
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
                File localFile = new File(localPath);
                if(localFile.isFile()){
                    sftpClient.put(localFile.getAbsolutePath(), remotePath.concat("/").concat(localFile.getName()));
                    logger.info("[{}] Uploaded '{}'", credentials.hostname(), localFile.getName());
                }else if(localFile.isDirectory()){
                    for (File file : Objects.requireNonNull(localFile.listFiles())) {
                        sftpClient.put(file.getAbsolutePath(), remotePath.concat("/").concat(file.getName()));
                        logger.info("[{}] Uploaded '{}'", credentials.hostname(), file.getName());
                    }
                }
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
