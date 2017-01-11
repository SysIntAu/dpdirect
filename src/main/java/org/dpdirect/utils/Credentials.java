package org.dpdirect.utils;

/**
 * A general representation of userName and password credentials.
 * 
 * @author Tim Goodwill
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
