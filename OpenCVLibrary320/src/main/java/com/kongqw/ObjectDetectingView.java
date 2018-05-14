package com.kongqw;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;

/**
 * Created by kqw on 2016/7/13.
 * RobotCameraView
 */
public class ObjectDetectingView extends BaseCameraView {

    private static final String TAG = "ObjectDetectingView";
    private ArrayList<ObjectDetector> mObjectDetects;

    private MatOfRect mObject;

    @Override
    public void onOpenCVLoadSuccess() {
        Log.i(TAG, "onOpenCVLoadSuccess: ");

        mObject = new MatOfRect();

        mObjectDetects = new ArrayList<>();
    }

    @Override
    public void onOpenCVLoadFail() {
        Log.i(TAG, "onOpenCVLoadFail: ");
    }

    public ObjectDetectingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // 子线程（非UI线程）
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        for (ObjectDetector detector : mObjectDetects) {
            // 检测目标
            Rect[] object = detector.detectObject(mGray, mObject);
            for (Rect rect : object) {
                Imgproc.rectangle(mRgba, rect.tl(), rect.br(), detector.getRectColor(), 3);
                savePicture(mRgba);
            }
        }

        return mRgba;
    }

    public boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    /**
     * 保存图片
     *
     * @param frameData 帧数据
     */
    private void savePicture(Mat frameData) {
        Bitmap bitmap = Bitmap.createBitmap(frameData.width(), frameData.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frameData, bitmap);
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // 创建一个文件夹对象，赋值为外部存储器的目录
            File sdcardDir = Environment.getExternalStorageDirectory();
            //得到一个路径，内容是sdcard的文件夹路径和名字
            //            String path = sdcardDir.getPath() + File.separator + "faceImages";
            String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "face";
            File path1 = new File(path);
            if (!path1.exists()) {
                //若不存在，创建目录，可以在应用启动的时候创建
                path1.mkdirs();
            }
            String fileName = path + File.separator + dateFormater.format(new Date(System.currentTimeMillis())) + ".jpg";
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(fileName);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else {
            return;
        }
    }

    /**
     * 特征保存
     *
     * @param image Mat
     * @param rect  人脸信息
     * @return 保存是否成功
     */
    public boolean saveImage(Mat image, Rect rect) {
        try {
            //            String PATH = Environment.getExternalStorageDirectory() + "/FaceDetect/" + fileName + ".jpg";
            // 把检测到的人脸重新定义大小后保存成文件
            Mat sub = image.submat(rect);
            //            Mat mat = new Mat();
            //            Size size = new Size(100, 100);
            //            Imgproc.resize(sub, mat, size);
            //            Highgui.imwrite(PATH, mat);
            savePicture(sub);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 添加检测器
     *
     * @param detector 检测器
     */
    public synchronized void addDetector(ObjectDetector detector) {
        if (!mObjectDetects.contains(detector)) {
            mObjectDetects.add(detector);
        }
    }

    /**
     * 移除检测器
     *
     * @param detector 检测器
     */
    public synchronized void removeDetector(ObjectDetector detector) {
        if (mObjectDetects.contains(detector)) {
            mObjectDetects.remove(detector);
        }
    }

}
