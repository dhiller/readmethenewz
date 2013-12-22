package de.zalando.hackweek.read_me_the_newz.extract.feed;

import com.google.common.base.Throwables;
import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author dhiller
 * @since 2013-12-20
 */
@RunWith(Enclosed.class)
public class FeedExtractorTest {

    @Ignore
    public abstract static class TestBase {

        protected FeedExtractor newFeedExtractorFromSource(Source s) {
            try {
                return new FeedExtractor(s);
            } catch (ParserConfigurationException e) {
                throw Throwables.propagate(e);
            } catch (SAXException e) {
                throw Throwables.propagate(e);
            }
        }

        protected Source newSourceFrom(URI source) {
            return new Source("test", "test", Type.FEED, source);
        }

        protected URI uriOfResource(String resourceName) {
            try {
                return getClass().getResource(resourceName).toURI();
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }

    }

    public static class Rss extends TestBase {

        private String resourceName;
        private FeedExtractor underTest;

        @Before
        public void setUp() {
            setResourceName("/rss/heise-rss-formatted.xml");
            underTest = newFeedExtractorFromSource(newSourceFrom(uriOfResource(getResourceName())));
        }

        @Test
        public void feedExtractor() throws IOException, SAXException {
            List<FeedItem> items = extractItems();
            assertThat(items,
                    hasItems(
                            Matchers.<FeedItem>allOf(
                                    hasProperty("title", notNullValue()),
                                    hasProperty("description", notNullValue()),
                                    hasProperty("link", notNullValue()),
                                    hasProperty("from", notNullValue()),
                                    hasProperty("guid", notNullValue())
                            )
                    )
            );
        }

        private List<FeedItem> extractItems() throws SAXException, IOException {
            return underTest.extract(getClass().getResourceAsStream(getResourceName()));
        }

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }
    }

}
