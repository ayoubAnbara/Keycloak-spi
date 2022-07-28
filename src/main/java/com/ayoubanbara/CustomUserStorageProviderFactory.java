package com.ayoubanbara;

import com.ayoubanbara.dao.UserDAO;
import com.ayoubanbara.model.User;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.ayoubanbara.utils.Helpers.isBlank;
import static com.ayoubanbara.utils.Helpers.isNumeric;

public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {
    public static final int PORT_LIMIT = 65535;
    Map<String,String> properties;
    Map<String, EntityManagerFactory> entityManagerFactories = new HashMap<>();

    protected static final List<ProviderConfigProperty> configMetadata;

    public static final String DB_CONNECTION_NAME_KEY = "db:connectionName";
    public static final String DB_HOST_KEY = "db:host";
    public static final String DB_DATABASE_KEY = "db:database";
    public static final String DB_USERNAME_KEY = "db:username";
    public static final String DB_PASSWORD_KEY = "db:password";
    public static final String DB_PORT_KEY = "db:port";
    public static final String DB_CONNECTION_OTHER_PARAMS = "db:params";

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                // Connection Name
                .property().name(DB_CONNECTION_NAME_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Connection Name")
                .defaultValue(System.currentTimeMillis())
                .helpText("Name of the connection, can be chosen individually. Enables connection sharing between providers if the same name is provided. Overrides currently saved connection properties.")
                .add()

                // Connection Host
                .property().name(DB_HOST_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Host")
                .defaultValue("localhost")
                .helpText("Host of the connection")
                .add()

                // Connection Params
                .property().name(DB_CONNECTION_OTHER_PARAMS)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("CONNECTION OTHER PARAMS")
                .defaultValue("autoReconnect=true&useSSL=false&useUnicode=yes&characterEncoding=UTF-8&characterSetResults=UTF-8")
                .helpText("like: key1=value1&key2=value2&keyN=valueN")
                .add()

                // Connection Database
                .property().name(DB_DATABASE_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Name")
                //.defaultValue()
                .add()

                // DB Username
                .property().name(DB_USERNAME_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Username")
                .defaultValue("root")
                .add()

                // DB Password
                .property().name(DB_PASSWORD_KEY)
                .type(ProviderConfigProperty.PASSWORD)
                .label("Database Password")
                .defaultValue("root")
                .add()

                // DB Port
                .property().name(DB_PORT_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Database Port")
                .defaultValue("3308")
                .add()
                .build();
    }



    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        properties = new HashMap<>();
        String dbConnectionName = model.getConfig().getFirst("db:connectionName");
        EntityManagerFactory entityManagerFactory = entityManagerFactories.get(dbConnectionName);
        if (entityManagerFactory == null) {
            MultivaluedHashMap<String, String> config = model.getConfig();
            properties.put("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
            properties.put("hibernate.connection.url",
                    String.format("jdbc:mysql://%s:%s/%s?%s",
                            config.getFirst(DB_HOST_KEY),
                            config.getFirst(DB_PORT_KEY),
                            config.getFirst(DB_DATABASE_KEY),
                            config.getFirst(DB_CONNECTION_OTHER_PARAMS)
                    )
            );
            properties.put("hibernate.connection.username", config.getFirst(DB_USERNAME_KEY));
            properties.put("hibernate.connection.password", config.getFirst(DB_PASSWORD_KEY));
            properties.put("hibernate.show-sql", "true");
            properties.put("hibernate.archive.autodetection", "class, hbm");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.connection.autocommit", "true");

            entityManagerFactory = new HibernatePersistenceProvider().createContainerEntityManagerFactory(getPersistenceUnitInfo("h2userstorage"), properties);
            entityManagerFactories.put(dbConnectionName, entityManagerFactory);
        }
        UserDAO userDAO = new UserDAO(entityManagerFactory.createEntityManager());
        return new CustomUserStorageProvider(session, model, userDAO);
    }


    @Override
    public String getId() {
        return "user-provider";
    }

    @Override
    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
        String oldCnName = oldModel.getConfig().getFirst(DB_CONNECTION_NAME_KEY);
        entityManagerFactories.remove(oldCnName);
        onCreate(session, realm, newModel);
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        MultivaluedHashMap<String, String> configMap = config.getConfig();
        if (isBlank(configMap.getFirst(DB_CONNECTION_NAME_KEY))) {
            throw new ComponentValidationException("Connection name empty.");
        }
        if (isBlank(configMap.getFirst(DB_HOST_KEY))) {
            throw new ComponentValidationException("Database host empty.");
        }
        if (!isNumeric(configMap.getFirst(DB_PORT_KEY)) || Long.parseLong(configMap.getFirst(DB_PORT_KEY)) > PORT_LIMIT) {
            throw new ComponentValidationException("Invalid port. (Empty or NaN)");
        }
        if (isBlank(configMap.getFirst(DB_DATABASE_KEY))) {
            throw new ComponentValidationException("Database name empty.");
        }
        if (isBlank(configMap.getFirst(DB_USERNAME_KEY))) {
            throw new ComponentValidationException("Database username empty.");
        }
        if (isBlank(configMap.getFirst(DB_PASSWORD_KEY))) {
            throw new ComponentValidationException("Database password empty.");
        }
    }

    private PersistenceUnitInfo getPersistenceUnitInfo(String name) {
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return name;
            }

            @Override
            public String getPersistenceProviderClassName() {
                return "org.hibernate.jpa.HibernatePersistenceProvider";
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                return null;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return null;
            }

            @Override
            public List<String> getMappingFileNames() {
                return Collections.emptyList();
            }

            @Override
            public List<URL> getJarFileUrls() {
                try {
                    return Collections.list(this.getClass()
                            .getClassLoader()
                            .getResources(""));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                List<String> managedClasses = new LinkedList<>();
                managedClasses.add(User.class.getName());
                return managedClasses;
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return SharedCacheMode.UNSPECIFIED;
            }

            @Override
            public ValidationMode getValidationMode() {
                return ValidationMode.AUTO;
            }

            @Override
            public Properties getProperties() {
                return new Properties();
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return "2.1";
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
//                return Thread.currentThread().getContextClassLoader();
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {
            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
    }


}
