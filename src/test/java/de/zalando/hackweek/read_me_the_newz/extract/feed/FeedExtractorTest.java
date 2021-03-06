package de.zalando.hackweek.read_me_the_newz.extract.feed;

import static org.hamcrest.Matchers.*;

import static org.junit.Assert.assertThat;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.hamcrest.Matchers;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.junit.experimental.runners.Enclosed;

import org.junit.runner.RunWith;

import org.xml.sax.SAXException;

import com.google.common.base.Throwables;

import de.zalando.hackweek.read_me_the_newz.extract.Source;
import de.zalando.hackweek.read_me_the_newz.extract.Type;

/**
 * @author dhiller
 * @since 2013-12-20
 */
@RunWith(Enclosed.class)
public class FeedExtractorTest {

    @Ignore
    public abstract static class TestBase {

        private String resourceName;
        private FeedExtractor underTest;

        protected FeedExtractor newFeedExtractorFromSource(final Source s) {
            try {
                return new FeedExtractor(s);
            } catch (ParserConfigurationException e) {
                throw Throwables.propagate(e);
            } catch (SAXException e) {
                throw Throwables.propagate(e);
            }
        }

        protected Source newSourceFrom(final URI source) {
            return new Source("test", "test", Type.FEED, source);
        }

        protected URI uriOfResource(final String resourceName) {
            try {
                return getClass().getResource(resourceName).toURI();
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }

        @Before
        public void setUp() {
        }

        @Test
        public void extract() {
            assertThat(extractItems(),
                    hasItems(
                            Matchers.<FeedItem>allOf(
                                    hasProperty("title", notNullValue()),
                                    hasProperty("description", notNullValue()),
                                    hasProperty("link", notNullValue()),
                                    hasProperty("from", notNullValue()),
                                    hasProperty("id", notNullValue())
                            )
                    )
            );
        }

        @Test
        public void olderNewsComeFirst() {
            Feed actual = extractItems();
            assertThat(actual.get(0).getFrom(), is(lessThan(actual.get(1).getFrom())));
            assertThat(actual.get(actual.size() - 2).getFrom(), is(lessThan(actual.get(actual.size() - 1).getFrom())));
        }

        private Feed extractItems() {
            try {
                return getUnderTest().extract(getClass().getResourceAsStream(getResourceName()));
            } catch (SAXException e) {
                throw Throwables.propagate(e);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(final String resourceName) {
            this.resourceName = resourceName;
        }

        public FeedExtractor getUnderTest() {
            return underTest;
        }

        public void setUnderTest(final FeedExtractor underTest) {
            this.underTest = underTest;
        }
    }

    public static class Rss extends TestBase {

        @Override
        @Before
        public void setUp() {
            setResourceName("/rss/heise-rss-formatted.xml");
            setUnderTest(newFeedExtractorFromSource(newSourceFrom(uriOfResource(getResourceName()))));
        }

    }

    public static class Atom extends TestBase {

        @Override
        @Before
        public void setUp() {
            setResourceName("/atom/martin-fowler-bliki.atom");
            setUnderTest(newFeedExtractorFromSource(newSourceFrom(uriOfResource(getResourceName()))));
        }

    }

}
