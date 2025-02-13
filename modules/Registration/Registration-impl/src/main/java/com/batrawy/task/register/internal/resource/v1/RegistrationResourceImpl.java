package com.batrawy.task.register.internal.resource.v1;

import com.batrawy.task.register.dto.v1.RegistrationRequest;
import com.batrawy.task.register.dto.v1.RegistrationResponse;
import com.batrawy.task.register.resource.v1.RegistrationResource;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.model.ExpandoTableConstants;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.*;
import com.liferay.portal.kernel.util.Validator;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Locale;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

@Component(properties = "OSGI-INF/liferay/rest/v1/registration.properties", scope = ServiceScope.PROTOTYPE, service = RegistrationResource.class)
public class RegistrationResourceImpl extends BaseRegistrationResourceImpl {

    @Reference
    private UserLocalService userLocalService;

    @Override
    public RegistrationResponse postRegister(RegistrationRequest registrationRequest) throws Exception {
        RegistrationResponse registrationResponse = new RegistrationResponse();

        // Validate required fields
        if (Validator.isNull(registrationRequest.getEmailAddress())) {
            registrationResponse.setStatusCode(400);
            registrationResponse.setStatusMessage("Email address is required.");
            return registrationResponse;
        }


        // get the Company ID, Locale, and ServiceContext
        long companyId = contextCompany.getCompanyId();
        Locale locale = contextAcceptLanguage.getPreferredLocale();
        ServiceContext serviceContext = ServiceContextFactory.getInstance(User.class.getName(), contextHttpServletRequest);

        // set the guest and global group IDs
        long guestGroupId = GroupLocalServiceUtil.getGroup(companyId, "Guest").getGroupId();
        long globalGroupId = GroupLocalServiceUtil.getCompanyGroup(companyId).getGroupId();


        // Create the user using UserLocalService.
        User user = userLocalService.addUser(contextUser.getUserId(),   // creatorUserId (var1)
                companyId,                                         // companyId (var3)
                false,                                             // autoPassword flag (var5) - we supply a password
                registrationRequest.getPassword(),                 // password1 (var6)
                registrationRequest.getPassword(),                 // password2 (var7)
                true,                                              // autoScreenName flag (var8) - we supply screenName
                registrationRequest.getFirstName(),                // screenName (var9) – using emailAddress as screenName
                registrationRequest.getEmailAddress(),             // emailAddress (var10)
                locale,                                            // locale (var11)
                registrationRequest.getFirstName(),                // firstName (var12)
                "",                                                // middleName (var13)
                registrationRequest.getLastName(),                 // lastName (var14)
                0L,                                                // prefixId (var15)
                0L,                                                // suffixId (var17)
                true,                                              // male flag (var19) – adjust as needed
                1,                                                 // birthdayMonth (var20)
                1,                                                 // birthdayDay (var21)
                1970,                                              // birthdayYear (var22)
                "",                                                // jobTitle (var23)
                1,                                                 // updateUserPersonalSite (var24) – typically 0 or 1
                new long[]{guestGroupId, globalGroupId},           // groupIds (var25)
                new long[0],                                       // organizationIds (var26)
                new long[0],                                       // roleIds (var27)
                new long[0],                                       // userGroupIds (var28)
                false,                                             // sendEmail flag (var29)
                serviceContext                                     // serviceContext (var30)
        );

        //---------------------------------------------------------------------------------
        //TOTP Generation and QR Code Creation
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String totpSecret = key.getKey();

        String issuer = "LiferayAuthTest";
        String account = registrationRequest.getEmailAddress();
        String totpUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, account, totpSecret, issuer);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(totpUri, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        String base64QRCode = Base64.getEncoder().encodeToString(pngData);
        //---------------------------------------------------------------------------------


        // Obtain the default Expando table for the User entity.
        ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getDefaultTable(companyId, User.class.getName());

        // Ensure the "IdentityType" column exists.
        if (ExpandoColumnLocalServiceUtil.getColumn(companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, "IdentityType") == null) {
            ExpandoColumnLocalServiceUtil.addColumn(expandoTable.getTableId(), "IdentityType", ExpandoColumnConstants.STRING);
        }
        // Ensure the "IdentityNumber" column exists.
        if (ExpandoColumnLocalServiceUtil.getColumn(companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, "IdentityNumber") == null) {
            ExpandoColumnLocalServiceUtil.addColumn(expandoTable.getTableId(), "IdentityNumber", ExpandoColumnConstants.STRING);
        }


        //---------------------------------------------------------------------------------
        // Persist the TOTP secret
        ExpandoColumnLocalServiceUtil.getColumn(companyId, User.class.getName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, "TOTPSecret");
        user.getExpandoBridge().setAttribute("TOTPSecret", totpSecret);
        //---------------------------------------------------------------------------------


        // Set custom Expando attributes (IdentityType and IdentityNumber)
        user.getExpandoBridge().setAttribute("IdentityType", registrationRequest.getIdentityType().toString());
        user.getExpandoBridge().setAttribute("IdentityNumber", registrationRequest.getIdentityNumber().toString());


        // Set the user's agreedToTermsOfUse flag to true
        user.setAgreedToTermsOfUse(true);
        // Update the user to persist Expando fields
        user = userLocalService.updateUser(user);


        // Assign the "Read-Only" role to the user
        Role defaultUserRole = RoleLocalServiceUtil.getRole(companyId, "Read-Only");
        userLocalService.addRoleUser(defaultUserRole.getRoleId(), user.getUserId());


        // Populate and return the response DTO

        registrationResponse.setUserId(user.getUserId());
        registrationResponse.setScreenName(user.getScreenName());
        //---------------------------------------------------------------------------------
        // Populate the TOTP QR Code and manual key
        registrationResponse.setTotpQRCode("data:image/png;base64," + base64QRCode);
        registrationResponse.setTotpManualKey(totpSecret);
        //---------------------------------------------------------------------------------
        registrationResponse.setStatusCode(200);
        registrationResponse.setStatusMessage("Registration successful. Scan the QR code with your authenticator app.");
        return registrationResponse;
    }
}
