package com.aptasystems.vernamcipher.model;

import java.io.Serializable;

/**
 * Created by jisaak on 2016-03-02.
 */
public class ModelEntity implements Serializable {

    private Long _id;

    public ModelEntity(Long id) {
        _id = id;
    }

    public final Long getId() {
        return _id;
    }
}
