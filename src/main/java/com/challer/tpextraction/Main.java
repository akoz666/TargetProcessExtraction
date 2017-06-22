package com.challer.tpextraction;

import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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
        //System.setProperty("webdriver.gecko.driver", "/home/dali/geckodriver");
        logger.info("TP EXTRACTION IS STARTING");

        final DateTime startDateTime = new DateTime();
        Main main = new Main();

        logger.info("CHECKING CONFIGURATION FILE");
        if (!main.checkConfiguration()) {
            logger.error("CONFIGURATION FILE IS INCORRECT - EXTRACTION IS CANCELLED");
            return;
        }
        logger.info("CONFIGURATION FILE IS OK");

        try {
            // Execute process extraction of User Stories
            logger.info("STARTING US EXTRACTION");
//            UserStoryExtractor extractor = UserStoryExtractor.getInstance(startDateTime.toString("YYYYMMDD-HHmm"));
            UserStoryExtractor extractor = UserStoryExtractor.getInstance("");
            extractor.processUserStoriesExtraction();
            logger.info("US EXTRACTION IS DONE");

            // Generate a static html website as parsing xml information extracted from Target Process
            logger.info("STARTING WEBSITE GENERATION");
//            WebsiteGenerator generator = WebsiteGenerator.getInstance(startDateTime.toString("YYYYMMDD-HHmm"));
            WebsiteGenerator generator = WebsiteGenerator.getInstance("");
            generator.generateHTML();
            logger.info("WEBSITE GENERATION IS DONE");

        } catch (IOException | ExtractionException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        logger.info("TP EXTRACTION COMPLETED SUCCESSFULLY");
    }

    /**
     * Check if all parameters in configuration file are OK.
     *
     * @return true if OK, else false
     */
    private Boolean checkConfiguration() {

        boolean check = true;
        List<String> paramsToCheck = new LinkedList<>();
        paramsToCheck.add("tp.username");
        paramsToCheck.add("tp.password");
        paramsToCheck.add("tp.baseurl");
        paramsToCheck.add("tp.nonsecurebaseurl");
        paramsToCheck.add("tp.userstory.url");
        paramsToCheck.add("tp.userstory.url.attachment");
        paramsToCheck.add("tp.attachment.url");
        paramsToCheck.add("tp.attachment.timeoutdownload");
        paramsToCheck.add("tp.attachment.typemime");
        paramsToCheck.add("tp.connection.url");
        paramsToCheck.add("inputuserstorieslistfile");
        paramsToCheck.add("outputpathuserstoriessaving");

        for (String param : paramsToCheck) {
            if (ConfigurationProperties.getProperty(param) == null || ConfigurationProperties.getProperty(param).trim().isEmpty()) {
                check = false;
                logger.error("CONFIGURATION FILE IS INCORRECT - " + param + " IS MISSING");
            }
        }

        return check;
    }
}
