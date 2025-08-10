package org.simpletransfer.services;

import net.schmizz.sshj.SSHClient;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.RemoteClient;
import org.simpletransfer.models.ServerConfig;
import org.simpletransfer.services.clients.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RemoteClientFactory {
    private final Logger logger;
    private final BiConsumer<List<String>, String> uploadedFilesConsumer;

    private final Supplier<SSHClient> sshClientSupplier;
    private final Supplier<FTPClient> ftpClientSupplier;
    private final Supplier<FTPSClient> ftpsClientSupplier;

    public RemoteClientFactory(
            Logger logger,
            BiConsumer<List<String>, String> consumer,
            Supplier<SSHClient> sshClientSupplier,
            Supplier<FTPClient> ftpClientSupplier,
            Supplier<FTPSClient> ftpsClientSupplier
    ) {
        this.logger = logger;
        this.uploadedFilesConsumer = consumer;
        this.sshClientSupplier = sshClientSupplier != null ? sshClientSupplier : SSHClient::new;
        this.ftpClientSupplier = ftpClientSupplier != null ? ftpClientSupplier : FTPClient::new;
        this.ftpsClientSupplier = ftpsClientSupplier != null ? ftpsClientSupplier : () -> new FTPSClient(false);
    }

    public RemoteClient create(ServerConfig config) {
        return switch (config.credentials().type()) {
            case SFTP -> new SftpRemoteClient.Builder()
                    .withCredentials(config.credentials())
                    .withFileInfos(new ArrayList<>())
                    .withLogger(logger)
                    .withSSHClient(sshClientSupplier.get())
                    .withConsumer(uploadedFilesConsumer)
                    .build();

            case FTP -> new FtpRemoteClient.Builder()
                    .withCredentials(config.credentials())
                    .withFileInfos(new ArrayList<>())
                    .withLogger(logger)
                    .withFTPClient(ftpClientSupplier.get())
                    .withConsumer(uploadedFilesConsumer)
                    .build();

            case FTPS -> new FtpsRemoteClient.Builder()
                    .withCredentials(config.credentials())
                    .withFileInfos(new ArrayList<>())
                    .withLogger(logger)
                    .withFTPSClient(ftpsClientSupplier.get())
                    .withConsumer(uploadedFilesConsumer)
                    .build();

            default -> throw new IllegalArgumentException("Unsupported protocol type: " + config.credentials().type());
        };
    }
}
