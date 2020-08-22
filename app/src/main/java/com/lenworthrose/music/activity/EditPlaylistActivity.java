package com.lenworthrose.music.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.lenworthrose.music.R;
import com.lenworthrose.music.adapter.PlayingNowPlaylistAdapter;
import com.lenworthrose.music.playback.PlaybackService;
import com.mobeta.android.dslv.DragSortListView;

/**
 * Activity that allows users to modify the current Playing Now playlist.
 */
public class EditPlaylistActivity extends AppCompatActivity implements ServiceConnection {
    private PlaybackService playbackService;
    private PlayingNowPlaylistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_playlist);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bindService(new Intent(this, PlaybackService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_playlist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!adapter.getEditActions().isEmpty()) {
                showUnsavedChangesDialog();
            } else {
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(this, intent);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSaveClicked(MenuItem unused) {
        if (!adapter.getEditActions().isEmpty())
            playbackService.performPlaylistActions(adapter);

        finish();
    }

    @Override
    public void onBackPressed() {
        if (!adapter.getEditActions().isEmpty())
            showUnsavedChangesDialog();
        else
            super.onBackPressed();
    }

    protected void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.unsaved_changed).setMessage(R.string.unsaved_changes_warning)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onSaveClicked(null);
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playbackService = ((PlaybackService.LocalBinder)service).getService();

        adapter = new PlayingNowPlaylistAdapter(this, playbackService.getPlaylist());
        adapter.setEditModeEnabled(true);

        DragSortListView listView = (DragSortListView)findViewById(R.id.ep_list_view);
        listView.setAdapter(adapter);
        listView.setSelection(playbackService.getPlaylistPosition());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playbackService = null;
    }
}
