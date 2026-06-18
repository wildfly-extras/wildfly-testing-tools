/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.junit.extension;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.wildfly.plugin.tools.Deployment;
import org.wildfly.plugin.tools.DeploymentResult;
import org.wildfly.plugin.tools.UndeployDescription;
import org.wildfly.plugin.tools.server.ServerManager;
import org.wildfly.plugin.tools.server.ServerManagerListener;
import org.wildfly.testing.junit.extension.annotation.ManualMode;
import org.wildfly.testing.junit.extension.annotation.ServerGroup;
import org.wildfly.testing.junit.extension.annotation.WildFlyDomainTest;
import org.wildfly.testing.junit.extension.annotation.WildFlyTest;
import org.wildfly.testing.junit.extension.api.ServerConfiguration;

/**
 * JUnit extension that manages WildFly server lifecycle and per-test-class deployments.
 * <p>
 * The server is started once and shared across all test classes (suite-level lifecycle).
 * Deployments are managed per-test-class (class-level lifecycle):
 * <ul>
 * <li>{@code beforeAll}: Start server (if needed) and deploy test's application</li>
 * <li>{@code afterAll}: Undeploy test's application</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class WildFlyExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Logger LOGGER = Logger.getLogger(WildFlyExtension.class);

    private static final String SERVER_LISTENER_KEY = "wildfly.server.listener";
    private static final ExtensionContext.Namespace CLASS_STORE_NAMESPACE = ExtensionContext.Namespace
            .create("WildFly.ClassStore");

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        // Validate test mode annotations first - runs for every test class
        validateTestModeAnnotations(context);

        // Start server (if not already started) - shared across all test classes
        final ServerManager serverManager = ServerContext.getOrCreateServerManager(context);

        final Optional<ManualMode> manualMode = TestSupport.getManualMode(context);

        if (manualMode.isEmpty()) {
            if (!serverManager.isRunning()) {
                // Get timeout from configuration (defaults to 60 seconds)
                final long timeout = ServerConfiguration.timeout(context);
                // Start the server
                serverManager.start(timeout, TimeUnit.SECONDS);
            }
            // Deploy any deployments and cache the deployment information
            deploy(serverManager, context);
        } else {
            final var listener = new ExtensionServerManagerListener(context, serverManager);
            getClassStore(context).put(SERVER_LISTENER_KEY, listener);
            serverManager.addServerManagerListener(listener);
            final var autoStart = manualMode.get().value();
            if (autoStart) {
                if (!serverManager.isRunning()) {
                    // Get timeout from configuration (defaults to 60 seconds)
                    final long timeout = ServerConfiguration.timeout(context);
                    // Start the server
                    serverManager.start(timeout, TimeUnit.SECONDS);
                }
                deploy(serverManager, context);
            } else {
                if (serverManager.isRunning()) {
                    LOGGER.debugf("Shutting down server for manual mode test %s", context.getRequiredTestClass()
                            .getName());
                    ServerContext.stopServer(context, serverManager);
                }
            }
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        final Optional<ServerManager> opt = ServerContext.getServer(context);
        if (opt.isEmpty()) {
            return;
        }
        final ServerManager serverManager = opt.get();
        // Get deployment info
        final Optional<DeploymentInfo> deploymentInfo = DeploymentContext.resolveDeployment(context);
        if (deploymentInfo.isEmpty()) {
            return;
        }
        undeploy(context, serverManager, deploymentInfo.get());
    }

    /**
     * Validates that a test class doesn't have conflicting test mode annotations.
     *
     * @param context the extension context
     *
     * @throws JUnitException if both @WildFlyTest and @WildFlyDomainTest are present
     */
    private void validateTestModeAnnotations(final ExtensionContext context) {
        final Class<?> testClass = context.getRequiredTestClass();
        final boolean hasWildFlyTest = AnnotationSupport
                .findAnnotation(testClass, WildFlyTest.class).isPresent();
        final boolean hasDomainTest = AnnotationSupport
                .findAnnotation(testClass, WildFlyDomainTest.class).isPresent();

        if (hasWildFlyTest && hasDomainTest) {
            throw new JUnitException(
                    "Test class %s cannot have both @WildFlyTest and @WildFlyDomainTest. Use only one to specify the test mode."
                            .formatted(testClass.getName()));
        }
    }

    /**
     * Deploys an archive to the server.
     *
     * @param serverManager the server manager
     * @param context       the extension context
     *
     * @throws JUnitException if deployment fails
     */
    private void deploy(final ServerManager serverManager, final ExtensionContext context) {
        // Check if deployment already exists in cache
        if (DeploymentContext.resolveDeployment(context).isPresent()) {
            return; // Already deployed
        }

        // Find deployment method for this test class
        final Optional<Archive<?>> deploymentArchive = resolveDeployment(context);

        if (deploymentArchive.isEmpty()) {
            return; // No deployment for this test
        }

        // Invoke deployment method to get Archive
        final Archive<?> archive = deploymentArchive.get();
        final String deploymentName = archive.getName();
        // Check for @WildFlyDomainTest and get server groups from deployment method
        final boolean isDomainTest = AnnotationSupport
                .findAnnotation(context.getRequiredTestClass(), WildFlyDomainTest.class).isPresent();
        final Set<String> serverGroups;
        if (isDomainTest) {
            serverGroups = resolveServerGroups(context);
            if (serverGroups.isEmpty()) {
                throw new JUnitException("No server groups defined for domain deployment. " +
                        "Add @ServerGroup annotation to the deployment method.");
            }
        } else {
            serverGroups = Set.of();
        }

        // Convert Archive to Deployment
        try (
                var in = archive.as(ZipExporter.class).exportAsInputStream();
                var deployment = Deployment.of(in, deploymentName)) {
            // Set the server groups, which could be empty for a standalone server
            deployment.setServerGroups(serverGroups);

            // Deploy to server
            final var deploymentResult = serverManager.deploymentManager().deploy(deployment);
            if (!deploymentResult.successful()) {
                throw new JUnitException("Failed to deploy %s to server: %s".formatted(deploymentName,
                        deploymentResult.getFailureMessage()));
            }
            DeploymentContext.cache(context, new DeploymentInfo(deploymentName, serverGroups));
        } catch (IOException e) {
            throw new JUnitException("Failed to export archive %s as deployment".formatted(deploymentName), e);
        }
    }

    private void undeploy(final ExtensionContext context, final ServerManager serverManager,
            final DeploymentInfo deploymentInfo) {
        // Check for @WildFlyDomainTest annotation
        final boolean isDomainTest = AnnotationSupport
                .findAnnotation(context.getRequiredTestClass(), WildFlyDomainTest.class).isPresent();
        final String deploymentName = deploymentInfo.deploymentName();
        final UndeployDescription undeployDescription = UndeployDescription.of(deploymentName);
        if (isDomainTest) {
            undeployDescription.addServerGroups(deploymentInfo.serverGroups());
        }
        // Undeploy from server
        try {
            final DeploymentResult result = serverManager.deploymentManager()
                    .undeploy(undeployDescription);
            if (!result.successful()) {
                LOGGER.warnf("Failed to undeploy application %s: %s", deploymentName, result.getFailureMessage());
            }
        } catch (Exception e) {
            LOGGER.warnf(e, "Failed to undeploy application %s.", deploymentName);
        }

        // Remove from cache
        DeploymentContext.remove(context);
    }

    private static ExtensionContext.Store getClassStore(final ExtensionContext context) {
        // We want to store on the class context, attempt to determine which that context
        final ExtensionContext usingContext;
        if (context.getTestMethod().isPresent()) {
            usingContext = context.getParent().orElse(context);
        } else {
            usingContext = context;
        }
        return usingContext.getStore(CLASS_STORE_NAMESPACE);
    }

    private static Optional<Archive<?>> resolveDeployment(final ExtensionContext context) {
        final Class<?> testClass = context.getRequiredTestClass();
        final Optional<Archive<?>> testDeployment = TestSupport.findDeploymentMethod(context);
        final Optional<Archive<?>> deploymentProducer = TestSupport.findDeploymentProducerMethod(context);
        if (testDeployment.isPresent() && deploymentProducer.isPresent()) {
            throw new JUnitException(
                    "Test %s cannot have both @GenerateDeployment and @DeploymentProducer methods. Use only one deployment method type per test class."
                            .formatted(testClass.getName()));
        }
        if (testDeployment.isPresent()) {
            return testDeployment;
        }
        return deploymentProducer;
    }

    /**
     * Resolves server groups from the {@link ServerGroup @ServerGroup} annotation on the deployment method.
     *
     * @param context the extension context
     *
     * @return set of server group names, or empty set if no {@link ServerGroup @ServerGroup} annotation found
     */
    private static Set<String> resolveServerGroups(final ExtensionContext context) {
        final Optional<java.lang.reflect.Method> deploymentMethod = TestSupport.findDeploymentMethodReference(context);

        if (deploymentMethod.isEmpty()) {
            return Set.of();
        }

        final Method method = deploymentMethod.get();
        final Optional<ServerGroup> serverGroupAnnotation = AnnotationSupport.findAnnotation(method, ServerGroup.class);

        return serverGroupAnnotation.map(serverGroup -> Set.of(serverGroup.value())).orElseGet(Set::of);

    }

    private class ExtensionServerManagerListener implements ServerManagerListener, AutoCloseable {
        private final ExtensionContext context;
        private final ServerManager serverManager;

        private ExtensionServerManagerListener(final ExtensionContext context, final ServerManager serverManager) {
            this.context = context;
            this.serverManager = serverManager;
        }

        @Override
        public void afterStart(final ServerManager serverManager) {
            // Deploy any deployments and cache the deployment information
            deploy(serverManager, context);
        }

        @Override
        public void beforeShutdown(final ServerManager serverManager) {
            // Get deployment info
            final Optional<DeploymentInfo> deploymentInfo = DeploymentContext.resolveDeployment(context);
            if (deploymentInfo.isEmpty()) {
                return;
            }
            undeploy(context, serverManager, deploymentInfo.get());
        }

        @Override
        public void close() {
            getClassStore(context).remove(SERVER_LISTENER_KEY, ServerManagerListener.class);
            serverManager.removeServerManagerListener(this);
        }
    }
}
