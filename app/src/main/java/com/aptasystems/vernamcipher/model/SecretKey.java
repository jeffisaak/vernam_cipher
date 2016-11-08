package com.aptasystems.vernamcipher.model;

import java.io.Serializable;

/**
 * Created by jisaak on 2016-03-02.
 */
public class SecretKey implements Serializable {

    // TODO - Change to long?
    private Integer _id;
    private String _name;
    private int _colour;
    private String _description;
    private byte[] _key;
    private int _bytesRemaining;

    public SecretKey(Integer id, String name, int colour, String description, byte[] key, int bytesRemaining) {
        _id = id;
        _name = name;
        _colour = colour;
        _description = description;
        _key = key;
        _bytesRemaining = bytesRemaining;
    }

    public SecretKey(Integer id, String name, int colour, String description, int bytesRemaining)
    {
        this(id, name, colour, description, null, bytesRemaining);
    }

    public SecretKey(String filename, int colour, String description, byte[] key) {
        this(null, filename, colour, description, key, key.length);
    }

    public Integer getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public int getColour() {
        return _colour;
    }

    public String getDescription() {
        return _description;
    }

    public byte[] getKey() {
        return _key;
    }

    public int getBytesRemaining() {
        return _bytesRemaining;
    }
}
