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
import android.util.Log;

import com.lenworthrose.music.R;
import com.lenworthrose.music.playback.PlaybackService;
import com.lenworthrose.music.util.Constants;

/**
 * Dialog for selecting repeat mode.
 */
public class RepeatDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, ServiceConnection {
    private Constants.RepeatMode[] supportedRepeatModes;
    private PlaybackService playbackService;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getActivity().bindService(new Intent(getActivity(), PlaybackService.class), this, Context.BIND_AUTO_CREATE);

        supportedRepeatModes = Constants.RepeatMode.values();
        String[] modeStrings = new String[supportedRepeatModes.length];

        for (int i = 0; i < supportedRepeatModes.length; i++) {
            int resId = 0;

            switch (supportedRepeatModes[i]) {
                case OFF:
                    resId = R.string.off;
                    break;
                case TRACK:
                    resId = R.string.track;
                    break;
                case PLAYLIST:
                    resId = R.string.playlist;
                    break;
                case STOP:
                    resId = R.string.stop;
                    break;
            }

            modeStrings[i] = getString(resId);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.choose_repeat_mode);
        builder.setSingleChoiceItems(modeStrings, -1, this);
        builder.setNegativeButton(R.string.cancel, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which >= 0 && which < supportedRepeatModes.length) {
            playbackService.setRepeatMode(supportedRepeatModes[which]);
            dismiss();
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();
        Constants.RepeatMode mode = playbackService.getRepeatMode();

        try {
            if (getDialog() != null)
                for (int i = 0; i < supportedRepeatModes.length; i++)
                    if (mode == supportedRepeatModes[i]) {
                        ((AlertDialog)getDialog()).getListView().setItemChecked(i, true);
                        return;
                    }
        } catch (IllegalStateException ex) {
            Log.w("RepeatDialogFragment", "IllegalStateException occurred attempting to set RepeatDialogFragment's checked item. Was the Dialog already destroyed?", ex);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}
