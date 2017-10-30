```
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.lz.lsf.exception.BusinessException;

/**
 * 
 * 功能概要：SFTP客户端
 * 
 * @author linbingwen
 * @since  2015年8月5日
 */
public class SftpClient {

    private String m_host = "127.0.0.1";

    private int m_port = 22;

    private String m_username = "ctsUser";

    private String m_password = "ctsUser";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Channel m_channel = null;

    public SftpClient(String host, int Port, String userName, String password) {
        this.m_host = host;
        this.m_port = Port;
        this.m_username = userName;
        this.m_password = password;
    }

    public void reConnect() {
        try {
            this.connect();
        }
        catch (Exception e) {
            logger.warn("m_channel disconnect fail!", e);
        }
    }

    public void connect() {
        JSch jsch = new JSch();
        try {
            jsch.getSession(m_username, m_host, m_port);
            Session sshSession = jsch.getSession(m_username, m_host, m_port);
            logger.info("username:" + m_username + ", host:" + m_host + ",port:" + m_port);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(config);
            logger.debug("StrictHostKeyChecking", "no");
            sshSession.setPassword(m_password);
            sshSession.connect();
            logger.debug("Session connected.");
            m_channel = sshSession.openChannel("sftp");
            logger.debug("Opening Channel.");
            m_channel.connect();
            logger.info("Connected to {} success! ", m_host);
        }
        catch (JSchException e) {
            logger.error("connected to " + m_host + "Fail! ");
            throw new BusinessException(CommonErrorCode.ERROR_CONNECT_SFTP_FAIL, e, "connected to " + m_host + "Fail! ");
        }
    }

    public void disConnect() {
        try {
            if (m_channel == null)
                return;

            synchronized (m_channel) {
                if (m_channel.getSession().isConnected())
                    m_channel.getSession().disconnect();
            }

            m_channel.disconnect();

        }
        catch (JSchException e) {
            logger.warn("m_channel disconnect fail!", e);
        }
        finally {
            if (m_channel != null)
                m_channel = null;
        }
    }

    public boolean isTryConnect() {
        int tryConnectCount = 0;
        try {
            while (true) {
                tryConnectCount++;
                if (m_channel.getSession().isConnected())
                    return true;
                else {
                    if (tryConnectCount >= 3)
                        return false;
                    else {
                        this.reConnect();
                    }
                }
            }
        }
        catch (JSchException e) {
            logger.warn("m_channel isConnected fail!", e);
            return false;
        }
    }

    /**
     * 上传文件
     * 
     * @param directoryName
     *            上传的目录
     * @param uploadFileName
     *            要上传的文件
     * @param sftp
     * @throws SftpException
     * @throws FileNotFoundException
     * @throws JSchException
     */
    public void upload(String remotePathDirName, String uploadFileName) {
        ChannelSftp sftp = (ChannelSftp) m_channel;
        if (!this.isTryConnect()) {
            logger.error("尝试连接SFTP服务器失败！");
           throw new BusinessException(CommonErrorCode.ERROR_CONNECT_SFTP_FAIL);
        }
        try {
            sftp.cd(remotePathDirName);
            File uploadFile = new File(uploadFileName);
            sftp.put(new FileInputStream(uploadFile), uploadFile.getName());
            logger.debug("Upload file:{} to remote dir:{}", uploadFileName, remotePathDirName);
        }
        catch (FileNotFoundException e) {
            logger.error("download remote path({})FileNotFound{}", remotePathDirName, uploadFileName);
            throw new BusinessException(CommonErrorCode.NOT_EXISTS_PATH_SFTP_REMOTE, e, "FileNotFound:" + uploadFileName);
        }
        catch (SftpException e) {
            logger.error("download remote path({}) not exists!{}", remotePathDirName, e);
            throw new BusinessException(CommonErrorCode.NOT_EXISTS_PATH_SFTP_REMOTE, e, "remote path:" + remotePathDirName);
        }
    }

    public void uploadBatch(String directoryName, List<String> fileNameList) {
        for (String fileName : fileNameList) {
            this.upload(directoryName, fileName);
        }
    }

    /**
     * 下载文件
     * 
     * @param directoryName
     *            下载目录
     * @param downloadFileName
     *            下载的文件
     * @param saveFileName
     *            存在本地的路径
     * @param sftp
     * @throws SftpException
     * @throws FileNotFoundException
     * @throws JSchException
     */
    public void download(String remotePathDirName, String localPathDirName, String downloadFileName) {
        ChannelSftp sftp = (ChannelSftp) m_channel;
        if (!this.isTryConnect()) {
            logger.error("尝试连接SFTP服务器失败！");
          //  throw new BusinessException(ActErrorCode.ERROR_CONNECT_SFTP_FAIL);
        }
        try {
            sftp.cd(remotePathDirName);
            File saveFile = new File(localPathDirName + "//" + downloadFileName);
            sftp.get(downloadFileName, new FileOutputStream(saveFile));
            logger.debug("Download file:{} save as {}", downloadFileName, localPathDirName + "//" + downloadFileName);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.error("download remote path{}//{}", remotePathDirName, downloadFileName);
            throw new BusinessException(CommonErrorCode.NOT_FINDFIELS_REMOTE_PATH, e, "FileNotFound:" + downloadFileName);
        }
        catch (SftpException e) {
            logger.error("download remote path({}) fail!{}", remotePathDirName + downloadFileName, e);
            throw new BusinessException(CommonErrorCode.NOT_EXISTS_PATH_SFTP_REMOTE, e, "remote path:" + remotePathDirName);
        }
    }

    public void downloadBatch(String directoryName, String localPathDirName, List<String> downloadFileNameList) {
        for (String fileName : downloadFileNameList) {
            this.download(directoryName, localPathDirName, fileName);
        }
    }

    public boolean isFileExists(String remotePathDirName) {
        ChannelSftp sftp = (ChannelSftp) m_channel;
        if (!this.isTryConnect()) {
            logger.error("尝试连接SFTP服务器失败！");
            throw new BusinessException(CommonErrorCode.ERROR_CONNECT_SFTP_FAIL);
        }
        try {
            Vector<LsEntry> filesName = sftp.ls(remotePathDirName);
            return filesName.size() > 0;
        }
        catch (SftpException e) {
            logger.warn("download remote path({}) not exists!{}", remotePathDirName, e);
            return false;
        }
    }

    /**
     * 删除文件
     * 
     * @param directory
     *            要删除文件所在目录
     * @param deleteFileName
     *            要删除的文件
     * @param sftp
     * @throws SftpException
     * @throws JSchException
     */
    public void delete(String directory, String deleteFileName) throws SftpException, JSchException {
        ChannelSftp sftp = (ChannelSftp) m_channel;
        if (!this.isTryConnect()) {
            logger.error("尝试连接SFTP服务器失败！");
           throw new BusinessException(CommonErrorCode.ERROR_CONNECT_SFTP_FAIL);
        }
        sftp.cd(directory);
        sftp.rm(deleteFileName);
        logger.info("Delete file:{} from remote dir:{}", deleteFileName, directory);
    }

    /**
     * 列出目录下的文件
     * 
     * @param directoryName
     *            要列出的目录
     * @param sftp
     * @return
     * @throws SftpException
     * @throws JSchException
     */
    @SuppressWarnings("unchecked")
    public Vector<LsEntry> listFiles(String directoryName) throws SftpException, JSchException {
        ChannelSftp sftp = (ChannelSftp) m_channel;
        if (!this.isTryConnect()) {
            logger.error("尝试连接SFTP服务器失败！");
           throw new BusinessException(CommonErrorCode.ERROR_CONNECT_SFTP_FAIL);
        }
        Vector<LsEntry> filesName = sftp.ls(directoryName);
        return filesName;
    }

    /**
     * 列出目录下符合要求的文件
     * 
     * @param directoryName
     *            要列出的目录
     * @param reg
     *            文件名前缀
     * @param postfix
     *            文件名后缀(格式)
     * @return
     * @throws SftpException
     * @throws JSchException
     */
    @SuppressWarnings("unchecked")
    public Vector<LsEntry> listFiles(String remotePathDirName, String reg, String postfix) {
        ChannelSftp sftp = (ChannelSftp) m_channel;
        if (!this.isTryConnect()) {
            logger.error("尝试连接SFTP服务器失败！");
           throw new BusinessException(CommonErrorCode.ERROR_CONNECT_SFTP_FAIL);
        }
        Vector<LsEntry> filesName;
        try {
            filesName = sftp.ls(remotePathDirName);
            Vector<LsEntry> filterFilesName = new Vector<LsEntry>();
            for (LsEntry lsEntry : filesName) {
                if (lsEntry.getFilename().indexOf(reg) > -1 && lsEntry.getFilename().endsWith(postfix)) {
                    filterFilesName.add(lsEntry);
                }
            }
            return filterFilesName;
        }
        catch (SftpException e) {
            logger.error("download remote path({}) not exists!{}", remotePathDirName, e);
            throw new BusinessException(CommonErrorCode.NOT_EXISTS_PATH_SFTP_REMOTE, e, "remote path" + remotePathDirName);
        }
	
    }

}
```
