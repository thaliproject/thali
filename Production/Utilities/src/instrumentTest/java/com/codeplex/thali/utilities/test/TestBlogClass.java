package com.codeplex.thali.utilities.test;

import org.ektorp.support.CouchDbDocument;

/**
 * Created by yarong on 11/12/13.
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

    public boolean equals(TestBlogClass compareTo) {
        return (this.getId().equals(compareTo.getId()) &&
                this.getRevision().equals(compareTo.getRevision()) &&
                this.getBlogArticleName().equals(compareTo.getBlogArticleName()) &&
                this.getBlogArticleContent().equals(compareTo.getBlogArticleContent()));
    }
}
