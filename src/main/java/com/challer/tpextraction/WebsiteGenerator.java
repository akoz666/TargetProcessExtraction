package com.challer.tpextraction;

import com.sun.istack.internal.NotNull;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides methods to generate Website from xml data coming from Target Process.
 *
 * @author Cyril Haller - cyril.haller@gmail.com
 */
public class WebsiteGenerator {


    /**
     * Logger Class
     */
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(WebsiteGenerator.class);

    private final static DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Singleton instance
     */
    private static WebsiteGenerator instance;

    /**
     * Date/Hour of starting extraction
     */
    private final String startDateTime;

    /**
     * Velocity engine
     */
    private VelocityEngine velocityEngine;

    private WebsiteGenerator(final @NotNull String startDateTime) {

        this.startDateTime = startDateTime;

        // Initialize Velocity Engine
        velocityEngine = new VelocityEngine();
        Properties velocityProps = new Properties();
        velocityProps.put(Velocity.RESOURCE_LOADER, "file");
        velocityProps.put(Velocity.FILE_RESOURCE_LOADER_CACHE, "true");
        velocityProps.put(Velocity.FILE_RESOURCE_LOADER_PATH, getClass().getClassLoader().getResource("generator/templates").getFile());
        velocityEngine.init(velocityProps);
    }

    /**
     * Singleton pattern
     *
     * @param startDateTime Start time of extractor performing
     * @return singleton of WebsiteGenerator
     */
    public static WebsiteGenerator getInstance(final @NotNull String startDateTime) {
        if (instance == null) {
            instance = new WebsiteGenerator(startDateTime);
        }
        return instance;
    }

    /**
     * Generate the static website
     *
     * @throws ExtractionException
     * @throws IOException
     */
    public void generateHTML() throws ExtractionException, IOException {

        // Get the directory where User Stories information are stored
        final String userStoriesPath = ConfigurationProperties.getProperty("outputpathuserstoriessaving") + "\\" + startDateTime;

        final File[] userStoriesFiles = listUserStoriesFiles(userStoriesPath);

        // List containing User Stories
        final List<UserStory> userStoriesList = new ArrayList<>();
        // Map listing User Stories linked to Feature
        final Map<String, List<UserStory>> featureUserStoriesMap = new TreeMap();

        // For each User Story, generate a dedicated HTML page
        for (File userStoryFile : userStoriesFiles) {

            logger.debug("START LOADING US " + userStoryFile.getAbsolutePath());

            try {
                final UserStory userStory = readUserStorieFromXmlFile(userStoryFile);

                // Add User Story to List and Map in order to display list in HTML
                List<UserStory> listUSofThisFeature = featureUserStoriesMap.get(userStory.getFeature());
                if (listUSofThisFeature == null) {
                    listUSofThisFeature = new ArrayList<>();
                    featureUserStoriesMap.put(userStory.getFeature(), listUSofThisFeature);
                }
                listUSofThisFeature.add(userStory);
                userStoriesList.add(userStory);

            } catch (IOException | JDOMException e) {
                throw new ExtractionException("Failed to load US " + userStoryFile.getAbsolutePath(), e);
            }
            logger.debug("LOADING ARE FINISHED FOR US " + userStoryFile.getAbsolutePath());
        }

        for (UserStory userStory : userStoriesList) {

            logger.debug("START GENERATE HTML PAGE FOR US " + userStory.getId());

            generateHtmlFromUserStory(userStory, userStoriesList);

            logger.debug("END GENERATE HTML PAGE FOR US " + userStory.getId());
        }

        // Now, generate the home page which is listing the user stories
        generateHtmlListingPage(featureUserStoriesMap, userStoriesList);

        // Copy css and boostrap files from
        final File bootstrap = new File(getClass().getClassLoader().getResource("generator/bootstrap").getFile());
        final File css = new File(getClass().getClassLoader().getResource("generator/css").getFile());

        FileUtils.copyDirectory(bootstrap, new File(userStoriesPath + "\\bootstrap"));
        FileUtils.copyDirectory(css, new File(userStoriesPath + "\\css"));
    }

    /**
     * Populate an object User Story from its XML file.
     *
     * @param userStoryFile XML File which are contained the User Story content
     * @return the User Story populated
     * @throws ExtractionException
     */
    private UserStory readUserStorieFromXmlFile(final @NotNull File userStoryFile) throws ExtractionException, JDOMException, IOException {

        final UserStory us = new UserStory();
        InputStream userStoryInputStream;
        try {
            userStoryInputStream = new FileInputStream(userStoryFile);
        } catch (FileNotFoundException e) {
            throw new ExtractionException("Failed to open file " + userStoryFile.getName(), e);
        }

        // Parse the XML flow describing the content of the User Story
        final SAXBuilder sb = new SAXBuilder();
        final Document jdomDocument = sb.build(userStoryInputStream);

        final String id = getValueofElementFromXml(jdomDocument, "//UserStory", "Id");
        us.setId(id);

        String description = getValueOfElementFromXml(jdomDocument, "//Description");

        // Check if link to another US are existing. If it is the case, then transforms the link
        // Change access URL to images (path and encoding)
        if (description != null) {

            // US links
            description = description.replaceAll("(id&#58;\\D*)(\\d*)", "<a href=\"us-$2.html\">$2</a>");

            // URL images
            final Pattern p = Pattern.compile("(img.*src=)(\"/)(.*\")");
            final Matcher m = p.matcher(description);
            final StringBuffer s = new StringBuffer();
            while (m.find())
                m.appendReplacement(s, m.group(1) + "\"" + m.group(3).replace("%", "%25"));

            if (m.find())
                description = s.toString();
        }


        us.setDescription(description);

        final String title = getValueofElementFromXml(jdomDocument, "//UserStory", "Name");
        us.setTitle(title);

        final String creationDate = getValueOfElementFromXml(jdomDocument, "//CreateDate");
        us.setCreationDate(dtf.parseDateTime(creationDate).toString("dd/MM/yyyy"));

        final String lastModificationDate = getValueOfElementFromXml(jdomDocument, "//ModifyDate");
        us.setLastModificationDate(dtf.parseDateTime(lastModificationDate).toString("dd/MM/yyyy"));

        String feature = getValueofElementFromXml(jdomDocument, "//Feature", "Name");
        if (feature == null) {
            feature = "";
        }
        us.setFeature(feature);

        final String state = getValueofElementFromXml(jdomDocument, "//EntityState", "Name");
        us.setState(state);

        // Load custom fields
        us.setCustomFields(getCustomFields(jdomDocument));

        // Search if attachments are existing
        // Get the directory where User Stories information have to be stored
        final String outputPathUserStoriesSaving = ConfigurationProperties.getProperty("outputpathuserstoriessaving") + "\\" + startDateTime;
        final File directoryToScan = new File(outputPathUserStoriesSaving + "\\" + "attachments");
        final File[] files = directoryToScan.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().startsWith(us.getId());
            }
        });
        us.setAttachments(files);

        return us;
    }

    /**
     * Return custom fields in the Map
     *
     * @param jdomDocument - XML Document loaded by Jdom
     * @return custm fields
     */
    private Map<String, String> getCustomFields(final @NotNull Document jdomDocument) {

        final Map<String, String> customFields = new HashMap<>();

        final XPathFactory xpfac = XPathFactory.instance();
        final XPathExpression<Element> expr = xpfac.compile("//CustomFields//Field", Filters.element());

        final List<Element> listFields = expr.evaluate(jdomDocument);

        for (Element field : listFields) {
            if (field.getCType().equals(Content.CType.Element)) {

                boolean isDate = false;
                if (field.getAttribute("Type").getValue().equals("Date")) {
                    isDate = true;
                }

                final List<Content> fieldContent = field.getContent();
                String name = null;
                String value = null;
                for (Content content : fieldContent) {
                    if (content.getCType().equals(Content.CType.Element) && ((Element) content).getName().equals("Name")) {
                        name = content.getValue();
                    }
                    if (content.getCType().equals(Content.CType.Element) && ((Element) content).getName().equals("Value")) {
                        value = content.getValue();

                        if (isDate && value != null && value.length() > 0) {
                            value = dtf.parseDateTime(value).toString("dd/MM/yyyy");
                        }
                    }
                }
                customFields.put(name, value);
            }
        }

        return customFields;
    }

    /**
     * Search a XML element and return its value.
     *
     * @param jdomDocument - XML Document loaded by Jdom
     * @param elementPath  - xpath of element researched
     * @return value of the element
     */
    private String getValueOfElementFromXml(final @NotNull Document jdomDocument, final @NotNull String elementPath) {
        final XPathFactory xpfac = XPathFactory.instance();
        final XPathExpression<Element> expr = xpfac.compile(elementPath, Filters.element());

        final List<Element> listElements = expr.evaluate(jdomDocument);
        if (listElements.isEmpty()) {
            return null;
        }

        final List<Content> listContents = listElements.get(0).getContent();
        if (listContents.isEmpty()) {
            return null;
        }

        return listContents.get(0).getValue();
    }

    /**
     * Search a XML element and one of ist attributes and return its value.
     *
     * @param jdomDocument - XML Document loaded by Jdom
     * @param elementPath  - xpath of element researched
     * @param attribute    - attribute researched
     * @return value of the element
     */
    private String getValueofElementFromXml(final @NotNull Document jdomDocument, final @NotNull String elementPath, final @NotNull String attribute) {
        final XPathFactory xpfac = XPathFactory.instance();
        final XPathExpression<Element> expr = xpfac.compile(elementPath, Filters.element());

        final List<Element> listElements = expr.evaluate(jdomDocument);
        if (listElements.isEmpty()) {
            return null;
        }

        return listElements.get(0).getAttributeValue(attribute);
    }

    /**
     * Generate HTML file for an User Story
     *
     * @param userStory       - User Story to generated
     * @param userStoriesList - List of User Stories required to generate the search box
     * @throws ExtractionException
     * @throws IOException
     */
    private void generateHtmlFromUserStory(final @NotNull UserStory userStory, final @NotNull List<UserStory> userStoriesList) throws ExtractionException, IOException {

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStoriesSaving = ConfigurationProperties.getProperty("outputpathuserstoriessaving") + "\\" + startDateTime;

        Template t = velocityEngine.getTemplate("userstory.vm");
        VelocityContext context = new VelocityContext();
        context.put("us", userStory);
        context.put("userStoriesList", userStoriesList);
        StringWriter stringWriter = new StringWriter();
        t.merge(context, stringWriter);

        // Now it has to save the content of the User Story
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPathUserStoriesSaving + "\\us-" + userStory.getId() + ".html");
            writer.write(stringWriter.toString());
        } catch (IOException ex) {
            throw new ExtractionException("Failed to write the file " + outputPathUserStoriesSaving + "\\us-" + userStory.getId() + ".html", ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Generate HTML Listing Page
     *
     * @param featureUserStoriesMap - Map containing US list classified by feature
     * @param userStoriesList       - List of User Stories required to generate the search box
     * @throws ExtractionException
     * @throws IOException
     */
    private void generateHtmlListingPage(final @NotNull Map<String, List<UserStory>> featureUserStoriesMap, final @NotNull List<UserStory> userStoriesList) throws ExtractionException, IOException {

        // Get the directory where User Stories information have to be stored
        final String outputPathUserStoriesSaving = ConfigurationProperties.getProperty("outputpathuserstoriessaving") + "\\" + startDateTime;

        logger.debug("START GENERATE HTML HOMEPAGE " + outputPathUserStoriesSaving + "\\index.html");

        Template t = velocityEngine.getTemplate("uslisting.vm");
        VelocityContext context = new VelocityContext();
        context.put("featureUserStoriesMap", featureUserStoriesMap);
        context.put("userStoriesList", userStoriesList);
        StringWriter stringWriter = new StringWriter();
        t.merge(context, stringWriter);

        // Now it has to save the content of the User Story
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputPathUserStoriesSaving + "\\index.html");
            writer.write(stringWriter.toString());
        } catch (IOException ex) {
            throw new ExtractionException("Failed to write the file " + outputPathUserStoriesSaving + "\\index.html", ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        logger.debug("END GENERATE HTML HOMEPAGE " + outputPathUserStoriesSaving + "\\index.html");
    }

    /**
     * List the User Storie XML files
     *
     * @param userStoryPath - Path to access to the User Stories Directory
     * @return the User Stories files
     */
    private File[] listUserStoriesFiles(final @NotNull String userStoryPath) {
        final File directoryToScan = new File(userStoryPath);
        final File[] files = directoryToScan.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".xml");
            }
        });
        return files;
    }

}
