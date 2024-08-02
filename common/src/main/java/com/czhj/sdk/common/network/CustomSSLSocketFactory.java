package com.czhj.sdk.common.network;

import android.net.SSLCertificateSocketFactory;
import android.os.Build;

import com.czhj.sdk.common.utils.Preconditions;
import com.czhj.sdk.common.utils.ReflectionUtil;
import com.czhj.sdk.logger.SigmobLog;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class CustomSSLSocketFactory extends SSLSocketFactory {


    private SSLSocketFactory mCertificateSocketFactory;

    private CustomSSLSocketFactory() {
    }


    public static CustomSSLSocketFactory getDefault(final int handshakeTimeoutMillis) {
        CustomSSLSocketFactory factory = new CustomSSLSocketFactory();
        factory.mCertificateSocketFactory = SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis, null);

        return factory;
    }


//   private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
//        @Override
//        public void checkClientTrusted(
//                java.security.cert.X509Certificate[] chain,
//                String authType) {
//        }
//
//        @Override
//        public void checkServerTrusted(
//                java.security.cert.X509Certificate[] chain,
//                String authType) {
//        }
//
//        @Override
//        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//            return null;
//        }
//    } };
//
//    public static CustomSSLSocketFactory getForceValid(){
//
//        CustomSSLSocketFactory factory = new CustomSSLSocketFactory();
//
//        // Install the all-trusting trust manager
//        SSLContext sslContext = null;
//
//        try {
//            sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, trustAllCerts,
//                    new java.security.SecureRandom());
//        } catch (KeyManagementException e) {
//             SigmobLog.e(e.getMessage());
//        } catch (NoSuchAlgorithmException e) {
//             SigmobLog.e(e.getMessage());
//        }
//        // Create an ssl socket factory with our all-trusting manager
//        if (sslContext == null) return null;
//        factory.mCertificateSocketFactory = sslContext
//                .getSocketFactory();
//        return factory;
//
//    }

    // Forward all methods. Enable TLS 1.1 and 1.2 before returning.

    // SocketFactory overrides
    @Override
    public Socket createSocket() throws IOException {
        if (mCertificateSocketFactory == null) {
            throw new SocketException("SSLSocketFactory was null. Unable to create socket.");
        }
        final Socket socket = mCertificateSocketFactory.createSocket();
        enableTlsIfAvailable(socket);
        return socket;
    }


    @Override
    public Socket createSocket(final String host, final int i) throws IOException {
        if (mCertificateSocketFactory == null) {
            throw new SocketException("SSLSocketFactory was null. Unable to create socket.");
        }
        final Socket socket = mCertificateSocketFactory.createSocket(host, i);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localhost, final int localPort) throws IOException {
        if (mCertificateSocketFactory == null) {
            throw new SocketException("SSLSocketFactory was null. Unable to create socket.");
        }
        final Socket socket = mCertificateSocketFactory.createSocket(host, port, localhost, localPort);
        enableTlsIfAvailable(socket);
        return socket;
    }


    @Override
    public Socket createSocket(final InetAddress address, final int port) throws IOException {
        if (mCertificateSocketFactory == null) {
            throw new SocketException("SSLSocketFactory was null. Unable to create socket.");
        }
        final Socket socket = mCertificateSocketFactory.createSocket(address, port);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final InetAddress address, final int port, final InetAddress localhost, final int localPort) throws IOException {
        if (mCertificateSocketFactory == null) {
            throw new SocketException("SSLSocketFactory was null. Unable to create socket.");
        }
        final Socket socket = mCertificateSocketFactory.createSocket(address, port, localhost, localPort);
        enableTlsIfAvailable(socket);
        return socket;
    }

    // SSLSocketFactory overrides

    @Override
    public String[] getDefaultCipherSuites() {
        if (mCertificateSocketFactory == null) {
            return new String[]{};
        }
        return mCertificateSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        if (mCertificateSocketFactory == null) {
            return new String[]{};
        }
        return mCertificateSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(final Socket socketParam, final String host, final int port, final boolean autoClose) throws IOException {
        if (mCertificateSocketFactory == null) {
            throw new SocketException("SSLSocketFactory was null. Unable to create socket.");
        }

        // There is a bug in Android before version 6.0 where SNI does not work, so we try to do
        // it manually here.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Don't use the original socket and create a new one. This closes the original socket
            // if the autoClose flag is set.
            if (autoClose && socketParam != null) {
                socketParam.close();
            }

            final Socket socket = mCertificateSocketFactory.createSocket(
                    InetAddressUtils.getInetAddressByName(host), port);
            enableTlsIfAvailable(socket);
            doManualServerNameIdentification(socket, host);
            return socket;
        }

        final Socket socket = mCertificateSocketFactory.createSocket(socketParam, host, port,
                autoClose);
        enableTlsIfAvailable(socket);
        return socket;
    }

    /**
     * Some versions of Android fail to do server name identification (SNI) even though they are
     * able to. This method forces SNI to happen, if possible. SNI is only used in https
     * connections, and this method will no-op for http connections. This method throws an
     * SSLHandshakeException if SNI fails. This method may also throw other socket-related
     * IOExceptions.
     *
     * @param socket The socket to do SNI on
     * @param host   The host to verify the server name
     * @throws IOException
     */
    private void doManualServerNameIdentification(final Socket socket,
                                                  final String host) throws IOException {
        Preconditions.NoThrow.checkNotNull(socket);

        if (mCertificateSocketFactory == null) {
            throw new SocketException("SSLSocketFactory was null. Unable to create socket.");
        }

        if (socket instanceof SSLSocket) {
            try {
                final SSLSocket sslSocket = (SSLSocket) socket;
                if (mCertificateSocketFactory instanceof SSLCertificateSocketFactory) {
                    setHostnameOnSocket((SSLCertificateSocketFactory) mCertificateSocketFactory, sslSocket,
                            host);
                    verifyServerName(sslSocket, host);
                }

            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }

        }
    }

    /**
     * Calling setHostname on a socket turns on the server name identification feature.
     * Unfortunately, this was introduced in Android version 17, so we do what we can.
     */

    private static void setHostnameOnSocket(final SSLCertificateSocketFactory certificateSocketFactory,
                                            final SSLSocket sslSocket, final String host) {
        Preconditions.NoThrow.checkNotNull(certificateSocketFactory);
        Preconditions.NoThrow.checkNotNull(sslSocket);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            certificateSocketFactory.setHostname(sslSocket, host);
        } else {
            try {
                new ReflectionUtil.MethodBuilder(sslSocket, "setHostname")
                        .addParam(String.class, host)
                        .execute();
            } catch (Throwable e) {
                SigmobLog.d("Unable to call setHostname() on the socket");
            }
        }
    }

    /**
     * This actually performs server name identification.
     */

    private static void verifyServerName(final SSLSocket sslSocket,
                                         final String host) throws IOException {
        Preconditions.NoThrow.checkNotNull(sslSocket);

        sslSocket.startHandshake();
        final HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        if (!hostnameVerifier.verify(host, sslSocket.getSession())) {
            throw new SSLHandshakeException("Server Name Identification failed.");
        }
    }

    private void enableTlsIfAvailable(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            String[] supportedProtocols = sslSocket.getSupportedProtocols();
            // Make sure all supported protocols are enabled. Android does not enable TLSv1.1 or
            // TLSv1.2 by default.
            sslSocket.setEnabledProtocols(supportedProtocols);
        }
    }

    @Deprecated
    void setCertificateSocketFactory(final SSLSocketFactory sslSocketFactory) {
        mCertificateSocketFactory = sslSocketFactory;
    }
}
