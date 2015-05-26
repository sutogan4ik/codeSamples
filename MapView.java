package ru.ibecom.mapmodule;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ru.ibecom.mapmodule.interfaces.DefaultSettings;
import ru.ibecom.mapmodule.interfaces.JSInterfaceListener;
import ru.ibecom.mapmodule.interfaces.JsMethods;
import ru.ibecom.mapmodule.interfaces.MapOnLoadListener;
import ru.ibecom.mapmodule.interfaces.TerminalListener;

/**
 * Created by prog on 01.04.15.
 */
public class MapView extends WebView{
    private DefaultSettings settings;
    private MapOnLoadListener loadListener;
    private JSTerminalListener terminalListener;
    public void setLoadListener(MapOnLoadListener loadListener) {
        this.loadListener = loadListener;
    }

    public MapView(Context context) {
        super(context);
        initMap();
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMap();
    }

    public MapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initMap();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initMap();
    }

    private void initMap(){
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        //client = new Client();
    }

    public void setJSListener(JSInterfaceListener listener){
        JSInterface jsInterface = new JSInterface(listener);
        addJavascriptInterface(jsInterface, "JSInterface");
        terminalListener = new JSTerminalListener();
        addJavascriptInterface(terminalListener, "TerminalListener");
    }

    public void clearRoute(){
        loadUrl(JsBuilder(JsMethods.clearRouteLayer));
    }

    public void resetPOIHighlight(){
        loadUrl(JsBuilder(JsMethods.resetPOIHighlight));

    }

    public void setDefaultSettings(DefaultSettings settings){
        this.settings = settings;
        destroyMap();
        loadUrl(this.settings.getMapData().getIndex());
        this.setWebViewClient(new InitClient());

    }

    public void findRouteFromCurrentPosition(String jsonTo, String style){
        final String params = jsonTo + "," + style;
        loadUrl(JsBuilder(JsMethods.findRouteFromCurrentPosition, params));

    }

    public void onTerminalPositionChange(double x, double y, int l, boolean algorithm){
        String params = x + "," + y + "," + l + ", " + algorithm;
        loadUrl(JsBuilder(JsMethods.onTerminalPositionChange, params));
    }

    public void isTerminalExist(TerminalListener listener){
        terminalListener.setListener(listener);
        loadUrl(JsBuilder("isTerminalExist"));
    }

    public void isStartRouteExist(TerminalListener listener){
        terminalListener.setStartListener(listener);
        loadUrl(JsBuilder("isStartRouteExist"));
    }

    public void isEndRouteExist(TerminalListener listener){
        terminalListener.setEndListener(listener);
        loadUrl(JsBuilder("isEndRouteExist"));
    }

    public void findRoute(String start, String end, String style){
        String params = start + "," + end + "," + style;
        loadUrl(JsBuilder(JsMethods.findRoute, params));
    }

    public void findRoute(){
        loadUrl(JsBuilder(JsMethods.findRoute));
    }

    public void higlightPOI(String zoneId, HighlightStyle style, boolean unHiglightPrev){
        String params = zoneId + "," + style + "," + unHiglightPrev;
        loadUrl(JsBuilder(JsMethods.higlightPOI, params));
    }

    public void setCenter(String centerJson){
        loadUrl(JsBuilder(JsMethods.setCenter, centerJson));
    }

    public void setRouteStart(String start){
        loadUrl(JsBuilder(JsMethods.setRouteStart, start));
    }

    public void setRouteEnd(String end){
        loadUrl(JsBuilder(JsMethods.setRouteEnd, end));
    }

    public void drawPossibleRoutes(){
        loadUrl(JsBuilder(JsMethods.drawPossibleRoutes, settings.getMapData().getVisualRoutes()));
    }

    public void clearPossibleRoutes(){
        loadUrl(JsBuilder(JsMethods.clearPossibleRoutes));
    }

    public void destroyMap(){
        clearCache(true);
        loadUrl(JsBuilder(JsMethods.destroyMap));
    }

    private void load() {
        loadUrl(JsInit());
        loadUrl(JsLoadCanvas());
        loadUrl(JsLoadPOI());
        loadUrl(JsLoadRoutes());
        if(loadListener != null){
            loadListener.mapLoaded();
        }
    }

    @Override
    public void loadUrl(final String url) {
        this.post(new Runnable() {
            @Override
            public void run() {
                MapView.super.loadUrl(url);
            }
        });
    }

    private String JsInit(){
        return JsBuilder(JsMethods.Init, settings.getMapData().getMapSettings().toString());
    }

    private String JsLoadCanvas(){
        return JsBuilder(JsMethods.loadMapCanvas, settings.getMapData().getCanvas());
    }

    private String JsLoadPOI(){
        return JsBuilder(JsMethods.loadMapPOI, settings.getMapData().getPoi());
    }

    private String JsLoadRoutes(){
        return JsBuilder(JsMethods.loadRoutes, settings.getMapData().getRoutes());
    }

    private String JsBuilder(String method,String params){

        return "javascript:" + method + "(" + params + ")";
    }
    private String JsBuilder(String method){
        return "javascript:" + method + "()";
    }

    class InitClient extends WebViewClient{
        @Override
        public void onPageFinished(WebView view, String url) {
            load();
        }
    }
}
