package org.simpletransfer.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpletransfer.models.Parent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConfigParser {
    private static final Logger logger = LogManager.getLogger();

    public static Parent getConfig(String path){
        try(InputStream inputStream = new FileInputStream(path)){
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(inputStream, Parent.class);
        } catch (IOException e) {
            logger.error("Error while parsing config: {}", e.getMessage());
        }
        return null;
    }
}
