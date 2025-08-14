package org.simpletransfer.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Util {
    protected static final Logger logger = LogManager.getLogger();

    public static void moveFile(String source, String destination){
        try {
            File sourceFile = new File(source);
            Path destinationPath = Path.of(destination);
            if(sourceFile.isDirectory()){
                for (File file : Objects.requireNonNull(sourceFile.listFiles())) {
                    if(file.isFile())
                        Files.move(file.toPath(), destinationPath.resolve(file.getName()), REPLACE_EXISTING);
                }
            }else if(sourceFile.isFile()){
                Files.move(sourceFile.toPath(), destinationPath.resolve(sourceFile.getName()), REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public static void deleteFile(String source){
        try {
            Files.deleteIfExists(Path.of(source));
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public static void createDir(String inboundFolder, String inboundFolderArchive, String name){
        String folder_in = inboundFolder.concat("\\").concat(name);
        String folder_in_archive = inboundFolderArchive.concat("\\").concat(name);

        try{
            Path dir_in = Path.of(folder_in);
            if(!Files.exists(dir_in)){
                Files.createDirectories(dir_in);
            }

            Path dir_in_archive = Path.of(folder_in_archive);
            if(!Files.exists(dir_in_archive)){
                Files.createDirectories(dir_in_archive);
            }
        } catch (IOException e) {
            logger.error("Error creating directory: ", e);
        }
    }

    public static void printDefaultConfig(){
        logger.info("Saving default config file in application directory");
        PrintWriter printWriter = null;
        String defaultConfig = "{\n" +
                "  \"configCollection\": [\n" +
                "    {\n" +
                "      \"source\": {\n" +
                "        \"connectionCreds\": {\n" +
                "          \"type\": \"FTP\",\n" +
                "          \"port\": 21,\n" +
                "          \"hostname\": \"hostname1\",\n" +
                "          \"username\": \"username1\",\n" +
                "          \"password\": \"password1\"\n" +
                "        },\n" +
                "        \"folderPath\": \"\\\\folderPath\"\n" +
                "      },\n" +
                "      \"destination\": [\n" +
                "        {\n" +
                "          \"connectionCreds\": {\n" +
                "            \"type\": \"FTP\",\n" +
                "            \"port\": 21,\n" +
                "            \"hostname\": \"hostname2\",\n" +
                "            \"username\": \"username2\",\n" +
                "            \"password\": \"password2\"\n" +
                "          },\n" +
                "          \"folderPath\": \"folderPath\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"connectionCreds\": {\n" +
                "            \"type\": \"SFTP\",\n" +
                "            \"port\": 22,\n" +
                "            \"hostname\": \"hostname3\",\n" +
                "            \"username\": \"username3\",\n" +
                "            \"password\": \"password3\"\n" +
                "          },\n" +
                "          \"folderPath\": \"folderPath\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"source\": {\n" +
                "        \"connectionCreds\": {\n" +
                "          \"type\": \"FTP\",\n" +
                "          \"port\": 21,\n" +
                "          \"hostname\": \"hostname4\",\n" +
                "          \"username\": \"username4\",\n" +
                "          \"password\": \"password4\"\n" +
                "        },\n" +
                "        \"folderPath\": \"\\\\folderPath\"\n" +
                "      },\n" +
                "      \"destination\": [\n" +
                "        {\n" +
                "          \"connectionCreds\": {\n" +
                "            \"type\": \"FTP\",\n" +
                "            \"port\": 21,\n" +
                "            \"hostname\": \"hostname5\",\n" +
                "            \"username\": \"username5\",\n" +
                "            \"password\": \"password5\"\n" +
                "          },\n" +
                "          \"folderPath\": \"folderPath\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"source\": {\n" +
                "        \"connectionCreds\": {\n" +
                "          \"type\": \"FTP\",\n" +
                "          \"port\": 21,\n" +
                "          \"hostname\": \"hostname6\",\n" +
                "          \"username\": \"username6\",\n" +
                "          \"password\": \"password6\"\n" +
                "        },\n" +
                "        \"folderPath\": \"\\\\folderPath\"\n" +
                "      },\n" +
                "      \"destination\": [\n" +
                "        {\n" +
                "          \"connectionCreds\": {\n" +
                "            \"type\": \"FTP\",\n" +
                "            \"port\": 21,\n" +
                "            \"hostname\": \"hostname7\",\n" +
                "            \"username\": \"username7\",\n" +
                "            \"password\": \"password7\"\n" +
                "          },\n" +
                "          \"folderPath\": \"folderPath\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        try{
            printWriter = new PrintWriter("config.json", StandardCharsets.UTF_8);
            printWriter.println(defaultConfig);
            printWriter.flush();
        }catch (IOException e){
            logger.error(e);
            throw new RuntimeException(e);
        }finally {
            if(printWriter != null)
                printWriter.close();
        }
    }
}