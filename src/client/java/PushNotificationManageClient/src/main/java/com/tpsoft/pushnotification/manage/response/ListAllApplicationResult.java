package com.tpsoft.pushnotification.manage.response;

import java.util.List;

import com.tpsoft.pushnotification.manage.model.Application;

public class ListAllApplicationResult extends Application.ClientParams {
	public static class ApplicationIdAndName {
		private String id;
		private String name;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private boolean success;
	private int count;
	private List<ApplicationIdAndName> list;
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

	public List<ApplicationIdAndName> getList() {
		return list;
	}

	public void setList(List<ApplicationIdAndName> list) {
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
