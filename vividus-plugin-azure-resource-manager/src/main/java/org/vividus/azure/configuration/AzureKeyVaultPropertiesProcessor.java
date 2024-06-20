/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.azure.configuration;

import java.io.Closeable;
import java.util.Optional;
import java.util.Properties;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.vividus.configuration.AbstractPropertiesProcessor;

public class AzureKeyVaultPropertiesProcessor extends AbstractPropertiesProcessor implements Closeable
{
    private static final String KEY_VAULT_PROPERTY_NAME = "azure.key-vault.name";

    private final Properties properties;
    private SecretClient secretClient;
    private AnnotationConfigApplicationContext context;

    public AzureKeyVaultPropertiesProcessor(Properties properties)
    {
        super("AZURE_KEY_VAULT");
        this.properties = properties;
    }

    @Override
    protected String processValue(String propertyName, String partOfPropertyValueToProcess)
    {
        String keyVaultName = properties.getProperty(KEY_VAULT_PROPERTY_NAME);
        Validate.notEmpty(keyVaultName, "Provide value into property `%s`", KEY_VAULT_PROPERTY_NAME);

        KeyVaultSecret retrievedSecret = getKeyVaultClient(keyVaultName).getSecret(partOfPropertyValueToProcess);
        return retrievedSecret.getValue();
    }

    @Override
    public void close()
    {
        if (context != null)
        {
            context.close();
        }
    }

    private SecretClient getKeyVaultClient(String keyVaultName)
    {
        if (secretClient == null)
        {
            context = new AnnotationConfigApplicationContext();
            PropertiesPropertySource propertySource = new PropertiesPropertySource("properties", properties);
            context.getEnvironment().getPropertySources().addFirst(propertySource);

            Optional.ofNullable(properties.getProperty(KEY_VAULT_PROPERTY_NAME)).ifPresent(name ->
                    context.registerBean(SecretClient.class, new SecretClientBuilder()
                            .vaultUrl(String.format("https://%s.vault.azure.net", keyVaultName))
                            .credential(new DefaultAzureCredentialBuilder().build())
                            .buildAsyncClient()));
            context.refresh();

            secretClient = context.getBean(SecretClient.class);
        }
        return secretClient;
    }
}
