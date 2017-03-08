package selenium;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

import iris.core.IrisAbstractSelenium;

public class PleaseGodPleaseWork extends IrisAbstractSelenium{

	@Test
	public void pleaseWork() {
		WebDriver driver = getDriver();
		driver.get("http://www.google.com");
		sleep(5000);
		driver.quit();
	}
	
}
