package com.aptasystems.vernamcipher.model;

import java.io.Serializable;

/**
 * Created by jisaak on 2016-03-02.
 */
public class Message implements Serializable {

    // TODO - Change to long?
    private Integer _id;
    private String _content;

    public Message(Integer id, String content) {
        _id = id;
        _content = content;
    }

    public Integer getId() {
        return _id;
    }

    public String getContent() {
        return _content;
    }
}
