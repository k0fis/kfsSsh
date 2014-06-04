package kfs.kfsUtils.kfsSsh;

import com.jcraft.jsch.*;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author pavedrim
 */
public abstract class kfsSsh implements UserInfo {

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
            throw new kfsSshException("Cannot get session for " + getUser() + "@" + getHost(), ex);
        }
        session.setUserInfo(this);
        try {
            session.connect();
        } catch (JSchException ex) {
            throw new kfsSshException("Cannot connect for " + getUser() + "@" + getHost(), ex);
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
                throw new kfsSshException("Cannot open chanel for sftp", ex);
            }
            try {
                ch.connect();
            } catch (JSchException ex) {
                throw new kfsSshException("Cannot connect on chanel sftp", ex);
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
            throw new kfsSshException("Cannot open input stream for file " + remoteFile, ex);
        }
    }

    public OutputStream put_owerwrite(String remoteFile) throws kfsSshException {
        if (channel == null) {
            connect();
        }
        try {
            return channel.put(remoteFile, ChannelSftp.OVERWRITE);
        } catch (SftpException ex) {
            throw new kfsSshException("Cannot put file " + remoteFile, ex);
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
            throw new kfsSshException("Cannot append file " + remoteFile, ex);
        }
    }

    public void get(String file, OutputStream out) throws kfsSshException {
        if (channel == null) {
            connect();
        }
        try {
            channel.get(file, out);
        } catch (SftpException ex) {
            throw new kfsSshException("Cannot get file " + file, ex);
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
            throw new kfsSshException("Cannot list folder " + remoteFolder, ex);
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

    public List<String> listFilesByDate(String remoteFolder, FilenameFilter filter, long fromTime, long toTime) throws kfsSshException {
        if (channel == null) {
            connect();
        }
        Vector v = null;
        try {
            v = channel.ls(remoteFolder);
        } catch (SftpException ex) {
            throw new kfsSshException("Cannot list folder " + remoteFolder, ex);
        }
        ArrayList<String> ret = new ArrayList<String>();
        if (v != null) {
            for (Iterator it = v.iterator(); it.hasNext();) {
                Object o = it.next();
                if (o != null) {
                    ChannelSftp.LsEntry le = (ChannelSftp.LsEntry) o;
                    if (le.getFilename().startsWith(".")) {
                        continue;
                    }
                    if (le.getAttrs().isDir()) {
                        continue;
                    }
                    if ((filter == null) || (filter.accept(null, le.getFilename()))) {
                        long mtime = 1000L*((long)le.getAttrs().getMTime());
                        if ((fromTime <= mtime) && (toTime >= mtime)) {
                            ret.add(le.getFilename());
                        }
                    }
                }
            }
        }
        return ret;
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
            throw new kfsSshException("Cannot list folder " + remoteFolder, ex);
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
            throw new kfsSshException("Cannot execute " + cmd, ex);
        }
        if (exCh != null) {
            try {
                exCh.setCommand(cmd);
                exCh.setInputStream(in);
                exCh.setErrStream(err);
                try {
                    exCh.connect();
                } catch (JSchException ex) {
                    throw new kfsSshException("Cannot execute " + cmd, ex);
                }
                try {
                    in = exCh.getInputStream();
                } catch (IOException ex) {
                    throw new kfsSshException("Cannot execute " + cmd, ex);
                }
                byte[] tmp = new byte[1024];
                while (true) {
                    try {
                        while (in.available() > 0) {
                            int i = 0;
                            try {
                                i = in.read(tmp, 0, 1024);
                            } catch (IOException ex) {
                                throw new kfsSshException("Cannot read " + i, ex);
                            }
                            if (i < 0) {
                                break;
                            }
                            try {
                                out.write(tmp, 0, i);
                            } catch (IOException ex) {
                                throw new kfsSshException("Cannot write " + i, ex);
                            }
                        }
                    } catch (IOException ex) {
                        throw new kfsSshException("Cannot read avialable", ex);
                    }
                    if (exCh.isClosed()) {
                        //System.out.println("exit-status: " + exCh.getExitStatus());
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
