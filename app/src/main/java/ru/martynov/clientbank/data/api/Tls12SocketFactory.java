package ru.martynov.clientbank.data.api;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Enables TLS v1.2 and modern protocols on Android 5.0/5.1 (API 21-22)
 * where TLS 1.2 is supported by the OS engine but not enabled by default.
 */
public class Tls12SocketFactory extends SSLSocketFactory {
    private static final String[] TLS_SUPPORT = {"TLSv1.2", "TLSv1.3"};
    private final SSLSocketFactory delegate;

    public Tls12SocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return patch(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return patch(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return patch(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return patch(delegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket patch(Socket s) {
        if (s instanceof SSLSocket) {
            try {
                ((SSLSocket) s).setEnabledProtocols(TLS_SUPPORT);
            } catch (Exception ignored) {
                try {
                    ((SSLSocket) s).setEnabledProtocols(((SSLSocket) s).getSupportedProtocols());
                } catch (Exception ignored2) {}
            }
        }
        return s;
    }
}
