package com.github.alinz.reactnativewebviewbridge;

import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.webview.ReactWebViewManager;

import java.util.Map;

import javax.annotation.Nullable;

public class WebViewBridgeManager extends ReactWebViewManager {
    private static final String REACT_CLASS = "RCTWebViewBridge";

    public static final int COMMAND_SEND_TO_BRIDGE = 101;
    private ThemedReactContext mContext;
    private ViewGroup mContainer;
    private WebView mWebview;
    private WebView mWebviewPop;

    private String target_host_suffix = "vtexcommercebeta.com.br";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public
    @Nullable
    Map<String, Integer> getCommandsMap() {
        Map<String, Integer> commandsMap = super.getCommandsMap();

        commandsMap.put("sendToBridge", COMMAND_SEND_TO_BRIDGE);

        return commandsMap;
    }

    @Override
    protected WebView createViewInstance(ThemedReactContext reactContext) {
        WebView root = super.createViewInstance(reactContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            root.setWebContentsDebuggingEnabled(true);
        }
        mContext = reactContext;
        mContainer = ((ViewGroup) reactContext.getCurrentActivity().findViewById(android.R.id.content));
        mWebview = root;
        root.addJavascriptInterface(new JavascriptBridge(root), "WebViewBridge");
        root.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        root.getSettings().setSupportMultipleWindows(true);
        root.getSettings().getUserAgentString();
        root.setWebViewClient(new MyCustomWebViewClient());
        root.setWebChromeClient(new MyCustomChromeClient());
        return root;
    }

    @Override
    public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
        super.receiveCommand(root, commandId, args);

        switch (commandId) {
            case COMMAND_SEND_TO_BRIDGE:
                sendToBridge(root, args.getString(0));
                break;
            default:
                //do nothing!!!!
        }
    }

    private void sendToBridge(WebView root, String message) {
        String script = "WebViewBridge.onMessage('" + message + "');";
        WebViewBridgeManager.evaluateJavascript(root, script);
    }

    static private void evaluateJavascript(WebView root, String javascript) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            root.evaluateJavascript(javascript, null);
        } else {
            root.loadUrl("javascript:" + javascript);
        }
    }

    @ReactProp(name = "allowFileAccessFromFileURLs")
    public void setAllowFileAccessFromFileURLs(WebView root, boolean allows) {
        root.getSettings().setAllowFileAccessFromFileURLs(allows);
    }

    @ReactProp(name = "allowUniversalAccessFromFileURLs")
    public void setAllowUniversalAccessFromFileURLs(WebView root, boolean allows) {
        root.getSettings().setAllowUniversalAccessFromFileURLs(allows);
    }

    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            Log.d("Webview", "url: " + url + " " + url.contains("/api/vtexid/pub/authentication/oauth/redirect"));
            if (url.contains("/api/vtexid/pub/authentication/oauth/redirect"))
            {
                Log.d("Webview", "pop: " + mWebviewPop);
                Log.d("Webview", "user agent2: " + mWebviewPop.getSettings().getUserAgentString());
                // This is my web site, so do not override; let my WebView load
                // the page
                if(mWebviewPop != null)
                {
                    mWebviewPop.setVisibility(View.GONE);
                    new android.os.Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                Log.d("Webview", "This'll run 2s later");
                                mContainer.removeView(mWebviewPop);
                                mWebviewPop = null;
                            }
                        },
                    2000);
                }
                return false;
            }

//            if(host.equals("m.facebook.com") || host.equals("www.facebook.com"))
//            {
//                return false;
//            }
            return false;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                       SslError error) {
            Log.d("Webview", "onReceivedSslError");
            //super.onReceivedSslError(view, handler, error);
        }

    }

    private class MyCustomChromeClient extends WebChromeClient
    {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
            Log.d("Webview", "onCreateWindow");
            mWebviewPop = new WebView(mContext);
            mWebviewPop.setVerticalScrollBarEnabled(false);
            mWebviewPop.setHorizontalScrollBarEnabled(false);
            mWebviewPop.setWebViewClient(new MyCustomWebViewClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.getSettings().setUserAgentString(mWebview.getSettings().getUserAgentString());
            Log.d("Webview", "user agent: " + mWebviewPop.getSettings().getUserAgentString());
            mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mWebviewPop);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.d("Webview", "onCloseWindow called");
        }

    }
}
