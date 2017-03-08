package iris.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeleniumResultProcessor extends TestWatcher
{

    /** Logger for the subclass. */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Selenium WebDriver object. */
    private WebDriver driver;

    private DriverService service;

    public void setDriver(final WebDriver driver)
    {

        if (null == driver)
        {
            log.error("WebDriver passed in is null");
        }
        else
        {
            this.driver = driver;
            log.debug("WebDriver set");
        }
    }

    public void setService(final DriverService service)
    {
        this.service = service;
    }

    @Override
    protected void failed(Throwable e, Description description)
    {
        String methodName = description.getClassName() + "#" + description.getMethodName();
        log.info("Taking screenshot of failed test : " + methodName);
        File srnShot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String filenameSS = "target/failsafe-reports/ScreenShot-" + methodName + ".png";
        try
        {
            FileUtils.copyFile(srnShot, new File(filenameSS));
        }
        catch (IOException e1)
        {
            log.error("Failed to move screenshot\n" + e1.getStackTrace());
        }

        log.info("Screenshot taken of failed test: " + filenameSS);

        String filenameSrc = "target/failsafe-reports/SOURCE-" + methodName + ".html";
        try
        {
            Files.write(Paths.get(filenameSrc), driver.getPageSource().getBytes());
        }
        catch (IOException e1)
        {
            log.error("Failed to create source file\n" + e1.getStackTrace());
        }

        super.failed(e, description);

        driver.quit();

        if (service != null)
        {
            try
            {
                service.stop();
            }
            catch (Exception e2)
            {
                log.error("Failed to stop service\n" + e2.getStackTrace());
            }
        }

    }

    @Override
    protected void succeeded(Description description)
    {
        super.succeeded(description);
        driver.quit();
        if (service != null)
        {
            try
            {
                service.stop();
            }
            catch (Exception e2)
            {
                log.error("Failed to stop service\n" + e2.getStackTrace());
            }
        }

    }

}
