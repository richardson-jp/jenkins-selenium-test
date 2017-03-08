package iris.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * @author vickery_b
 * 
 *         The purpose of this class is to abstract common methods that can be
 *         used throughout the selenium tests.
 * 
 *         Maps are used throughout the framework to locally temporarily store
 *         data.
 *
 */
public abstract class IrisAbstractSelenium extends IrisSeleniumBase {

	private static final Logger logger = LoggerFactory.getLogger(IrisAbstractSelenium.class);

	/**
	 * Login and then Navigate to a page of your specification. Xpath is used
	 * when the link being navigated to is on the left hand side panel rather
	 * than a text link in the main body of the page.
	 * 
	 * @param xPath
	 *            The xpath of the link in the left-hand side panel (if used).
	 * @param testName
	 *            The name of the test in which the login occurs.
	 * @param loginUser
	 *            The username credential used to login.
	 * @return The web driver for this test session.
	 */
	protected WebDriver loadIrisCesiumMap(String testName) {
		getLogger().info("============ " + testName + " Start ============");

		WebDriver driver = getDriver();
		driver.get(getBaseURL());
		resizeTest(driver);

		return driver;
	}

	/**
	 * Submit the element and quit. Normal process for stopping the test.
	 * 
	 * @param testName
	 *            The class name or method name of the test.
	 */
	protected void quitDriver(String testName) {
		// Quit the browser
		getDriver().quit();

		getLogger().info("============ " + testName + " Complete ============");
	}

	/**
	 * Sometimes Selenium simply doesn't work unless the window is maximized...
	 * 
	 * @param driver
	 *            The web driver.
	 */
	protected void resizeTest(WebDriver driver) {
		driver.manage().window().maximize();
	}

	/**
	 * Makes the test wait. This is normally used when interacting with
	 * dynamically refreshing pages.
	 * 
	 * @param milliSeconds
	 *            The number of milliseconds in which to sleep for.
	 */
	protected void sleep(int milliSeconds) {
		try {
			Thread.sleep(milliSeconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends/Types the specified keys (text) into the text field with the
	 * specified ID. Throws AssertionError if the text field with the specified
	 * ID is not found or cannot have its text set.
	 * 
	 * @param searchContext
	 *            The search context in which the text field is to be found.
	 * @param textFieldID
	 *            The ID of the text field to send keys to.
	 * @param keys
	 *            The keys to send to the text field.
	 * @throws AssertionError
	 *             If the text field with the specified ID is not found or
	 *             cannot have its text set.
	 */
	protected void sendKeysToTextBox(SearchContext searchContext, final String textFieldID, final String keys) {
		Predicate<WebElement> condition = new Predicate<WebElement>() {
			public boolean apply(WebElement textField) {
				try {
					textField.sendKeys(keys);
					return true;
				} catch (StaleElementReferenceException e) {
					throw e;
				} catch (NoSuchElementException e) {
					throw e;
				} catch (Exception e) {
					return false;
				}
			}
		};

		try {
			waitForElementCondition(searchContext, By.id(textFieldID), condition);
		}
		// Text field cannot be found or cannot have its text set.
		catch (TimeoutException e) {
			try {
				searchContext.findElement(By.id(textFieldID));
				// Text field cannot have its text set.
				getLogger().info("Fail - Could not set the text of the text field with ID '" + textFieldID + "'.");
				getLogger().error("FAILED - " + e);
				Assert.fail("Exception " + e.getMessage());
			}
			// Text field cannot be found.
			catch (Exception e2) {
				getLogger().info("Fail - Could not find the text field with ID '" + textFieldID + "'.");
				getLogger().error("FAILED - " + e2);
				Assert.fail("Exception " + e2.getMessage());
			}
		}
	}

	/**
	 * Sets the text of the specified text field to the specified value.
	 * 
	 * @param textField
	 *            The text field whose text to set.
	 * @param text
	 *            The text to set.
	 */
	private void setTextField(WebElement textField, String text) {
		textField.clear();

		// Check whether the specified text value
		// is not null before sending text.
		if (null != text) {
			textField.sendKeys(text);
		}
	}

	/**
	 * Clicks/opens the drop-down menu with the specified ID and subsequently
	 * selects the option with the specified ID from that drop-down menu. Throws
	 * TimeoutException if the drop-down field could not be found or opened.
	 * Throws an AssertionError if the drop-down option could not be found or
	 * selected subsequent to clicking/opening the drop-down field.
	 * 
	 * @param searchContext
	 *            The search context in which the drop-down is to be found.
	 * @param dropDownID
	 *            The ID of the drop-down field (or click-able drop-down field
	 *            label).
	 * @param dropDownChoiceID
	 *            The ID of the drop-down option/choice.
	 * @throws TimeoutException
	 *             If the drop-down field could not be found or opened.
	 * @throws AssertionError
	 *             If the drop-down option could not be found or selected
	 *             subsequent to clicking/ opening the drop-down field.
	 */
	protected void clickDropDownAndSelect(final SearchContext searchContext, final String dropDownID,
			final String dropDownChoiceID) {
		waitForLoaded(By.id(dropDownID));
		// Retrieve the ID of the drop-down field in the case that the drop-down
		// label was specified rather than the drop-down field.
		String temp = dropDownID;
		if (temp.endsWith("_label")) {
			temp = temp.substring(0, temp.length() - "_label".length());
		}
		final String dropDownFieldID = temp;

		Function<WebElement, WebElement> condition = new Function<WebElement, WebElement>() {

			public WebElement apply(WebElement dropDownField) {

				if (!isElementEnabled(dropDownField))
					return null;

				WebElement choice = getDriver().findElement(By.id(dropDownChoiceID));
				dropDownField.findElement(By.id(dropDownID)).click();
				return choice;
			}

		};

		final WebElement option = waitForElementConditionAndReturn(searchContext, By.id(dropDownFieldID), condition);

		// Select option from the opened drop-down field.
		try {
			option.click();
		} catch (Exception e) {

			try {
				waitForThenClickId(searchContext, dropDownChoiceID);
			} catch (Exception e2) {
				getLogger().info("Fail - Unable to select the option with ID '" + dropDownChoiceID
						+ "' from the opened drop-down menu.");
				getLogger().error("FAILED - " + e2);
				Assert.fail("Exception " + e2.getMessage());
			}
		}
	}

	/**
	 * Associates the text of the selected choice, of the drop-down field label
	 * with the specified ID, to the key value specified by 'fieldName' within
	 * the specified map. Throws AssertionError if the drop-down field with the
	 * specified ID cannot be found.
	 * 
	 * @param searchContext
	 *            The search context in which the drop-down is found.
	 * @param fieldName
	 *            The key of which the text of the selected choice in the
	 *            drop-down field will be associated to.
	 * @param fieldId
	 *            The ID of the drop-down field label.
	 * @param map2
	 *            The map in which the the selected choice in the drop-down
	 *            field will be associated to the specified 'fieldName'.
	 * @throws AssertionError
	 *             If the drop-down field with the specified ID cannot be found.
	 */
	protected void addContentsToMapFromDropdown(SearchContext searchContext, String fieldName, String fieldId,
			Map<String, String> map2) {
		Function<WebElement, String> function = new Function<WebElement, String>() {
			public String apply(WebElement element) {
				return element.getText();
			}
		};

		try {
			String string = waitForElementConditionAndReturn(searchContext, By.id(fieldId), function);
			map2.put(fieldName, string);
		}
		// Drop-down field with specified ID cannot be found.
		catch (TimeoutException e) {
			getLogger().info("Fail - Drop-down field with ID '" + fieldId + "' not found.");
			getLogger().error("FAILED - " + e);
			Assert.fail("Exception " + e.getMessage());
		}
	}

	/**
	 * Retrieves the text value from the text field with the specified ID. This
	 * text value is compared to the value in the specified map which is
	 * associated to the specified key. Throws a TimeoutException if the text
	 * field with the specified ID cannot be found. Throws an AssertionError if
	 * the text values are not equal.
	 * 
	 * @param searchContext
	 *            The search context in which the drop-down is found,
	 * @param textFieldId
	 *            The ID of the text field.
	 * @param key
	 *            The key whose associated value in the specified map is to be
	 *            compared with the text in the text field.
	 * @param map
	 *            The map containing the value of which to compare with the text
	 *            in the text field.
	 * @throws TimeoutException
	 *             If the text field with the specified ID cannot be found.
	 * @throws AssertionError
	 *             If the text in the text field with the specified ID is not
	 *             equal to the value in the specified map which is associated
	 *             to the specified key.
	 */
	protected void compareValueFromTextBoxToValueInMap(SearchContext searchContext, String textFieldId, String key,
			Map<String, String> map) {
		String stringFromTextBox = waitForLoadedIn(searchContext, By.id(textFieldId)).getAttribute("value");

		if (!stringFromTextBox.trim().equals(map.get(key))) {
			logger.info("String from dropdown: " + stringFromTextBox + " key from map: " + map.get(key));
		}
		assertTrue(stringFromTextBox.trim().equals(map.get(key)));
	}

	/**
	 * Retrieves the text from the selected choice in the drop-down field with
	 * the specified ID. This text value is compared to the value in the
	 * specified map which is associated to the specified key. Throws a
	 * TimeoutException if the drop-down field with the specified ID cannot be
	 * found. Throws an AssertionError if the text values are not equal.
	 * 
	 * @param searchContext
	 *            The search context in which the drop-down is found,
	 * @param dropDownId
	 *            The ID of the drop-down field.
	 * @param key
	 *            The key whose associated value in the specified map is to be
	 *            compared with the text from the selected choice in the
	 *            drop-down field.
	 * @param map
	 *            The map containing the value of which to compare with the text
	 *            in the text field.
	 * @throws TimeoutException
	 *             If the drop-down field with the specified ID cannot be found.
	 * @throws AssertionError
	 *             If the the text from the selected choice in the drop-down
	 *             field with the specified ID is not equal to the value in the
	 *             specified map which is associated to the specified key.
	 */
	protected void compareValueFromDropDownToValueInMap(SearchContext searchContext, String dropDownId, String key,
			Map<String, String> map) {
		String stringFromDropDown = waitForLoadedIn(searchContext, By.id(dropDownId)).getText();

		if (!stringFromDropDown.equals(map.get(key))) {
			logger.info("String from dropdown: " + stringFromDropDown + " key from map: " + map.get(key));
		}
		assertTrue(stringFromDropDown.equals(map.get(key)));
	}

	/**
	 * Clicks the button with the specified ID in a confirmation dialog. Throws
	 * an AssertionError if the button with the specified ID cannot be found.
	 * 
	 * @param searchContext
	 *            The search context in which the button is to be found.
	 * @param btn
	 *            The ID of the button to click.
	 * @param hasAlert
	 *            True: The dialog box has a subsequent alert message. False:
	 *            The dialog box does not have a subsequent alert message.
	 * @throws AssertionError
	 *             If the button with the specified ID cannot be found.
	 */
	protected void confirmSubmission(SearchContext searchContext, String btn, boolean hasAlert) {
		// Allow time for the dialog to load
		WebElement element = waitForLoadedIn(searchContext, By.id(btn));
		Actions actions = new Actions(getDriver());
		actions.moveToElement(element).click().perform();
		sleep(1000);

		// If an alert message appears subsequent to handling the
		// confirmation dialog, accept the alert.
		if (hasAlert) {
			acceptAlert(getDriver());
			sleep(1000);
		}
	}

	/**
	 * Searches for the specified values in the specified table and returns the
	 * list of values which were not found.
	 * 
	 * @param fieldValue
	 *            The values to search for in the specified table.
	 * @param tableId
	 *            The table to search for the value in.
	 * @return A list containing the values which were specified and were not
	 *         found in the table.
	 * @throws StaleElementReferenceException
	 *             If the reference to the table is corrupted/incorrect (likely
	 *             due to a change in the DOM).
	 */
	private boolean valuesInTable(final List<String> fieldValues, WebElement table) {
		List<WebElement> tableRows = table.findElements(By.tagName("td"));

		// Iterate through all elements in the table.
		for (WebElement tableCell : tableRows) {
			// Check whether the current element contains the specified text.
			fieldValues.remove(tableCell.getText());
		}

		return fieldValues.isEmpty();
	}

	/**
	 * Generates a random string of the specified size consisting of
	 * alphanumeric characters.
	 * 
	 * @param size
	 *            The size of the string to generate.
	 * @return The random string which is generated.
	 */
	protected static String generateRandomString(int size) {
		char[] chars = "abcdefghijklmnopqrstuvwxyz123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();

		for (int i = 0; i < size; i++) {
			char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		String output = sb.toString();
		return output;
	}

	/**
	 * Generates a random string of the specified size consisting of numeric
	 * characters.
	 * 
	 * @param size
	 *            The size of the string to generate.
	 * @return The random string which is generated.
	 */
	protected static String generateRandomNumber(int size) {
		char[] chars = "0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < size; i++) {
			char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		String output = sb.toString();
		return output;
	}

	/**
	 * Tests whether the element is visible yet.
	 * 
	 * @param element
	 *            on the page.
	 * @return true or false depending if the element is hidden.
	 */
	protected boolean isVisible(WebElement element) {
		String classValue = element.getAttribute("class");
		String ariaValue = element.getAttribute("aria-hidden");
		return element.isDisplayed() && !classValue.contains("ui-state-hidden") && !ariaValue.contains("true");
	}

	/**
	 * Checks whether a web element is enabled.
	 * 
	 * @param element
	 *            The web element to check.
	 * @return True if the specified web element is enabled. False otherwise.
	 * @throws StaleElementReferenceException
	 *             If the reference to the element is corrupted/incorrect
	 *             (likely due to a change in the DOM).
	 */
	protected boolean isElementEnabled(WebElement element) throws StaleElementReferenceException {
		// Check whether the element is null, whether it is disabled
		// and whether it's class attribute contains the word 'disabled'.
		if (null != element && element.isEnabled() && element.isDisplayed() && (null == element.getAttribute("class")
				|| (null != element.getAttribute("class") && !element.getAttribute("class").contains("disabled")))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks whether the text of the specified element contains the specified
	 * text.
	 * 
	 * @param element
	 *            The web element whose text to compare to the specified text.
	 * @param text
	 *            The text to compare to the text of the specified web element.
	 * @return True if the text of the specified web element contains the
	 *         specified text. False otherwise.
	 */
	private boolean elementTextContains(final WebElement element, final String text) {
		Predicate<String> condition = new Predicate<String>() {
			public boolean apply(String elementText) {
				return elementText.trim().contains(text);
			}
		};
		return applyElementTextPredicate(element, condition);
	}

	/**
	 * Checks whether the text of the specified element is equal to the
	 * specified text.
	 * 
	 * @param element
	 *            The web element whose text to compare to the specified text.
	 * @param text
	 *            The text to compare to the text of the specified web element.
	 * @return True if the text of the specified web element is equal to the
	 *         specified text. False otherwise.
	 */
	private boolean elementTextEquals(final WebElement element, final String text) {
		Predicate<String> condition = new Predicate<String>() {
			public boolean apply(String elementText) {
				return elementText.trim().equalsIgnoreCase(text);
			}
		};
		return applyElementTextPredicate(element, condition);
	}

	/**
	 * Applies the specified predicate to the text of the specified element and
	 * returns the result.
	 * 
	 * @param element
	 *            The web element - the specified predicate will be applied to
	 *            the text of this element.
	 * @param predicate
	 *            The predicate which will be applied to the text of the
	 *            specified web element.
	 * @return The result of applying the specified predicate to the text of the
	 *         the specified element.
	 */
	private boolean applyElementTextPredicate(WebElement element, Predicate<String> predicate) {

		if (null != element.getText() && predicate.apply(element.getText())) {
			return true;
		} else if (null != element.getAttribute("value") && predicate.apply(element.getAttribute("value"))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Verifies that a check-box located by the specified locator is functioning
	 * correctly. Throws an AssertionError if unexpected behaviour is found.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param locator
	 *            The locator of the check-box.
	 * @throws AssertionError
	 *             If unexpected behaviour is found while verifying whether the
	 *             check-box is functioning correctly.
	 */
	protected void verifyCheckBox(SearchContext searchContext, By locator) {
		WebElement element = waitForEnabledIn(searchContext, locator);
		// Check whether the check-box located by the specified locator
		// is checked or unchecked and then click it.
		boolean checked = false;
		if (element.getAttribute("class").contains("check")) {
			checked = true;
		}
		element.click();
		// Check whether the check-box is checked or unchecked subsequent
		// to clicking it.
		boolean checkedAfter = false;
		if (element.getAttribute("class").contains("check")) {
			checkedAfter = true;
		}
		// Verify that the state of the check-box (checked or unchecked)
		// is the same before and after clicking it.
		if (checked != checkedAfter) {
			assertTrue(true);
		} else {
			getLogger().info(getData("Screen") + ": tick box wasn't clicked, problem with the tick box functionality.");
			assertTrue(false);
		}
	}

	/**
	 * Check whether the specified value is not contained within the table with
	 * the specified ID. Throws an AssertionError if the specified value is
	 * found in the table with the specified ID.
	 * 
	 * @param searchContext
	 *            The search context in which the table is to be found.
	 * @param fieldValue
	 *            The value to search for in the table.
	 * @param tableID
	 *            The ID of the table to search for the value in.
	 * @throws AssertionError
	 *             If the specified value is found in the table with the
	 *             specified ID.
	 */
	protected void testFalse(final SearchContext searchContext, final String fieldValue, final String tableId) {
		ArrayList<String> values = new ArrayList<String>();
		values.add(fieldValue);
		testFalse(searchContext, values, tableId);
	}

	/**
	 * Check whether all of the specified values are not contained within the
	 * table with the specified ID. Throws an AssertionError if the specified
	 * value is found in the table with the specified ID.
	 * 
	 * @param searchContext
	 *            The search context in which the table is to be found.
	 * @param fieldValues
	 *            The values to search for in the table.
	 * @param tableID
	 *            The ID of the table to search for the value in.
	 * @throws AssertionError
	 *             If the specified value is found in the table with the
	 *             specified ID.
	 */
	protected void testFalse(final SearchContext searchContext, final List<String> fieldValues, final String tableId) {
		Predicate<WebElement> condition = new Predicate<WebElement>() {
			public boolean apply(WebElement table) {
				return !valuesInTable(fieldValues, table);
			}
		};

		waitForElementCondition(searchContext, By.id(tableId), condition);
	}

	/**
	 * Checks whether the title of the current page (defined as the text with
	 * the 'h2' css selector) is equal to the specified text. Throws an
	 * AssertionError if the text values are not equal or the page title is not
	 * found, within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the page title is to be found.
	 * @param text
	 *            The text to check for.
	 * @throws AssertionError
	 *             If the specified text values are not equal or the page title
	 *             is not found, within 15 seconds of calling this method.
	 */
	protected void testPageTitle(SearchContext searchContext, String text) {

		try {
			waitForTextIn(searchContext, By.cssSelector("h2"), text);
		}
		// Page title not found or title text was not equal to the
		// specified text.
		catch (TimeoutException e) {
			try {
				WebElement element = searchContext.findElement(By.cssSelector("h2"));
				// Title text not equal to the specified text.
				getLogger().info("Fail - Title text was '" + element.getText() + "'. Expected: '" + text + "'.");
				getLogger().error("FAILED - " + e);
				Assert.fail(e.getMessage() + "\nTitle text was '" + element.getText() + "'. Expected: '" + text + "'.");
			}
			// Page title not found.
			catch (Exception e2) {
				getLogger().info("Fail - Page title not found.");
				getLogger().error("FAILED - " + e2);
				Assert.fail(e2.getMessage() + "\nPage title not found.");
			}
		}
	}

	/**
	 * Checks whether the field with the specified ID is disabled. Throws an
	 * AssertionError if the field with the specified ID is not disabled.
	 * 
	 * @param searchContext
	 *            The search context in which the field is to be found.
	 * @param fieldId
	 *            The ID of the field to check.
	 * @throws AssertionError
	 *             If the field with the specified ID is not disabled.
	 */
	protected void checkIfDisabled(SearchContext searchContext, String fieldId) {
		WebElement field = waitForLoadedIn(searchContext, By.id(getData(fieldId)));
		String fieldClass = field.getAttribute("class");
		assertTrue(fieldClass.contains("disabled") || !field.isEnabled());
	}

	/**
	 * Checks whether the field with the specified ID is not disabled. Throws an
	 * AssertionError if the field with the specified ID is disabled.
	 * 
	 * @param searchContext
	 *            The search context in which the field is to be found.
	 * @param fieldId
	 *            The ID of the field to check.
	 * @throws AssertionError
	 *             If the field with the specified ID is disabled.
	 */
	protected void checkIfNotDisabled(SearchContext searchContext, String fieldId) {
		WebElement field = waitForLoadedIn(searchContext, By.id(getData(fieldId)));
		String fieldClass = field.getAttribute("class");
		assertFalse(fieldClass.contains("disabled") || !field.isEnabled());
	}

	/**
	 * Accepts a pop-up alert which has been presented.
	 * 
	 * @param driver
	 *            The web driver.
	 */
	protected void acceptAlert(WebDriver driver) {
		Alert alert = driver.switchTo().alert();
		alert.accept();
	}

	/**
	 * Dismisses a pop-up alert which has been presented.
	 * 
	 * @param driver
	 *            The web driver.
	 */
	protected void dismissAlert(WebDriver driver) {
		Alert alert = driver.switchTo().alert();
		alert.dismiss();
	}

	/**
	 * Waits until the text field with the specified ID is enabled to have its
	 * text set and then sets its text. Throws a TimeoutException if the text
	 * field was not found or its text could not be set.
	 * 
	 * @param searchContext
	 *            The search context in which the text field is to be found.
	 * @param textFieldID
	 *            The ID of the text field whose text to set.
	 * @param text
	 *            The text to set.
	 * @throws TimeoutException
	 *             If the text field was not found or its text could not be set.
	 */
	protected void waitForClearTextThenSet(SearchContext searchContext, final By locator, final String text) {

		Predicate<WebElement> condition = new Predicate<WebElement>() {
			public boolean apply(WebElement element) {

				try {
					setTextField(element, text);
					return true;
				} catch (StaleElementReferenceException e) {
					throw e;
				} catch (NoSuchElementException e) {
					throw e;
				} catch (Exception e) {
					return false;
				}
			}
		};

		waitForElementCondition(searchContext, locator, condition);
	}

	/**
	 * Waits for the specified function as applied to the specified input to
	 * return a non-null, non-false value.
	 * 
	 * @param
	 * 			<P>
	 *            The type of object which the specified function is applied to.
	 * @param <T>
	 *            The type of object returned by the specified function.
	 * @param input
	 *            The input to apply the specified predicate to.
	 * @param function
	 *            The function to be applied to the specified input.
	 * @return The first non-null, non-false value returned by the specified
	 *         function, as applied to the specified input.
	 * @throws TimeoutException
	 *             If the specified function, as applied to the specified input,
	 *             does not return true within 15 seconds of calling this
	 *             method.
	 */
	protected <P, T> T waitForConditionAndReturn(final P input, final Function<P, T> function) {
		FluentWait<P> wait = new FluentWait<P>(input).withTimeout(15, TimeUnit.SECONDS).pollingEvery(500,
				TimeUnit.MILLISECONDS);

		// Check whether the specified function, as applied to the specified
		// input, initially returns a non-null, non-false value. If this is
		// true, return that value immediately.
		try {
			T returnObject = function.apply(input);

			if (null != returnObject) {
				return returnObject;
			}
		} catch (Exception e) {
		}

		// Wait until the specified function, as applied to the specified,
		// input, returns a non-null, non-false value.
		return wait.until(function);
	}

	/**
	 * Waits for the specified predicate as applied to the specified input to
	 * return true.
	 * 
	 * @param
	 * 			<P>
	 *            The type of object which the specified predicate is applied
	 *            to.
	 * @param input
	 *            The input to apply the specified predicate to.
	 * @param condition
	 *            The predicate to be applied to the specified input.
	 * @throws TimeoutException
	 *             If the specified predicate, as applied to the specified
	 *             input, does not return true within 15 seconds of calling this
	 *             method.
	 */
	protected <P> void waitForCondition(final P input, final Predicate<P> condition) {
		// Create a function which takes the specified input and returns
		// the boolean result of applying the specified predicate to the
		// specified input.
		Function<P, Boolean> function = new Function<P, Boolean>() {
			public Boolean apply(P input) {
				return condition.apply(input);
			}
		};

		// Wait for the created function to return a value of true.
		waitForConditionAndReturn(input, function);
	}

	/**
	 * Waits for a specified function, which takes the web element located by
	 * the specified locator as a parameter and returns an object, to return a
	 * non-null value and then returns that value.
	 * 
	 * @param <T>
	 *            The type of object returned by the specified function.
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param elementLocator
	 *            The locator of the element for which the specified function is
	 *            applied.
	 * @param function
	 *            The function to apply.
	 * @return The first non-null value returned by the function, as applied to
	 *         the element located by the specified locator.
	 * @throws TimeoutException
	 *             If the specified function, as applied to to the element
	 *             located by the specified locator, does not return a non-null
	 *             value within 15 seconds of calling this method.
	 */
	protected <T> T waitForElementConditionAndReturn(final SearchContext searchContext, final By elementLocator,
			final Function<WebElement, T> function) {
		final WebElement outerElement = waitForLoadedIn(searchContext, elementLocator);

		Function<SearchContext, T> elementCondFunc = new Function<SearchContext, T>() {
			private WebElement element = outerElement;

			public T apply(SearchContext searchContext) {

				try {
					T returnObject = function.apply(element);

					// Check if the returned object is non-null.
					if (null != returnObject) {
						return returnObject;
					}
				}
				// Reference to the element is corrupted/incorrect (likely due
				// to
				// a change in the DOM) and therefore needs to be updated.
				catch (StaleElementReferenceException e) {
					element = waitForLoadedIn(searchContext, elementLocator);
				} catch (NoSuchElementException e) {
					element = waitForLoadedIn(searchContext, elementLocator);
				} catch (Exception e) {
					return null;
				}

				return null;
			}
		};

		return waitForConditionAndReturn(searchContext, elementCondFunc);
	}

	/**
	 * Waits for a specified predicate, which takes the web element located by
	 * the specified locator as a parameter, to return true.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param elementLocator
	 *            The locator of the element for which the specified predicate
	 *            is applied.
	 * @param predicate
	 *            The predicate to apply.
	 * @throws TimeoutException
	 *             If the specified predicate, as applied to to the element
	 *             located by the specified locator, does not return true within
	 *             15 seconds of calling this method.
	 */
	protected void waitForElementCondition(final SearchContext searchContext, final By elementLocator,
			final Predicate<WebElement> predicate) {
		final WebElement outerElement = waitForLoadedIn(searchContext, elementLocator);

		Predicate<SearchContext> condition = new Predicate<SearchContext>() {
			private WebElement element = outerElement;

			public boolean apply(SearchContext searchContext) {
				try {
					return predicate.apply(element);
				}
				// Reference to the element is corrupted/incorrect (likely due
				// to
				// a change in the DOM) and therefore needs to be updated.
				catch (StaleElementReferenceException e) {
					element = waitForLoadedIn(searchContext, elementLocator);
					return false;
				} catch (NoSuchElementException e) {
					element = waitForLoadedIn(searchContext, elementLocator);
					return false;
				} catch (Exception e) {
					return false;
				}
			}
		};

		waitForCondition(searchContext, condition);
	}

	/**
	 * Waits for the specified value to appear anywhere in the table with the
	 * specified ID. Throws a TimeoutException if the table with the specified
	 * ID cannot be found or the the specified value cannot be found in the
	 * table.
	 * 
	 * @param searchContext
	 *            The searchContext in which the table is found.
	 * @param fieldValue
	 *            The value to check for in the table.
	 * @param tableId
	 *            The ID of the table to search for the value in.
	 * @throws TimeoutException
	 *             If the table cannot be found or the specified value cannot be
	 *             found in the table.
	 */
	protected void waitForValuesInTableAndAssert(final SearchContext searchContext, final String fieldValue,
			final String tableId) {
		ArrayList<String> values = new ArrayList<String>();
		values.add(fieldValue);
		waitForValuesInTableAndAssert(searchContext, values, tableId);
	}

	/**
	 * Waits for all of the specified values to appear anywhere in the table
	 * with the specified ID. Throws a TimeoutException if the table with the
	 * specified ID cannot be found or the the specified value cannot be found
	 * in the table.
	 * 
	 * @param searchContext
	 *            The searchContext in which the table is found.
	 * @param fieldValue
	 *            The value to check for in the table.
	 * @param tableId
	 *            The ID of the table to search for the value in.
	 * @throws TimeoutException
	 *             If the table cannot be found or the specified value cannot be
	 *             found in the table.
	 */
	protected void waitForValuesInTableAndAssert(final SearchContext searchContext, final List<String> fieldValues,
			final String tableId) {
		Predicate<WebElement> condition = new Predicate<WebElement>() {
			public boolean apply(WebElement table) {
				return valuesInTable(fieldValues, table);
			}
		};

		waitForElementCondition(searchContext, By.id(tableId), condition);
	}

	/**
	 * Waits for the web element, which is located by the specified locator, to
	 * be loaded within the specified search context. Throws a TimeoutException
	 * if the element located by the specified locator is not found within 15
	 * seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the web element is found.
	 * @param locator
	 *            The locator of the web element.
	 * @return The loaded web element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not found
	 *             within 15 seconds.
	 */
	protected WebElement waitForLoadedIn(final SearchContext searchContext, final By locator) {
		Function<SearchContext, WebElement> function = new Function<SearchContext, WebElement>() {
			public WebElement apply(SearchContext context) {
				try {
					return context.findElement(locator);
				} catch (Exception e) {
					return null;
				}
			}
		};

		return waitForConditionAndReturn(searchContext, function);
	}

	/**
	 * Waits for the web element, which is located by the specified locator, to
	 * be loaded. Throws a TimeoutException if the element located by the
	 * specified locator is not found within 15 seconds of calling this method.
	 * 
	 * @param locator
	 *            The locator of the web element.
	 * @return The loaded web element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not found
	 *             within 15 seconds.
	 */
	protected WebElement waitForLoaded(final By locator) {
		return waitForLoadedIn(getDriver(), locator);
	}

	/**
	 * Waits for the web element, which is located by the specified locator, to
	 * be unloaded within the specified search context. Throws a
	 * TimeoutException if the element located by the specified locator is not
	 * unloaded within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the web element is found.
	 * @param locator
	 *            The locator of the web element.
	 * @return The loaded web element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not found
	 *             within 15 seconds.
	 */
	protected void waitForUnloadedIn(final SearchContext searchContext, final By locator) {

		Predicate<SearchContext> condition = new Predicate<SearchContext>() {
			public boolean apply(SearchContext driver) {
				try {
					searchContext.findElement(locator);
					return false;
				} catch (Exception e) {
					return true;
				}
			}
		};

		waitForCondition(searchContext, condition);
	}

	/**
	 * Waits for the web element, which is located by the specified locator, to
	 * be unloaded. Throws a TimeoutException if the element located by the
	 * specified locator is not unloaded within 15 seconds of calling this
	 * method.
	 * 
	 * @param locator
	 *            The locator of the web element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not
	 *             unloaded within 15 seconds of calling this method.
	 */
	protected void waitForUnloaded(final By locator) {
		waitForUnloadedIn(getDriver(), locator);
	}

	/**
	 * Waits for the web element, which is located in the specified search
	 * context by the specified locator, to be enabled. An enabled web element
	 * is defined as a web element which is loaded and can be interacted with.
	 * Throws a TimeoutException if the element located by the specified locator
	 * is not enabled within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param locator
	 *            The locator of the web element.
	 * @return The enabled element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not
	 *             enabled within 15 seconds of calling this method.
	 */
	protected WebElement waitForEnabledIn(SearchContext searchContext, final By locator) {
		Function<WebElement, WebElement> function = new Function<WebElement, WebElement>() {
			public WebElement apply(WebElement element) {
				if (isElementEnabled(element)) {
					return element;
				}
				return null;
			}
		};

		return waitForElementConditionAndReturn(searchContext, locator, function);
	}

	/**
	 * Waits for the web element, which is located in the specified search
	 * context by the specified locator, to be visible. A visible web element is
	 * defined as a web element which is loaded and visible/displayed on the web
	 * page. Throws a TimeoutException if the element located by the specified
	 * locator is not hidden within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param locator
	 *            The locator of the web element.
	 * @return The visible element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not
	 *             visible within 15 seconds of calling this method.
	 */
	protected WebElement waitForVisible(SearchContext searchContext, final By locator) {
		Function<WebElement, WebElement> function = new Function<WebElement, WebElement>() {
			public WebElement apply(WebElement element) {
				return isVisible(element) ? element : null;
			}
		};

		return waitForElementConditionAndReturn(searchContext, locator, function);
	}

	/**
	 * Waits for the web element, which is located in the specified search
	 * context by the specified locator, to be hidden. A hidden web element is
	 * defined as a web element which is loaded and not visible/displayed on the
	 * web page. Throws a TimeoutException if the element located by the
	 * specified locator is not hidden within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param locator
	 *            The locator of the web element.
	 * @return The hidden element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not hidden
	 *             within 15 seconds of calling this method.
	 */
	protected WebElement waitForHidden(SearchContext searchContext, final By locator) {
		Function<WebElement, WebElement> function = new Function<WebElement, WebElement>() {
			public WebElement apply(WebElement element) {
				return isVisible(element) ? null : element;
			}
		};

		return waitForElementConditionAndReturn(searchContext, locator, function);
	}

	/**
	 * Waits for the web element, which is located by the specified locator, to
	 * be enabled. An enabled web element is defined as a web element which is
	 * loaded and can be interacted with. Throws a TimeoutException if the
	 * element located by the specified locator is not enabled within 15 seconds
	 * of calling this method.
	 * 
	 * @param locator
	 *            The locator of the web element.
	 * @return The enabled element.
	 * @throws TimeoutException
	 *             If the element located by the specified locator is not
	 *             enabled within 15 seconds of calling this method.
	 */
	protected WebElement waitForEnabled(final By locator) {
		return waitForEnabledIn(getDriver(), locator);
	}

	/**
	 * Waits for the text of the text field (located in the specified search
	 * context by the specified locator in) to be equal to the specified value.
	 * Throws a TimeoutException if the text field is not found or the specified
	 * value within the text field is not found, within 15 seconds of calling
	 * this method.
	 * 
	 * @param searchContext
	 *            The search context in which the text field is to be found.
	 * @param locator
	 *            The locator of the text field.
	 * @param text
	 *            The text value to wait for.
	 * @throws TimeoutException
	 *             If the text field is not found or the specified value within
	 *             the text field is not found, within 15 seconds of calling
	 *             this method.
	 */
	protected void waitForTextIn(final SearchContext searchContext, final By locator, final String text) {
		Predicate<WebElement> condition = new Predicate<WebElement>() {
			public boolean apply(WebElement textField) {
				return elementTextEquals(textField, text);
			}
		};

		waitForElementCondition(getDriver(), locator, condition);
	}

	/**
	 * Waits for the text of the text field (located by the specified locator)
	 * to be equal to the specified value. Throws a TimeoutException if the text
	 * field is not found or the specified value within the text field is not
	 * found, within 15 seconds of calling this method.
	 * 
	 * @param locator
	 *            The locator of the text field.
	 * @param text
	 *            The text value to wait for.
	 * @throws TimeoutException
	 *             If the text field is not found or the specified value within
	 *             the text field is not found, within 15 seconds of calling
	 *             this method.
	 */
	protected void waitForText(final By locator, final String text) {
		waitForTextIn(getDriver(), locator, text);
	}

	/**
	 * Waits for the text of the text field (located in the specified search
	 * context by the specified locator) to contain the specified value. Throws
	 * a TimeoutException if the text field is not found or the specified value
	 * within the text field is not found, within 15 seconds of calling this
	 * method.
	 * 
	 * @param searchContext
	 *            The search context in which the text field is to be found.
	 * @param locator
	 *            The locator of the text field.
	 * @param text
	 *            The text value to wait for.
	 * @throws TimeoutException
	 *             If the text field is not found or the specified value within
	 *             the text field is not found, within 15 seconds of calling
	 *             this method.
	 */
	protected void waitForContainsTextIn(SearchContext searchContext, final By locator, final String text) {
		Predicate<WebElement> condition = new Predicate<WebElement>() {
			public boolean apply(WebElement textField) {
				return elementTextContains(textField, text);
			}
		};

		waitForElementCondition(getDriver(), locator, condition);
	}

	/**
	 * Waits for the text of the text field (located by the specified locator)
	 * to contain the specified value. Throws a TimeoutException if the text
	 * field is not found or the specified value within the text field is not
	 * found, within 15 seconds of calling this method.
	 * 
	 * @param locator
	 *            The locator of the text field.
	 * @param text
	 *            The text value to wait for.
	 * @throws TimeoutException
	 *             If the text field is not found or the specified value within
	 *             the text field is not found, within 15 seconds of calling
	 *             this method.
	 */
	protected void waitForContainsText(final By locator, final String text) {
		waitForContainsTextIn(getDriver(), locator, text);
	}

	/**
	 * Wait for an element located by the specified locator to be click-able and
	 * and subsequently clicks it. Throws a TimeoutException if the element is
	 * not found or cannot be clicked, within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param locator
	 *            The locator of the element.
	 * @return The element that was clicked.
	 * @throws TimeoutException
	 *             If the element is not found or cannot be clicked, within 15
	 *             seconds of calling this method.
	 */
	protected WebElement waitForThenClick(SearchContext searchContext, final By locator) {
		Function<WebElement, WebElement> function = new Function<WebElement, WebElement>() {
			public WebElement apply(WebElement element) {

				if (isElementEnabled(element)) {
					element.click();
					return element;
				}

				return null;
			}
		};

		return waitForElementConditionAndReturn(searchContext, locator, function);
	}

	/**
	 * Wait for the element with the specified ID to be click-able and
	 * subsequently clicks it. Throws a TimedoutException if the element is not
	 * found or cannot be clicked, within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param id
	 *            The ID of the element.
	 * @return The element that was clicked.
	 * @throws TimedoutException
	 *             If the element is not found or cannot be clicked, within 15
	 *             seconds of calling this method.
	 */
	protected WebElement waitForThenClickId(SearchContext searchContext, final String id) {
		return waitForThenClick(searchContext, By.id(id));
	}

	/**
	 * Wait for the element with the specified xpath to be click-able and
	 * subsequently clicks it. Throws a TimedoutException if the element is not
	 * found or cannot be clicked, within 15 seconds of calling this method.
	 * 
	 * @param searchContext
	 *            The search context in which the element is to be found.
	 * @param xpath
	 *            The xpath of the element.
	 * @return The element that was clicked.
	 * @throws TimedoutException
	 *             If the element is not found or cannot be clicked, within 15
	 *             seconds of calling this method.
	 */
	protected WebElement waitForThenClickXpath(SearchContext searchContext, final String xpath) {
		return waitForThenClick(searchContext, By.xpath(xpath));
	}

}