
package com.cash.hunterchartdemo;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.cash.hunterchartdemo.http.WsManager;
import com.cash.hunterchartdemo.http.WsStatusListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.ByteString;

public class LineChartActivity1 extends DemoBase {

    private String TAG = "LineChartActivity1";

    private LineChart chart;
    private WsManager wsBaseManager;

    private List<ChartBean> chartBeans = new ArrayList<>();
    private List<Entry> values = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            initChart();
            startConnect();
        }
    };

    WsStatusListener wsBaseStatusListener = new WsStatusListener() {
        @Override
        public void onOpen(Response response) {
            super.onOpen(response);
            //协议初始化  心跳等
            Log.i("消息s", "" + response.toString());
        }

        @Override
        public void onMessage(String text) {
            super.onMessage(text);
            //消息处理
            ChartBean bean = new Gson().fromJson(text, ChartBean.class);
            Log.i("消息s", bean.toString());
            //放入总数据列，使数据列完整
            chartBeans.add(bean);
        }

        @Override
        public void onMessage(ByteString bytes) {
            super.onMessage(bytes);
            //消息处理
            Log.i("消息ss", "" + bytes.toString());
        }

        @Override
        public void onClosing(int code, String reason) {
            super.onClosing(code, reason);
        }

        @Override
        public void onClosed(int code, String reason) {
            super.onClosed(code, reason);
        }

        @Override
        public void onFailure(Throwable t, Response response) {
            super.onFailure(t, response);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_linechart);

        setTitle("LineChartActivity1");

        sendSyncRequest();

        initSocket();
    }

    private void sendSyncRequest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.w(TAG, "" + Thread.currentThread());
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url("https://supertrade.vip/cache/history?subjectId=R_100")
                            .get()
                            .build();
                    Call call = client.newCall(request);
                    Response response = call.execute();
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        Gson gson = new Gson();
                        ChartResponseInfo chartResponseInfo = gson.fromJson(responseData, ChartResponseInfo.class);
                        chartBeans = chartResponseInfo.getData();

                        mHandler.sendEmptyMessage(0);
                        Log.w(TAG + " success", responseData);
                    } else {
                        Log.w(TAG + " fail", "" + Thread.currentThread());
                        mHandler.sendEmptyMessage(1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initSocket() {
        wsBaseManager = new WsManager.Builder(getBaseContext())
                .client(new OkHttpClient().newBuilder()
                        .pingInterval(15, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .build())
                .needReconnect(true)
                .wsUrl("wss://supertrade.vip/cachews/?subjectId=R_100")
                .build();
        wsBaseManager.setWsStatusListener(wsBaseStatusListener);
    }

    private void startConnect() {
        if (!wsBaseManager.isWsConnected()) {
            Log.e("LineChartActivity1", "startConnect");
            wsBaseManager.startConnect();
        }
    }

    private void initChart() {
        {   // // Chart Style // //
            chart = findViewById(R.id.chart1);

            // background color
            chart.setBackgroundColor(Color.parseColor("#2B3549"));

            // disable description text
            chart.getDescription().setEnabled(false);

            // enable touch gestures
            chart.setTouchEnabled(false);

            chart.setDrawGridBackground(false);

            MyMarkerView2 mv = new MyMarkerView2(this, R.layout.custom_marker_view2);

            // Set the marker to the chart
            mv.setChartView(chart);
            chart.setMarker(mv);

            // force pinch zoom along both axis
            chart.setPinchZoom(true);
        }

        XAxis xAxis;
        {   // // X-Axis Style // //
            xAxis = chart.getXAxis();

            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            xAxis.setLabelCount(30);

            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    Log.d("LineChartActivity1", Util.getDateToString((int) value * 1000l));
                    return Util.getDateToString((int) value * 1000l);
                }
            });
        }

        YAxis yAxis;
        {   // // Y-Axis Style // //
            yAxis = chart.getAxisLeft();

            // disable dual axis (only use LEFT axis)
//            chart.getAxisLeft().setEnabled(false);

            // horizontal grid lines
//            yAxis.enableGridDashedLine(10f, 10f, 0f);

            // axis range
//            yAxis.setAxisMaximum(200f);
//            yAxis.setAxisMinimum(-50f);
        }

        setData();

        {   // // Create Limit Lines // //
//            LimitLine ll2 = new LimitLine(values.get(values.size() - 1).getY(), String.valueOf(values.get(values.size() - 1).getY()));
//            ll2.setLineWidth(1.5f);
//            ll2.setLineColor(Color.WHITE);
//            ll2.setLabelPosition(LimitLabelPosition.RIGHT_BOTTOM);
//            ll2.setTextSize(10f);
//            ll2.setTextColor(Color.WHITE);
//            ll2.setTypeface(tfRegular);
//
//            // draw limit lines behind data instead of on top
//            yAxis.setDrawLimitLinesBehindData(true);
//            // add limit lines
//            yAxis.addLimitLine(ll2);

            Highlight highlight = new Highlight(values.get(values.size() - 1).getX(), 0, -1);
            chart.highlightValue(highlight, false);
        }


        // draw points over time
        chart.animateX(1500);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();

        // draw legend entries as lines
        l.setForm(LegendForm.LINE);
    }

    private void setData() {
        for (int i = 0; i < 30; i++) {
            ChartBean bean = chartBeans.get(chartBeans.size() - i - 1);
            Entry entry = new Entry(bean.getEpoch(), Util.getAverage(bean.getAsk(), bean.getBid()));
//            Log.d("LineChartActivity1", "x = " + entry.getX() + "  y = " + Util.getAverage(bean.getAsk(), bean.getBid())
//                    + "   " + Util.getDateToString((bean.getEpoch() * 1000)));
            values.add(entry);
        }

        LineDataSet set1;

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            set1.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "");

            set1.setDrawIcons(false);
            set1.setDrawCircles(false);
            set1.setColor(Color.parseColor("#67ABF7"));

            // line thickness and point size
            set1.setLineWidth(2f);

            // draw points as solid circles
            set1.setDrawCircleHole(false);

            // customize legend entry
            set1.setFormLineWidth(2f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            // text size of values
//            set1.setValueTextSize(9f);
            set1.setDrawValues(false);

            set1.setDrawVerticalHighlightIndicator(false);
            set1.setHighLightColor(Color.WHITE);
            set1.setHighlightLineWidth(1f);

            // set the filled area
            set1.setDrawFilled(true);
            set1.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return chart.getAxisLeft().getAxisMinimum();
                }
            });

            // set color of filled area
            if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets

            // create a data object with the data sets
            LineData data = new LineData(dataSets);

            // set data
            chart.setData(data);
        }
    }

}