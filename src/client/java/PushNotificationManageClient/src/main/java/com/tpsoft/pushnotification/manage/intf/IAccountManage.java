package com.tpsoft.pushnotification.manage.intf;

import java.util.Set;

import com.tpsoft.pushnotification.manage.exception.CreateAccountException;
import com.tpsoft.pushnotification.manage.exception.DeleteAccountException;
import com.tpsoft.pushnotification.manage.exception.GetAccountException;
import com.tpsoft.pushnotification.manage.exception.UpdateAccountException;
import com.tpsoft.pushnotification.manage.model.Account;

/**
 * 账号管理接口
 * 
 * @author joebin.don@gmail.com
 * @since 2013-04-10
 */
public interface IAccountManage {

	/**
	 * 新建账号
	 * 
	 * @param account
	 *            账号信息
	 * @return 新账号ID
	 * @throws CreateAccountException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public String create(Account account) throws CreateAccountException;

	/**
	 * 修改账号信息
	 * 
	 * @param id
	 *            账号ID
	 * @param account
	 *            账号信息
	 * @param updatedFields
	 *            修改过的字段(不支持enabled)
	 * @throws UpdateAccountException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public void update(String id, Account account, Set<String> updatedFields)
			throws UpdateAccountException;

	/**
	 * 禁用账号
	 * 
	 * @param id
	 *            账号ID
	 * @throws UpdateAccountException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public void disable(String id) throws UpdateAccountException;

	/**
	 * 启用账号
	 * 
	 * @param id
	 *            账号ID
	 * @throws UpdateAccountException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public void enable(String id) throws UpdateAccountException;

	/**
	 * 删除账号
	 * 
	 * @param id
	 *            账号ID
	 * @throws DeleteAccountException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public void delete(String id) throws DeleteAccountException;

	/**
	 * 获取账号数量
	 * 
	 * @throws GetAccountException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public int count() throws GetAccountException;
}
