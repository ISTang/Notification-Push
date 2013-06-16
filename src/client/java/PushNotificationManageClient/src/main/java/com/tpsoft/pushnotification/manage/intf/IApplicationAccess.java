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
 * Ӧ�ý���ӿ�
 * 
 * @author joebin.don@gmail.com
 * @since 2013-04-10
 */
public interface IApplicationAccess {

	/**
	 * ע����Ӧ��
	 * 
	 * @param name
	 *            Ӧ������: 1-40���ַ�
	 * @param params
	 *            �������: ��ѡ
	 * @return �ͻ��˲���
	 * @throws RegisterApplicationException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public Application.ClientParams register(String name,
			Application.AccessParams params)
			throws RegisterApplicationException;

	/**
	 * �޸�Ӧ�ý�����Ϣ
	 * 
	 * @param id
	 *            Ӧ��ID
	 * @param name
	 *            Ӧ������
	 * @param newName
	 *            ������(��ֵ��ʾ���Ʋ���)
	 * @param params
	 *            �������
	 * @param updatedFields
	 *            �޸Ĺ����ֶ�
	 * @throws UpdateApplicationException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public void update(String id, String name, String newName,
			Application.AccessParams params, Set<String> updatedFields)
			throws UpdateApplicationException;

	/**
	 * ע��Ӧ��
	 * 
	 * @param id
	 *            Ӧ��ID
	 * @throws UnregisterApplicationException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public void unregister(String id) throws UnregisterApplicationException;

	/**
	 * ��ȡ�ѽ���Ӧ��
	 * 
	 * @return Ӧ��ID/���ƶ��ձ�
	 * @throws GetApplicationException
	 */
	public Map<String, String> listAll() throws GetApplicationException;

	/**
	 * ��ȡӦ�ý�����Ϣ
	 * 
	 * @param id
	 *            Ӧ��ID
	 * @return Ӧ�ý�����Ϣ
	 * @throws GetApplicationException
	 */
	public Application get(String id) throws GetApplicationException;

	/**
	 * ���Ӧ�������Ƿ��Ѿ�����
	 * 
	 * @param name
	 *            Ӧ������
	 * @return ָ��Ӧ�������Ƿ����
	 * @throws CheckApplicationException
	 */
	public boolean existsName(String name) throws CheckApplicationException;
}
