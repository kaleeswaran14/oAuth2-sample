package com.ohadr.auth_flows.core.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import com.ohadr.auth_flows.core.AbstractAuthenticationAccountRepository;
import com.ohadr.auth_flows.interfaces.AuthenticationUser;
import com.ohadr.auth_flows.mocks.InMemoryAuthenticationUserImpl;
import com.ohadr.auth_flows.types.AccountState;
import com.ohadr.auth_flows.types.AuthenticationPolicy;


//@Repository
public class JdbcAuthenticationAccountRepositoryImpl extends AbstractAuthenticationAccountRepository
		implements InitializingBean
{
	private static Logger log = Logger.getLogger(JdbcAuthenticationAccountRepositoryImpl.class);

	private static final String TABLE_NAME = "auth_users";

	private static final String AUTHENTICATION_USER_FIELDS = "email, password, enabled, "
			+ "LOGIN_ATTEMPTS_COUNTER,"
			+ "LAST_PSWD_CHANGE_DATE";

	private static final String DEFAULT_USER_INSERT_STATEMENT = "insert into " + TABLE_NAME + "(" + AUTHENTICATION_USER_FIELDS
			+ ") values (?,?,?,?,?)";

	private static final String DEFAULT_USER_SELECT_STATEMENT = "select " + AUTHENTICATION_USER_FIELDS
			+ " from " + TABLE_NAME + " where EMAIL = ?";

	private static final String DEFAULT_USER_DELETE_STATEMENT = "delete from " + TABLE_NAME + " where EMAIL = ?";
	
	private static final String DEFAULT_UPDATE_PASSWORD_STATEMENT = "update " + TABLE_NAME + " set password = ? where EMAIL = ?";
	
	private static final String DEFAULT_UPDATE_ACTIVATED_STATEMENT = "update " + TABLE_NAME + " set enabled = ? where EMAIL = ?";

	
	@Autowired
	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	@Override
	public void afterPropertiesSet() throws Exception 
	{
		Assert.notNull(dataSource, "DataSource required");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public AccountState createAccount(String email, String encodedPassword
			//NOT IMPLEMENTED: String secretQuestion, String encodedAnswer
			)
	{
		int rowsUpdated = jdbcTemplate.update(DEFAULT_USER_INSERT_STATEMENT,
				new Object[] { email, encodedPassword, false, 0, new Date( System.currentTimeMillis()) },
				new int[] { Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.INTEGER, Types.DATE });

		if(rowsUpdated != 1)
		{
			throw new RuntimeException("could not insert new entry to DB");
		}
		
		return AccountState.OK;

	}

	@Override
	public AuthenticationUser getUser(String email) 
	{
		AuthenticationUser userFromDB = null;
		try
		{
			log.info("query: " + DEFAULT_USER_SELECT_STATEMENT + " " + email);
			userFromDB = jdbcTemplate.queryForObject(DEFAULT_USER_SELECT_STATEMENT, 
					new AuthenticationUserRowMapper(), email);
		}
		catch (EmptyResultDataAccessException e) 
		{
			log.info("no record was found for email=" + email);
//			throw new NoSuchElementException("No user with email: " + email);
		}


		return userFromDB;
	}

	@Override
	public void deleteOAuthAccount(String email)
	{
		int count = jdbcTemplate.update(DEFAULT_USER_DELETE_STATEMENT, email);
		if (count != 1)
		{
			throw new NoSuchElementException("No user with email: " + email);
		}
	}

	@Override
	public boolean changePassword(String username, String newEncodedPassword) 
	{
		AuthenticationUser user = getUser(username);
		if(user != null)
		{
			user.setPassword(newEncodedPassword);
			user.setPasswordLastChangeDate(new Date( System.currentTimeMillis() ));
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public AuthenticationPolicy getAuthenticationPolicy()
	{
		// TODO read from DB!!!
		AuthenticationPolicy policy = new AuthenticationPolicy();
		policy.setMaxPasswordEntryAttempts( 5 );
		policy.setPasswordMaxLength( 8 );
		policy.setRememberMeTokenValidityInDays( 30 );

		return policy;
	}

	
	
	private static class AuthenticationUserRowMapper implements RowMapper<AuthenticationUser>
	{
		public AuthenticationUser mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			AuthenticationUser user = new InMemoryAuthenticationUserImpl();
			user.setEmail(rs.getString(1));
			user.setPassword(rs.getString(2));
			user.setActivated(rs.getBoolean(3));
			user.setLoginAttemptsCounter(rs.getInt(4));
			user.setPasswordLastChangeDate(rs.getDate(5));
			
			return user;
		}
	}


	@Override
	public void setEnabled(String email) 
	{
		int count = jdbcTemplate.update(DEFAULT_UPDATE_ACTIVATED_STATEMENT, true, email);
		if (count != 1)
		{
			throw new NoSuchElementException("No user with email: " + email);
		}
	}

}
