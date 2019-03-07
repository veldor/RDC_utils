package net.velor.rdc_utils.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

import net.velor.rdc_utils.R;
import net.velor.rdc_utils.SalaryActivity;

import utils.App;
import utils.CashHandler;
import utils.SalaryCalculator;

public class SalaryWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int id) {
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        Float[] info = SalaryCalculator.calculateSalary();
        if (info == null) {
            widgetView.setTextViewText(R.id.totalSumm, CashHandler.addRuble(0));
            widgetView.setTextViewText(R.id.medianSumm, CashHandler.addRuble(0));
            widgetView.setTextViewText(R.id.debetSumm, CashHandler.addRuble(0));
        }
        else{
            widgetView.setTextViewText(R.id.totalSumm, CashHandler.addRuble(info[0]));
            widgetView.setTextViewText(R.id.medianSumm, CashHandler.addRuble(info[1]));
            widgetView.setTextViewText(R.id.debetSumm, CashHandler.addRuble(info[2]));
            int color;
            if(info[2] >= 0){
                color = Color.parseColor("#FF008577");
            }
            else {
                color = Color.parseColor("#FFFF0000");
            }
            widgetView.setInt(R.id.totalSumm, "setTextColor", color);
            widgetView.setInt(R.id.medianSumm, "setTextColor", color);
            widgetView.setInt(R.id.debetSumm, "setTextColor", color);
        }
        Intent startSalaryActivityIntent = new Intent(context, SalaryActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, id, startSalaryActivityIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.widgetRoot, pending);
        appWidgetManager.updateAppWidget(id, widgetView);
    }

    public static void forceUpdateWidget(){
        Context context = App.getInstance();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, SalaryWidget.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(componentName);
        if(widgetIds.length > 0){
            for (int id : widgetIds) {
                updateWidget(context, appWidgetManager, id);
            }
        }
    }
}
