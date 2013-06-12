package com.tpsoft.pushnotification.manage.exception;

/**
 * 获取应用接入信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-04-10
 */
public class GetApplicationException extends Exception {

	private static final long serialVersionUID = 1L;

	public GetApplicationException() {
		super();
	}

	public GetApplicationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public GetApplicationException(String arg0) {
		super(arg0);
	}

	public GetApplicationException(Throwable arg0) {
		super(arg0);
	}

}
