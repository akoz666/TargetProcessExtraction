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
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

/**
 * This class provides methods to extract User Stories from Target Process.
 *
 * @author Cyril Haller - cyril.haller@gmail.com
 */
public class UserStorieExtractor {

    /**
     * Logger Class
     */
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(UserStorieExtractor.class);

    /**
     * Webdriver used to connect to Target Process
     */
    private final WebDriver driver;

    /**
     * @param driver WebDriver - Webdriver used to connect to Target Process
     */
    public UserStorieExtractor(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Save all User Stories referenced in the csv file.
     *
     * @throws ExtractionException
     */
    public void saveUserStories() throws ExtractionException {

        // Get the csv User Storie list
        final String inputUserStoriesListFile = ConfigurationProperties.getProperty("inputuserstorieslistfile");

        InputStream userStorieInputStream;
        try {
            userStorieInputStream = new FileInputStream(inputUserStoriesListFile);
        } catch (FileNotFoundException e) {
            throw new ExtractionException("Failed to open file " + inputUserStoriesListFile, e);
        }

        InputStreamReader userStorieInputStreamReader = new InputStreamReader(userStorieInputStream);
        BufferedReader br = new BufferedReader(userStorieInputStreamReader);
        try {
            String userStorieId;
            while ((userStorieId = br.readLine()) != null) {
                try {
                    saveUserStorie(userStorieId);
                } catch (ExtractionException e) {
                    logger.error("Failed to save US " + userStorieId, e);
                }
            }

            br.close();
        } catch (IOException e) {
            throw new ExtractionException("Failed to read file " + inputUserStoriesListFile, e);
        }
    }

    /**
     * Save the content of an User Storie.
     *
     * @param userStorieId String - ID a the User Storie to save
     * @throws ExtractionException
     * @throws IOException
     */
    private void saveUserStorie(final @NotNull String userStorieId) throws ExtractionException, IOException {

        logger.debug("US SAVING - IS STARTING - US " + userStorieId);

        // Get the url to download the content of an User Storie
        final String userStorieUrl = ConfigurationProperties.getProperty("tp.userstorie.url");

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStorieSaving = ConfigurationProperties.getProperty("outputpathuserstoriesaving");

        // Connection to Target Process URL allowing to download content of an User Storie
        logger.debug("GETTING US CONTENT - " + userStorieUrl + "/" + userStorieId);
        driver.get(userStorieUrl + "/" + userStorieId);

        // Downloading the content of the User Storie
        String userStorieContent = driver.getPageSource();

        // It has to detect if the User Storie references one or many images and attachments.
        // If it is the case, then images and attachments have also to be saved.
        try {
            detectAndSaveImagesOfUserStorie(userStorieId, userStorieContent);
            detectAndSaveAttachmentOfUserStorie(userStorieId);
        } catch (IOException | JDOMException e) {
            throw new ExtractionException("Failed to save images of the US-" + userStorieId, e);
        }

        // Now it has to save the content of the User Storie
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPathUserStorieSaving + "\\us-" + userStorieId + ".xml");
            writer.write(userStorieContent);
        } catch (IOException ex) {
            throw new ExtractionException("Failed to write the file " + outputPathUserStorieSaving + "us-" + userStorieId + userStorieId, ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        logger.debug("US SAVING - DONE - US " + userStorieId);
    }

    /**
     * Detect and save images contained in the User Storie.
     *
     * @param userStorieId      String - User Storie ID
     * @param userStorieContent String - HTML content of the User Storie.
     * @throws IOException
     * @throws JDOMException
     */
    private void detectAndSaveImagesOfUserStorie(final @NotNull String userStorieId, final @NotNull String userStorieContent) throws IOException, JDOMException {

        // Get the base URL of Target Process
        final String baseUrl = ConfigurationProperties.getProperty("tp.baseurl");

        // Get the non-secure base URL of Target Process
        final String nonSecureBaseUrl = ConfigurationProperties.getProperty("tp.nonsecurebaseurl");

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStorieSaving = ConfigurationProperties.getProperty("outputpathuserstoriesaving");

        // Parse the XML flow describing the content of the User Storie
        SAXBuilder sb = new SAXBuilder();
        Document jdomDocument = sb.build(new StringReader(userStorieContent));
        XPathFactory xpfac = XPathFactory.instance();
        XPathExpression<Element> expr = xpfac.compile("//Description", Filters.element());

        // Get the description part of the User Storie, which one also contains references to images
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

        // Search images referenced in the User Storie
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

                logger.debug("DOWNLOADING IMAGE - US " + userStorieId + " - " + baseUrl + imageSrc);
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
                FileUtils.copyFile(screenshot, new File(outputPathUserStorieSaving + "\\" + imageSrc));
            }
        }
    }

    /**
     * Detect and save attachments contained in the User Storie.
     *
     * @param userStorieId String - User Storie ID
     * @throws JDOMException
     * @throws IOException
     * @throws ExtractionException
     */
    private void detectAndSaveAttachmentOfUserStorie(final @NotNull String userStorieId) throws JDOMException, IOException, ExtractionException {

        // Get the url to download the content of an User Storie
        final String userStorieUrl = ConfigurationProperties.getProperty("tp.userstorie.url");

        // Get params of the url to download the content of an User Storie
        final String userStorieUrlAttachmentParams = ConfigurationProperties.getProperty("tp.userstorie.url.attachment");

        // Get the url to download an attachment of an User Storie
        final String attachmentUrl = ConfigurationProperties.getProperty("tp.attachment.url");

        // Get the timeout for downloading an attachment of an User Storie
        final String timeoutdownload = ConfigurationProperties.getProperty("tp.attachment.timeoutdownload");

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStorieSaving = ConfigurationProperties.getProperty("outputpathuserstoriesaving");

        // Connection to Target Process URL to download attachments list of an User Storie
        driver.get(userStorieUrl + "/" + userStorieId + "/" + userStorieUrlAttachmentParams);

        // Downloading the attachments' list of the User Storie
        String userStorieAttachments = driver.getPageSource();

        // Parse the XML flow describing the content of the User Storie
        SAXBuilder sb = new SAXBuilder();
        Document jdomDocument = sb.build(new StringReader(userStorieAttachments));
        XPathFactory xpfac = XPathFactory.instance();
        XPathExpression<Element> expr = xpfac.compile("//Attachment", Filters.element());

        // Get the description part of the User Storie, which one also contains references to images
        List<Element> listAttachments = expr.evaluate(jdomDocument);

        String id;
        String name;
        for (Element attachment : listAttachments) {
            id = attachment.getAttributeValue("Id");
            name = attachment.getAttributeValue("Name");

            // Download the attachment
            logger.debug("DOWNLOADING ATTACHMENT - US " + userStorieId + " - " + attachmentUrl + id);
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
            File file2 = new File(outputPathUserStorieSaving + "\\attachments\\" + userStorieId + "-" + name);
            if (file2.exists())
                throw new java.io.IOException("File " + outputPathUserStorieSaving + "\\" + userStorieId + "-" + name + " already exists");

            // Rename file (or directory)
            file.renameTo(file2);
        }
    }
}
