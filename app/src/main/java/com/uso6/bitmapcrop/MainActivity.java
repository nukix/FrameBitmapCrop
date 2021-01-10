package com.uso6.bitmapcrop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_PHOTO = 999;

    private Button btnOpen;
    private ImageView ivSource;
    private ImageView ivTarget;
    private EditText etRow, etCol;
    private Button btnCrop;
    private TextView tvWidth, tvHeight;
    private TextView tvFrame;
    private Button btnPrevious, btnNext;

    private int srcWidth, srcHeight; // 原图宽高

    private int inputRow, inputCol;

    private int oneWidth, oneHeight; // 单独图片的宽高
    private EditText etSpace; // 播放间隔
    private Button btnPlay; // 播放暂停按钮
    private Button btnSave; // 保存按钮
    private Button btnOpenFile; // 打开所在文件夹
    private EditText etName; // 导出名称
    private Switch btnSwitch; // 导出格式
    private Button btnDelete; // 删除当前帧

    private int position;

    private List<Bitmap> cropBitmaps = new ArrayList<>();

    private boolean isPlay;

    private Handler updateUI = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable = new Runnable() { // 播放线程
        @Override
        public void run() {
            if (isPlay) {
                updateUI.postDelayed(this, delayTime);
            }
            position++;
            position = position % cropBitmaps.size();
            ivTarget.setImageBitmap(cropBitmaps.get(position));
            setFrame();
        }
    };
    private long delayTime; // 延迟时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.btnOpen = findViewById(R.id.btnOpen);
        this.ivSource = findViewById(R.id.ivSource);
        this.ivTarget = findViewById(R.id.ivTarget);
        this.etRow = findViewById(R.id.etRow);
        this.etCol = findViewById(R.id.etCol);
        this.btnCrop = findViewById(R.id.btnCrop);
        this.tvWidth = findViewById(R.id.tvWidth);
        this.tvHeight = findViewById(R.id.tvHeight);
        this.tvFrame = findViewById(R.id.tvFrame);
        this.btnPrevious = findViewById(R.id.btnPrevious);
        this.btnNext = findViewById(R.id.btnNext);
        this.etSpace = findViewById(R.id.etSpace);
        this.btnPlay = findViewById(R.id.btnPlay);
        this.btnSave = findViewById(R.id.btnSave);
        this.btnOpenFile = findViewById(R.id.btnOpenFile);
        this.etName = findViewById(R.id.etName);
        this.btnSwitch = findViewById(R.id.btnSwitch);
        this.btnDelete = findViewById(R.id.btnDelete);

        this.etRow.setEnabled(false);
        this.etCol.setEnabled(false);
        this.btnCrop.setEnabled(false);
        this.btnPrevious.setEnabled(false);
        this.btnNext.setEnabled(false);
        this.etSpace.setEnabled(false);
        this.btnPlay.setEnabled(false);
        this.btnSave.setEnabled(false);
        this.btnDelete.setEnabled(false);

        this.btnOpen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*"); // 图片
            startActivityForResult(intent, REQUEST_PHOTO); //跳转，传递打开相册请求码
        });

        this.btnOpenFile.setOnClickListener(v -> {
            File rootFile = SDCardUtil.getSDCardFilesDir(this);
            if (!rootFile.exists()) {
                rootFile.mkdirs();
            }
            Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", rootFile);

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(photoURI, "file/*");
            startActivity(intent);
        });

        this.btnDelete.setOnClickListener(v -> {
            if (this.cropBitmaps.size() <= 1) {
                Toast.makeText(this, "必须留下一张图片!", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = this.cropBitmaps.get(this.position);
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            this.cropBitmaps.remove(this.position);

            if (this.position >= this.cropBitmaps.size()) {
                this.position = this.cropBitmaps.size() - 1;
            }

            this.ivTarget.setImageBitmap(this.cropBitmaps.get(position));
            setFrame();
        });

        this.btnSave.setOnClickListener(v -> {
            String name = this.etName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "文件名不能为空!", Toast.LENGTH_SHORT).show();
                return;
            }
            Pattern p = Pattern.compile("^[\\w]+$");
            Matcher m = p.matcher(name);
            if (!m.find()) {
                Toast.makeText(this, "文件名只能为英文字母/数字/下划线!", Toast.LENGTH_SHORT).show();
                return;
            }
            File rootFile = SDCardUtil.getSDCardFilesDir(this);
            if (!rootFile.exists()) {
                rootFile.mkdirs();
            }

            File dir = new File(rootFile, name);

            try {
                deleteDir(dir);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!dir.exists()) {
                dir.mkdirs();
            }

            boolean isPng = this.btnSwitch.isChecked();



            try {
                for (int i = 0; i < cropBitmaps.size(); i++) {
                    Bitmap b = cropBitmaps.get(i);
                    File f = new File(dir, name + "_" + i + "." + (isPng ? "png" : "jpg"));
                    FileOutputStream out = new FileOutputStream(f);
                    if (isPng) {
                        b.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } else {
                        b.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    }
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "保存失败!", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "保存成功!", Toast.LENGTH_SHORT).show();


        });

        this.btnPlay.setOnClickListener(v -> {
            try {
                this.delayTime = Long.parseLong(this.etSpace.getText().toString());
                if (this.delayTime <= 0) {
                    Toast.makeText(this, "时间间隔设置错误!", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "时间间隔设置错误!", Toast.LENGTH_SHORT).show();
                return;
            }
            hideKeyboard(this);
            if (this.isPlay) {
                this.isPlay = false;
                this.btnPlay.setText("播放");
                this.updateUI.removeCallbacks(this.updateRunnable);
            } else {
                this.isPlay = true;
                this.updateUI.postDelayed(this.updateRunnable, this.delayTime);
                this.btnPlay.setText("暂停");
            }
        });

        this.btnPrevious.setOnClickListener(v -> {
            if (this.position > 0) {
                this.position--;
            }
            this.ivTarget.setImageBitmap(this.cropBitmaps.get(position));
            setFrame();
        });

        this.btnNext.setOnClickListener(v -> {
            if (this.position < this.cropBitmaps.size() - 1) {
                this.position++;
            }
            this.ivTarget.setImageBitmap(this.cropBitmaps.get(position));
            setFrame();
        });

        this.btnCrop.setOnClickListener(v -> {
            hideKeyboard(this);
            try {
                this.inputRow = Integer.parseInt(this.etRow.getText().toString());
                this.inputCol = Integer.parseInt(this.etCol.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "行列请输入正确的数字!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (this.srcWidth % this.inputCol != 0 || this.srcHeight % this.inputRow != 0) {
                Toast.makeText(this, "行列不能被整除, 请输入正确的数字!", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Bitmap b : cropBitmaps) {
                if (b != null && !b.isRecycled()) {
                    b.recycle();
                    b = null;
                }
            }
            this.cropBitmaps.clear();

            this.oneWidth = this.srcWidth / this.inputCol;
            this.oneHeight = this.srcHeight / this.inputRow;

            for (int i = 0; i < this.inputRow; i++) {
                for (int j = 0; j < this.inputCol; j++) {
                    Bitmap one = Bitmap.createBitmap(this.srcBitmap, j * this.oneWidth, i * this.oneHeight, this.oneWidth, this.oneHeight);
                    this.cropBitmaps.add(one);
                }
            }

            if (this.cropBitmaps.size() > 0) {
                this.position = 0;
                this.ivTarget.setImageBitmap(this.cropBitmaps.get(0));
                this.etSpace.setEnabled(true);
                this.btnPlay.setEnabled(true);
                setFrame();
            }

        });

    }

    private void deleteDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isFile()) { // 是文件直接删除
            dir.delete();
        } else { // 文件夹删除里面文件后再删除
            for (File f : dir.listFiles()) {
                deleteDir(f);
            }
            dir.delete();
        }
    }

    private void setFrame() {
        this.tvFrame.setText(String.format("当前: %s/%s", this.position + 1, this.cropBitmaps.size()));
        if (this.position == 0) {
            this.btnPrevious.setEnabled(false);
        } else {
            this.btnPrevious.setEnabled(true);
        }

        if (this.position < this.cropBitmaps.size() - 1) {
            this.btnNext.setEnabled(true);
        } else {
            this.btnNext.setEnabled(false);
        }
    }

    private Bitmap srcBitmap; // 原图
    private Bitmap compressBitmap; // 压缩后的图片


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri), null, options);
                srcWidth = options.outWidth;
                srcHeight = options.outHeight;
                options.inSampleSize = Math.max(srcWidth, srcHeight) / 1024;
                options.inJustDecodeBounds = false;


                if (this.compressBitmap != null && !this.compressBitmap.isRecycled()) {
                    this.compressBitmap.recycle();
                    this.compressBitmap = null;
                }

                // 显示压缩后图片
                this.compressBitmap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri), null, options);
                this.ivSource.setImageBitmap(this.compressBitmap);

                if (this.srcBitmap != null && !this.srcBitmap.isRecycled()) {
                    this.srcBitmap.recycle();
                    this.srcBitmap = null;
                }
                this.srcBitmap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri));

                this.tvWidth.setText(String.valueOf(srcWidth));
                this.tvHeight.setText(String.valueOf(srcHeight));

                this.etRow.setEnabled(true);
                this.etCol.setEnabled(true);
                this.btnCrop.setEnabled(true);
                this.btnSave.setEnabled(true);
                this.btnDelete.setEnabled(true);

                this.isPlay = false;
                this.btnPlay.setText("播放");
                this.updateUI.removeCallbacks(this.updateRunnable);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    public void recycleImageView(ImageView view) {
        Drawable drawable = ((ImageView) view).getDrawable();
        if (drawable instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
            if (bmp != null && !bmp.isRecycled()) {
                ((ImageView) view).setImageBitmap(null);
                bmp.recycle();
                bmp = null;
            }
        }
    }

    /**
     * 自动弹软键盘
     *
     * @param context
     * @param et
     */
    public static void showSoftInput(final Context context, final EditText et) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                ((Activity) context).runOnUiThread(() -> {
                    et.setFocusable(true);
                    et.setFocusableInTouchMode(true);
                    //请求获得焦点
                    et.requestFocus();
                    //调用系统输入法
                    InputMethodManager inputManager = (InputMethodManager) et
                            .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.showSoftInput(et, 0);
                });
            }
        }, 200);
    }

    /**
     * 自动关闭软键盘
     *
     * @param activity
     */
    public static void closeKeybord(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    /**
     * 打开关闭相互切换
     *
     * @param activity
     */
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            if (activity.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
}