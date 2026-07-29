/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import ua.nanit.limbo.NanoLimbo;
import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.data.InfoForwarding;
import xyz.kyngs.librelogin.common.integration.nanolimbo.NanoLimboIntegration;

public class VelocityNanoLimboIntegration extends NanoLimboIntegration<RegisteredServer> {

    private final ClassLoader classLoader;
    private final ProxyServer proxyServer;

    public VelocityNanoLimboIntegration(ProxyServer proxyServer, String portRange) {
        super(portRange);
        this.classLoader = NanoLimbo.class.getClassLoader();
        this.proxyServer = proxyServer;
    }

    @Override
    public RegisteredServer createLimbo(String serverName) {
        InetSocketAddress address = findLocalAvailableAddress().orElseThrow(() -> new IllegalStateException("Cannot find available port for limbo server!"));
        LimboServer server = createLimboServer(address);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proxyServer.registerServer(new ServerInfo(serverName, address));
    }

    @Override
    protected InfoForwarding createForwarding() {
        Object velocityConfiguration = proxyServer.getConfiguration();

        try {
            Object forwardingMode = velocityConfiguration.getClass()
                    .getMethod("getPlayerInfoForwardingMode")
                    .invoke(velocityConfiguration);
            byte[] forwardingSecret = (byte[]) velocityConfiguration.getClass()
                    .getMethod("getForwardingSecret")
                    .invoke(velocityConfiguration);

            return switch (((Enum<?>) forwardingMode).name()) {
                case "NONE" -> FORWARDING_FACTORY.none();
                case "LEGACY" -> FORWARDING_FACTORY.legacy();
                case "MODERN" -> FORWARDING_FACTORY.modern(forwardingSecret);
                case "BUNGEEGUARD" ->
                        FORWARDING_FACTORY.bungeeGuard(Collections.singleton(new String(forwardingSecret, StandardCharsets.UTF_8)));
                default -> throw new IllegalStateException("Unsupported Velocity forwarding mode: " + forwardingMode);
            };
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read Velocity forwarding configuration", exception);
        }
    }

    @Override
    protected ClassLoader classLoader() {
        return classLoader;
    }

}
