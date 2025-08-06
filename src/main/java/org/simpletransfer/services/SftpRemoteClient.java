package org.simpletransfer.services;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.Credentials;
import org.simpletransfer.models.RemoteClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class SftpRemoteClient implements RemoteClient {
    protected static final Logger logger = LogManager.getLogger();
    private final Credentials credentials;
    private final SSHClient sshClient;

    public SftpRemoteClient(Credentials credentials){
        this.credentials = credentials;
        this.sshClient = new SSHClient();
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

    @Override
    public void upload(Path localPath, Path remotePath) throws IOException {
        try(SFTPClient sftpClient = sshClient.newSFTPClient()){
            File localFile = localPath.toFile();
            if(localFile.isFile()){
                sftpClient.put(localPath.toString(), remotePath.resolve(localPath.getFileName()).toString());
            }else if(localFile.isDirectory()){
                for (File file : Objects.requireNonNull(localFile.listFiles())) {
                    sftpClient.put(file.getAbsolutePath(), remotePath.resolve(file.getName()).toString());
                }
            }
        }
    }

    @Override
    public void download(Path localPath, Path remotePath) throws IOException {
        try(SFTPClient sftpClient = sshClient.newSFTPClient()){
            for (RemoteResourceInfo resourceInfo : sftpClient.ls(remotePath.toString())) {
                Path fullRemotePath = remotePath.resolve(resourceInfo.getName());
                sftpClient.get(fullRemotePath.toString(), localPath.toString());
                sftpClient.rm(fullRemotePath.toString());
            }
        }
    }
}
