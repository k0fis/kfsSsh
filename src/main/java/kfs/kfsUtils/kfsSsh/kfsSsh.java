package kfs.kfsUtils.kfsSsh;

import com.jcraft.jsch.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author pavedrim
 */
public abstract class kfsSsh implements UserInfo {

    private static final org.apache.log4j.Logger l = org.apache.log4j.Logger.getLogger(kfsSsh.class);
    //
    private final JSch ssh;
    private Session session;
    private ChannelSftp channel;

    public kfsSsh() throws kfsSshException {
        ssh = new JSch();
        session = null;
        channel = null;
    }

    public void connect2() throws kfsSshException {
        try {
            session = ssh.getSession(getUser(), getHost(), getPort());
        } catch (JSchException ex) {
            String s = "Cannot get session for " + getUser() + "@" + getHost();
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
        session.setUserInfo(this);
        try {
            session.connect();
        } catch (JSchException ex) {
            String s = "Cannot connect for " + getUser() + "@" + getHost();
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
    }

    public void connect() throws kfsSshException {
        if (session == null) {
            connect2();
        }
        if (channel == null) {
            Channel ch;
            try {
                ch = session.openChannel("sftp");
            } catch (JSchException ex) {
                String s = "Cannot open chanel for sftp";
                l.error(s, ex);
                throw new kfsSshException(s, ex);
            }
            try {
                ch.connect();
            } catch (JSchException ex) {
                String s = "Cannot connect on chanel sftp";
                l.error(s, ex);
                throw new kfsSshException(s, ex);
            }
            channel = (ChannelSftp) ch;
        }
    }

    public InputStream getStream(String remoteFile) throws kfsSshException {
        if (channel == null) {
            connect();
        }
        try {
            return channel.get(remoteFile);
        } catch (SftpException ex) {
            String s = "Cannot open input stream for file " + remoteFile;
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
    }

    public OutputStream put_owerwrite(String remoteFile) throws kfsSshException {
        if (channel == null) {
            connect();
        }
        try {
            return channel.put(remoteFile, ChannelSftp.OVERWRITE);
        } catch (SftpException ex) {
            String s = "Cannot put file " + remoteFile;
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
    }

    public void put_append(InputStream input, String remoteFile) throws //
            kfsSshException {
        if (channel == null) {
            connect();
        }
        try {
            channel.put(input, remoteFile, ChannelSftp.APPEND);
        } catch (SftpException ex) {
            String s = "Cannot append file " + remoteFile;
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
    }

    public void get(String file, OutputStream out) throws kfsSshException {
        if (channel == null) {
            connect();
        }
        try {
            channel.get(file, out);
        } catch (SftpException ex) {
            String s = "Cannot get file " + file;
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
    }

    public String[] ls(String remoteFolder) throws kfsSshException {
        if (channel == null) {
            connect();
        }
        ArrayList<String> al = new ArrayList<String>();
        Vector v = null;
        try {
            v = channel.ls(remoteFolder);
        } catch (SftpException ex) {
            String s = "Cannot list folder " + remoteFolder;
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
        if (v != null) {
            for (Object o : v) {
                if (o != null) {
                    ChannelSftp.LsEntry le = (ChannelSftp.LsEntry) o;
                    al.add(le.getFilename());
                }
            }
        }
        return al.toArray(new String[0]);
    }

    public String[] lsl(String remoteFolder) throws kfsSshException {
        if (channel == null) {
            connect();
        }

        SortingList<String> al = new SortingList<String>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        Vector v = null;
        try {
            v = channel.ls(remoteFolder);
        } catch (SftpException ex) {
            String s = "Cannot list folder " + remoteFolder;
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
        if (v != null) {
            for (Iterator it = v.iterator(); it.hasNext();) {
                Object o = it.next();
                if (o != null) {
                    ChannelSftp.LsEntry le = (ChannelSftp.LsEntry) o;
                    if (le.getFilename().startsWith(".")) {
                        continue;
                    }
                    al.add(String.format("| %1$tm-%1$td %1$tR | %2$10d | %3$-30s |", //
                            new java.util.Date(le.getAttrs().getMTime() * 1000L),//
                            le.getAttrs().getSize(), //
                            (le.getAttrs().isDir() ? "/" : "") + le.getFilename()));
                }
            }
        }
        ArrayList<String> s = new ArrayList<String>();
        s.add("|-------------|------------|--------------------------------|");
        s.add("|     time    |   size [B] | name                           |");
        s.add("|-------------|------------|--------------------------------|");
        s.addAll(al);
        s.add("|-------------|------------|--------------------------------|");
        return s.toArray(new String[0]);
    }

    public void done() {
        if (channel != null) {
            channel.exit();
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
        channel = null;
        session = null;
    }

    public void execute(String cmd, InputStream in, OutputStream out, OutputStream err) throws kfsSshException {
        if (session == null) {
            connect2();
        }
        ChannelExec exCh;
        try {
            exCh = (ChannelExec) session.openChannel("exec");
        } catch (JSchException ex) {
            String s = "Cannot execute " + cmd;
            l.error(s, ex);
            throw new kfsSshException(s, ex);
        }
        if (exCh != null) {
            try {
                exCh.setCommand(cmd);
                exCh.setInputStream(in);
                exCh.setErrStream(err);
                try {
                    exCh.connect();
                } catch (JSchException ex) {
                    String s = "Cannot execute " + cmd;
                    l.error(s, ex);
                    throw new kfsSshException(s, ex);
                }
                try {
                    in = exCh.getInputStream();
                } catch (IOException ex) {
                    String s = "Cannot execute " + cmd;
                    l.error(s, ex);
                    throw new kfsSshException(s, ex);
                }
                byte[] tmp = new byte[1024];
                while (true) {
                    try {
                        while (in.available() > 0) {
                            int i = 0;
                            try {
                                i = in.read(tmp, 0, 1024);
                            } catch (IOException ex) {
                                l.error("Cannot read " + i);
                                throw new kfsSshException("Cannot read " + i, ex);
                            }
                            if (i < 0) {
                                break;
                            }
                            try {
                                out.write(tmp, 0, i);
                            } catch (IOException ex) {
                                l.error("Cannot write " + i);
                                throw new kfsSshException("Cannot write " + i, ex);
                            }
                        }
                    } catch (IOException ex) {
                        l.error("Cannot read avialable", ex);
                        throw new kfsSshException("Cannot read avialable", ex);
                    }
                    if (exCh.isClosed()) {
                        System.out.println("exit-status: " + exCh.getExitStatus());
                        break;
                    }
                }

            } finally {
                exCh.disconnect();
            }
        }

    }

    public int getPort() {
        return 22;
    }

    @Override
    public String getPassphrase() {
        return null;
    }

    @Override
    public boolean promptPassword(String message) {
        return true;
    }

    @Override
    public boolean promptPassphrase(String message) {
        return true;
    }

    @Override
    public boolean promptYesNo(String message) {
        return true;
    }

    @Override
    public void showMessage(String message) {
    }

    public abstract String getUser();

    public abstract String getHost();

    @Override
    public abstract String getPassword();
}
