package com.testcontainers.catalog.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.testcontainers.catalog.BaseIntegrationTest;

import java.io.File;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.VncRecordingContainer;
import org.testcontainers.junit.jupiter.Container;

@ContextConfiguration(initializers = E2ESmokeTest.Initializer.class)
@org.testcontainers.junit.jupiter.Testcontainers
public class E2ESmokeTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private File target = new File(System.getProperty("user.dir"), "target");

    @Container
    public BrowserWebDriverContainer<?> firefox =
            new BrowserWebDriverContainer<>()
                    .withCapabilities(new FirefoxOptions())
                    .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL, target, VncRecordingContainer.VncRecordingFormat.MP4);

    @Test
    void shouldVerifyProductsAreVisibleOnThePage() {
        RemoteWebDriver driver = new RemoteWebDriver(firefox.getSeleniumAddress(), new FirefoxOptions());
        driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
        driver.get("http://host.testcontainers.internal:" + port);
        driver.findElement(By.id("product-list")).isDisplayed();
        int numberOfProductsAvailable =
                driver.findElements(By.className("product")).size();
        assertThat(numberOfProductsAvailable).isEqualTo(3);
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            applicationContext.addApplicationListener((ApplicationListener<WebServerInitializedEvent>) event -> {
                Testcontainers.exposeHostPorts(event.getWebServer().getPort());
            });
        }
    }
}
