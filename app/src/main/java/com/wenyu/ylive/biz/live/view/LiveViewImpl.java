package com.wenyu.ylive.biz.live.view;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.wenyu.danmuku.DanMaView;
import com.wenyu.danmuku.interfaces.IDanMa;
import com.wenyu.danmuku.type.TextDanMa;
import com.wenyu.mvp.view.BaseMvpView;
import com.wenyu.rtmp.camera.CameraListener;
import com.wenyu.rtmp.configuration.AudioConfiguration;
import com.wenyu.rtmp.configuration.CameraConfiguration;
import com.wenyu.rtmp.configuration.VideoConfiguration;
import com.wenyu.rtmp.stream.packer.rtmp.RtmpPacker;
import com.wenyu.rtmp.stream.sender.rtmp.RtmpSender;
import com.wenyu.rtmp.ui.CameraLivingView;
import com.wenyu.rtmp.video.effect.GrayEffect;
import com.wenyu.rtmp.video.effect.NullEffect;
import com.wenyu.ylive.R;
import com.wenyu.ylive.biz.live.dialog.RoomConfigDialog;
import com.wenyu.ylive.biz.live.presenter.LiveEventListener;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

/**
 * Created by chan on 17/5/2.
 */

public class LiveViewImpl extends BaseMvpView<LiveEventListener> implements ILiveView {

    private GrayEffect mGrayEffect;
    private NullEffect mNullEffect;

    @Bind(R.id.live_view)
    CameraLivingView mLFLiveView;

    @Bind(R.id.live_danma)
    DanMaView mDanMaView;

    private String mTitle;
    private int mCategory;
    private GestureDetector mGestureDetector;
    private RtmpSender mRtmpSender;
    private VideoConfiguration mVideoConfiguration;
    private int mCurrentBps;
    private RoomConfigDialog mRoomConfigDialog;

    private RtmpSender.OnSenderListener mSenderListener = new RtmpSender.OnSenderListener() {
        @Override
        public void onConnecting() {

        }

        @Override
        public void onConnected() {
            mLFLiveView.start();
            mCurrentBps = mVideoConfiguration.maxBps;
        }

        @Override
        public void onDisConnected() {
            Toast.makeText(getActivity(), "fail to live", Toast.LENGTH_SHORT).show();
            mLFLiveView.stop();
        }

        @Override
        public void onPublishFail() {
            Toast.makeText(getActivity(), "fail to publish stream", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNetGood() {
            if (mCurrentBps + 50 <= mVideoConfiguration.maxBps) {
                int bps = mCurrentBps + 50;
                if (mLFLiveView != null) {
                    boolean result = mLFLiveView.setVideoBps(bps);
                    if (result) {
                        mCurrentBps = bps;
                    }
                }
            }
        }

        @Override
        public void onNetBad() {
            if (mCurrentBps - 100 >= mVideoConfiguration.minBps) {
                int bps = mCurrentBps - 100;
                if (mLFLiveView != null) {
                    boolean result = mLFLiveView.setVideoBps(bps);
                    if (result) {
                        mCurrentBps = bps;
                    }
                }
            }
        }
    };

    @Override
    public void release() {
        mLFLiveView.stop();
        mLFLiveView.release();
    }

    @Override
    public void pause() {
        mLFLiveView.pause();
    }

    @Override
    public void resume() {
        mLFLiveView.resume();
    }

    @Override
    public void render(String liveUrl) {
        mRtmpSender.setAddress(liveUrl);
        Toast.makeText(getActivity(), "开始推流", Toast.LENGTH_SHORT).show();
        mRtmpSender.connect();
    }

    @Override
    public int fetchCategory() {
        return mCategory;
    }

    @Override
    public String fetchTitle() {
        return mTitle;
    }

    @Override
    public void showRoomConfigDialog() {
        if (mRoomConfigDialog == null) {
            mRoomConfigDialog = new RoomConfigDialog(getActivity());
            mRoomConfigDialog.setCallback(new RoomConfigDialog.Callback() {
                @Override
                public void onAcceptClicked(String title, int category) {
                    mTitle = title;
                    mCategory = category;
                    if (getEventListener() != null) {
                        getEventListener().onRoomConfigFinished();
                    }
                }
            });
        }

        if (!mRoomConfigDialog.isShowing()) {
            mRoomConfigDialog.show();
        }
    }

    @Override
    public void renderDanma(String message) {
        TextDanMa text = new TextDanMa(message, mDanMaView.getWidth(), mDanMaView.getHeight() / 2, 16, IDanMa.RIGHT_TO_LEFT);
        text.setColor(Color.WHITE);
        mDanMaView.pushDanMa(text);
    }

    @Override
    public void closeOpenBroadcast() {
        mLFLiveView.stop();
    }

    @Override
    public void switchCamera(boolean front) {
        mLFLiveView.switchCamera();
    }

    @Override
    public void enableMagic(boolean enable) {
        mLFLiveView.setEffect(enable ? mGrayEffect : mNullEffect);
    }

    @Inject
    public LiveViewImpl(@NonNull Activity activity) {
        super(activity);

        mGrayEffect = new GrayEffect(getActivity());
        mNullEffect = new NullEffect(getActivity());

        View root = activity.findViewById(R.id.live_container);
        ButterKnife.bind(this, root);

        mLFLiveView.init();
        CameraConfiguration.Builder cameraBuilder = new CameraConfiguration.Builder();
        cameraBuilder.setOrientation(CameraConfiguration.Orientation.LANDSCAPE)
                .setFacing(CameraConfiguration.Facing.BACK);
        CameraConfiguration cameraConfiguration = cameraBuilder.build();
        mLFLiveView.setCameraConfiguration(cameraConfiguration);

        VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
        videoBuilder.setSize(640, 360);
        mVideoConfiguration = videoBuilder.build();
        mLFLiveView.setVideoConfiguration(mVideoConfiguration);

        //设置预览监听
        mLFLiveView.setCameraOpenListener(new CameraListener() {
            @Override
            public void onOpenSuccess() {
                Toast.makeText(getActivity(), "摄像头已经打开", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOpenFail(int error) {
                Toast.makeText(getActivity(), "摄像头打开失败", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCameraChange() {
                Toast.makeText(getActivity(), "切换摄像头", Toast.LENGTH_LONG).show();
            }
        });

        //设置手势识别
        mGestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener());
        mLFLiveView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        //初始化flv打包器
        RtmpPacker packer = new RtmpPacker();
        packer.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mLFLiveView.setPacker(packer);

        //设置发送器
        mRtmpSender = new RtmpSender();
        mRtmpSender.setVideoParams(640, 360);
        mRtmpSender.setAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mRtmpSender.setSenderListener(mSenderListener);
        mLFLiveView.setSender(mRtmpSender);
        mLFLiveView.setLivingStartListener(new CameraLivingView.LivingStartListener() {
            @Override
            public void startError(int error) {
                //直播失败
                Toast.makeText(getActivity(), "推流失败", Toast.LENGTH_SHORT).show();
                mLFLiveView.stop();
            }

            @Override
            public void startSuccess() {
                //直播成功
                Toast.makeText(getActivity(), "推流成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnCheckedChanged(R.id.live_start)
    void onLiveChecked(boolean checked) {
        if (getEventListener() != null) {
            getEventListener().onLiveChecked(checked);
        }
    }

    @OnCheckedChanged(R.id.live_switch)
    void onSwitchChecked(boolean checked) {
        if (getEventListener() != null) {
            getEventListener().onSwitchChecked(checked);
        }
    }

    @OnCheckedChanged(R.id.live_magic)
    void onMagicChecked(boolean checked) {
        if (getEventListener() != null) {
            getEventListener().onMagicChecked(checked);
        }
    }
}
