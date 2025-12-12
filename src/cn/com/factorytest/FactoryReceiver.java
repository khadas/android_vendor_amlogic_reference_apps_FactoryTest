package cn.com.factorytest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import cn.com.factorytest.MainActivity;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FactoryReceiver extends BroadcastReceiver {
  private static final String TAG = Tools.TAG;
  private static final String[] UDISK_FILES = {
      "khadas_test.xml",      "khadas_test_n.xml",  "khadas_test_mcu.xml",
      "khadas_test_test.xml", "khadas_test_2.xml",  "khadas_test_4.xml",
      "khadas_test_8.xml",    "khadas_test_12.xml", "khadas_test_24.xml",
      "khadas_test_48.xml"};
  private static final int[] AGEING_TIMES = {1, 2, 4, 8, 12, 24, 48};

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    Log.d(TAG, "Factory action=" + action);

    if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
      handleBootCompleted(context);
      return;
    }

    Uri uri = intent.getData();
    if (uri != null && "file".equals(uri.getScheme())) {
      handleFileUri(context, uri);
    }
  }

  private void handleBootCompleted(Context context) {
    try {
      String rec =
          Tools.execCommand(new String[] {"sh", "-c", "ls /mnt/media_rw/"});
      Log.e(TAG, "rec=" + rec);
      if (rec == null || rec.isEmpty()) {
        return;
      }
      String[] directories = rec.trim().split("\\s+");
      for (String dir : directories) {
        String bootpath0 = "/storage/" + dir;
        if (checkFiles(context, bootpath0)) {
          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleFileUri(Context context, Uri uri) {
    String path = uri.getPath();
    String externalStoragePath =
        Environment.getExternalStorageDirectory().getPath();
    String legacyPath =
        Environment.getLegacyExternalStorageDirectory().getPath();

    try {
      path = new File(path).getCanonicalPath();
    } catch (IOException e) {
      Log.e(TAG, "couldn't canonicalize " + path);
      return;
    }
    if (path.startsWith(legacyPath)) {
      path = externalStoragePath + path.substring(legacyPath.length());
    }

    checkFiles(context, path);
  }

  private boolean checkFiles(Context context, String path) {
    for (int i = 0; i < UDISK_FILES.length; i++) {
      String fullpath = path + "/" + UDISK_FILES[i];
      Log.e(TAG, "fullpath=" + fullpath);
      File file = new File(fullpath);
      if (file.exists() && file.isFile()) {
        MainActivity.udisk_backup = path;
        Log.e(TAG, "MainActivity.udisk_backup=" + MainActivity.udisk_backup);
        if (i == 2) {
          MainActivity.burn_efuse_flag = false;
        } else {
          MainActivity.burn_efuse_flag = true;
        }
        if (i == 1) {
          try {
            String rec =
                Tools.execCommand(new String[] {"sh", "-c", "cat " + fullpath});
            MainActivity.ageing_flag = 1;
            setAgeingCpuMax(rec);
            setAgeingTime(rec);
            MainActivity.ageing_test = true;
            Log.e(TAG, "hlm ageing_flag=" + MainActivity.ageing_flag +
                           "  ageing_time=" + MainActivity.ageing_time);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (i >= 3) {
          return is_file_exit(context, path, UDISK_FILES[i],
                              AGEING_TIMES[i - 3]);
        }
        goto_factorytest(context, fullpath);
        return true;
      }
    }
    return false;
  }

  private boolean is_file_exit(Context context, String path, String udiskfile,
                               int time) {
    String fullpath = path + "/" + udiskfile;
    File file = new File(fullpath);
    if (file.exists() && file.isFile()) {
      MainActivity.ageing_flag = 1;
      MainActivity.ageing_cpu_max = 2;
      MainActivity.ageing_time = time;
      MainActivity.ageing_test = true;
      Log.e(TAG, "hlm ageing_flag=" + MainActivity.ageing_flag +
                     "  ageing_time=" + MainActivity.ageing_time);

      goto_factorytest(context, fullpath);
      return true;
    }
    return false;
  }

  private void goto_factorytest(Context context, String fullpath) {
    File file = new File(fullpath);
    if (file.exists() && file.isFile()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      try {
        String rec =
            Tools.execCommand(new String[] {"sh", "-c", "cat " + fullpath});
        if (rec.contains("reboot_test=1")) {
          int reboot_num = Settings.System.getInt(context.getContentResolver(),
                                                  "Khadas_reboot_test_num", 0);
          Toast
              .makeText(context, "Reboot Test : " + reboot_num,
                        Toast.LENGTH_LONG)
              .show();
          try {
            Thread.sleep(10 * 1000);
            Settings.System.putInt(context.getContentResolver(),
                                   "Khadas_reboot_test_num", reboot_num + 1);
            Thread.sleep(1 * 1000);
            java.lang.Process proc =
                Runtime.getRuntime().exec(new String[] {"reboot"});
            proc.waitFor();
          } catch (Exception e) {
            e.printStackTrace();
          }
          return;
        }
        Log.e(TAG, "factorytest=[" + rec + "]");
        if (setTestBoard(rec)) {
          setTestFlags(rec);
        }
        getCheckVersionInfo(fullpath);
        startMainActivity(context);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private boolean setTestBoard(String rec) {
        boolean ret = true;
    if (rec.contains("test_board=VIM1S")) {
      MainActivity.test_board = "VIM1S";
    } else if (rec.contains("test_board=VIM2")) {
      MainActivity.test_board = "VIM2";
    } else if (rec.contains("test_board=VIM3")) {
      MainActivity.test_board = "VIM3";
    } else if (rec.contains("test_board=VIM4")) {
      MainActivity.test_board = "VIM4";
    } else {
      MainActivity.test_board = "VIM4";
      //setTestFlags(rec);
      // tfcard_test
      MainActivity.tfcard_test = true;
      // usb20_test
      MainActivity.usb20_test = true;
      // usb30_test
      MainActivity.usb30_test = true;
      // mcu_test
      MainActivity.mcu_test = true;
      // hdmi_test
      MainActivity.hdmi_test = true;
      // gigabit_test
      MainActivity.gigabit_test = true;
      // lan_test
      MainActivity.lan_test = true;
      // rtc_test
      MainActivity.rtc_test = true;
      // wifi_test
      MainActivity.wifi_test = true;
      // bt_test
      MainActivity.bt_test = true;
      MainActivity.gsensor_test = true;
      MainActivity.fusb302_test = true;
      MainActivity.key_test = true;
      // wirte_mac
      MainActivity.wirte_mac = true;
      MainActivity.reset_mcu = true;
      MainActivity.mic_test = true;
      ret = false;
    }
    return ret;
  }

  private void setTestFlags(String rec) {
    MainActivity.tfcard_test = rec.contains("tfcard_test=1");
    MainActivity.usb20_test = rec.contains("usb20_test=1");
    MainActivity.usb30_test = rec.contains("usb30_test=1");
    MainActivity.spi_test = rec.contains("spi_test=1");
    MainActivity.gsensor_test = rec.contains("gsensor_test=1");
    MainActivity.mcu_test = rec.contains("mcu_test=1");
    MainActivity.hdmi_test = rec.contains("hdmi_test=1");
    MainActivity.fusb302_test = rec.contains("fusb302_test=1");
    MainActivity.gigabit_test = rec.contains("gigabit_test=1");
    MainActivity.lan_test = rec.contains("lan_test=1");
    MainActivity.wifi_test = rec.contains("wifi_test=1");
    MainActivity.bt_test = rec.contains("bt_test=1");
    MainActivity.rtc_test = rec.contains("rtc_test=1");
    MainActivity.power_led_test = false; // remove power_led_test
    MainActivity.irkey_test = rec.contains("irkey_test=1");
    MainActivity.wol_enable = rec.contains("wol_enable=1");
    MainActivity.mic_test = rec.contains("mic_test=1");
    MainActivity.mipi_camera_test = rec.contains("mipi_camera_test=1");
    MainActivity.board_key_test = rec.contains("board_key_test=1");
    MainActivity.reset_mcu = rec.contains("reset_mcu=1");
    MainActivity.wirte_mac = rec.contains("wirte_mac=1");
    MainActivity.wirte_sn = rec.contains("wirte_sn=1");
    MainActivity.mipi_lcd_test = rec.contains("mipi_lcd_test=1");
    MainActivity.tp_test = rec.contains("tp_test=1");
    MainActivity.key_test = rec.contains("key_test=1");
  }

    private void getCheckVersionInfo(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Log.e(TAG, "line:" + line);
                if (line != null && !line.isEmpty()) {
                    if (line.toLowerCase().contains("mcu_ver")) {
                        MainActivity.check_mcu_ver = extractMcuVersion(line);
                        Log.e(TAG, "Get mcu version:" + MainActivity.check_mcu_ver);
                    } else if (line.toLowerCase().contains("fw_ver")) {
                        MainActivity.check_fw_ver = extractFwVersion(line);
                        Log.e(TAG, "Get fw version:" + MainActivity.check_fw_ver);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractMcuVersion(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        Pattern pattern1 = Pattern.compile("TEST_MCU_VER\\s*=\\s*(\\S*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(input);

        if (matcher1.find()) {
            String value = matcher1.group(1).trim();
            return value.isEmpty() ? "" : value;
        }

        Pattern pattern2 = Pattern.compile("mcu_ver\\s*=\\s*(\\S*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(input);

        if (matcher2.find()) {
            String value = matcher2.group(1).trim();
            return value.isEmpty() ? "" : value;
        }

        return "";
    }

    private String extractFwVersion(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        Pattern pattern1 = Pattern.compile("TEST_FW_VER\\s*=\\s*(\\S*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(input);

        if (matcher1.find()) {
            String value = matcher1.group(1).trim();
            return value.isEmpty() ? "" : value;
        }

        Pattern pattern2 = Pattern.compile("fw_ver\\s*=\\s*(\\S*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(input);

        if (matcher2.find()) {
            String value = matcher2.group(1).trim();
            return value.isEmpty() ? "" : value;
        }

        return "";
    }


  private void setAgeingTime(String rec) {
    Pattern pattern = Pattern.compile("ageing_time=(\\d+)");
    Matcher matcher = pattern.matcher(rec);
    if (matcher.find()) {
      try {
        int time = Integer.parseInt(matcher.group(1));
        if (time > 0) {
          MainActivity.ageing_time = time;
          Log.e(TAG, "Set ageing_time from file: " + time);
        }
      } catch (NumberFormatException e) {
        Log.e(TAG, "Invalid ageing_time value");
      }
    }
  }

  private void setAgeingCpuMax(String rec) {
    Pattern pattern = Pattern.compile("ageing_cpu_max=(\\d+)");
    Matcher matcher = pattern.matcher(rec);
    if (matcher.find()) {
      try {
        int max = Integer.parseInt(matcher.group(1));
        if (max >= 0 && max <= 8) {
          MainActivity.ageing_cpu_max = max;
          Log.e(TAG, "Set ageing_cpu_max from file:  " + max);
        } else {
          Log.e(TAG, "ageing_cpu_max out of range (0-8): " + max);
        }
      } catch (NumberFormatException e) {
        Log.e(TAG, "Invalid ageing_cpu_max value");
      }
    }
  }

  private void startMainActivity(Context context) {
    Intent i = new Intent();
    i.setClassName("cn.com.factorytest", "cn.com.factorytest.MainActivity");
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(i);
  }
}
