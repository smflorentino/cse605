package gnu.classpath;

import java.util.Properties;

final class VMSystemProperties {
    static void preInit(Properties properties) {
	FCSystemProperties.preInit(properties);
    }
    
    static void postInit(Properties properties) {
	FCSystemProperties.postInit(properties);
    }
}

