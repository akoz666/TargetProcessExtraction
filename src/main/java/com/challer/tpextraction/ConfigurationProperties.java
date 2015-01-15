package com.challer.tpextraction;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.util.Properties;

/**
 * Load configuration file of this application
 *
 * @author Cyril Haller - cyril.haller@gmail.com
 */
public class ConfigurationProperties {

    /**
     * Name of configuration file
     */
    public final static String NOM_RESSOURCES = "config.properties";

    /**
     * Properties present in the configuration file
     */
    private static Properties propertiesResource = null;

    /**
     * Return the value of a key written the configuration file
     *
     * @param key String - Key of the property
     * @return String - Value of the property
     */
    public static
    @NotNull
    String getProperty(@NotNull String key) {
        return getPropertiesResource().getProperty(key);
    }

    /**
     * Return properties of configuration file
     *
     * @return Properties
     */
    public static
    @NotNull
    Properties getPropertiesResource() {

        if (propertiesResource == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            java.io.InputStream is = cl.getResourceAsStream(NOM_RESSOURCES);
            if (is != null) {
                propertiesResource = new Properties();
                try {
                    propertiesResource.load(is);
                } catch (IOException e) {
                    //logger.error(e);
                }
            }
        }
        return propertiesResource;
    }

}
