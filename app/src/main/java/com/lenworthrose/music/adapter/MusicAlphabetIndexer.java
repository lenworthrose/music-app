package com.lenworthrose.music.adapter;

import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.AlphabetIndexer;

/**
 * An {@link AlphabetIndexer} that properly handles the sort in the {@link MediaStore} database.
 */
public class MusicAlphabetIndexer extends AlphabetIndexer {
    public MusicAlphabetIndexer(Cursor cursor, int sortedColumn, String alphabet) {
        super(cursor, sortedColumn, alphabet);
    }

    @Override
    protected int compare(String word, String letter) {
        String wordKey = MediaStore.Audio.keyFor(word);
        String letterKey = MediaStore.Audio.keyFor(letter);

        if (wordKey.startsWith(letter)) {
            return 0;
        } else {
            return wordKey.compareTo(letterKey);
        }
    }
}
