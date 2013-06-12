package com.tpsoft.pushnotification.manage.exception;

/**
 * 检查应用接入信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-04-10
 */
public class CheckApplicationException extends Exception {

	private static final long serialVersionUID = 1L;

	public CheckApplicationException() {
		super();
	}

	public CheckApplicationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public CheckApplicationException(String arg0) {
		super(arg0);
	}

	public CheckApplicationException(Throwable arg0) {
		super(arg0);
	}

}
