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
 * �˺Ź���ӿ�
 * 
 * @author joebin.don@gmail.com
 * @since 2013-04-10
 */
public interface IAccountManage {

	/**
	 * �½��˺�
	 * 
	 * @param account
	 *            �˺���Ϣ(����disabled�ֶ�)
	 * @return ���˺�ID
	 * @throws CreateAccountException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public String create(Account account) throws CreateAccountException;

	/**
	 * �޸��˺���Ϣ
	 * 
	 * @param id
	 *            �˺�ID
	 * @param account
	 *            �˺���Ϣ
	 * @param updatedFields
	 *            �޸Ĺ����ֶ�(��֧��disabled)
	 * @throws UpdateAccountException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public void update(String id, Account account, Set<String> updatedFields)
			throws UpdateAccountException;

	/**
	 * �����˺�
	 * 
	 * @param id
	 *            �˺�ID
	 * @throws UpdateAccountException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public void disable(String id) throws UpdateAccountException;

	/**
	 * �����˺�
	 * 
	 * @param id
	 *            �˺�ID
	 * @throws UpdateAccountException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public void enable(String id) throws UpdateAccountException;

	/**
	 * ɾ���˺�
	 * 
	 * @param id
	 *            �˺�ID
	 * @throws DeleteAccountException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public void delete(String id) throws DeleteAccountException;

	/**
	 * ��ȡ�˺�����
	 * 
	 * @throws GetAccountException
	 *             ������Ϣ��ʽ: "#<�������>:<ʧ��ԭ��>"
	 */
	public int count() throws GetAccountException;

	/**
	 * ��ȡ�˺��б�
	 * 
	 * @param start
	 *            ��ʼ��¼��
	 * @param records
	 *            ����¼��
	 * @return �˺�ID/��Ϣ���ձ�(password�ֶ�ʼ��Ϊ��ֵ)
	 * @throws GetAccountException
	 */
	public Map<String, Account> list(int start, int records)
			throws GetAccountException;

	/**
	 * ��ȡ�˺���Ϣ
	 * 
	 * @param id
	 *            �˺�ID
	 * @return �˺���Ϣ(password�ֶ�ʼ��Ϊ��ֵ)
	 * @throws GetAccountException
	 */
	public Account get(String id) throws GetAccountException;

	/**
	 * ����˺������Ƿ��Ѿ�����
	 * 
	 * @param name
	 *            �˺�����
	 * @return ָ���˺������Ƿ����
	 * @throws CheckAccountException
	 */
	public boolean existsName(String name) throws CheckAccountException;

	/**
	 * ���绰�����Ƿ��Ѿ�����
	 * 
	 * @param phone
	 *            �绰����
	 * @return ָ���绰�����Ƿ����
	 * @throws CheckAccountException
	 */
	public boolean existsPhone(String phone) throws CheckAccountException;

	/**
	 * ��������ַ�Ƿ��Ѿ�����
	 * 
	 * @param email
	 *            �����ַ
	 * @return ָ�������ַ�Ƿ����
	 * @throws CheckAccountException
	 */
	public boolean existsEmail(String email) throws CheckAccountException;
}
