package com.tpsoft.pushnotification.manage.response;

import java.util.List;

import com.tpsoft.pushnotification.manage.model.Account;

public class ListAccountsResult {

	public static class AccountWithId extends Account {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}

	private boolean success;
	private int count;
	private List<AccountWithId> list;
	private int errcode;
	private String errmsg;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<AccountWithId> getList() {
		return list;
	}

	public void setList(List<AccountWithId> list) {
		this.list = list;
	}

	public int getErrcode() {
		return errcode;
	}

	public void setErrcode(int errcode) {
		this.errcode = errcode;
	}

	public String getErrmsg() {
		return errmsg;
	}

	public void setErrmsg(String errmsg) {
		this.errmsg = errmsg;
	}

}
