package org.atlasapi.remotesite.pa;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;

import com.google.common.base.Strings;
import com.metabroadcast.common.security.UsernameAndPassword;

public class PaFtpFileUpdater {
    
    private static final Logger log = LoggerFactory.getLogger(PaFtpFileUpdater.class);
    
    private static FTPFileFilter ftpFilenameFilter = new FTPFileFilter() {
        @Override
        public boolean accept(FTPFile file) {
            return file.isFile() && !file.getName().endsWith(".md5");
        }
    };
    
    private final String ftpHost;
    private final UsernameAndPassword ftpCredentials;
    private final String ftpFilesPath;
    private final PaProgrammeDataStore dataStore;

    public PaFtpFileUpdater(String ftpHost, UsernameAndPassword ftpCredentials, String ftpFilesPath, PaProgrammeDataStore dataStore) {
        this.ftpHost = ftpHost;
        this.ftpCredentials = ftpCredentials;
        this.ftpFilesPath = ftpFilesPath;
        this.dataStore = dataStore;
    }

    public void updateFilesFromFtpSite() throws IOException {
        if (Strings.isNullOrEmpty(ftpHost) || Strings.isNullOrEmpty(ftpCredentials.username()) || Strings.isNullOrEmpty(ftpCredentials.password()) || ftpFilesPath == null) {
            log.warn("FTP details incomplete / missing, skipping FTP update");
            return;
        }

        FTPClient client = null;
        
        try {
            client = loggedInClient();
            FTPFile[] listFiles = client.listFiles(ftpFilesPath, ftpFilenameFilter);
            for (final FTPFile ftpFile : listFiles) {
                try {
                    if(dataStore.requiresUpdating(ftpFile)) {
                        InputStream dataStream = client.retrieveFileStream(ftpFile.getName());
                        dataStore.save(ftpFile.getName(), dataStream);
                        dataStream.close();
                        if(!client.completePendingCommand()){
                            throw new Exception("File transfer failed");
                        }
                    }
                } catch (Exception e) {
                    log.error("Error updating file " + ftpFile.getName(), e);
                    if (e instanceof FTPConnectionClosedException) {
                        throw new IOException(e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error when trying to copy files from FTP", e);
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
    }

    private FTPClient loggedInClient() throws SocketException, IOException, Exception {
        FTPClient client;
        client = new FTPClient();
        client.connect(ftpHost);
        client.enterLocalPassiveMode();
        if (!client.login(ftpCredentials.username(), ftpCredentials.password())) {
            throw new Exception("Unable to connect to " + ftpHost + " with username: " + ftpCredentials.username() + " and password...");
        }
        if (!Strings.isNullOrEmpty(ftpFilesPath) && !client.changeWorkingDirectory(ftpFilesPath)) {
            throw new Exception("Unable to change working directory to " + ftpFilesPath);
        }
        return client;
    }

}
