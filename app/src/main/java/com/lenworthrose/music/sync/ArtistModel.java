package com.lenworthrose.music.sync;

/**
 * Represents info about an Artist.
 */
public class ArtistModel {
    private String name;
    private long id;

    public ArtistModel(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
