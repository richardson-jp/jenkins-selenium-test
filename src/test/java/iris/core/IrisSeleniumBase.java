package iris.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author vickery_b & Ken
 * 
 * @Notes Abstract class to implement Selenium. Sets up a WebDriver based upon
 *        the parameter given in the constructor. To use: inherit from this
 *        class, create a default constructor calling the one constructor in
 *        this class, passing in the browser type and a path to the driver.
 *
 */
public abstract class IrisSeleniumBase {
	/** Selenium WebDriver object. */
	private WebDriver driver;

	/** Time out to wait for an operation on the web driver to complete. */
	private int timeout;

	/** Logger for the subclass. */
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** Logger for this base class. */
	private final Logger baseLog = LoggerFactory.getLogger(IrisSeleniumBase.class);

	/** path to physical driver file. */
	private String driverPath;

	/** The browser as defined by the Enum {@link Browser}. */
	private Browser browser;

	/** Internal structure to hold input data. */
	private Map<Object, Object> data = null;

	/** Base URL, used at create a cookie to login */
	private String baseURL;

	private String gridURL = "http://10.252.36.52:4444/wd/hub";

	private static ChromeDriverService service;

	/**
	 * Default Constructor - use VM arg to either go local firefox (default) or
	 * -Dbrowser=IR_REMOTE to use Selenium Grid via Jenkins (this is for Jenkins
	 * builds to auto run regression tests). Grid URL is also hard coded but can
	 * be overridden with the -DgridURL=<new url> VM arg.
	 */
	public IrisSeleniumBase() {
		if ((System.getProperty("browser") != null)
				&& (System.getProperty("browser").equalsIgnoreCase("CHROME_REMOTE"))) {
			this.driverPath = null;
			this.browser = Browser.CHROME_REMOTE;
		} else {
			this.driverPath = null;
			this.browser = Browser.CHROME;
		}

		if (System.getProperty("gridURL") != null) {
			gridURL = System.getProperty("gridURL");
		}

		if (System.getProperty("baseURL") != null) {
			baseURL = System.getProperty("baseURL");
		} else {
			baseLog.info("baseURL not provided, using default value of http://10.252.134.232:8080/Iris");
			baseURL = "http://10.252.134.232:8080/Iris";
		}
	}

	/**
	 * Get the WebDriver.
	 * 
	 * @return The current web driver.
	 */
	protected WebDriver getDriver() {
		return driver;
	}

	/**
	 * Get the logger.
	 * 
	 * @return The logger of the subclass.
	 */
	protected Logger getLogger() {
		return log;
	}

	/**
	 * Get the value of one data item. Each test class has an associated data
	 * file. This data file is the same name as the java class. For every java
	 * class there should be a text data file, this will contain a String key
	 * value mapping.
	 * 
	 * @param key
	 *            The key to the key/value pair.
	 * @return Value defined by the key.
	 */
	protected String getData(final String key) {
		if (null == data) {
			baseLog.info("data is null. Not populating the map");
		}
		if (data.containsKey(key)) {
			return (String) data.get(key);
		} else {
			throw new IllegalArgumentException("Key not found in data: " + key);
		}
	}

	/**
	 * Get the browser operation timeout.
	 * 
	 * @return number of seconds that the timeout is set to.
	 */
	protected int getTimeout() {
		return timeout;
	}

	/**
	 * Set the browser operation timeout.
	 * 
	 * @param timeout
	 *            the number of seconds.
	 */
	protected void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	private void outputLogStatus() {
		System.out.format("Logging levels.  Error:%b, Warn:%b, Info:%b, Debug:%b, Trace:%b\n", baseLog.isErrorEnabled(),
				baseLog.isWarnEnabled(), baseLog.isInfoEnabled(), baseLog.isDebugEnabled(), baseLog.isTraceEnabled());

	}

	/**
	 * Set up the driver and the data.
	 */
	@Before
	public void before() {
		try {
			outputLogStatus();
			baseLog.info("============ Start Test ============");

			if (null != driverPath) {
				System.setProperty(browser.toString(), driverPath);
			}
			// If you comment out the line below then it forces it to run in the
			browser = Browser.CHROME_REMOTE;

			switch (browser) {
			case CHROME:
				// Locally
				service = new ChromeDriverService.Builder()
						.usingDriverExecutable(new File("C:\\Selenium\\chromedriver.exe")).usingAnyFreePort().build();
				service.start();
				driver = new RemoteWebDriver(service.getUrl(), DesiredCapabilities.chrome());
				break;
			case CHROME_REMOTE:
				// For the linux box
				System.setProperty("webdriver.chrome.driver", "D:\\Selenium\\chromedriver.exe");
				// For a local CHROME_REMOTE use this:
				// System.setProperty("webdriver.chrome.driver",
				// "C:\\Users\\vickery_b-r\\Downloads\\chromedriver.exe");
				driver = new RemoteWebDriver(new URL(gridURL), DesiredCapabilities.chrome());
				break;
			case FIREFOX:
				driver = new FirefoxDriver();
				break;
			case IE:
				driver = new InternetExplorerDriver();
				break;
			default:
				throw new Exception("Browser type not supported");
			}

			data = new Properties();

			String propFileName = this.getClass().getSimpleName() + ".txt";
			String propPackage = this.getClass().getPackage().getName() + ".data";
			String fn = "/" + propPackage.replace(".", "/") + '/' + propFileName;

			baseLog.info("If data file exists, then it will be loaded.  Looking for file: " + fn);

			URL url = this.getClass().getResource(fn);
			if (null != url) {
				InputStream inputStream = url.openStream();

				if (inputStream != null) {
					((Properties) data).load(inputStream);
					baseLog.info("Found data file, loaded " + data.size() + " item(s).");
				} else {
					throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
				}
			} else {
				baseLog.info("No datafile found:" + fn);
			}

			driver.manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);
		} catch (Exception e) {
			baseLog.error(e.getMessage());
		}
		baseLog.info("Browser open");
	}

	public String getBaseURL() {
		return this.baseURL;
	}

	public void setBaseURL(final String baseURL) {
		this.baseURL = baseURL;
	}

	/**
	 * Clean up.
	 * 
	 * @throws Exception
	 *             when the driver fails to quit.
	 */
	@After
	public void after() throws Exception {

		baseLog.info("============ Test Finished ============");
		try {
			if (browser == Browser.CHROME) {
				service.stop();
			}
			if (null != driver) {
				driver.quit();
			}
		} catch (Exception e) {
			baseLog.error(e.getMessage());
		}
	}

	/**
	 * @return The browser the test is using.
	 */
	public Browser getBrowser() {
		return browser;
	}

}