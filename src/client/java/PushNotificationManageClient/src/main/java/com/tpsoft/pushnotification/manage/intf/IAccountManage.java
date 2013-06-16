package com.tpsoft.pushnotification.manage.intf;

import java.util.Map;
import java.util.Set;

import com.tpsoft.pushnotification.manage.exception.CheckAccountException;
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
	 *            账号信息(忽略disabled字段)
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
	 *            修改过的字段(不支持disabled)
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

	/**
	 * 获取账号列表
	 * 
	 * @param start
	 *            起始记录号
	 * @param records
	 *            最多记录数
	 * @return 账号ID/信息对照表(password字段始终为空值)
	 * @throws GetAccountException
	 */
	public Map<String, Account> list(int start, int records)
			throws GetAccountException;

	/**
	 * 获取账号信息
	 * 
	 * @param id
	 *            账号ID
	 * @return 账号信息(password字段始终为空值)
	 * @throws GetAccountException
	 */
	public Account get(String id) throws GetAccountException;

	/**
	 * 检查账号名称是否已经存在
	 * 
	 * @param name
	 *            账号名称
	 * @return 指定账号名称是否存在
	 * @throws CheckAccountException
	 */
	public boolean existsName(String name) throws CheckAccountException;

	/**
	 * 检查电话号码是否已经存在
	 * 
	 * @param phone
	 *            电话号码
	 * @return 指定电话号码是否存在
	 * @throws CheckAccountException
	 */
	public boolean existsPhone(String phone) throws CheckAccountException;

	/**
	 * 检查邮箱地址是否已经存在
	 * 
	 * @param email
	 *            邮箱地址
	 * @return 指定邮箱地址是否存在
	 * @throws CheckAccountException
	 */
	public boolean existsEmail(String email) throws CheckAccountException;
}
