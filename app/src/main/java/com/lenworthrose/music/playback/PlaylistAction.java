package com.lenworthrose.music.playback;

public class PlaylistAction {
    public PlaylistAction(int toRemove) {
        action = Action.REMOVE;
        position = toRemove;
    }

    public PlaylistAction(int oldPos, int newPos) {
        action = Action.MOVE;
        position = oldPos;
        newPosition = newPos;
    }

    public enum Action {
        MOVE, REMOVE
    }

    public Action action;
    public int position = -1, newPosition = -1;
}
