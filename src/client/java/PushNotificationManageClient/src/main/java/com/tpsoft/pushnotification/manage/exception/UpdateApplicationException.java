package com.tpsoft.pushnotification.manage.exception;

/**
 * 修改应用接入信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-06-10
 */
public class UpdateApplicationException extends Exception {

	private static final long serialVersionUID = 1L;

	public UpdateApplicationException() {
		super();
	}

	public UpdateApplicationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public UpdateApplicationException(String arg0) {
		super(arg0);
	}

	public UpdateApplicationException(Throwable arg0) {
		super(arg0);
	}

}
