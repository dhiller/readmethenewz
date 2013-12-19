package de.zalando.hackweek.read_me_the_newz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            final KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getRepeatCount() == 0) {
                final Intent mediaButtonsIntent = new Intent(ReadNewz.Intents.MEDIA_BUTTONS);
                mediaButtonsIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                context.sendBroadcast(mediaButtonsIntent);
            }
        }
    }
}
