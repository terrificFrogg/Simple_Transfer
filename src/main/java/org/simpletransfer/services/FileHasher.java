package org.simpletransfer.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class FileHasher {
    private static final Logger logger = LogManager.getLogger();

    public static Optional<String> hashFile(File file, String algorithm){
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                logger.warn("Unable to hash file. Error: {}", e.getMessage());
                return Optional.empty();
            }

            byte[] hashBytes = digest.digest();
            return Optional.of(bytesToHex(hashBytes));
        } catch (NoSuchAlgorithmException e) {
            logger.error(e);
            return Optional.empty();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}