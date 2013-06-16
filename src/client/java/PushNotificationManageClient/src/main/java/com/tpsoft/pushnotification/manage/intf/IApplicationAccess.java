package com.tpsoft.pushnotification.manage.intf;

import java.util.Map;
import java.util.Set;

import com.tpsoft.pushnotification.manage.exception.CheckApplicationException;
import com.tpsoft.pushnotification.manage.exception.GetApplicationException;
import com.tpsoft.pushnotification.manage.exception.RegisterApplicationException;
import com.tpsoft.pushnotification.manage.exception.UnregisterApplicationException;
import com.tpsoft.pushnotification.manage.exception.UpdateApplicationException;
import com.tpsoft.pushnotification.manage.model.Application;

/**
 * 应用接入接口
 * 
 * @author joebin.don@gmail.com
 * @since 2013-04-10
 */
public interface IApplicationAccess {

	/**
	 * 注册新应用
	 * 
	 * @param name
	 *            应用名称: 1-40个字符
	 * @param params
	 *            接入参数: 可选
	 * @return 客户端参数
	 * @throws RegisterApplicationException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public Application.ClientParams register(String name,
			Application.AccessParams params)
			throws RegisterApplicationException;

	/**
	 * 修改应用接入信息
	 * 
	 * @param id
	 *            应用ID
	 * @param name
	 *            应用名称
	 * @param newName
	 *            新名称(空值表示名称不变)
	 * @param params
	 *            接入参数
	 * @param updatedFields
	 *            修改过的字段
	 * @throws UpdateApplicationException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public void update(String id, String name, String newName,
			Application.AccessParams params, Set<String> updatedFields)
			throws UpdateApplicationException;

	/**
	 * 注销应用
	 * 
	 * @param id
	 *            应用ID
	 * @throws UnregisterApplicationException
	 *             错误消息格式: "#<错误代码>:<失败原因>"
	 */
	public void unregister(String id) throws UnregisterApplicationException;

	/**
	 * 获取已接入应用
	 * 
	 * @return 应用ID/名称对照表
	 * @throws GetApplicationException
	 */
	public Map<String, String> listAll() throws GetApplicationException;

	/**
	 * 获取应用接入信息
	 * 
	 * @param id
	 *            应用ID
	 * @return 应用接入信息
	 * @throws GetApplicationException
	 */
	public Application get(String id) throws GetApplicationException;

	/**
	 * 检查应用名称是否已经存在
	 * 
	 * @param name
	 *            应用名称
	 * @return 指定应用名称是否存在
	 * @throws CheckApplicationException
	 */
	public boolean existsName(String name) throws CheckApplicationException;
}
