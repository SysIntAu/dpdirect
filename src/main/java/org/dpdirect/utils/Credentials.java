package org.dpdirect.utils;

/**
 * A general representation of userName and password credentials.
 *
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
 
public class Credentials {

   /**
    * The username.
    */
   private String userName = null;

   /**
    * The password.
    */
   private char[] password = null;

   /**
    * Constructs a new <code>Credentials</code> class.
    */
   public Credentials() {
   }

   /**
    * Constructs a new <code>Credentials</code> class.
    * 
    * @param userName the userName.
    * @param password the password.
    */
   public Credentials(String userName, char[] password) {
      this.setUserName(userName);
      this.setPassword(password);
   }

   /**
    * @return the userName
    */
   public String getUserName() {
      return userName;
   }

   /**
    * @param userName the userName to set
    */
   public void setUserName(String userName) {
      this.userName = userName;
   }

   /**
    * @return the password
    */
   public char[] getPassword() {
      return password;
   }

   /**
    * @param password the password to set
    */
   public void setPassword(char[] password) {
      this.password = password;
   }

}
