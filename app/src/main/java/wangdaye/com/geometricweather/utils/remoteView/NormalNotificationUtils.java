package wangdaye.com.geometricweather.utils.remoteView;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.data.entity.model.weather.Weather;
import wangdaye.com.geometricweather.utils.TimeUtils;
import wangdaye.com.geometricweather.utils.helpter.IntentHelper;
import wangdaye.com.geometricweather.utils.helpter.WeatherHelper;

/**
 * Normal notification utils.
 * */

public class NormalNotificationUtils {
    // data
    private static final int NOTIFICATION_ID = 317;

    /** <br> UI. */

    public static void buildNotificationAndSendIt(Context context, Weather weather) {
        if (weather == null) {
            return;
        }

        // get sp & realTimeWeather.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // get time & background color.
        boolean isDay = TimeUtils.getInstance(context).getDayTime(context, weather, false).isDayTime();
        boolean backgroundColor = sharedPreferences.getBoolean(
                context.getString(R.string.key_notification_background),
                false);

        // get text color.
        String textColor = sharedPreferences.getString(
                context.getString(R.string.key_notification_text_color),
                "grey");
        int mainColor;
        int subColor;
        switch (textColor) {
            case "dark":
                mainColor = ContextCompat.getColor(context, R.color.colorTextDark);
                subColor = ContextCompat.getColor(context, R.color.colorTextDark2nd);
                break;
            case "grey":
                mainColor = ContextCompat.getColor(context, R.color.colorTextGrey);
                subColor = ContextCompat.getColor(context, R.color.colorTextGrey2nd);
                break;
            case "light":
            default:
                mainColor = ContextCompat.getColor(context, R.color.colorTextLight);
                subColor = ContextCompat.getColor(context, R.color.colorTextLight2nd);
                break;
        }

        // get manager & builder.
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // set notification level.
        if (sharedPreferences.getBoolean(context.getString(R.string.key_notification_hide_icon), false)) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        // set notification visibility.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (sharedPreferences.getBoolean(context.getString(R.string.key_notification_hide_in_lockScreen), false)) {
                builder.setVisibility(Notification.VISIBILITY_SECRET);
            } else {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
        }

        // set small icon.
        builder.setSmallIcon(
                WeatherHelper.getMiniWeatherIcon(weather.realTime.weatherKind, isDay));

        // buildWeather base view.
        RemoteViews base = new RemoteViews(context.getPackageName(), R.layout.notification_base);
        int[] imageId = WeatherHelper.getWeatherIcon(weather.realTime.weatherKind, isDay);
        base.setImageViewResource( // set icon.
                R.id.notification_base_icon,
                imageId[3]);
        base.setTextViewText( // set title.
                R.id.notification_base_title,
                weather.realTime.weather + " " + weather.realTime.temp + "℃");
        base.setTextViewText( // set content.
                R.id.notification_base_content,
                weather.dailyList.get(0).temps[1] + "/" + weather.dailyList.get(0).temps[0] + "°");
        base.setTextViewText( // set time.
                R.id.notification_base_time, weather.base.city + "." + weather.base.time);
        if (backgroundColor) { // set background.
            base.setViewVisibility(R.id.notification_base_background, View.VISIBLE);
        } else {
            base.setViewVisibility(R.id.notification_base_background, View.GONE);
        }
        // set text color.
        base.setTextColor(R.id.notification_base_title, mainColor);
        base.setTextColor(R.id.notification_base_content, subColor);
        base.setTextColor(R.id.notification_base_time, subColor);
        builder.setContent(base); // commit.
        // set intent.
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, IntentHelper.buildMainActivityIntent(context, null), 0);
        builder.setContentIntent(pendingIntent);

        // buildWeather big view.
        RemoteViews big = new RemoteViews(context.getPackageName(), R.layout.notification_big);
        // today
        imageId = WeatherHelper.getWeatherIcon(weather.realTime.weatherKind, isDay);
        big.setImageViewResource( // set icon.
                R.id.notification_base_icon,
                imageId[3]);
        big.setTextViewText( // set title.
                R.id.notification_base_title,
                weather.realTime.weather + " " + weather.realTime.temp + "℃");
        big.setTextViewText( // set content.
                R.id.notification_base_content,
                weather.dailyList.get(0).temps[1] + "/" + weather.dailyList.get(0).temps[0] + "°");
        big.setTextViewText( // set time.
                R.id.notification_base_time, weather.base.city + "." + weather.base.time);
        big.setViewVisibility(R.id.notification_base_background, View.GONE);
        // 1
        big.setTextViewText( // set week 1.
                R.id.notification_big_week_1,
                context.getString(R.string.today));
        big.setTextViewText( // set temps 1.
                R.id.notification_big_temp_1,
                weather.dailyList.get(0).temps[1] + "/" + weather.dailyList.get(0).temps[0] + "°");
        imageId = WeatherHelper.getWeatherIcon( // get icon 1 resource id.
                isDay ? weather.dailyList.get(0).weatherKinds[0] : weather.dailyList.get(0).weatherKinds[1],
                isDay);
        big.setImageViewResource( // set icon 1.
                R.id.notification_big_icon_1,
                imageId[3]);
        // 2
        big.setTextViewText( // set week 2.
                R.id.notification_big_week_2,
                weather.dailyList.get(1).week);
        big.setTextViewText( // set temps 2.
                R.id.notification_big_temp_2,
                weather.dailyList.get(1).temps[1] + "/" + weather.dailyList.get(1).temps[0] + "°");
        imageId = WeatherHelper.getWeatherIcon( // get icon 2 resource id.
                isDay ? weather.dailyList.get(1).weatherKinds[0] : weather.dailyList.get(1).weatherKinds[1],
                isDay);
        big.setImageViewResource( // set icon 2.
                R.id.notification_big_icon_2,
                imageId[3]);
        // 3
        big.setTextViewText( // set week 3.
                R.id.notification_big_week_3,
                weather.dailyList.get(2).week);
        big.setTextViewText( // set temps 3.
                R.id.notification_big_temp_3,
                weather.dailyList.get(2).temps[1] + "/" + weather.dailyList.get(2).temps[0] + "°");
        imageId = WeatherHelper.getWeatherIcon( // get icon 3 resource id.
                isDay ? weather.dailyList.get(2).weatherKinds[0] : weather.dailyList.get(2).weatherKinds[1],
                isDay);
        big.setImageViewResource( // set icon 3.
                R.id.notification_big_icon_3,
                imageId[3]);
        // 4
        big.setTextViewText( // set week 4.
                R.id.notification_big_week_4,
                weather.dailyList.get(3).week);
        big.setTextViewText( // set temps 4.
                R.id.notification_big_temp_4,
                weather.dailyList.get(3).temps[1] + "/" + weather.dailyList.get(3).temps[0] + "°");
        imageId = WeatherHelper.getWeatherIcon( // get icon 4 resource id.
                isDay ? weather.dailyList.get(3).weatherKinds[0] : weather.dailyList.get(3).weatherKinds[1],
                isDay);
        big.setImageViewResource( // set icon 4.
                R.id.notification_big_icon_4,
                imageId[3]);
        // 5
        big.setTextViewText( // set week 5.
                R.id.notification_big_week_5,
                weather.dailyList.get(4).week);
        big.setTextViewText( // set temps 5.
                R.id.notification_big_temp_5,
                weather.dailyList.get(4).temps[1] + "/" + weather.dailyList.get(4).temps[0] + "°");
        imageId = WeatherHelper.getWeatherIcon( // get icon 5 resource id.
                isDay ? weather.dailyList.get(4).weatherKinds[0] : weather.dailyList.get(4).weatherKinds[1],
                isDay);
        big.setImageViewResource( // set icon 5.
                R.id.notification_big_icon_5,
                imageId[3]);
        // set text color.
        big.setTextColor(R.id.notification_base_title, mainColor);
        big.setTextColor(R.id.notification_base_content, subColor);
        big.setTextColor(R.id.notification_base_time, subColor);
        big.setTextColor(R.id.notification_big_week_1, subColor);
        big.setTextColor(R.id.notification_big_week_2, subColor);
        big.setTextColor(R.id.notification_big_week_3, subColor);
        big.setTextColor(R.id.notification_big_week_4, subColor);
        big.setTextColor(R.id.notification_big_week_5, subColor);
        big.setTextColor(R.id.notification_big_temp_1, subColor);
        big.setTextColor(R.id.notification_big_temp_2, subColor);
        big.setTextColor(R.id.notification_big_temp_3, subColor);
        big.setTextColor(R.id.notification_big_temp_4, subColor);
        big.setTextColor(R.id.notification_big_temp_5, subColor);
        // set background.
        big.setViewVisibility(R.id.notification_base_background, View.GONE);
        if (backgroundColor) {
            big.setViewVisibility(R.id.notification_base_background, View.VISIBLE);
            big.setViewVisibility(R.id.notification_big_background, View.VISIBLE);
        } else {
            big.setViewVisibility(R.id.notification_base_background, View.GONE);
            big.setViewVisibility(R.id.notification_big_background, View.GONE);
        }

        // set big view.
        builder.setCustomBigContentView(big);

        // get notification.
        Notification notification = builder.build();

        // set clear flag
        if (sharedPreferences.getBoolean(context.getString(R.string.key_notification_can_be_cleared), false)) {
            // the notification can be cleared
            notification.flags = Notification.FLAG_AUTO_CANCEL;
        } else {
            // the notification can not be cleared
            notification.flags = Notification.FLAG_ONGOING_EVENT;
        }

        // commit.
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /** <br> data. */

    public static boolean isEnable(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(
                        context.getString(R.string.key_notification),
                        false);
    }
}
