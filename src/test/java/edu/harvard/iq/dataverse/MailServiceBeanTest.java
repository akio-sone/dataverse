package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.mail.CustomMailTextServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MailServiceBeanTest {
    
    @Nested
    class Delegation {
        /**
         * We need to reset the BrandingUtil mocks for every test, as we rely on them being set to default.
         */
        @BeforeEach
        void setup() {
            BrandingUtilTest.setupMocks();
        }
        @AfterAll
        static void tearDown() {
            BrandingUtilTest.tearDownMocks();
        }
        
        @ParameterizedTest
        @CsvSource(value  = {
            // with name in admin mail address
            "Foo Bar <foo@bar.org>, NULL, NULL, Foo Bar",
            // without name, but installation branding name set
            "dataverse@dataverse.org, NULL, LibraScholar Dataverse, LibraScholar Dataverse",
            // without name, but root dataverse name available
            "dataverse@dataverse.org, NotLibraScholar, NULL, NotLibraScholar",
            // without name, without root dataverse name, without installation name -> default to bundle string.
            "dataverse@dataverse.org, NULL, NULL, Dataverse Installation Admin"
        }, nullValues = {"NULL"})
        void setContactDelegation(String fromMail, String rootDataverseName, String installationName, String expectedStartsWith) throws AddressException {
            BrandingUtilTest.setRootDataverseName(rootDataverseName);
            BrandingUtilTest.setInstallationName(installationName);
            
            InternetAddress fromAddress = new InternetAddress(fromMail);
            MailServiceBean mailServiceBean = new MailServiceBean();
            try {
                mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
                assertTrue(fromAddress.getPersonal().startsWith(expectedStartsWith));
                assertTrue(fromAddress.getPersonal().endsWith(" on behalf of user@example.edu"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Nested
    @LocalJvmSettings
    class LookupMailAddresses {
        
        @Mock
        SettingsServiceBean settingsServiceBean;
        
        @InjectMocks
        MailServiceBean mailServiceBean = new MailServiceBean();
        
        private static final String email = "test@example.org";
        
        @Test
        void lookupSystemWithoutAnySetting() {
            assertTrue(mailServiceBean.getSystemAddress().isEmpty());
        }
        
        @Test
        void lookupSystemWithDBOnly() {
            Mockito.when(settingsServiceBean.getValueForKey(Key.SystemEmail)).thenReturn(email);
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        void lookupSystemWithMPConfig() {
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        void lookupSystemWhereMPConfigTakesPrecedenceOverDB() {
            Mockito.lenient().when(settingsServiceBean.getValueForKey(Key.SystemEmail)).thenReturn("foobar@example.org");
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
        }
        
        @Test
        void lookupSupportWithoutAnySetting() {
            assertTrue(mailServiceBean.getSupportAddress().isEmpty());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        void lookupSupportNotSetButWithSystemPresent() {
            assertEquals(email, mailServiceBean.getSupportAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SUPPORT_EMAIL, value = email)
        void lookupSupportWithoutSystemSet() {
            assertTrue(mailServiceBean.getSystemAddress().isEmpty());
            assertEquals(email, mailServiceBean.getSupportAddress().get().getAddress());
        }
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = email)
        @JvmSetting(key = JvmSettings.SUPPORT_EMAIL, value = "support@example.org")
        void lookupSupportSetWithSystemPresent() {
            assertEquals(email, mailServiceBean.getSystemAddress().get().getAddress());
            assertEquals("support@example.org", mailServiceBean.getSupportAddress().get().getAddress());
        }
    }
    
    @Nested
    @LocalJvmSettings
    class SendSystemMail {
        @Mock
        SettingsServiceBean settingsServiceBean;
        @InjectMocks
        MailServiceBean mailServiceBean = new MailServiceBean();
        
        @Test
        @JvmSetting(key = JvmSettings.SYSTEM_EMAIL, value = "")
        void skipIfNoSystemAddress() {
            assertFalse(mailServiceBean.sendSystemEmail("target@example.org", "Test", "Test", false));
        }
    }

    /**
     * Tests for {@link MailServiceBean#getMailText(String)} /
     * {@link MailServiceBean#getMailText(String, java.util.List)}, i.e. the
     * logic that decides whether an automatic email string comes from the
     * "custommailtext" DB table or from Bundle.properties.
     */
    @Nested
    class GetMailText {

        @Mock
        SettingsServiceBean settingsServiceBean;

        @Mock
        CustomMailTextServiceBean customMailTextService;

        @InjectMocks
        MailServiceBean mailServiceBean = new MailServiceBean();

        // A key that really exists in Bundle.properties, so we can compare
        // against BundleUtil directly instead of hard-coding English text.
        private static final String KEY = "notification.email.closing";
        private static final String KEY_WITH_ARGS = "notification.email.createDataverse";

        @Test
        void usesBundleWhenSettingIsOff() {
            // isTrueForKey() is not stubbed, so the mock returns the Mockito
            // default (false) for the boolean, meaning the DB is never consulted.
            String expected = BundleUtil.getStringFromBundle(KEY);

            String actual = mailServiceBean.getMailText(KEY);

            assertEquals(expected, actual);
            Mockito.verifyNoInteractions(customMailTextService);
        }

        @Test
        void usesDatabaseOverrideWhenSettingIsOnAndOverrideExists() {
            Mockito.when(settingsServiceBean.isTrueForKey(Key.CustomMailTextsEnabled, false)).thenReturn(true);
            Mockito.when(customMailTextService.getText(Mockito.eq(KEY), Mockito.any()))
                    .thenReturn("Our custom closing text.");

            String actual = mailServiceBean.getMailText(KEY);

            assertEquals("Our custom closing text.", actual);
        }

        @Test
        void fallsBackToBundleWhenSettingIsOnButNoOverrideExists() {
            Mockito.when(settingsServiceBean.isTrueForKey(Key.CustomMailTextsEnabled, false)).thenReturn(true);
            Mockito.when(customMailTextService.getText(Mockito.eq(KEY), Mockito.any())).thenReturn(null);
            String expected = BundleUtil.getStringFromBundle(KEY);

            String actual = mailServiceBean.getMailText(KEY);

            assertEquals(expected, actual);
        }

        @Test
        void formatsArgumentsIntoDatabaseOverride() {
            Mockito.when(settingsServiceBean.isTrueForKey(Key.CustomMailTextsEnabled, false)).thenReturn(true);
            Mockito.when(customMailTextService.getText(Mockito.eq(KEY_WITH_ARGS), Mockito.any()))
                    .thenReturn("Welcome to {0}, created on {1}.");

            String actual = mailServiceBean.getMailText(KEY_WITH_ARGS, Arrays.asList("My Dataverse", "2026-07-08"));

            assertEquals("Welcome to My Dataverse, created on 2026-07-08.", actual);
        }

        @Test
        void formatsArgumentsWhenFallingBackToBundle() {
            // Not stubbed -> isTrueForKey() defaults to false -> straight to Bundle.properties.
            // notification.email.createDataverse has placeholders {0} through {5}.
            List<String> arguments = Arrays.asList("My Dataverse", "http://example.org/dv", "Root",
                    "http://example.org/dv/root", "http://example.org", "en");
            String expected = BundleUtil.getStringFromBundle(KEY_WITH_ARGS, arguments);

            String actual = mailServiceBean.getMailText(KEY_WITH_ARGS, arguments);

            assertEquals(expected, actual);
        }
    }

}