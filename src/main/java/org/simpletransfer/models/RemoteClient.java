package org.simpletransfer.models;

import org.simpletransfer.services.clients.FtpRemoteClient;

import java.io.IOException;
import java.util.List;

public interface RemoteClient {
    void connect() throws IOException;
    void disconnect() throws IOException;
    boolean isConnected();
    void upload(String localPath, String remotePath) throws IOException;
    void download(String localPath, String remotePath) throws IOException;
    void createDirectory(String directoryPath) throws IOException;
    List<FileInfo> listContents(String path) throws IOException;
    void delete(String path) throws IOException;
}
