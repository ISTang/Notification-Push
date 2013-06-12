package com.tpsoft.pushnotification.manage.exception;

/**
 * 注销应用异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-04-10
 */
public class UnregisterApplicationException extends Exception {

	private static final long serialVersionUID = 1L;

	public UnregisterApplicationException() {
		super();
	}

	public UnregisterApplicationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public UnregisterApplicationException(String arg0) {
		super(arg0);
	}

	public UnregisterApplicationException(Throwable arg0) {
		super(arg0);
	}

}
