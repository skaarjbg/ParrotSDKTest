package co.flyver.parrotsdktest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

/**
 * Created by Petar Petrov on 4/3/15.
 */
public class NetworkingManager {

/**
 * Created by smith on 2/25/15.
 */

    public static class NetworkHelp {
        private static final String TAG_LOG = "ExamplePrj";

        Context context;
        WifiManager wifiMan = null;
        WifiManager.WifiLock wifiLock = null;

        public NetworkHelp(Context context) {
            super();
            this.context = context;
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            StrictMode.setThreadPolicy(policy);
        }

        /**
         * Enable mobile connection for a specific address
         * @param context a Context (application or activity)
         * @param address the address to enable
         * @return true for success, else false
         */
        public static boolean forceMobileConnectionForAddress(Context context, String address) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (null == connectivityManager) {
                Log.d(TAG_LOG, "ConnectivityManager is null, cannot try to force a mobile connection");
                return false;
            }

            //check if mobile connection is available and connected
            State state = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
            Log.d(TAG_LOG, "TYPE_MOBILE_HIPRI network state: " + state);
            if (0 == state.compareTo(State.CONNECTED) || 0 == state.compareTo(State.CONNECTING)) {
                return true;
            }

            //activate mobile connection in addition to other connection already activated
            int resultInt = connectivityManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableHIPRI");
            Log.d(TAG_LOG, "startUsingNetworkFeature for enableHIPRI result: " + resultInt);

            //-1 means errors
            // 0 means already enabled
            // 1 means enabled
            // other values can be returned, because this method is vendor specific
            if (-1 == resultInt) {
                Log.e(TAG_LOG, "Wrong result of startUsingNetworkFeature, maybe problems");
                return false;
            }
            if (0 == resultInt) {
                Log.d(TAG_LOG, "No need to perform additional network settings");
                return true;
            }

            //find the host name to route
            String hostName = extractAddressFromUrl(address);
            Log.d(TAG_LOG, "Source address: " + address);
            Log.d(TAG_LOG, "Destination host address to route: " + hostName);
            if (TextUtils.isEmpty(hostName)) hostName = address;

            //create a route for the specified address
            int hostAddress = lookupHost(hostName);
            if (-1 == hostAddress) {
                Log.e(TAG_LOG, "Wrong host address transformation, result was -1");
                return false;
            }
            //wait some time needed to connection manager for waking up
            try {
                for (int counter=0; counter<30; counter++) {
                    State checkState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
                    if (0 == checkState.compareTo(State.CONNECTED))
                        break;
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                //nothing to do
            }
            boolean resultBool = connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_HIPRI, hostAddress);
            Log.d(TAG_LOG, "requestRouteToHost result: " + resultBool);
            if (!resultBool)
                Log.e(TAG_LOG, "Wrong requestRouteToHost result: expected true, but was false");

            state = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();
            Log.d(TAG_LOG, "TYPE_MOBILE_HIPRI network state after routing: " + state);

            return resultBool;
        }

        /**
         * This method extracts from address the hostname
         * @param url eg. http://some.where.com:8080/sync
         * @return some.where.com
         */
        public static String extractAddressFromUrl(String url) {
            String urlToProcess = null;

            //find protocol
            int protocolEndIndex = url.indexOf("://");
            if(protocolEndIndex>0) {
                urlToProcess = url.substring(protocolEndIndex + 3);
            } else {
                urlToProcess = url;
            }

            // If we have port number in the address we strip everything
            // after the port number
            int pos = urlToProcess.indexOf(':');
            if (pos >= 0) {
                urlToProcess = urlToProcess.substring(0, pos);
            }

            // If we have resource location in the address then we strip
            // everything after the '/'
            pos = urlToProcess.indexOf('/');
            if (pos >= 0) {
                urlToProcess = urlToProcess.substring(0, pos);
            }

            // If we have ? in the address then we strip
            // everything after the '?'
            pos = urlToProcess.indexOf('?');
            if (pos >= 0) {
                urlToProcess = urlToProcess.substring(0, pos);
            }
            return urlToProcess;
        }

        /**
         * Transform host name in int value used by {@link ConnectivityManager . requestRouteToHost}
         * method
         *
         * @param hostname
         * @return -1 if the host doesn't exists, elsewhere its translation
         * to an integer
         */
        private static int lookupHost(String hostname) {
            InetAddress inetAddress = null;

            try {
                inetAddress = InetAddress.getByName(hostname);
            } catch (UnknownHostException e) {
                return -1;
            }catch (Exception e) {
                e.printStackTrace();
            }
            if (inetAddress == null){
                return -1;
            }
            byte[] addrBytes;
            int addr;
            addrBytes = inetAddress.getAddress();
            addr = ((addrBytes[3] & 0xff) << 24)
                    | ((addrBytes[2] & 0xff) << 16)
                    | ((addrBytes[1] & 0xff) << 8 )
                    |  (addrBytes[0] & 0xff);
            return addr;
        }

        @SuppressWarnings("unused")
        private int lookupHost2(String hostname) {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(hostname);
            } catch (UnknownHostException e) {
                return -1;
            }
            byte[] addrBytes;
            int addr;
            addrBytes = inetAddress.getAddress();
            addr = ((addrBytes[3] & 0xff) << 24)


                    | ((addrBytes[2] & 0xff) << 16)
                    | ((addrBytes[1] & 0xff) << 8 )
                    |  (addrBytes[0] & 0xff);
            return addr;
        }

        public Boolean disableWifi() {
            wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiMan != null) {
                wifiLock = wifiMan.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "HelianRCAWifiLock");
            }
            return wifiMan.setWifiEnabled(false);
        }

        public Boolean enableWifi() {
            Boolean success = false;

            if (wifiLock != null && wifiLock.isHeld())
                wifiLock.release();
            if (wifiMan != null)
                success = wifiMan.setWifiEnabled(true);
            return success;
        }

        public static boolean downloadFile(String url, String dest_path, HttpContext context) {
            try {
                URLConnection u = new URL(url).openConnection();
                u.setConnectTimeout(200);
                InputStream is = u.getInputStream();

                DataInputStream dis = new DataInputStream(is);

                byte[] buffer = new byte[1500];
                int length;

                final String filePath = dest_path;
                final File file = new File(filePath);

                FileOutputStream fos = new FileOutputStream(file);
                while ((length = dis.read(buffer)) > 0) {

                    fos.write(buffer, 0, length);
                    if (length < 1448) {
                        break;
                    }
                }

                fos.close();
                Log.d(TAG_LOG, filePath);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public static  boolean uploadFile(String pathToFile, Socket socket, HttpContext context ) {

            try {

                File fil=new File(pathToFile);

                OutputStream os = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);

                int filesize = (int) fil.length();
                byte [] buffer = new byte [filesize];

                FileInputStream fis = new FileInputStream(fil.toString());
                BufferedInputStream bis = new BufferedInputStream(fis);

                bis.read(buffer, 0, buffer.length);
                dos.write(buffer, 0, buffer.length);
                dos.flush();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        public static boolean uploadFile(String pathToOurFile, String urlServer, HttpContext context) {
            HttpURLConnection connection = null;
            DataOutputStream outputStream = null;
            DataInputStream inputStream = null;

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 3024;

            try {
                FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile));

                URL url = new URL(urlServer);

                connection = (HttpURLConnection) url.openConnection();

                HttpClient httpclient = new DefaultHttpClient();

                // Allow Inputs &amp; Outputs.
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setConnectTimeout(500);

                // Set HTTP method to POST.
                connection.setRequestMethod("GET");

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                BasicCookieStore cookieStore = (BasicCookieStore)context.getAttribute(ClientContext.COOKIE_STORE);
                String cookie_val = cookieStore.getCookies().get(0).getValue();
                String cookie_name = cookieStore.getCookies().get(0).getName();

                String cookie = cookie_name+"="+cookie_val;//TextUtils.join(",", cookieStore.getCookies());
                cookie = cookie.trim();
                connection.setRequestProperty("Cookie", cookie);

                outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"piece\";filename=\"" + pathToOurFile + "\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                int serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                bytesRead = connection.getInputStream().read(buffer);
                String text = new String(buffer, 0, bytesRead);
                Log.d("Network Utils", "Read: " + text);

                fileInputStream.close();
                outputStream.flush();
                outputStream.close();
                connection.disconnect();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        }
    }
}
