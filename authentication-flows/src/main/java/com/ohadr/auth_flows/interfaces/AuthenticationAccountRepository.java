package com.ohadr.auth_flows.interfaces;

import java.util.Date;

import com.ohadr.auth_flows.types.AccountState;
import com.ohadr.auth_flows.types.AuthenticationPolicy;


public interface AuthenticationAccountRepository 
{
	AccountState createAccount(String email, 
			String encodedPassword
//			String secretQuestion, 		NOT IMPLEMENTED
//			String encodedAnswer		NOT IMPLEMENTED
			);

	/**
	 * 
	 * @param email
	 * @return null if username was not found
	 */
	AuthenticationUser getUser(String email);
	
	void deleteAccount(String email);

	void setEnabled(String email);
	void setDisabled(String email);
	boolean isActivated(String email);

	AccountState isAccountLocked(String email);

	boolean changePassword(String username, String newEncodedPassword);
	
	/**
	 * 
	 * @param email
	 */
	void incrementAttemptsCounter(String email); 
	void resetAttemptsCounter(String email);

	/**
	 * sets a password for a given user
	 * @param email - the user's email
	 * @param newPassword - new password to set
	 * @return 
	 */
	boolean setPassword(String email, String newPassword); 
	
	String getEncodedPassword(String username);
	Date getPasswordLastChangeDate(String email);

	AuthenticationPolicy getAuthenticationPolicy();

	/**
	 * NOT IMPLEMENTED
	 * 
	 * 
	String getEncodedSecretAnswer(String email);
	*/


}
