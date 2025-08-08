package org.simpletransfer.services.clients;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.Credentials;
import org.simpletransfer.models.FileInfo;
import org.simpletransfer.models.FileType;
import org.simpletransfer.models.RemoteClient;
import org.simpletransfer.services.ReUseFTPSClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FtpsRemoteClient implements RemoteClient {
    protected static final Logger logger = LogManager.getLogger();
    private final FTPSClient ftpsClient;
    private final Credentials credentials;
    private final List<FileInfo> fileInfos;

    public FtpsRemoteClient(Credentials credentials, FTPSClient ftpsClient, List<FileInfo> fileInfos){
        this.ftpsClient = ftpsClient;
        this.credentials = credentials;
        this.fileInfos = fileInfos;
    }

    public FtpsRemoteClient(Credentials credentials){
        this(credentials, new FTPSClient(false), new ArrayList<>());
    }


    @Override
    public void connect() throws IOException {
        ftpsClient.connect(credentials.hostname(), credentials.port());
        if(!FTPReply.isPositiveCompletion(ftpsClient.getReplyCode())){
            ftpsClient.disconnect();
            logger.error("{} refused connection request. Reply Code: {}", credentials.hostname(), ftpsClient.getReplyCode());
            return;
        }
        logger.info("Connected to {}", credentials.hostname());

        if(!ftpsClient.login(credentials.username(), credentials.password())){
            logger.error("Failed to login");
            return;
        }
        ftpsClient.setEnabledSessionCreation(false);
        ftpsClient.setEndpointCheckingEnabled(false);
        ftpsClient.setUseClientMode(true);

        ftpsClient.execPBSZ(0);
        ftpsClient.execPROT("P");
        ftpsClient.enterLocalPassiveMode();
    }

    @Override
    public void disconnect() throws IOException {
        if(ftpsClient.isConnected()){
            ftpsClient.disconnect();
        }
    }

    @Override
    public boolean isConnected() {
        return ftpsClient.isConnected();
    }

    @Override
    public void upload(String localPath, String remotePath) throws IOException {
        if(isConnected()){
            try(InputStream localFileStream = new FileInputStream(localPath)){
                ftpsClient.storeFile(remotePath, localFileStream);
            }
        }
    }

    @Override
    public void download(String localPath, String remotePath) throws IOException {
        int downloadCount = 0;
        if(isConnected()){
            for (FTPFile ftpFile : ftpsClient.listFiles(remotePath)) {
                if(ftpFile.isFile()){
                    logger.info("File Link: {}", ftpFile.getLink());
                    try(OutputStream outputStream = new FileOutputStream(localPath.concat("\\").concat(ftpFile.getName()))){
                        ftpsClient.retrieveFile(ftpFile.getLink(), outputStream);
                        downloadCount++;
                    }
                }
            }
            logger.info("Downloaded {} files from {}", downloadCount, credentials.hostname());
        }
    }

    @Override
    public void createDirectory(String directoryPath) throws IOException {
        if(isConnected()){
            ftpsClient.makeDirectory(directoryPath);
        }
    }

    @Override
    public List<FileInfo> listContents(String path) throws IOException {
        fileInfos.clear();
        if(isConnected()){
            for (FTPFile ftpFile : ftpsClient.listFiles(path)) {
                FileType fileType = null;
                if(ftpFile.isFile()){
                    fileType = FileType.FILE;
                }else if(ftpFile.isDirectory()){
                    fileType = FileType.DIRECTORY;
                }
                fileInfos.add(new FileInfo(ftpFile.getName(), ftpFile.getLink(), fileType));
            }
        }
        return fileInfos;
    }

    @Override
    public void delete(String path) throws IOException {
        if(isConnected()){
            ftpsClient.deleteFile(path);
        }
    }
}
