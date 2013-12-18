
package de.zalando.hackweek.read_me_the_newz.rss.item;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;

public class HttpURIContentProvider extends ContentProvider {

    public HttpURIContentProvider(Source source) {
        super(source);
    }

    @Override
    public InputStream provideContent() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.setURI(source().uri());
        HttpResponse response = client.execute(request);
        return response.getEntity().getContent();
    }

}