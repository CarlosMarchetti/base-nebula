
package dev.nebula.client;

import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.injection.Injector;
import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.utils.ReflectionUtils;

public final class Bootstrap {

    private Bootstrap() {}

    public static void start() {
        try {
            final ClassLoader gameClassLoader = Bootstrap.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(gameClassLoader);

            NativeBridge.log("Bootstrap.start invocado.");

            Thread initThread = new Thread(() -> {
                Thread.currentThread().setContextClassLoader(gameClassLoader);

                try {
                    NativeBridge.log("Bootstrap: Inicializando Nebula...");
                    new Main().init();
                    NativeBridge.log("Bootstrap: Main inicializado com sucesso!");

                    int attempts = 0;
                    while (attempts < 50) { 
                        try {
                            if (Mappings.MINECRAFT_INSTANCE != null) {
                                Object mc = ReflectionUtils.getFieldValue(
                                    (java.lang.reflect.Field) Mappings.MINECRAFT_INSTANCE,
                                    null
                                );
                                if (mc != null) break;
                            }
                        } catch (Exception ignored) {
                        }
                        attempts++;
                        Thread.sleep(100);
                    }

                    try {
                        NativeBridge.log("Bootstrap: Executando Injector.inject()...");
                        Injector.inject();
                        NativeBridge.log("Bootstrap: Injector concluido com sucesso.");
                    } catch (Throwable t) {
                        NativeBridge.log("AVISO Injector: " + t.getMessage());
                    }

                } catch (Throwable t) {
                    NativeBridge.log("ERRO no Bootstrap: " + t.toString());
                    for (StackTraceElement ste : t.getStackTrace()) {
                        NativeBridge.log("   em " + ste.toString());
                    }
                    t.printStackTrace();
                }
            }, "Nebula-Init");

            initThread.start();

        } catch (Throwable t) {
            NativeBridge.log("ERRO CRITICO no Bootstrap.start: " + t.toString());
            t.printStackTrace();
        }
    }
}
