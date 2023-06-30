package it.pagopa.pn.user.attributes.utils;

import freemarker.template.Configuration;
import freemarker.template.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class TemplateGeneratorTest {

    private static final String TEST_DIR_NAME = "target" + File.separator + "generated-test-HTML";
    private static final Path TEST_DIR_PATH = Paths.get(TEST_DIR_NAME);


    private TemplateGenerator templateGenerator;

    @BeforeEach
    public void beforeEach() throws IOException {
        Configuration freemarker = new Configuration(new Version(2,3,0)); //Version is a final class
        DocumentComposition documentComposition = new DocumentComposition(freemarker);

        templateGenerator = new TemplateGenerator(documentComposition);

        //create target test folder, if not exists
        if (Files.notExists(TEST_DIR_PATH)) {
            Files.createDirectory(TEST_DIR_PATH);
        }
    }

    @Test
    void generatePecBody() {
        Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_generatePecBody.html");


        Assertions.assertDoesNotThrow(() -> {
                    String element = templateGenerator.generatePecBody("12345");
                    PrintWriter out = new PrintWriter(filePath.toString());
                    out.println(element);
                    out.close();
                    System.out.println("element "+element);
                }
        );

        System.out.print("*** PEC BODY successfully created");
    }

    @Test
    void generateEmailBody() {
        Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_generateEmailBody.html");


        Assertions.assertDoesNotThrow(() -> {
                    String element = templateGenerator.generateEmailBody("12345");
                    PrintWriter out = new PrintWriter(filePath.toString());
                    out.println(element);
                    out.close();
                    System.out.println("element "+element);
                }
        );

        System.out.print("*** EMAIL BODY successfully created");
    }

    @Test
    void generatePecConfirmBody() {
        Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_generatePecConfirmBody.html");


        Assertions.assertDoesNotThrow(() -> {
                    String element = templateGenerator.generatePecConfirmBody();
                    PrintWriter out = new PrintWriter(filePath.toString());
                    out.println(element);
                    out.close();
                    System.out.println("element "+element);
                }
        );

        System.out.print("*** PEC CONFIRM BODY successfully created");
    }

    @Test
    void generatePecRejectBody() {
        Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_generatePecRejectBody.html");


        Assertions.assertDoesNotThrow(() -> {
                    String element = templateGenerator.generatePecRejectBody();
                    PrintWriter out = new PrintWriter(filePath.toString());
                    out.println(element);
                    out.close();
                    System.out.println("element "+element);
                }
        );

        System.out.print("*** PEC REJECT BODY successfully created");
    }
}
