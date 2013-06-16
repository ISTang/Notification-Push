package com.tpsoft.pushnotification.manage.test;

import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.tpsoft.pushnotification.manage.config.ServiceConfig;
import com.tpsoft.pushnotification.manage.exception.CheckApplicationException;
import com.tpsoft.pushnotification.manage.exception.GetApplicationException;
import com.tpsoft.pushnotification.manage.exception.RegisterApplicationException;
import com.tpsoft.pushnotification.manage.exception.UnregisterApplicationException;
import com.tpsoft.pushnotification.manage.exception.UpdateApplicationException;
import com.tpsoft.pushnotification.manage.impl.ApplicationAccessImpl;
import com.tpsoft.pushnotification.manage.intf.IApplicationAccess;
import com.tpsoft.pushnotification.manage.model.Application;

public class ApplicationAccessTest {

	private ServiceConfig serviceConfig = new ServiceConfig();
	private IApplicationAccess applicationAccess = new ApplicationAccessImpl(
			serviceConfig);

	@Before
	public void setUp() throws Exception {
		serviceConfig.setServer("118.244.9.191");
		serviceConfig.setPort(4567);
	}

	@Test
	public void testRegister() {
		Application.AccessParams params = new Application.AccessParams(true,
				true, true, true, false);
		try {
			Application.ClientParams clientParams = applicationAccess.register(
					"≤‚ ‘APP", params);
			System.out.println(String.format("%s:%s, %s, %s",
					clientParams.getId(), clientParams.getPassword(),
					clientParams.getPublicKey(), clientParams.getPrivateKey()));
		} catch (RegisterApplicationException e) {
			System.err.println(e.getMessage());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testUpdate() {
		String appId = "63B3C952-F3FE-44F4-84AB-E483A4BD9CF6";
		String appName = "≤‚ ‘APP3";
		Application.AccessParams params = new Application.AccessParams(false,
				false, false, true, false);
		try {
			applicationAccess.update(appId, appName, null, params,
					new HashSet<String>() {
						{
							//add("name");
							//add("need_login");
							//add("need_login_password");
							//add("auto_create_account");
							add("protect_login");
							add("encrypt_message");
						}
					});
			System.out.println(String.format("Application %s updated", appId));
		} catch (UpdateApplicationException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testUnregister() {
		String appId = "BA92856B-EE74-4ECA-BEE1-A1D96A8CF160";
		try {
			applicationAccess.unregister(appId);
			System.out.println(String.format("Application %s removed.", appId));
		} catch (UnregisterApplicationException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testListAll() {
		try {
			Map<String, String> apps = applicationAccess.listAll();
			System.out.println(String.format("Total %d applications:",
					apps.size()));
			for (String appId : apps.keySet()) {
				System.out.println(String.format("%s: %s", appId,
						apps.get(appId)));
			}
		} catch (GetApplicationException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testGet() {
		String appId = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
		try {
			Application app = applicationAccess.get(appId);
			System.out.println(String.format("%s: %s,%s,%s,%s,%s;%s,%s,%s,%s",
					app.getName(), app.getAccessParams().isNeedLogin(), app
							.getAccessParams().isNeedLoginPassword(), app
							.getAccessParams().isAutoCreateAccount(), app
							.getAccessParams().isProtectLogin(), app
							.getAccessParams().isEncryptMessage(), app
							.getClientParams().getId(), app.getClientParams()
							.getPassword(), app.getClientParams()
							.getPublicKey(), app.getClientParams()
							.getPrivateKey()));
		} catch (GetApplicationException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testExistsName() {
		String appName = "testapp2";
		try {
			boolean exists = applicationAccess.existsName(appName);
			System.out.println(String.format("Application name %s %s", appName,
					exists));
		} catch (CheckApplicationException e) {
			System.err.println(e.getMessage());
		}
	}
}
