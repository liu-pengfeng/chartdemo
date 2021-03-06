
package com.cash.hunterchartdemo;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.cash.hunterchartdemo.http.WsManager;
import com.cash.hunterchartdemo.http.WsStatusListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private List<LimitLine> limitLines = new ArrayList<>();

//    private int socketTime = 0;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            initChart();
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
                    client.newBuilder().connectTimeout(5000, TimeUnit.SECONDS);
//                    client.retryOnConnectionFailure();
                    Request request = new Request.Builder()
                            .url("http://47.98.111.79:88/cache/history?subjectId=R_100")
                            .get()
                            .build();
                    Call call = client.newCall(request);
                    Response response = call.execute();
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        Gson gson = new Gson();
                        ChartResponseInfo chartResponseInfo = gson.fromJson(responseData, ChartResponseInfo.class);
                        chartBeans = chartResponseInfo.getData();
                        Log.e("LineChartActivity1", "chartBeans.size = " + chartBeans.size());

                        mHandler.sendEmptyMessage(0);
                        Log.w(TAG + " success", responseData);
                    } else {
                        Log.w(TAG + " fail", "" + Thread.currentThread());
//                        mHandler.sendEmptyMessage(1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

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
            if (!chartBeans.isEmpty()) {
                chartBeans.add(bean);
                refreshChartLastData();
            }
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

    private void initSocket() {
        wsBaseManager = new WsManager.Builder(getBaseContext())
                .client(new OkHttpClient().newBuilder()
                        .pingInterval(15, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .build())
                .needReconnect(true)
                .wsUrl("ws://47.98.111.79:88/cachews/?subjectId=R_100")
                .build();
        wsBaseManager.setWsStatusListener(wsBaseStatusListener);
        wsBaseManager.startConnect();
    }

    private void initChart() {
        {   // // Chart Style // //
            chart = findViewById(R.id.chart1);
            chart.setBackgroundColor(Color.parseColor("#242E4B"));
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
            xAxis.setLabelCount(5, true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextColor(Color.parseColor("#656E87"));
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    //两秒一次，因此 *2
//                    long timestamp = ((long) (value * 2) + currentTime) * 1000;
                    long timestamp = ((long) value + currentTime) * 1000;
//                    Log.e("LineChartActivity1", "long = " + value);
                    return Util.getDateToString(timestamp);
                }
            });
        }

        YAxis leftAxis, rightAxis;
        {   // // Y-Axis Style // //
            leftAxis = chart.getAxisLeft();
            leftAxis.setEnabled(false);

            rightAxis = chart.getAxisRight();
            rightAxis.setTextColor(Color.parseColor("#AEB5C7"));
        }

        setInitData();

        // draw points over time
        chart.animateX(200);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        // draw legend entries as lines
        l.setForm(LegendForm.LINE);
    }

    //时间区间 默认3分钟 两秒请求一次，所以默认90
    private int timeInterval = 90;

    private List<LimitLine> outTimeLine = new ArrayList<>();

    private void refreshChartLastData() {
//        socketTime++;


        ChartBean bean = chartBeans.get(chartBeans.size() - 1);
        Entry entry = new Entry(bean.getEpoch() - currentTime, Util.getAverage(bean.getAsk(), bean.getBid()));
//        Entry entry = new Entry(timeInterval + socketTime, Util.getAverage(bean.getAsk(), bean.getBid()));
        values.add(entry);

        if (chart == null) {
            return;
        }

        Highlight highlight = new Highlight(values.get(values.size() - 1).getX(), 0, -1);
        chart.highlightValue(highlight, false);


        //判断限制线是否已经过期
        for (int i = 0; i < limitLines.size(); i++) {
            LimitLine line = limitLines.get(i);
            if (System.currentTimeMillis() - line.getTimestamp() > 62000) {
                outTimeLine.add(line);
            } else {
                break;
            }
        }
        for (LimitLine line : outTimeLine) {
            limitLines.remove(line);
            chart.getAxisRight().removeLimitLine(line);
        }
        outTimeLine.clear();

        LineData data = chart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                return;
            }

            data.addEntry(entry, 0);
            data.notifyDataChanged();

            // 设置X轴最大值，为了指示器不重叠，需要根据显示的分钟值来动态设置
//            chart.getXAxis().setAxisMaximum(timeInterval + socketTime + /*(int) (timeInterval / 6) + */16);
            chart.getXAxis().setAxisMaximum(bean.getEpoch() - currentTime + (int) (timeInterval / 3) + 2);

            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(timeInterval * 2); //TODO 理论上来说这里不需要 *2 啊，难道说这里传的值不是指个数
            //TODO 加动画的话，锚点不是同步进行的，再找其他办法看看
//            chart.moveViewToAnimated(entry.getX(),entry.getY(),set.getAxisDependency(),500);

            chart.moveViewToX(data.getEntryCount());
        }
    }

    private long currentTime;

    private LineDataSet set1;

    /**
     * 设置初始数据
     */
    private void setInitData() {
        currentTime = chartBeans.get(chartBeans.size() - timeInterval).getEpoch();
        for (int i = 0; i < timeInterval; i++) {
            ChartBean bean = chartBeans.get(chartBeans.size() - (timeInterval - i));
            Log.d("LineChartActivity1", i + "  " + bean.toString());
            Entry entry = new Entry(bean.getEpoch() - currentTime, Util.getAverage(bean.getAsk(), bean.getBid()));
            values.add(entry);
        }
        Log.e("LineChartActivity1", "values.size():" + values.size());

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

            //自定义高亮时的十字指示
            // draw selection line as dashed
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setHighLightColor(Color.parseColor("#717A96"));
            set1.setDrawHorizontalHighlightIndicator(false);

            // set the filled area
            set1.setDrawFilled(true);

            // set color of filled area
            if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.BLACK);
            }

            List<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets

            // create a data object with the data sets
            LineData data = new LineData(dataSets);

            // set data
            chart.setData(data);
        }
    }

    //450
    public void click_15m(View view) {
        clearDataAndReset(450);
    }

    //300
    public void click_10m(View view) {
        clearDataAndReset(300);
    }

    //150
    public void click_5m(View view) {
        clearDataAndReset(150);

//        Map map = new HashMap();
//        map.put("amount", String.valueOf(chartBeans.get(chartBeans.size() - 1).getAsk()));
//        map.put("accountType", "0");
//        addLimitLine(map);
    }

    //90
    public void click_3m(View view) {
        clearDataAndReset(90);
//        startActivity(new Intent(this, MainMpChartActivity.class));
//        Map map = new HashMap();
//        map.put("amount", String.valueOf(chartBeans.get(chartBeans.size() - 1).getAsk()));
//        map.put("accountType", "1");
//        addLimitLine(map);
    }

    private void clearDataAndReset(int i) {
        values.clear();
        chart.getLineData().clearValues();
        timeInterval = i;
        setInitData();
    }

    private void addLimitLine(Map<String, String> map) {
        if (map == null) {
            return;
        }
        int accountType = Integer.parseInt(map.get("accountType"));
        int lineColor = accountType == 0 ? Color.parseColor("#D23104") : Color.parseColor("#62A763");
        String amount = map.get("amount");
        LimitLine ll1 = new LimitLine(Float.parseFloat(amount == null ? "0" : amount), amount);
        ll1.setLineWidth(1f);
        ll1.setLineColor(lineColor);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextColor(lineColor);
        ll1.setTextSize(12f);
        ll1.setTimestamp(System.currentTimeMillis());

        limitLines.add(ll1);

        chart.getAxisRight().addLimitLine(ll1);
    }


    // 页面都销毁了，还留着长链接干啥
    @Override
    protected void onDestroy() {
        super.onDestroy();
        wsBaseManager.stopConnect();
        wsBaseManager = null;
    }
}