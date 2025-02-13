package com.batrawy.task.login.internal.graphql.servlet.v1;

import com.batrawy.task.login.internal.graphql.mutation.v1.Mutation;
import com.batrawy.task.login.internal.graphql.query.v1.Query;

import com.liferay.portal.vulcan.graphql.servlet.ServletData;

import javax.annotation.Generated;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * @author ahmed
 * @generated
 */
@Component(immediate = true, service = ServletData.class)
@Generated("")
public class ServletDataImpl implements ServletData {

	@Activate
	public void activate(BundleContext bundleContext) {
	}

	@Override
	public Mutation getMutation() {
		return new Mutation();
	}

	@Override
	public String getPath() {
		return "/customlogin-graphql/v1";
	}

	@Override
	public Query getQuery() {
		return new Query();
	}

}