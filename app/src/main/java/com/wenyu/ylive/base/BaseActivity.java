package com.wenyu.ylive.base;


import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.wenyu.ylive.R;

/**
 * Created by jiacheng.li on 17/1/7.
 * Copyright © 2016年 扇贝网(shanbay.com).
 * All rights reserved.
 */
public class BaseActivity extends RxAppCompatActivity {
	private Toolbar mToolbar;
	private YLiveProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			ab.setDisplayHomeAsUpEnabled(true);
		}
	}


	protected Toolbar findToolbarById() {
		return (Toolbar) findViewById(R.id.toolbar_base);
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		getToolbar();
	}

	protected Toolbar getToolbar() {
		if (mToolbar == null) {
			mToolbar = findToolbarById();
			if (mToolbar != null) {
				setSupportActionBar(mToolbar);
				ActionBar ab = getSupportActionBar();
				if (ab != null && displayHomeAsUpEnabled()) {
					ab.setDisplayHomeAsUpEnabled(true);
					mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_black);
				}
			}
		}
		return mToolbar;
	}

	protected boolean displayHomeAsUpEnabled() {
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mToolbar != null && mToolbar.getBackground() != null) {
			mToolbar.getBackground().setAlpha(255);
		}
	}

	public void dismissProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	public void showProgressDialog() {
		showProgressDialog(null);
	}

	public void showProgressDialog(String msg) {
		if (isFinishing()) {
			return;
		}
		if (mProgressDialog == null) {
			mProgressDialog = new YLiveProgressDialog(this);
		}
		mProgressDialog.showProgressDialog(msg);
	}

	public void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	public void showToast(int resId) {
		Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
	}
}
