package com.tpsoft.pushnotification.manage.model;

/**
 * 账号信息
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-10
 */
public class Account {

	private String name; // 帐号名称: 可以是用户姓名，1-20个字符，唯一
	private String phone; // 电话号码: 手机号码，可以带区号，可选，可用做用户名，唯一
	private String email; // 邮箱地址: 可选，可用着用户名，唯一
	private String password = "666666"; // 密码: 可选，默认为6个6
	private boolean disabled = false; // 是否禁用: 是否禁止客户端使用该账号(true/false[默认])

	public Account() {
	}

	public Account(String name, String phone, String email, String password) {
		this.name = name;
		this.phone = phone;
		this.email = email;
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

}
