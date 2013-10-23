package com.codeplex.peerly.couchdbtest;

import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.CouchDbDocument;

/**
 * Created by yarong on 10/22/13.
 */
public class TestBlogClass extends CouchDbDocument {
    private String blogArticleName;
    private String blogArticleContent;

    public String getBlogArticleName() {
        return blogArticleName;
    }

    public void setBlogArticleName(String blogArticleName) {
        this.blogArticleName = blogArticleName;
    }

    public String getBlogArticleContent() {
        return blogArticleContent;
    }

    public void setBlogArticleContent(String blogArticleContent) {
        this.blogArticleContent = blogArticleContent;
    }
}
