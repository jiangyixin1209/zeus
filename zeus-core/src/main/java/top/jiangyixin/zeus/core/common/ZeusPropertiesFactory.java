package top.jiangyixin.zeus.core.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * @author jiangyixin
 */
public class ZeusPropertiesFactory {
    private static final Logger logger = LoggerFactory.getLogger(ZeusPropertiesFactory.class);
    private static final Properties PROPERTIES = new Properties();
    static {
        try {
          PROPERTIES.load(ZeusPropertiesFactory.class.getClassLoader().getResourceAsStream("zeus.properties"));
        } catch (IOException e) {
            logger.error("Load zeus.properties error: ", e);
        }
    }
    public static Properties getProperties() {
        return PROPERTIES;
    }
}
