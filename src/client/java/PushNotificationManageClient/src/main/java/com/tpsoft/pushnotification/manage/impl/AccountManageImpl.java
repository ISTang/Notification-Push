package com.tpsoft.pushnotification.manage.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

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
import com.tpsoft.pushnotification.manage.response.CheckAccountEmailResult;
import com.tpsoft.pushnotification.manage.response.CheckAccountNameResult;
import com.tpsoft.pushnotification.manage.response.CheckAccountPhoneResult;
import com.tpsoft.pushnotification.manage.response.CountAccountsResult;
import com.tpsoft.pushnotification.manage.response.CreateAccountResult;
import com.tpsoft.pushnotification.manage.response.DeleteAccountResult;
import com.tpsoft.pushnotification.manage.response.DisableAccountResult;
import com.tpsoft.pushnotification.manage.response.EnableAccountResult;
import com.tpsoft.pushnotification.manage.response.GetAccountResult;
import com.tpsoft.pushnotification.manage.response.ListAccountsResult;
import com.tpsoft.pushnotification.manage.response.ListAccountsResult.AccountWithId;
import com.tpsoft.pushnotification.manage.response.UpdateAccountResult;

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
		Map<String, Object> map = new HashMap<String, Object>();
		if (account.getPhone() != null) {
			map.put("phone", account.getPhone());
		}
		if (account.getEmail() != null) {
			map.put("email", account.getEmail());
		}
		if (account.getPassword() != null) {
			map.put("password", account.getPassword());
		}

		ClientRequest req = new ClientRequest(
				serviceConfig.getCreateAccountEndpoint());
		req.pathParameter("name", account.getName()).body(
				ServiceConfig.CONTENT_TYPE, gson.toJson(map));

		ClientResponse<CreateAccountResult> res;
		try {
			res = req.post(CreateAccountResult.class);
		} catch (Exception e) {
			throw new CreateAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		CreateAccountResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new CreateAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		return result.getId();
	}

	public void update(String id, Account account, Set<String> updatedFields)
			throws UpdateAccountException {
		Map<String, Object> map = new HashMap<String, Object>();
		if (updatedFields.contains("name")) {
			map.put("new_name", account.getName());
		}
		if (updatedFields.contains("phone")) {
			map.put("new_phone", account.getPhone());
		}
		if (updatedFields.contains("email")) {
			map.put("new_email", account.getEmail());
		}
		if (updatedFields.contains("password")) {
			map.put("new_password", account.getPassword());
		}

		ClientRequest req = new ClientRequest(
				serviceConfig.getUpdateAccountEndpoint());
		req.pathParameter("id", id).body(ServiceConfig.CONTENT_TYPE,
				gson.toJson(map));

		ClientResponse<UpdateAccountResult> res;
		try {
			res = req.put(UpdateAccountResult.class);
		} catch (Exception e) {
			throw new UpdateAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		UpdateAccountResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new UpdateAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public void disable(String id) throws UpdateAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getDisableAccountEndpoint());
		req.pathParameter("id", id).body(ServiceConfig.CONTENT_TYPE, "{}");

		ClientResponse<DisableAccountResult> res;
		try {
			res = req.put(DisableAccountResult.class);
		} catch (Exception e) {
			throw new UpdateAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		DisableAccountResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new UpdateAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public void enable(String id) throws UpdateAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getEnableAccountEndpoint());
		req.pathParameter("id", id).body(ServiceConfig.CONTENT_TYPE, "{}");

		ClientResponse<EnableAccountResult> res;
		try {
			res = req.put(EnableAccountResult.class);
		} catch (Exception e) {
			throw new UpdateAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		EnableAccountResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new UpdateAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public void delete(String id) throws DeleteAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getDeleteAccountEndpoint());
		req.pathParameter("id", id);

		ClientResponse<DeleteAccountResult> res;
		try {
			res = req.delete(DeleteAccountResult.class);
		} catch (Exception e) {
			throw new DeleteAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		DeleteAccountResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new DeleteAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public int count() throws GetAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getCountAccountsEndpoint());

		ClientResponse<CountAccountsResult> res;
		try {
			res = req.get(CountAccountsResult.class);
		} catch (Exception e) {
			throw new GetAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		CountAccountsResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new GetAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		return result.getCount();
	}

	public Map<String, Account> list(int start, int records)
			throws GetAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getListAccountsEndpoint());
		req.queryParameter("start", start).queryParameter("records", records);

		ClientResponse<ListAccountsResult> res;
		try {
			res = req.get(ListAccountsResult.class);
		} catch (Exception e) {
			throw new GetAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		ListAccountsResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new GetAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}

		Map<String, Account> map = new HashMap<String, Account>();
		for (AccountWithId accountWithId : result.getList()) {
			map.put(accountWithId.getId(), new Account(accountWithId.getName(),
					accountWithId.getPhone(), accountWithId.getEmail(),
					accountWithId.getPassword()));
		}
		return map;
	}

	public Account get(String id) throws GetAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getGetAccountEndpoint());
		req.pathParameter("id", id);

		ClientResponse<GetAccountResult> res;
		try {
			res = req.get(GetAccountResult.class);
		} catch (Exception e) {
			throw new GetAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		GetAccountResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new GetAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}

		return new Account(result.getName(), result.getPhone(),
				result.getEmail(), result.getPassword());
	}

	public boolean existsName(String name) throws CheckAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getCheckAccountNameEndpoint());
		req.pathParameter("name", name);

		ClientResponse<CheckAccountNameResult> res;
		try {
			res = req.get(CheckAccountNameResult.class);
		} catch (Exception e) {
			throw new CheckAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		CheckAccountNameResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new CheckAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		return result.isExists();
	}

	public boolean existsPhone(String phone) throws CheckAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getCheckAccountPhoneEndpoint());
		req.pathParameter("phone", phone);

		ClientResponse<CheckAccountPhoneResult> res;
		try {
			res = req.get(CheckAccountPhoneResult.class);
		} catch (Exception e) {
			throw new CheckAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		CheckAccountPhoneResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new CheckAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		return result.isExists();
	}

	public boolean existsEmail(String email) throws CheckAccountException {
		ClientRequest req = new ClientRequest(
				serviceConfig.geCheckAccountEmailEndpoint());
		req.pathParameter("email", email);

		ClientResponse<CheckAccountEmailResult> res;
		try {
			res = req.get(CheckAccountEmailResult.class);
		} catch (Exception e) {
			throw new CheckAccountException(String.format("#-1:%s",
					e.getMessage()));
		}
		CheckAccountEmailResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new CheckAccountException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		return result.isExists();
	}

}
