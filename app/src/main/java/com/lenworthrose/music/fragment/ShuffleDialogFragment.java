package com.lenworthrose.music.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;

/**
 * Dialog for choosing a shuffle mode: Shuffle All, or Shuffle Remaining.
 */
public class ShuffleDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, ServiceConnection {
    private PlaybackService playbackService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().bindService(new Intent(getActivity(), PlaybackService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_shuffle_mode);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setNeutralButton(R.string.remaining, this);
        builder.setPositiveButton(R.string.all, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL: //Shuffle remaining.
                playbackService.shuffleRemaining();
                break;
            case DialogInterface.BUTTON_POSITIVE: //Shuffle all.
                playbackService.shuffleAll();
                break;
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.playbackService = ((PlaybackService.LocalBinder)service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        this.playbackService = null;
    }
}
