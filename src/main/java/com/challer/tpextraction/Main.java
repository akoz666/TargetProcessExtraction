package com.challer.tpextraction;

import com.sun.istack.internal.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Extract all information of User Stories which are contained in Target Process
 *
 * @author Cyril Haller - cyril.haller@gmail.com
 */
public class Main {

    /**
     * Logger Class
     */
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {

        logger.info("TP Extraction is starting");

        Main main = new Main();

        // Perfom process extraction of User Stories
        try {
            main.processUserStoriesExtraction();
        } catch (ExtractionException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        // Generate a static html website as parsing xml information extracted from Target Process
        // main.processWebsiteGeneration();

        logger.info("TP Extraction is completed successfully");
    }

    /**
     * Extract User Stories from Target Process
     *
     * @throws ExtractionException
     */
    private void processUserStoriesExtraction() throws ExtractionException {

        // Configure and call the browser FireFox
        final FirefoxProfile firefoxProfile = new FirefoxProfile();

        firefoxProfile.setPreference("browser.download.folderList", 2);
        firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStorieSaving = ConfigurationProperties.getProperty("outputpathuserstoriesaving");
        firefoxProfile.setPreference("browser.download.dir", outputPathUserStorieSaving + "\\attachments");

        // Get different typemine allowed for the downloading
        final String typemime = ConfigurationProperties.getProperty("tp.attachment.typemime");
        firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", typemime);

        // Disable Firefox's built-in PDF viewer
        firefoxProfile.setPreference("pdfjs.disabled", true);

        // Disable Adobe Acrobat PDF preview plugin
        //firefoxProfile.setPreference("plugin.scan.plid.all", false);
        //firefoxProfile.setPreference("plugin.scan.Acrobat", "99.0");

        // Call the browser Firefox
        final WebDriver driver = new FirefoxDriver(firefoxProfile);
        final UserStorieExtractor extractor = new UserStorieExtractor(driver);

        // Set the timeout at 30 seconds
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        // Connection to Target Process in order to all the use of TP RESTFULL URL
        authentification(driver);

        // Save User Stories content
        extractor.saveUserStories();

        // Close the browser
        driver.quit();
    }

    /**
     * Perform the authentification on Target Process website.
     *
     * @param driver WebDriver - Webdrive used to connect to Target Process
     * @throws ExtractionException
     */
    private void authentification(@NotNull final WebDriver driver) throws ExtractionException {

        logger.debug("AUTHENTIFICATION - IS STARTING");

        // Loading of Target Process information connection
        final String username = ConfigurationProperties.getProperty("tp.username");
        final String password = ConfigurationProperties.getProperty("tp.password");
        final String connectionUrl = ConfigurationProperties.getProperty("tp.connection.url");

        driver.get(connectionUrl);
        driver.findElement(By.id("UserName")).clear();
        driver.findElement(By.id("UserName")).sendKeys(username);
        driver.findElement(By.id("Password")).clear();
        driver.findElement(By.id("Password")).sendKeys(password);
        driver.findElement(By.id("btnLogin")).click();

        // Need to do a tempo in order to load the global context of Target Process
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new ExtractionException("Thread sleep failed", e);
        }

        logger.debug("AUTHENTIFICATION - DONE");
    }
}
