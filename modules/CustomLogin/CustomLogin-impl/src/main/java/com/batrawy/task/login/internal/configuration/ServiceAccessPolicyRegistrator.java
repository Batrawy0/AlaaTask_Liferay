package com.batrawy.task.login.internal.configuration;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.security.service.access.policy.model.SAPEntry;
import com.liferay.portal.security.service.access.policy.service.SAPEntryLocalService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = ServiceAccessPolicyRegistrator.class)
public class ServiceAccessPolicyRegistrator {


    private static final Log _log = LogFactoryUtil.getLog(ServiceAccessPolicyRegistrator.class);

    @Activate
    public void activate() {
        try {

            //get the default company ID
            long companyId = PortalUtil.getDefaultCompanyId();
            // Define the service access policy name
            String sapEntryName = "LOGIN_REGISTER_SAP";
            // Check if policy already exists
            SAPEntry existingEntry = _sapEntryLocalService.fetchSAPEntry(companyId, sapEntryName);


            // Define the service access policy properties
            long userId = 20123;
            String allowedServiceSignatures =
                    "com.batrawy.task.login.internal.resource.v1.LoginResourceImpl#postLogin\n" +
                            "com.batrawy.task.register.internal.resource.v1.RegistrationResourceImpl#postRegister";
            boolean defaultSAPEntry = true;
            boolean enabled = true;
            String name = "LOGIN_REGISTER_SAP";
            Map<Locale, String> titleMap = new HashMap<>();
            titleMap.put(LocaleUtil.getDefault(), "Login & Register Service Access Policy");
            titleMap.put(LocaleUtil.US, "Login & Register Service Access Policy");

            // Define the service context
            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(companyId);

            // Check if the service access policy already exists
            if (existingEntry == null) {
                // Add the service access policy
                _sapEntryLocalService.addSAPEntry(
                        userId,
                        allowedServiceSignatures,
                        defaultSAPEntry,
                        enabled,
                        name,
                        titleMap,
                        serviceContext
                );

                _log.info("Custom service access policy added successfully");
            }
            else {
                // Update the service access policy
                _sapEntryLocalService.updateSAPEntry(
                        existingEntry.getSapEntryId(),
                        allowedServiceSignatures,
                        true,
                        true,
                        sapEntryName,
                        titleMap,
                        serviceContext
                );
                _log.info("Custom service access policy updated successfully");
            }
        }
        catch (PortalException pe) {
            _log.error("Error adding service access policy", pe);
        }
    }

    @Reference
    private SAPEntryLocalService _sapEntryLocalService;
}
