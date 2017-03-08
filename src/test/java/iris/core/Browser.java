package iris.core;

/**
 * 
 * @author ken
 *
 *         Enum defining browser types
 */
public enum Browser
{
    /**
     * Enum defined.
     */
    CHROME("http://localhost:4444/wd/hub"), FIREFOX(""), IE("c:\\IEDriverServer.exe"), IE_REMOTE("http://iris01:4444/wd/hub"), CHROME_REMOTE("http://iris01:4444/wd/hub");

    /**
     * Variable to store the property name of the specific selenium driver.
     */
    private String property;

    /**
     * Constructor, setting the selenium property.
     * 
     * @param property
     *            Value.
     */
    Browser(final String property)
    {
        this.property = property;
    }

    /**
     * Get the Selenium property.
     * 
     * @return String defining the property.
     */
    public String toString()
    {
        return property;
    }

}
