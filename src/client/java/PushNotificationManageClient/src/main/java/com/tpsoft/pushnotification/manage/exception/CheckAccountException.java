package com.tpsoft.pushnotification.manage.exception;

/**
 * 检查账号信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-06-15
 */
public class CheckAccountException extends Exception {

	private static final long serialVersionUID = 1L;

	public CheckAccountException() {
		super();
	}

	public CheckAccountException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public CheckAccountException(String arg0) {
		super(arg0);
	}

	public CheckAccountException(Throwable arg0) {
		super(arg0);
	}

}
