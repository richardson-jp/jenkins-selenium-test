package selenium;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import iris.core.IrisAbstractSelenium;

public class FilterTests extends IrisAbstractSelenium{
	
	/**
	 * Create a DWITHIN filter and apply it.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Ignore
	@Test
	public void createDWITHINFilter() throws IOException, InterruptedException {
		
		WebDriver driver = loadIrisCesiumMap("quick test");
        
		// Wait for then click the filter
        waitForThenClickXpath(driver, getData("FilterButton"));
        
        // Wait for then click the advanced filter options
        waitForThenClickXpath(driver, getData("AdvancedFilterOptions"));
        
        // This will be the name of your filter
        waitForClearTextThenSet(driver, By.xpath(getData("FilterName")), getData("FilterNameKeys"));
        
        // The filter to be applied
        waitForClearTextThenSet(driver, By.xpath(getData("FilterToApply")), getData("FilterToApplyKeys"));

        // Apply the filter
        waitForThenClickXpath(driver, getData("ApplyTheFilter"));
        
        sleep(4000);
        
        quitDriver("quick test");
	}

}
