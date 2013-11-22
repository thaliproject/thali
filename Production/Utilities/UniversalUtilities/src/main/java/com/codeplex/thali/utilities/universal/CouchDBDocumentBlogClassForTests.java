package com.codeplex.thali.utilities.universal;

import org.ektorp.support.CouchDbDocument;

/**
 * Created by yarong on 10/22/13.
 */
public class CouchDBDocumentBlogClassForTests extends CouchDbDocument {
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

    @Override
    public boolean equals(Object object) {
        if ((object instanceof CouchDBDocumentBlogClassForTests) == false) {
            return false;
        }
        CouchDBDocumentBlogClassForTests compareTo = (CouchDBDocumentBlogClassForTests) object;
        return (this.getId().equals(compareTo.getId()) &&
                this.getRevision().equals(compareTo.getRevision()) &&
                this.getBlogArticleName().equals(compareTo.getBlogArticleName()) &&
                this.getBlogArticleContent().equals(compareTo.getBlogArticleContent()));
    }
}
