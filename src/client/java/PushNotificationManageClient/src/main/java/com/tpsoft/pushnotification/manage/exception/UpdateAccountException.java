package com.tpsoft.pushnotification.manage.exception;

/**
 * 修改账号信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-04-10
 */
public class UpdateAccountException extends Exception {

	private static final long serialVersionUID = 1L;

	public UpdateAccountException() {
		super();
	}

	public UpdateAccountException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public UpdateAccountException(String arg0) {
		super(arg0);
	}

	public UpdateAccountException(Throwable arg0) {
		super(arg0);
	}

}
