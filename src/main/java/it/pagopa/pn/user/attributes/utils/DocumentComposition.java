package it.pagopa.pn.user.attributes.utils;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static it.pagopa.pn.user.attributes.exceptions.PnUserattributesExceptionCodes.ERROR_CODE_USERATTRIBUTES_DOCUMENTCOMPOSITIONFAILED;


@Component
@Slf4j
public class DocumentComposition {

    private static final String TEMPLATES_DIR_NAME = "verificationcodemessages";

    public enum TemplateType {
        EMAIL_VERIFICATION_TEMPLATE(TEMPLATES_DIR_NAME + "/emailbody.html"),
        PEC_VERIFICATION_TEMPLATE(TEMPLATES_DIR_NAME + "/pecbody.html"),
        PEC_CONFIRM_TEMPLATE(TEMPLATES_DIR_NAME + "/pecbodyconfirm.html"),
        PEC_REJECT_TEMPLATE(TEMPLATES_DIR_NAME + "/pecbodyreject.html");

        private final String htmlTemplate;

        TemplateType(String htmlTemplate) {
            this.htmlTemplate = htmlTemplate;
        }

        public String getHtmlTemplate() {
            return htmlTemplate;
        }
    }

    private final Configuration freemarker;

    public DocumentComposition(Configuration freemarker) throws IOException {
        this.freemarker = freemarker;

        log.info("Preload templates START");
        StringTemplateLoader stringLoader = new StringTemplateLoader();

        for( TemplateType templateType : TemplateType.values() ) {
            log.info(" - begin to preload template with templateType={}", templateType );
            BaseUriAndTemplateBody info = preloadTemplate( templateType );

            stringLoader.putTemplate( templateType.name(), info.templateBody);
        }
        log.debug("Configure freemarker ... ");
        this.freemarker.setTemplateLoader( stringLoader );
        log.debug(" ... freemarker configured.");
        log.info("Preload templates END");
    }


    @Value
    private static class BaseUriAndTemplateBody {
        String baseUri;
        String templateBody;
    }

    private static BaseUriAndTemplateBody preloadTemplate( TemplateType templateType ) throws IOException {
        log.debug("Start pre-loading template with templateType={}", templateType);

        String templateResourceName = templateType.getHtmlTemplate();
        URL templateUrl = getClasspathResourceURL( templateResourceName );
        log.debug("Template with templateResourceName={} located at URL={}", templateResourceName, templateUrl );

        String baseUri = templateUrl.toString().replaceFirst("/[^/]*$", "/");
        String templateBody = loadTemplateBody( templateUrl );

        log.debug("Template resources baseUri={}", baseUri);
        return new BaseUriAndTemplateBody( baseUri, templateBody );
    }

    private static String loadTemplateBody( URL templateUrl ) throws IOException {

        String templateContent;
        try( InputStream templateIn = templateUrl.openStream()) {
            templateContent = StreamUtils.copyToString( templateIn, StandardCharsets.UTF_8 );
        } catch (IOException exc) {
            log.error("Loading Document Composition Template " + templateUrl, exc );
            throw exc;
        }
        return templateContent;
    }

    @Nullable
    private static URL getClasspathResourceURL( String resourceName ) {
        return Thread.currentThread().getContextClassLoader().getResource( resourceName );
    }

    public String executeTextTemplate( TemplateType templateType, Object model) {
        log.info("Execute templateType={} START", templateType );
        StringWriter stringWriter = new StringWriter();

        try {
            Template template = freemarker.getTemplate( templateType.name() );
            template.process( model, stringWriter );

        } catch (IOException | TemplateException exc) {
            throw new PnInternalException(
                    "Processing template " + templateType,
                    ERROR_CODE_USERATTRIBUTES_DOCUMENTCOMPOSITIONFAILED,
                    exc);
        }

        log.info("Execute templateType={} END", templateType );
        return stringWriter.getBuffer().toString();
    }


}
