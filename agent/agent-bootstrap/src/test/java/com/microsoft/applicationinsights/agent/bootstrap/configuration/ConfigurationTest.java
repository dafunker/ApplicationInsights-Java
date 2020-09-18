package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.PreviewConfiguration;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class ConfigurationTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @Test
    public void shouldParse() throws IOException {

        Configuration configuration = loadConfiguration();

        InstrumentationSettings instrumentationSettings = configuration.instrumentationSettings;
        PreviewConfiguration preview = instrumentationSettings.preview;

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", instrumentationSettings.connectionString);
        assertEquals("Something Good", preview.roleName);
        assertEquals("xyz123", preview.roleInstance);
        assertEquals((Double) 10.0, preview.sampling.fixedRate.percentage);
        assertEquals(60, preview.heartbeat.intervalSeconds);
        assertEquals(3, preview.jmxMetrics.size());
        assertEquals("java.lang:type=Threading", preview.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", preview.jmxMetrics.get(0).attribute);
        assertEquals("Thread Count", preview.jmxMetrics.get(0).display);
        assertEquals(ImmutableMap.of("threshold", "error"), preview.instrumentation.get("logging"));
        assertEquals("myproxy", preview.httpProxy.host);
        assertEquals(8080, preview.httpProxy.port);

        assertEquals("file", preview.selfDiagnostics.destination);
        assertEquals("/var/log/applicationinsights", preview.selfDiagnostics.directory);
        assertEquals("error", preview.selfDiagnostics.level);
        assertEquals(10, preview.selfDiagnostics.maxSizeMB);
    }

    @Test
    public void shouldUseDefaults() throws IOException {

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        Configuration configuration = jsonAdapter.fromJson("{}");

        InstrumentationSettings instrumentationSettings = configuration.instrumentationSettings;
        PreviewConfiguration preview = instrumentationSettings.preview;

        assertEquals(null, instrumentationSettings.connectionString);
        assertEquals(null, preview.roleName);
        assertEquals(null, preview.roleInstance);
        assertEquals(900, preview.heartbeat.intervalSeconds);
        assertEquals(0, preview.jmxMetrics.size());
        assertEquals(0, preview.instrumentation.size());
    }

    @Test
    public void shouldOverrideRoleNameWithEnvVar() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_ROLE_NAME", "testrolename");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("testrolename", configuration.instrumentationSettings.preview.roleName);
    }

    private static Configuration loadConfiguration() throws IOException {
        CharSource json = Resources.asCharSource(Resources.getResource("ApplicationInsights.json"), Charsets.UTF_8);
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        return jsonAdapter.fromJson(json.read());
    }
}
