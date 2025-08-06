package org.simpletransfer.models;

import java.io.IOException;
import java.nio.file.Path;

public interface RemoteClient {
    void connect() throws IOException;
    void disconnect() throws IOException;
    boolean isConnected();
    void upload(Path localPath, Path remotePath) throws IOException;
    void download(Path localPath, Path remotePath) throws IOException;
}
