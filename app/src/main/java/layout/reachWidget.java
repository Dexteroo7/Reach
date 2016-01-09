package layout;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import reach.project.R;
import reach.project.reachProcess.reachService.MusicHandler;
import reach.project.reachProcess.reachService.ProcessManager;

/**
 * Implementation of App Widget functionality.
 */
public class reachWidget extends AppWidgetProvider
{

    /*static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,int appWidgetId)
    {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.reach_widget);
        views.setTextViewText(R.id.appwidget_text, widgetText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }*/
    public static String state="play";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them

        for (int appWidgetId : appWidgetIds)
        {
            RemoteViews remoteViews=new RemoteViews(context.getPackageName(),R.layout.reach_widget);

            Log.i("reachwidget", "start");


            //Intent pauseIntent=new Intent();
            //pauseIntent.setAction("widgetPause");
            //PendingIntent pausePendingIntent=PendingIntent.getBroadcast(context,0,pauseIntent,PendingIntent.FLAG_CANCEL_CURRENT);
            //remoteViews.setOnClickPendingIntent(R.id.play_widget,pausePendingIntent);

            remoteViews.setOnClickPendingIntent(R.id.play_widget,PendingIntent.getService(context, 0, new Intent(MusicHandler.ACTION_PLAY_PAUSE, null, context, ProcessManager.class), 0));
            remoteViews.setOnClickPendingIntent(R.id.next_widget,PendingIntent.getService(context, 0, new Intent(MusicHandler.ACTION_NEXT, null, context, ProcessManager.class), 0));
            remoteViews.setOnClickPendingIntent(R.id.prev_widget,PendingIntent.getService(context, 0, new Intent(MusicHandler.ACTION_PREVIOUS, null, context, ProcessManager.class), 0));
            if(state.equals("play"))
                remoteViews.setTextViewText(R.id.play_widget,"PLAY");
            else
                remoteViews.setTextViewText(R.id.play_widget,"PAUSE");
            //Intent nextIntent=new Intent();
            //nextIntent.setAction("widgetNext");
            //PendingIntent nextPendingIntent=PendingIntent.getBroadcast(context,0,nextIntent,PendingIntent.FLAG_CANCEL_CURRENT);
            //remoteViews.setOnClickPendingIntent(R.id.next_widget,nextPendingIntent);

            //Intent prevIntent=new Intent();
            //prevIntent.setAction("widgetPrev");
            //PendingIntent prevPendingIntent=PendingIntent.getBroadcast(context,0,prevIntent,PendingIntent.FLAG_CANCEL_CURRENT);
            //remoteViews.setOnClickPendingIntent(R.id.prev_widget,prevPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId,remoteViews);
        }
    }

    @Override
    public void onEnabled(Context context)
    {
        Log.i("reachwidget","enabled");
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context)
    {
        Log.i("reachwidget","disabled");
        // Enter relevant functionality for when the last widget is disabled
    }
    public static void changeUI(Context context) {
        ComponentName name = new ComponentName(context, reachWidget.class);
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
        for (int appWidgetId : ids) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.reach_widget);
            if (state.equals("play"))
                remoteViews.setTextViewText(R.id.play_widget, "PLAY");
            else
                remoteViews.setTextViewText(R.id.play_widget, "PAUSE");
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews);

        }
    }

}

