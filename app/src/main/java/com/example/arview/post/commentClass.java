package com.example.arview.post;

import java.util.Date;

public class commentClass {

    private String postId;
    private String userName;
    private Date commentDate;
    private String comment;

    public commentClass(){}
    public commentClass(String postId,String userName, Date commentDate ,String comment  ){
        this.postId = postId;
        this.userName = userName;
        this.commentDate = commentDate;
        this.comment = comment;
    }


    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCommentDate() {
        return commentDate;
    }

    public void setCommentDate(Date commentDate) {
        this.commentDate = commentDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
