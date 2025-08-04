package org.simpletransfer.models;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface RemoteClient {
    void connect() throws IOException;
    void disconnect() throws IOException;
    boolean isConnected();
    void upload(String localPath, String remotePath) throws IOException;
    void download(String localPath, String remotePath) throws IOException;
}
