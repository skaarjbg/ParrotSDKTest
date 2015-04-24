package co.flyver.parrotsdktest.devicecontroller.datatransfer;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Petar Petrov on 4/7/15.
 */
public class FTPConnection {
    private final static String TAG = "FTPConnection";
    private FTPClient ftpClient;
    private Thread worker;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    public FTPConnection(final String ip, final int port, final String login, final String password) {
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                ftpClient = new FTPClient();
                try {
                    ftpClient.connect(ip, port);
                    showServerResponse();

                    if(!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                        Log.d(TAG, "Connect failed.");
                    }

                    boolean success = ftpClient.login(login, password == null ? "" : password);
                    showServerResponse();
                    Timer timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                ftpClient.sendNoOp();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, 5000);

                    if(!success) {
                        Log.d(TAG, "Login failed");
                    }
                    navigateToFolder("internal_000/Bebop_Drone/media/");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        worker.start();
    }

    public void navigateToFolder(String path) {
        checkConnection();
        try {
            ftpClient.changeWorkingDirectory(path);
            showServerResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] listFilesCurrentDirectory() {
        String[] result = new String[0];
        try {
            result = executor.submit(new Callable<String[]>() {
                @Override
                public String[] call() throws Exception {
                    checkConnection();
                    String[] fileDetails = new String[0];
                    try {
                        FTPFile[] files = ftpClient.listFiles();
                        showServerResponse();
                        fileDetails = new String[files.length];
                        DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        for (int i = 0; i < files.length; i++) {
                            String details;
                            if (files[i].isDirectory()) {
                                details = " [ " + files[i].getName() + " ] ";
                            } else {
                                details = files[i].getName();
                            }
                            details += "\t\t" + (files[i].getSize() / 1024) + "KB\t\t" + dateFormater.format(files[i].getTimestamp().getTime());
                            fileDetails[i] = details;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return fileDetails;
                }
            }).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getLastFileNameCurrentDirectory() {
        checkConnection();
        String result = null;
        try {
            result = executor.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    FTPFile[] files = ftpClient.listFiles();
                    showServerResponse();
                    return files[files.length - 1].getName();
                }
            } ).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    public File getFileCurrentDirectory(final String filename) {
        checkConnection();
        final File[] file = {null};
        try {
            file[0] = executor.submit(new Callable<File>() {
                @Override
                public File call() throws Exception {
                    boolean success = false;
                    ftpClient.enterLocalPassiveMode();
                    try {
                        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        String remoteFilePath = ftpClient.printWorkingDirectory().concat("/".concat(filename));
                        file[0] = new File(Environment.getExternalStorageDirectory().getPath().concat("/co.flyver/droneselfie/pictures/".concat(filename.replace('-', '_').replace('+', '_').trim())));
                        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file[0]));
//                        success = ftpClient.retrieveFile(remoteFilePath, os);
                        InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);
                        byte[] bytes = new byte[4096];
                        int bytesRead = -1;
                        while((bytesRead = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, bytesRead);
                        }
                        success = ftpClient.completePendingCommand();
                        outputStream.close();
                        inputStream.close();
                        showServerResponse();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return success ? file[0] : null;
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Downloading file: ".concat(file[0].getName()));
        return file[0];
    }

    public int getFileCountCurrentDirectory() {
        checkConnection();
        int result = 0;
        try {
            result = executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    FTPFile[] files = ftpClient.listFiles();
                    return files.length;
                }
            }).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void showServerResponse() {
        String[] replies = ftpClient.getReplyStrings();
        if(replies != null && replies.length > 0) {
            for (String reply : replies) {
                Log.d(TAG, reply);
            }
        }
    }

    private void checkConnection() {
        if(ftpClient == null || !ftpClient.isConnected()) {
            throw new IllegalStateException("Not connected to the ftp server");
        }
    }

    @Override
    protected void finalize() {
        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        try {
            ftpClient.logout();
            ftpClient.disconnect();
            worker.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
