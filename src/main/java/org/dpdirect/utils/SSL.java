package org.dpdirect.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSL {

   public static TrustManager[] createTrustManagers() throws KeyStoreException,
                                                     NoSuchAlgorithmException,
                                                     CertificateException,
                                                     IOException {
      TrustManager[] trustManagers = null;
      Properties props = FileUtils.loadPropertiesFromBundle("org.dpdirect.datapower.utils.ssl");

      // create Inputstream to truststore file
      InputStream inputStream = new FileInputStream(props.getProperty("trust.store.filename"));
      // create keystore object, load it with truststorefile data
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(inputStream, props.getProperty("trust.store.password").toCharArray());
      // create trustmanager factory and load the keystore object in it
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);
      
      trustManagers = trustManagerFactory.getTrustManagers();
      return trustManagers;
   }

   public static SSLSocketFactory initSSLcontext(TrustManager[] trustManagers) throws NoSuchAlgorithmException,
                                                                              KeyManagementException {
      SSLSocketFactory sslSocketfactory = null;
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, trustManagers, null);
      sslSocketfactory = context.getSocketFactory();
      return sslSocketfactory;
   }

   public static TrustManager[] createAllTrustingManagers() throws KeyStoreException,
                                                           NoSuchAlgorithmException,
                                                           CertificateException,
                                                           IOException,
                                                           KeyManagementException {
      TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
         public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
         }

         public void checkClientTrusted(X509Certificate[] certs,
                                        String authType) {
         }

         public void checkServerTrusted(X509Certificate[] certs,
                                        String authType) {
         }
      } };
      HostnameVerifier allHostsValid = new HostnameVerifier() {
         public boolean verify(String hostname,
                               SSLSession session) {
            return true;
         }
      };
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
      return trustAllCerts;
   }

   public static SSLSocketFactory initTrustAllSSLcontext(TrustManager[] trustAllCerts) throws NoSuchAlgorithmException,
                                                                                      KeyManagementException {
      // Install the all-trusting trust manager
      SSLSocketFactory sslSocketfactory = null;
      final SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      sslSocketfactory = sc.getSocketFactory();
      return sslSocketfactory;
   }

}
