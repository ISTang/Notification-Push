package com.tpsoft.pushnotification.manage.exception;

/**
 * 注册新应用接入信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-06-10
 */
public class RegisterApplicationException extends Exception {

	private static final long serialVersionUID = 1L;

	public RegisterApplicationException() {
		super();
	}

	public RegisterApplicationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public RegisterApplicationException(String arg0) {
		super(arg0);
	}

	public RegisterApplicationException(Throwable arg0) {
		super(arg0);
	}

}
