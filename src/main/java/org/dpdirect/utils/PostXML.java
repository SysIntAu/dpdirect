package org.dpdirect.utils;

/**
 * Copyright 2016 Tim Goodwill
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;

import org.apache.xerces.impl.dv.util.Base64;
import org.dpdirect.schema.DocumentHelper;

public class PostXML {

   public static String postSecure(String host,
                                   String port,
                                   String path,
                                   String stringWriter,
                                   Credentials credentials) {

      String hostString = host;
      if (port != null) {
         hostString = hostString + ":" + port;
      }

      TrustManager[] trustManagers;
      String response = null;
      try {
         trustManagers = SSL.createTrustManagers();

         URL url = new URL("https://" + hostString + path);
         URLConnection connection = url.openConnection();
         if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(SSL.initSSLcontext(trustManagers));
         }
         response = post(connection, stringWriter, credentials);

      }
      catch (IOException ex) {
         try {
            response = DocumentHelper.stringAsXML(ex.getLocalizedMessage(), "HttpErrorResponse");
         }
         catch (Exception ex1) {
            Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex1);
         }
      }
      catch (KeyStoreException ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (CertificateException ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (KeyManagementException ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (java.lang.Exception ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      return response;
   }

   public static String postTrusting(String host,
                                     String port,
                                     String path,
                                     String stringWriter,
                                     Credentials credentials) {

      String hostString = host;
      if (port != null) {
         hostString = hostString + ":" + port;
      }

      TrustManager[] trustManagers;
      String response = null;
      try {
         trustManagers = SSL.createAllTrustingManagers();

         URL url = new URL("https://" + hostString + path);
         URLConnection connection = url.openConnection();
         if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(SSL.initTrustAllSSLcontext(trustManagers));
         }
         response = post(connection, stringWriter, credentials);
      }
      catch (IOException ex) {
         try {
            response = DocumentHelper.stringAsXML(ex.getLocalizedMessage(), "HttpErrorResponse");
         }
         catch (Exception ex1) {
            Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex1);
         }
      }
      catch (KeyStoreException ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (CertificateException ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (KeyManagementException ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (java.lang.Exception ex) {
         Logger.getLogger(PostXML.class.getName()).log(Level.SEVERE, null, ex);
      }
      return response;
   }

   public static String post(URLConnection connection,
                             String stringWriter,
                             Credentials credentials) throws Exception {

      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setRequestProperty("Authorization",
                                    "Basic "
                                          + Base64.encode((credentials.getUserName() + ":" + new String(
                                                credentials.getPassword())).getBytes()));
      OutputStreamWriter postData = new OutputStreamWriter(connection.getOutputStream());
      postData.write(stringWriter);
      postData.flush();
      postData.close();

      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String response = "";
      String line = "";
      while ((line = in.readLine()) != null)
         response = response + line;
      in.close();

      return response;
   }

}
