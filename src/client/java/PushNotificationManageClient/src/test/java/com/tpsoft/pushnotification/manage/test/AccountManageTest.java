package com.tpsoft.pushnotification.manage.test;

import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.tpsoft.pushnotification.manage.config.ServiceConfig;
import com.tpsoft.pushnotification.manage.exception.CheckAccountException;
import com.tpsoft.pushnotification.manage.exception.CreateAccountException;
import com.tpsoft.pushnotification.manage.exception.DeleteAccountException;
import com.tpsoft.pushnotification.manage.exception.GetAccountException;
import com.tpsoft.pushnotification.manage.exception.UpdateAccountException;
import com.tpsoft.pushnotification.manage.impl.AccountManageImpl;
import com.tpsoft.pushnotification.manage.intf.IAccountManage;
import com.tpsoft.pushnotification.manage.model.Account;

public class AccountManageTest {

	private ServiceConfig serviceConfig = new ServiceConfig();
	private IAccountManage accountManage = new AccountManageImpl(serviceConfig);

	@Before
	public void setUp() throws Exception {
		serviceConfig.setServer("118.244.9.191");
		serviceConfig.setPort(4567);
	}

	@Test
	public void testCreate() {
		Account account = new Account("≤‚ ‘’À∫≈", "15881449872", "test@gmail.com",
				"12345678");
		try {
			String accountId = accountManage.create(account);
			System.out.println(String.format("New account %s created.",
					accountId));
		} catch (CreateAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testUpdate() {
		String accountId = "A1A130C4-6D60-4BD2-A396-B3F4708A8708";
		Account account = new Account("≤‚ ‘’À∫≈", "13808188051", "test2@gmail.com",
				"87654321");
		try {
			accountManage.update(accountId, account, new HashSet<String>() {
				{
					add("email");
					add("password");
				}
			});
			System.out.println(String.format("Account %s updated.", accountId));
		} catch (UpdateAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testDisable() {
		String accountId = "A1A130C4-6D60-4BD2-A396-B3F4708A8708";
		try {
			accountManage.disable(accountId);
			System.out
					.println(String.format("Account %s disabled.", accountId));
		} catch (UpdateAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testEnable() {
		String accountId = "A1A130C4-6D60-4BD2-A396-B3F4708A8708";
		try {
			accountManage.enable(accountId);
			System.out.println(String.format("Account %s enabled.", accountId));
		} catch (UpdateAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testDelete() {
		String accountId = "080AE452-7C76-40D9-BE16-75641F412E5F";
		try {
			accountManage.delete(accountId);
			System.out.println(String.format("Account %s deleted.", accountId));
		} catch (DeleteAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testCount() {
		try {
			int accountCount = accountManage.count();
			System.out.println(String
					.format("Total %d accounts.", accountCount));
		} catch (GetAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testList() {
		try {
			Map<String, Account> accounts = accountManage.list(0, 10);
			System.out.println(String.format("Total %d accounts:",
					accounts.size()));
			for (String accountId : accounts.keySet()) {
				System.out.println(String.format("%s: %s, %s, %s", accountId,
						accounts.get(accountId).getName(),
						accounts.get(accountId).getPhone(),
						accounts.get(accountId).getEmail()));
			}
		} catch (GetAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testGet() {
		String accountId = "080AE452-7C76-40D9-BE16-75641F412E5F";
		try {
			Account account = accountManage.get(accountId);
			System.out.println(String.format("%s: %s, %s, %s", accountId,
					account.getName(), account.getPhone(), account.getEmail()));
		} catch (GetAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testExistsName() {
		String accountName = "Ã∆ΩÃ±¯";
		try {
			boolean exists = accountManage.existsName(accountName);
			System.out.println(String.format("Account name %s %s.",
					accountName, exists ? "exists" : "not exists"));
		} catch (CheckAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testExistsPhone() {
		String accountPhone = "13808188051";
		try {
			boolean exists = accountManage.existsPhone(accountPhone);
			System.out.println(String.format("Account phone %s %s.",
					accountPhone, exists ? "exists" : "not exists"));
		} catch (CheckAccountException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testExistsEmail() {
		String accountEmail = "joebin.don@gmail.com";
		try {
			boolean exists = accountManage.existsEmail(accountEmail);
			System.out.println(String.format("Account email %s %s.",
					accountEmail, exists ? "exists" : "not exists"));
		} catch (CheckAccountException e) {
			System.err.println(e.getMessage());
		}
	}

}
