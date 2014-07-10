package com.ft.methodetransformer;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.mustachemods.SwitchableMustacheViewBundle;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.methodetransformer.configuration.MethodeTransformerConfiguration;
import com.ft.methodetransformer.health.MethodeTransformerHealthCheck;
import com.ft.methodetransformer.resources.MethodeTransformerResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.servlets.SlowRequestFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class MethodeTransformerService extends Application<MethodeTransformerConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MethodeTransformerService().run(args);
    }

    @Override
    public void initialize(final Bootstrap bootstrap) {
        bootstrap.addBundle(new SwitchableMustacheViewBundle());
        bootstrap.addBundle(new AssetsBundle("/views"));
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
    }

    @Override
    public void run(final MethodeTransformerConfiguration configuration, final Environment environment) throws Exception {
        environment.jersey().register(new BuildInfoResource());
        environment.jersey().register(new MethodeTransformerResource());

        environment.servlets().addFilter(
                "Slow Servlet Filter",
                new SlowRequestFilter(Duration.milliseconds(configuration.getSlowRequestTimeout()))).addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                false,
                configuration.getSlowRequestPattern());

        environment.healthChecks().register("My Health", new MethodeTransformerHealthCheck("replace me"));

    }

}
