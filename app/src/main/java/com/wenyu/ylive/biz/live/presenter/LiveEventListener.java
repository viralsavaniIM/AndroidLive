package com.wenyu.ylive.biz.live.presenter;

import com.wenyu.mvp.presenter.MvpEventListener;

/**
 * Created by chan on 17/5/2.
 */

public interface LiveEventListener extends MvpEventListener {
    void onLiveChecked(boolean checked);

    void onRoomConfigFinished();

    void onSwitchChecked(boolean checked);

    void onMagicChecked(boolean checked);
}
