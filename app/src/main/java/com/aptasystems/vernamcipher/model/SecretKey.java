package com.aptasystems.vernamcipher.model;

/**
 * Created by jisaak on 2016-03-02.
 */
public class SecretKey extends ModelEntity {

    private String _name;
    private int _colour;
    private String _description;
    private byte[] _key;
    private int _bytesRemaining;

    public SecretKey(Long id, String name, int colour, String description, byte[] key, int bytesRemaining) {
        super(id);
        _name = name;
        _colour = colour;
        _description = description;
        _key = key;
        _bytesRemaining = bytesRemaining;
    }

    public SecretKey(String filename, int colour, String description, byte[] key) {
        this(null, filename, colour, description, key, key.length);
    }

    public SecretKey(Long id, String name, int colour, String description, int bytesRemaining) {
        this(id, name, colour, description, null, bytesRemaining);
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
