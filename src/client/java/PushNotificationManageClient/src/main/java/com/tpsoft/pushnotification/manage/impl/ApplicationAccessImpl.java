package com.tpsoft.pushnotification.manage.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tpsoft.pushnotification.manage.config.ServiceConfig;
import com.tpsoft.pushnotification.manage.exception.CheckApplicationException;
import com.tpsoft.pushnotification.manage.exception.GetApplicationException;
import com.tpsoft.pushnotification.manage.exception.RegisterApplicationException;
import com.tpsoft.pushnotification.manage.exception.UnregisterApplicationException;
import com.tpsoft.pushnotification.manage.exception.UpdateApplicationException;
import com.tpsoft.pushnotification.manage.intf.IApplicationAccess;
import com.tpsoft.pushnotification.manage.model.Application;
import com.tpsoft.pushnotification.manage.model.Application.AccessParams;
import com.tpsoft.pushnotification.manage.model.Application.ClientParams;
import com.tpsoft.pushnotification.manage.response.CheckApplicationNameResult;
import com.tpsoft.pushnotification.manage.response.GetApplicationResult;
import com.tpsoft.pushnotification.manage.response.ListAllApplicationResult;
import com.tpsoft.pushnotification.manage.response.RegisterApplicationResult;
import com.tpsoft.pushnotification.manage.response.UnregisterApplicationResult;
import com.tpsoft.pushnotification.manage.response.UpdateApplicationResult;

public class ApplicationAccessImpl implements IApplicationAccess {

	private ServiceConfig serviceConfig;

	private Gson gson = new GsonBuilder().create();

	public ApplicationAccessImpl() {

	}

	public ApplicationAccessImpl(ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public ServiceConfig getServiceConfig() {
		return serviceConfig;
	}

	public void setServiceConfig(ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public ClientParams register(String name, AccessParams params)
			throws RegisterApplicationException {

		ClientRequest req = new ClientRequest(
				serviceConfig.getRegisterApplicationEndpoint());
		req.pathParameter("name", name).body(ServiceConfig.CONTENT_TYPE,
				gson.toJson(params));

		ClientResponse<RegisterApplicationResult> res;
		try {
			res = req.post(RegisterApplicationResult.class);
		} catch (Exception e) {
			throw new RegisterApplicationException(String.format("#-1:%s",
					e.getMessage()));
		}
		RegisterApplicationResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new RegisterApplicationException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		return new ClientParams(result.getId(), result.getPassword(),
				result.getPublic_key(), result.getPrivate_key());
	}

	public void update(String id, String name, String newName,
			AccessParams params, Set<String> updatedFields)
			throws UpdateApplicationException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		if (updatedFields.contains("name"))
			map.put("new_name", newName);
		if (updatedFields.contains("need_login"))
			map.put("need_login", params.isNeedLogin());
		if (updatedFields.contains("need_login_password"))
			map.put("need_login_password", params.isNeedLoginPassword());
		if (updatedFields.contains("auto_create_account"))
			map.put("auto_create_account", params.isAutoCreateAccount());
		if (updatedFields.contains("protect_login"))
			map.put("protect_login", params.isProtectLogin());
		if (updatedFields.contains("encrypt_message"))
			map.put("encrypt_message", params.isEncryptMessage());

		ClientRequest req = new ClientRequest(
				serviceConfig.getUpdateApplicationEndpoint());
		req.pathParameter("id", id).body(ServiceConfig.CONTENT_TYPE,
				gson.toJson(map));

		ClientResponse<UpdateApplicationResult> res;
		try {
			res = req.put(UpdateApplicationResult.class);
		} catch (Exception e) {
			throw new UpdateApplicationException(String.format("#-1:%s",
					e.getMessage()));
		}
		UpdateApplicationResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new UpdateApplicationException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public void unregister(String id) throws UnregisterApplicationException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getUnregisterApplicationEndpoint());
		req.pathParameter("id", id);

		ClientResponse<UnregisterApplicationResult> res;
		try {
			res = req.delete(UnregisterApplicationResult.class);
		} catch (Exception e) {
			throw new UnregisterApplicationException(String.format("#-1:%s",
					e.getMessage()));
		}
		UnregisterApplicationResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new UnregisterApplicationException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public Map<String, String> listAll() throws GetApplicationException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getListAllApplicationsEndpoint());

		ClientResponse<ListAllApplicationResult> res;
		try {
			res = req.get(ListAllApplicationResult.class);
		} catch (Exception e) {
			throw new GetApplicationException(String.format("#-1:%s",
					e.getMessage()));
		}
		ListAllApplicationResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new GetApplicationException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		Map<String, String> map = new HashMap<String, String>();
		for (ListAllApplicationResult.ApplicationIdAndName app : result
				.getList()) {
			map.put(app.getId(), app.getName());
		}
		return map;
	}

	public Application get(String id) throws GetApplicationException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getGetApplicationEndpoint());
		req.pathParameter("id", id);

		ClientResponse<GetApplicationResult> res;
		try {
			res = req.get(GetApplicationResult.class);
		} catch (Exception e) {
			throw new GetApplicationException(String.format("#-1:%s",
					e.getMessage()));
		}
		GetApplicationResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new GetApplicationException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		Application app = new Application(result.getName(),
				new Application.AccessParams(result.isNeed_login(), result
						.isNeed_login_password(), result
						.isAuto_create_account(), result.isProtect_login(),
						result.isEncrypt_message()),
				new Application.ClientParams(id, result.getPassword(), result
						.getPublic_key(), result.getPrivate_key()));
		return app;
	}

	public boolean existsName(String name) throws CheckApplicationException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getCheckApplicationNameEndpoint());
		req.pathParameter("name", name);

		ClientResponse<CheckApplicationNameResult> res;
		try {
			res = req.get(CheckApplicationNameResult.class);
		} catch (Exception e) {
			throw new CheckApplicationException(String.format("#-1:%s",
					e.getMessage()));
		}
		CheckApplicationNameResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new CheckApplicationException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
		return result.isExists();
	}

}
