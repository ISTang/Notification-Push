package com.tpsoft.pushnotification.manage.exception;

/**
 * 获取账号信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-06-10
 */
public class GetAccountException extends Exception {

	private static final long serialVersionUID = 1L;

	public GetAccountException() {
		super();
	}

	public GetAccountException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public GetAccountException(String arg0) {
		super(arg0);
	}

	public GetAccountException(Throwable arg0) {
		super(arg0);
	}

}
