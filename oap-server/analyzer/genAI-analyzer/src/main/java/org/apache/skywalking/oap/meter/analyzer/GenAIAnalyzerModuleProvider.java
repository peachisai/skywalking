package org.apache.skywalking.oap.meter.analyzer;

import org.apache.skywalking.oap.meter.analyzer.config.GenAIConfig;
import org.apache.skywalking.oap.meter.analyzer.config.GenAIConfigLoader;
import org.apache.skywalking.oap.meter.analyzer.matcher.GenAIProviderPrefixMatcher;
import org.apache.skywalking.oap.meter.analyzer.module.GenAIAnalyzerModule;
import org.apache.skywalking.oap.meter.analyzer.service.GenAIMeterAnalyzer;
import org.apache.skywalking.oap.meter.analyzer.service.IGenAIMeterAnalyzerService;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.yaml.snakeyaml.Yaml;

public class GenAIAnalyzerModuleProvider extends ModuleProvider {

    private GenAIConfig config;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return GenAIAnalyzerModule.class;
    }

    @Override
    public ConfigCreator<? extends ModuleConfig> newConfigCreator() {
        return new ConfigCreator<GenAIConfig>() {
            @Override
            public Class<GenAIConfig> type() {
                return GenAIConfig.class;
            }

            @Override
            public void onInitialized(final GenAIConfig initialized) {
                config = initialized;
            }
        };
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        GenAIConfigLoader loader = new GenAIConfigLoader(config, new Yaml());
        config = loader.loadConfig();
        GenAIProviderPrefixMatcher matcher = GenAIProviderPrefixMatcher.build(config);
        this.registerServiceImplementation(
                IGenAIMeterAnalyzerService.class,
                new GenAIMeterAnalyzer(matcher)
        );
    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public String[] requiredModules() {
        return new String[0];
    }
}
