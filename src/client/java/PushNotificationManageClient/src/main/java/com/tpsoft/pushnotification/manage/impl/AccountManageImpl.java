package com.tpsoft.pushnotification.manage.impl;

import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tpsoft.pushnotification.manage.config.ServiceConfig;
import com.tpsoft.pushnotification.manage.exception.CheckAccountException;
import com.tpsoft.pushnotification.manage.exception.CreateAccountException;
import com.tpsoft.pushnotification.manage.exception.DeleteAccountException;
import com.tpsoft.pushnotification.manage.exception.GetAccountException;
import com.tpsoft.pushnotification.manage.exception.UpdateAccountException;
import com.tpsoft.pushnotification.manage.intf.IAccountManage;
import com.tpsoft.pushnotification.manage.model.Account;

public class AccountManageImpl implements IAccountManage {

	private ServiceConfig serviceConfig;

	private Gson gson = new GsonBuilder().create();

	public AccountManageImpl() {

	}

	public AccountManageImpl(ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public ServiceConfig getServiceConfig() {
		return serviceConfig;
	}

	public void setServiceConfig(ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public String create(Account account) throws CreateAccountException {
		// TODO Auto-generated method stub
		return null;
	}

	public void update(String id, Account account, Set<String> updatedFields)
			throws UpdateAccountException {
		// TODO Auto-generated method stub

	}

	public void disable(String id) throws UpdateAccountException {
		// TODO Auto-generated method stub

	}

	public void enable(String id) throws UpdateAccountException {
		// TODO Auto-generated method stub

	}

	public void delete(String id) throws DeleteAccountException {
		// TODO Auto-generated method stub

	}

	public int count() throws GetAccountException {
		// TODO Auto-generated method stub
		return 0;
	}

	public Map<String, Account> list(int start, int records)
			throws GetAccountException {
		// TODO Auto-generated method stub
		return null;
	}

	public Account get(String id) throws GetAccountException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean existsName(String name) throws CheckAccountException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean existsPhone(String phone) throws CheckAccountException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean existsEmail(String email) throws CheckAccountException {
		// TODO Auto-generated method stub
		return false;
	}

}
