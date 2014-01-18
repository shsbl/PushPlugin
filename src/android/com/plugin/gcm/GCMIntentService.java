package com.plugin.gcm;

import java.util.List;

import com.google.android.gcm.GCMBaseIntentService;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	public static final String DEFAULT_NOTIFICATION_ID = "1";
	private static final String TAG = "GCMIntentService";

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			String action = extras.getString("action");
			if (action == null) {
				Log.d(TAG, "onMessage - missing action");
			} else {
				Log.d(TAG, "onMessage - action: [" + action + "]");
			}
			if (action != null && action.equals("dismiss_notification")) {
				dismissNotification(context, extras);
				return;
			}
			// if we are in the foreground, surface the payload with foreground = true.
			if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
				extras.putBoolean("user_click", false);
				PushPlugin.sendExtras(extras);
			}
			// Send a notification if there is a message
			if (extras.getString("message") != null && extras.getString("message").length() != 0) {
				extras.putBoolean("foreground", false);
				extras.putBoolean("user_click", true);
				createNotification(context, extras);
			}
		}
	}

	public void createNotification(Context context, Bundle extras)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, getTagName(context, extras).hashCode(), notificationIntent, PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context)
				.setDefaults(Notification.DEFAULT_ALL)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setTicker(extras.getString("title"))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);

		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}

		mNotificationManager.notify(getTagName(context, extras), 0, mBuilder.build());
	}

	public static void dismissNotification(Context context, Bundle extras)
	{
		Log.d(TAG, "dismissNotification " + getMessageID(extras));
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(getTagName(context, extras), 0);
	}

	private static String getTagName(Context context, Bundle extras)
	{
		return getAppName(context) + getMessageID(extras);
	}

	private static String getAppName(Context context)
	{
		CharSequence appName =
				context
				.getPackageManager()
				.getApplicationLabel(context.getApplicationInfo());

		return (String)appName;
	}

	public static String getMessageID(Bundle extras) {
		if (extras == null) {
			Log.i(TAG, "extras is null, using default message id.");
			return DEFAULT_NOTIFICATION_ID;
		}
		String id = extras.getString("notification_id");
		if (id == null) {
			Log.i(TAG, "notification id not specified, using default one.");
			return DEFAULT_NOTIFICATION_ID;
		}
		return id;
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
