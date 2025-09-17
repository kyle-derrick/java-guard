package io.kyle.javaguard.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2025/5/20 11:10
 */
public final class InternalResourceURLConnection extends JarURLConnection {
    private final JarURLConnection connection;

    private InternalResourceURLConnection(JarURLConnection connection) throws MalformedURLException {
        super(connection.getURL());
        this.connection = connection;
    }

    public static URLConnection handleConnection(URLConnection connection) {
        if (connection instanceof JarURLConnection) {
            URL url = connection.getURL();
            if ("jar".equals(url.getProtocol())) {
                try {
                    return new InternalResourceURLConnection((JarURLConnection) connection);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return connection;
    }

    @Override
    public void connect() throws IOException {
        connection.connect();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream inputStream = connection.getInputStream();
        TinyHeadInputStream headStream = new TinyHeadInputStream(inputStream);
        if (headStream.isJgResource()) {
            return new InternalResourceDecryptInputStream(inputStream);
        }
        return headStream;
    }

    @Override
    public Object getContent() throws IOException {
        return connection.getContent();
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
        return connection.getContent(classes);
    }

    @Override
    public void setConnectTimeout(int timeout) {
        connection.setConnectTimeout(timeout);
    }

    @Override
    public int getConnectTimeout() {
        return connection.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(int timeout) {
        connection.setReadTimeout(timeout);
    }

    @Override
    public int getReadTimeout() {
        return connection.getReadTimeout();
    }

    @Override
    public URL getURL() {
        return connection.getURL();
    }

    @Override
    public int getContentLength() {
        return connection.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return connection.getContentLengthLong();
    }

    @Override
    public String getContentType() {
        return connection.getContentType();
    }

    @Override
    public String getContentEncoding() {
        return connection.getContentEncoding();
    }

    @Override
    public long getExpiration() {
        return connection.getExpiration();
    }

    @Override
    public long getDate() {
        return connection.getDate();
    }

    @Override
    public long getLastModified() {
        return connection.getLastModified();
    }

    @Override
    public String getHeaderField(String name) {
        return connection.getHeaderField(name);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return connection.getHeaderFields();
    }

    @Override
    public int getHeaderFieldInt(String name, int Default) {
        return connection.getHeaderFieldInt(name, Default);
    }

    @Override
    public long getHeaderFieldLong(String name, long Default) {
        return connection.getHeaderFieldLong(name, Default);
    }

    @Override
    public long getHeaderFieldDate(String name, long Default) {
        return connection.getHeaderFieldDate(name, Default);
    }

    @Override
    public String getHeaderFieldKey(int n) {
        return connection.getHeaderFieldKey(n);
    }

    @Override
    public String getHeaderField(int n) {
        return connection.getHeaderField(n);
    }

    @Override
    public Permission getPermission() throws IOException {
        return connection.getPermission();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return connection.getOutputStream();
    }

    @Override
    public String toString() {
        return connection.toString();
    }

    @Override
    public void setDoInput(boolean doinput) {
        connection.setDoInput(doinput);
    }

    @Override
    public boolean getDoInput() {
        return connection.getDoInput();
    }

    @Override
    public void setDoOutput(boolean dooutput) {
        connection.setDoOutput(dooutput);
    }

    @Override
    public boolean getDoOutput() {
        return connection.getDoOutput();
    }

    @Override
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        connection.setAllowUserInteraction(allowuserinteraction);
    }

    @Override
    public boolean getAllowUserInteraction() {
        return connection.getAllowUserInteraction();
    }

    @Override
    public void setUseCaches(boolean usecaches) {
        connection.setUseCaches(usecaches);
    }

    @Override
    public boolean getUseCaches() {
        return connection.getUseCaches();
    }

    @Override
    public void setIfModifiedSince(long ifmodifiedsince) {
        connection.setIfModifiedSince(ifmodifiedsince);
    }

    @Override
    public long getIfModifiedSince() {
        return connection.getIfModifiedSince();
    }

    @Override
    public boolean getDefaultUseCaches() {
        return connection.getDefaultUseCaches();
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
        connection.setDefaultUseCaches(defaultusecaches);
    }

    @Override
    public void setRequestProperty(String key, String value) {
        connection.setRequestProperty(key, value);
    }

    @Override
    public void addRequestProperty(String key, String value) {
        connection.addRequestProperty(key, value);
    }

    @Override
    public String getRequestProperty(String key) {
        return connection.getRequestProperty(key);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return connection.getRequestProperties();
    }

    @Override
    public URL getJarFileURL() {
        return connection.getJarFileURL();
    }

    @Override
    public String getEntryName() {
        return connection.getEntryName();
    }

    @Override
    public Manifest getManifest() throws IOException {
        return connection.getManifest();
    }

    @Override
    public JarEntry getJarEntry() throws IOException {
        return connection.getJarEntry();
    }

    @Override
    public Attributes getAttributes() throws IOException {
        return connection.getAttributes();
    }

    @Override
    public Attributes getMainAttributes() throws IOException {
        return connection.getMainAttributes();
    }

    @Override
    public Certificate[] getCertificates() throws IOException {
        return connection.getCertificates();
    }

    @Override
    public JarFile getJarFile() throws IOException {
        return connection.getJarFile();
    }
}
