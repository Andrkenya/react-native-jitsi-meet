package com.reactnativejitsimeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.module.annotations.ReactModule;
import org.jitsi.meet.sdk.BroadcastAction;
import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import java.net.MalformedURLException;
import java.net.URL;

@ReactModule(name = JitsiMeetModule.NAME)
public class JitsiMeetModule extends ReactContextBaseJavaModule {
  public static final String NAME = "JitsiMeet";

  private BroadcastReceiver onConferenceTerminatedReceiver;

  public JitsiMeetModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  @Deprecated
  @ReactMethod
  public void hangUp() {
    Intent hangUpBroadcastIntent = new  Intent("org.jitsi.meet.HANG_UP");
    LocalBroadcastManager.getInstance(getReactApplicationContext()).sendBroadcast(hangUpBroadcastIntent);
  }

  @ReactMethod
  public void sendActions(ReadableMap actions) {
    ReadableMapKeySetIterator actionsIterator = actions.keySetIterator();
    while (actionsIterator.hasNextKey()) {
      String key = actionsIterator.nextKey();
      Intent genericBroadcastIntent = new Intent("org.jitsi.meet." + key);
      if(!actions.isNull(key)) {
        ReadableMap valueMap = actions.getMap(key);
        ReadableMapKeySetIterator valueIterator = valueMap.keySetIterator();
        while (valueIterator.hasNextKey()) {
          String valKey = valueIterator.nextKey();
          ReadableType type = valueMap.getType(valKey);
          switch (type) {
            case String:
              String stringVal = valueMap.getString(valKey);
              genericBroadcastIntent.putExtra(valKey, stringVal);
              break;
            case Boolean:
              Boolean booleanVal = valueMap.getBoolean(valKey);
              genericBroadcastIntent.putExtra(valKey, booleanVal);
              break;

            default:
              throw new IllegalArgumentException("Could not read object with key: " + key);
          }
        }

      }
      LocalBroadcastManager.getInstance(getReactApplicationContext()).sendBroadcast(genericBroadcastIntent);
    }
  }


  @ReactMethod
  public void launchJitsiMeetView(ReadableMap options, Promise onConferenceTerminated) {
    JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();

    if (options.hasKey("room")) {
      builder.setRoom(options.getString("room"));
    } else {
      throw new RuntimeException("Room must not be empty");
    }

    try {
      builder.setServerURL(
        new URL(options.hasKey("serverUrl") ? options.getString("serverUrl") : "https://app.meet-yard.com"));
    } catch (MalformedURLException e) {
      throw new RuntimeException("Server url invalid");
    }

    if (options.hasKey("userInfo")) {
      ReadableMap userInfoMap = options.getMap("userInfo");

      if (userInfoMap != null) {
        JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();

        if (userInfoMap.hasKey("displayName")) {
          userInfo.setDisplayName(userInfoMap.getString("displayName"));
        }

        if (userInfoMap.hasKey("email")) {
          userInfo.setEmail(userInfoMap.getString("email"));
        }

        if (userInfoMap.hasKey("avatar")) {
          try {
            userInfo.setAvatar(new URL(userInfoMap.getString("avatar")));
          } catch (MalformedURLException e) {
            throw new RuntimeException("Avatar url invalid");
          }
        }

        builder.setUserInfo(userInfo);
      }
    }

    if (options.hasKey("token")) {
      builder.setToken(options.getString("token"));
    }

    // Set built-in config overrides
    if (options.hasKey("subject")) {
      builder.setSubject(options.getString("subject"));
    }

    if (options.hasKey("audioOnly")) {
      builder.setAudioOnly(options.getBoolean("audioOnly"));
    }

    if (options.hasKey("audioMuted")) {
      builder.setAudioMuted(options.getBoolean("audioMuted"));
    }

    if (options.hasKey("videoMuted")) {
      builder.setVideoMuted(options.getBoolean("videoMuted"));
    }

    // Set the feature flags
    if (options.hasKey("featureFlags")) {
      ReadableMap featureFlags = options.getMap("featureFlags");
      ReadableMapKeySetIterator iterator = featureFlags.keySetIterator();
      while (iterator.hasNextKey()) {
        String flag = iterator.nextKey();
        Boolean value = featureFlags.getBoolean(flag);
        builder.setFeatureFlag(flag, value);
      }
    }

    JitsiMeetActivityExtended.launchExtended(getReactApplicationContext(), builder.build());

    this.registerOnConferenceTerminatedListener(onConferenceTerminated);
  }

  @ReactMethod
  public void launch(ReadableMap options, Promise onConferenceTerminated) {
    launchJitsiMeetView(options, onConferenceTerminated);
  }

  private void registerOnConferenceTerminatedListener(Promise onConferenceTerminated) {
    onConferenceTerminatedReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        BroadcastEvent event = new BroadcastEvent(intent);

        onConferenceTerminated.resolve(null);

        LocalBroadcastManager.getInstance(getReactApplicationContext()).unregisterReceiver(onConferenceTerminatedReceiver);
      }
    };

    IntentFilter intentFilter = new IntentFilter(BroadcastEvent.Type.CONFERENCE_TERMINATED.getAction());

    LocalBroadcastManager.getInstance(getReactApplicationContext()).registerReceiver(this.onConferenceTerminatedReceiver, intentFilter);
  }
}
