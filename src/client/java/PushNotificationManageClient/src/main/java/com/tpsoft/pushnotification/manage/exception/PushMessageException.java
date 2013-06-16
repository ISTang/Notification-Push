package com.tpsoft.pushnotification.manage.exception;

/**
 * 检查应用接入信息异常
 * 
 * @author jeobin.don@gmail.com
 * @since 2013-06-15
 */
public class PushMessageException extends Exception {

	private static final long serialVersionUID = 1L;

	public PushMessageException() {
		super();
	}

	public PushMessageException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public PushMessageException(String arg0) {
		super(arg0);
	}

	public PushMessageException(Throwable arg0) {
		super(arg0);
	}

}
