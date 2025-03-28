package cn.com.factorytest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.*;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TouchTestActivity extends Activity {

  private TouchTestView touchTestView;
  private LinearLayout resultButtons;
  private Context mContext;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    View decorView = getWindow().getDecorView();
    int uiOptions =
        View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    decorView.setSystemUiVisibility(uiOptions);
    setContentView(R.layout.touchtest);

    sendBroadcast(new Intent("com.android.hide_upper_bar"));
    sendBroadcast(new Intent("com.android.hide_bottom_bar"));

    mContext = this;

    touchTestView = findViewById(R.id.touchView);
    resultButtons = findViewById(R.id.resultButtons);

    Button btnSuccess = findViewById(R.id.btn_success);
    Button btnFail = findViewById(R.id.btn_fail);

    btnSuccess.setOnClickListener(v -> handleTestResult(true));
    btnFail.setOnClickListener(v -> handleTestResult(false));
  }

  public void showResultButtons() {
    runOnUiThread(() -> resultButtons.setVisibility(View.VISIBLE));
  }

  public void hideResultButtons() {
    runOnUiThread(() -> resultButtons.setVisibility(View.GONE));
  }

  private void handleTestResult(boolean success) {
    if (success) {
      Settings.System.putInt(mContext.getContentResolver(), "Khadas_tp_test",
                             1);
      finish();
    } else {
      Settings.System.putInt(mContext.getContentResolver(), "Khadas_tp_test",
                             0);
      finish();
    }
    touchTestView.resetTest();
  }
}
