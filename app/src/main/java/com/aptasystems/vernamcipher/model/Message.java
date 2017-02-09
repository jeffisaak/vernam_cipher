package com.aptasystems.vernamcipher.model;

import java.io.Serializable;

/**
 * Created by jisaak on 2016-03-02.
 */
public class Message extends ModelEntity {

    private boolean _incoming;
    private String _content;

    public Message(Long id, boolean incoming, String content) {
        super(id);
        _incoming = incoming;
        _content = content;
    }

    public boolean isIncoming() { return _incoming; }

    public String getContent() {
        return _content;
    }
}
