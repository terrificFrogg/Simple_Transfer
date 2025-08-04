package org.simpletransfer.services;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.simpletransfer.models.Credentials;
import org.simpletransfer.models.RemoteClient;

import java.io.IOException;

public class SftpRemoteClient implements RemoteClient {
    private final Credentials creds;
    private final SSHClient sshClient;

    public SftpRemoteClient(Credentials creds){
        this.creds = creds;
        this.sshClient = new SSHClient();
    }

    @Override
    public void connect() throws IOException {
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(creds.hostname(), creds.port());
        sshClient.authPassword(creds.hostname(), creds.password());
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
    public void upload(String localPath, String remotePath) throws IOException {
        SFTPClient sftpClient = sshClient.newSFTPClient();
        sftpClient.put(localPath, remotePath);
        sftpClient.close();
    }

    @Override
    public void download(String localPath, String remotePath) throws IOException {
        SFTPClient sftpClient = sshClient.newSFTPClient();
        for (RemoteResourceInfo resourceInfo : sftpClient.ls(remotePath)) {
            String fullRemotePath = remotePath.concat("/").concat(resourceInfo.getName());
            sftpClient.get(fullRemotePath, localPath);
            sftpClient.rm(fullRemotePath);
        }
        sftpClient.close();
    }
}
