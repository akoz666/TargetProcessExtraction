package com.challer.tpextraction;

import com.sun.istack.internal.NotNull;
import org.apache.commons.io.FileUtils;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class provides methods to extract User Stories from Target Process.
 *
 * @author Cyril Haller - cyril.haller@gmail.com
 */
public class UserStoryExtractor {

    /**
     * Logger Class
     */
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(UserStoryExtractor.class);

    /**
     * Singleton instance
     */
    private static UserStoryExtractor instance;

    /**
     * Webdriver used to connect to Target Process
     */
    private final WebDriver driver;

    /**
     * Date/Hour of starting extraction
     */
    private final String startDateTime;

    /**
     * Constructor
     *
     * @param startDateTime Start time of extractor performing
     */
    private UserStoryExtractor(final @NotNull String startDateTime) {
        this.startDateTime = startDateTime;

        // Configure and call the browser FireFox
        final FirefoxProfile firefoxProfile = new FirefoxProfile();

        firefoxProfile.setPreference("browser.download.folderList", 2);
        firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStoriesSaving = ConfigurationProperties.getProperty("outputpathuserstoriessaving") + "\\" + startDateTime;
        // Create directories
        final File usDirectory = new File(outputPathUserStoriesSaving);
        usDirectory.mkdir();
        final File attachmentsDirectory = new File(outputPathUserStoriesSaving + "\\attachments");
        attachmentsDirectory.mkdir();
        final File imagesDirectory = new File(outputPathUserStoriesSaving + "\\images");
        imagesDirectory.mkdir();

        firefoxProfile.setPreference("browser.download.dir", outputPathUserStoriesSaving + "\\attachments");

        // Get different typemine allowed for the downloading
        final String typemime = ConfigurationProperties.getProperty("tp.attachment.typemime");
        firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", typemime);

        // Disable Firefox's built-in PDF viewer
        firefoxProfile.setPreference("pdfjs.disabled", true);

        // Disable Adobe Acrobat PDF preview plugin
        //firefoxProfile.setPreference("plugin.scan.plid.all", false);
        //firefoxProfile.setPreference("plugin.scan.Acrobat", "99.0");

        // Call the browser Firefox
        driver = new FirefoxDriver(firefoxProfile);

        // Set the timeout at 30 seconds
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    /**
     * Singleton pattern
     *
     * @return singleton of UserStorieExtractor
     */
    public static UserStoryExtractor getInstance(final @NotNull String startDateTime) {
        if (instance == null) {
            instance = new UserStoryExtractor(startDateTime);
        }

        return instance;
    }

    /**
     * Save all User Stories referenced in the csv file.
     *
     * @throws ExtractionException
     */
    public void saveUserStories() throws ExtractionException {

        // Get the csv User Stories list
        final String inputUserStoriesListFile = ConfigurationProperties.getProperty("inputuserstorieslistfile");

        InputStream userStoriesInputStream;
        try {
            userStoriesInputStream = new FileInputStream(inputUserStoriesListFile);
        } catch (FileNotFoundException e) {
            throw new ExtractionException("Failed to open file " + inputUserStoriesListFile, e);
        }

        InputStreamReader userStoriesInputStreamReader = new InputStreamReader(userStoriesInputStream);
        BufferedReader br = new BufferedReader(userStoriesInputStreamReader);
        try {
            String userStoryId;
            while ((userStoryId = br.readLine()) != null) {
                try {
                    saveUserStorie(userStoryId);
                } catch (ExtractionException e) {
                    logger.error("Failed to save US " + userStoryId, e);
                }
            }

            br.close();
        } catch (IOException e) {
            throw new ExtractionException("Failed to read file " + inputUserStoriesListFile, e);
        }
    }

    /**
     * Save the content of an User Story.
     *
     * @param userStoryId - ID a the User Storie to save
     * @throws ExtractionException
     * @throws IOException
     */
    private void saveUserStorie(final @NotNull String userStoryId) throws ExtractionException, IOException {

        logger.debug("US SAVING - IS STARTING - US " + userStoryId);

        // Get the url to download the content of an User Story
        final String userStoryUrl = ConfigurationProperties.getProperty("tp.userstory.url");

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStoriesSaving = ConfigurationProperties.getProperty("outputpathuserstoriessaving") + "\\" + startDateTime;

        // Connection to Target Process URL allowing to download content of an User Storie
        logger.debug("GETTING US CONTENT - " + userStoryUrl + "/" + userStoryId);
        driver.get(userStoryUrl + "/" + userStoryId);

        // Downloading the content of the User Story
        String userStoryContent = driver.getPageSource();

        // It has to detect if the User Story references one or many images and attachments.
        // If it is the case, then images and attachments have also to be saved.
        try {
            detectAndSaveImagesOfUserStorie(userStoryId, userStoryContent);
            detectAndSaveAttachmentOfUserStorie(userStoryId);
        } catch (IOException | JDOMException e) {
            throw new ExtractionException("Failed to save images of the US-" + userStoryId, e);
        }

        // Now it has to save the content of the User Story
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPathUserStoriesSaving + "\\us-" + userStoryId + ".xml");
            writer.write(userStoryContent);
        } catch (IOException ex) {
            throw new ExtractionException("Failed to write the file " + outputPathUserStoriesSaving + "\\us-" + userStoryId + ".xml", ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        logger.debug("US SAVING - DONE - US " + userStoryId);
    }

    /**
     * Detect and save images contained in the User Story.
     *
     * @param userStoryId      - User Story ID
     * @param userStoryContent - HTML content of the User Story.
     * @throws IOException
     * @throws JDOMException
     */
    private void detectAndSaveImagesOfUserStorie(final @NotNull String userStoryId, final @NotNull String userStoryContent) throws IOException, JDOMException {

        // Get the base URL of Target Process
        final String baseUrl = ConfigurationProperties.getProperty("tp.baseurl");

        // Get the non-secure base URL of Target Process
        final String nonSecureBaseUrl = ConfigurationProperties.getProperty("tp.nonsecurebaseurl");

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStoriesSaving = ConfigurationProperties.getProperty("outputpathuserstoriessaving") + "\\" + startDateTime;

        // Parse the XML flow describing the content of the User Story
        SAXBuilder sb = new SAXBuilder();
        Document jdomDocument = sb.build(new StringReader(userStoryContent));
        XPathFactory xpfac = XPathFactory.instance();
        XPathExpression<Element> expr = xpfac.compile("//Description", Filters.element());

        // Get the description part of the User Story, which one also contains references to images
        final List<Element> listElements = expr.evaluate(jdomDocument);
        if (listElements.isEmpty()) {
            return;
        }
        List<Content> listContents = listElements.get(0).getContent();
        if (listContents.isEmpty()) {
            return;
        }

        String description = listContents.get(0).getValue();
        jdomDocument = sb.build(new StringReader("<body>" + description + "</body>"));

        // Search images referenced in the User Story
        expr = xpfac.compile("//img", Filters.element());
        List<Element> listImages = expr.evaluate(jdomDocument);
        for (Element image : listImages) {
            String imageSrc = image.getAttributeValue("src");
            if (imageSrc != null && !imageSrc.equals("#") && imageSrc.contains("images/")) {

                // Get the image via Selenium API
                if (imageSrc.contains("~")) {
                    imageSrc = imageSrc.replace("~", "");
                }
                if (imageSrc.startsWith(baseUrl)) {
                    imageSrc = imageSrc.replace(baseUrl, "");
                } else if (imageSrc.startsWith(nonSecureBaseUrl)) {
                    imageSrc = imageSrc.replace(nonSecureBaseUrl, "");
                }

                logger.debug("DOWNLOADING IMAGE - US " + userStoryId + " - " + baseUrl + imageSrc);
                driver.get(baseUrl + imageSrc);

                WebElement imgElement = driver.findElement(By.xpath("/html/body/img"));

                // Make a screenshot
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                BufferedImage fullImg = ImageIO.read(screenshot);

                // Crop the image
                Point point = imgElement.getLocation();
                int eleWidth = imgElement.getSize().getWidth();
                int eleHeight = imgElement.getSize().getHeight();
                BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(), point.getY(), eleWidth, eleHeight);
                ImageIO.write(eleScreenshot, "png", screenshot);

                // Save the image in the appropriate directory
                FileUtils.copyFile(screenshot, new File(outputPathUserStoriesSaving + "\\" + imageSrc));
            }
        }
    }

    /**
     * Detect and save attachments contained in the User Story.
     *
     * @param userStoryId - User Storie ID
     * @throws JDOMException
     * @throws IOException
     * @throws ExtractionException
     */
    private void detectAndSaveAttachmentOfUserStorie(final @NotNull String userStoryId) throws JDOMException, IOException, ExtractionException {

        // Get the url to download the content of an User Storie
        final String userStoryUrl = ConfigurationProperties.getProperty("tp.userstory.url");

        // Get params of the url to download the content of an User Storie
        final String userStoryUrlAttachmentParams = ConfigurationProperties.getProperty("tp.userstory.url.attachment");

        // Get the url to download an attachment of an User Storie
        final String attachmentUrl = ConfigurationProperties.getProperty("tp.attachment.url");

        // Get the timeout for downloading an attachment of an User Storie
        final String timeoutdownload = ConfigurationProperties.getProperty("tp.attachment.timeoutdownload");

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStorieSaving = ConfigurationProperties.getProperty("outputpathuserstoriesaving") + "\\" + startDateTime;

        // Connection to Target Process URL to download attachments list of an User Storie
        driver.get(userStoryUrl + "/" + userStoryId + "/" + userStoryUrlAttachmentParams);

        // Downloading the attachments' list of the User Storie
        String userStoryAttachments = driver.getPageSource();

        // Parse the XML flow describing the content of the User Story
        SAXBuilder sb = new SAXBuilder();
        Document jdomDocument = sb.build(new StringReader(userStoryAttachments));
        XPathFactory xpfac = XPathFactory.instance();
        XPathExpression<Element> expr = xpfac.compile("//Attachment", Filters.element());

        // Get the description part of the User Story, which one also contains references to images
        List<Element> listAttachments = expr.evaluate(jdomDocument);

        String id;
        String name;
        for (Element attachment : listAttachments) {
            id = attachment.getAttributeValue("Id");
            name = attachment.getAttributeValue("Name");

            // Download the attachment
            logger.debug("DOWNLOADING ATTACHMENT - US " + userStoryId + " - " + attachmentUrl + id);
            driver.get(attachmentUrl + id);

            // Need to do a tempo in order to download the current file
            try {
                Thread.sleep(Long.parseLong(timeoutdownload));
            } catch (InterruptedException e) {
                throw new ExtractionException("Thread sleep failed", e);
            }

            // Rename the document with adding the US ID in prefix
            File file = new File(outputPathUserStorieSaving + "\\attachments\\" + name);

            // File (or directory) with new name
            File file2 = new File(outputPathUserStorieSaving + "\\attachments\\" + userStoryId + "-" + name);
            if (file2.exists())
                throw new java.io.IOException("File " + outputPathUserStorieSaving + "\\" + userStoryId + "-" + name + " already exists");

            // Rename file (or directory)
            file.renameTo(file2);
        }
    }

    /**
     * Extract User Stories from Target Process
     *
     * @throws ExtractionException
     */
    public void processUserStoriesExtraction() throws ExtractionException {

        // Connection to Target Process in order to all the use of TP RESTFULL URL
        authentification();

        // Save User Stories content
        saveUserStories();

        // Close the browser
        driver.quit();
    }

    /**
     * Perform the authentification on Target Process website.
     *
     * @throws ExtractionException
     */
    private void authentification() throws ExtractionException {

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
