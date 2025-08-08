package org.simpletransfer.services;

import org.apache.commons.net.ftp.FTPSClient;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;

public class ReUseFTPSClient extends FTPSClient {
    public ReUseFTPSClient(boolean isImplicit) {
        super(isImplicit);
    }

    @Override
    protected void _prepareDataSocket_(Socket socket) throws IOException {
        if (socket instanceof SSLSocket) {
            SSLSession session = ((SSLSocket) _socket_).getSession(); // control connection session
            ((SSLSocket) socket).setEnableSessionCreation(false);     // don't start a new session
            ((SSLSocket) socket).startHandshake();
            // The key: reuse control connection's SSL session
            ((SSLSocket) socket).getSession().putValue("javax.net.ssl.session", session);
        }
        super._prepareDataSocket_(socket);
    }
}
