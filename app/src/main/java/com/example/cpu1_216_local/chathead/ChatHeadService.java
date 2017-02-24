package com.example.cpu1_216_local.chathead;

/**
 * Created by cpu1-216-local on 17/02/2017.
 */

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class ChatHeadService extends Service {
    private static final int DISTANCE_OUT_SCREEN = 25;
    private static final int TIME_ANIMATION_IN = 250;
    private static final int TIME_ANIMATION_OUT = 500;

    private WindowManager windowManager;
    private RelativeLayout chatheadView, removeView;
    private ImageView chatheadImg, removeImg;

    private int x_init_cord, y_init_cord, x_init_margin, y_init_margin, x_pos_click, y_pos_click;
    private Point szWindow = new Point();
    private boolean isOpen = false;

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            chathead_click(0, 0, true);
        }
    };
    @SuppressWarnings("deprecation")

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        if (serviceReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(Utils.ACTION_CLOSE_DIALOG);
            registerReceiver(serviceReceiver, intentFilter);
        }
    }

    private void handleStart(){
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

        //get size screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        //add chatHead button on screen
        WindowManager.LayoutParams paramChatHead = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramChatHead.gravity = Gravity.TOP | Gravity.LEFT;
        paramChatHead.x = 0;
        paramChatHead.y = 100;
        chatheadView = (RelativeLayout) inflater.inflate(R.layout.chathead, null);
        chatheadImg = (ImageView)chatheadView.findViewById(R.id.chathead_img);
        windowManager.addView(chatheadView, paramChatHead);

        //add remove button on screen
        removeView = (RelativeLayout)inflater.inflate(R.layout.remove, null);
        WindowManager.LayoutParams paramRemove = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramRemove.gravity = Gravity.TOP | Gravity.LEFT;
        removeView.setVisibility(View.GONE);
        removeImg = (ImageView)removeView.findViewById(R.id.remove_img);
        windowManager.addView(removeView, paramRemove);

        //set Touch listener for chatHead button
        chatheadView.setOnTouchListener(new View.OnTouchListener() {
            long time_start = 0, time_end = 0;
            boolean isLongclick = false, inBounded = false;
            int remove_img_width = 0, remove_img_height = 0;

            Handler handler_longClick = new Handler();
            Runnable runnable_longClick = new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    isLongclick = true;
                    removeView.setVisibility(View.VISIBLE);
                    chathead_longclick();
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

                int x_cord = (int) event.getRawX();
                int y_cord = (int) event.getRawY();
                int x_cord_Destination, y_cord_Destination;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        time_start = System.currentTimeMillis();
                        handler_longClick.postDelayed(runnable_longClick, 600);

                        remove_img_width = removeImg.getLayoutParams().width;
                        remove_img_height = removeImg.getLayoutParams().height;

                        x_init_cord = x_cord;
                        y_init_cord = y_cord;

                        x_init_margin = layoutParams.x;
                        y_init_margin = layoutParams.y;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int x_diff_move = x_cord - x_init_cord;
                        int y_diff_move = y_cord - y_init_cord;

                        x_cord_Destination = x_init_margin + x_diff_move;
                        y_cord_Destination = y_init_margin + y_diff_move;

                        //when move chatHead button, if chat dialog is open then close chat dialog
                        if (isOpen){
                            ChatDialog.chatDialog.finish();
                        }

                        //EVENT LONG CLICK
                        //when long click chatHead button, show remove button on bottom screen
                        if(isLongclick){
                            int x_bound_left = szWindow.x / 2 - (int)(remove_img_width * 1.5);
                            int x_bound_right = szWindow.x / 2 +  (int)(remove_img_width * 1.5);
                            int y_bound_top = szWindow.y - (int)(remove_img_height * 1.5);

                            if((x_cord >= x_bound_left && x_cord <= x_bound_right) && y_cord >= y_bound_top){
                                inBounded = true;

                                int x_cord_remove = (int) ((szWindow.x - (remove_img_height * 1.5)) / 2);
                                int y_cord_remove = (int) (szWindow.y - ((remove_img_width * 1.5) + getStatusBarHeight() ));

                                if(removeImg.getLayoutParams().height == remove_img_height){
                                    removeImg.getLayoutParams().height = (int) (remove_img_height * 1.5);
                                    removeImg.getLayoutParams().width = (int) (remove_img_width * 1.5);

                                    WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                                    param_remove.x = x_cord_remove;
                                    param_remove.y = y_cord_remove;

                                    windowManager.updateViewLayout(removeView, param_remove);
                                }

                                layoutParams.x = x_cord_remove + (Math.abs(removeView.getWidth() - chatheadView.getWidth())) / 2;
                                layoutParams.y = y_cord_remove + (Math.abs(removeView.getHeight() - chatheadView.getHeight())) / 2 ;

                                windowManager.updateViewLayout(chatheadView, layoutParams);
                                break;
                            }else{
                                inBounded = false;
                                removeImg.getLayoutParams().height = remove_img_height;
                                removeImg.getLayoutParams().width = remove_img_width;

                                WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                                int x_cord_remove = (szWindow.x - removeView.getWidth()) / 2;
                                int y_cord_remove = szWindow.y - (removeView.getHeight() + getStatusBarHeight() );

                                param_remove.x = x_cord_remove;
                                param_remove.y = y_cord_remove;

                                windowManager.updateViewLayout(removeView, param_remove);
                            }

                        }
                        layoutParams.x = x_cord_Destination;
                        layoutParams.y = y_cord_Destination;
                        windowManager.updateViewLayout(chatheadView, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP:
                        isLongclick = false;
                        removeView.setVisibility(View.GONE);
                        removeImg.getLayoutParams().height = remove_img_height;
                        removeImg.getLayoutParams().width = remove_img_width;
                        handler_longClick.removeCallbacks(runnable_longClick);

                        //when touching chatHead button is finish, if chatHead button on remove button then remove chatHead button
                        if(inBounded){
                            if(ChatDialog.active){
                                ChatDialog.chatDialog.finish();
                            }

                            stopService(new Intent(ChatHeadService.this, ChatHeadService.class));
                            inBounded = false;
                            break;
                        }


                        int x_diff = x_cord - x_init_cord;
                        int y_diff = y_cord - y_init_cord;

                        //EVENT CLICK
                        if(Math.abs(x_diff) < 5 && Math.abs(y_diff) < 5){
                            time_end = System.currentTimeMillis();
                            if((time_end - time_start) < 300){
                                if (!isOpen){
                                    x_pos_click = x_cord - x_init_cord + x_init_margin;
                                    y_pos_click = y_cord - y_init_cord + y_init_margin;
                                }
                                chathead_click(x_cord - x_init_cord + x_init_margin, y_cord - y_init_cord + y_init_margin, isOpen);
                                isOpen = !isOpen;
                                break;

                            }
                        }

                        y_cord_Destination = y_init_margin + y_diff;
                        x_cord_Destination = x_init_margin + x_diff;

                        int BarHeight =  getStatusBarHeight();
                        if (y_cord_Destination < 0) {
                            y_cord_Destination = 0;
                        } else if (y_cord_Destination + (chatheadView.getHeight() + BarHeight) > szWindow.y) {
                            y_cord_Destination = szWindow.y - (chatheadView.getHeight() + BarHeight);
                        }
                        layoutParams.y = y_cord_Destination;

                        inBounded = false;

                        //when touching chatHead button is finish,
                        //if chat dialog open then chatHead button move to the right-top screen corner
                        //else chatHead button move to left screen border or right screen border
                        if (!isOpen) {
                            resetPosition(x_cord, y_cord);
                        } else{
                            chathead_click(x_cord_Destination, y_cord_Destination, false);
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if(layoutParams.y + (chatheadView.getHeight() + getStatusBarHeight()) > szWindow.y){
                layoutParams.y = szWindow.y - (chatheadView.getHeight() + getStatusBarHeight());
                windowManager.updateViewLayout(chatheadView, layoutParams);
            }

            if(layoutParams.x != 0 && layoutParams.x < szWindow.x){
                resetPosition(szWindow.x, 0);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){

            if(layoutParams.x > szWindow.x){
                resetPosition(szWindow.x, 0);
            }

        }

    }

    //chatHead button move to left screen border or right screen border
    private void resetPosition(int x_cord_now, int y_cord_row) {
        if(x_cord_now <= szWindow.x / 2){
            moveToLeft(x_cord_now, y_cord_row);
        } else {
            moveToRight(x_cord_now, y_cord_row);
        }

    }

    //Move animation for view from startPos to endPos
    private class MoveAnimation{
        public ValueAnimator animator;
        public MoveAnimation(final View view, final Point startPos, final Point endPos, int duration){
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.ofFloat(0f, 1f);
            animator.setDuration(duration);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) view.getLayoutParams();
                    mParams.x = (int)(startPos.x + (endPos.x - startPos.x) * value);
                    mParams.y = (int)(startPos.y + (endPos.y - startPos.y) * value);
                    windowManager.updateViewLayout(view, mParams);
                }
            });
        }
    }

    //chatHead move to left screen border
    private void moveToLeft(int currentPosX, int currentPosY){

        //chatHead button move from current position to the out left screen position
        int endInPosX = -DISTANCE_OUT_SCREEN;
        int endInPosY = currentPosY;
        if (endInPosY < 0) {
            endInPosY = 0;
        } else if (endInPosY > szWindow.y - chatheadView.getHeight()){
            endInPosY = szWindow.y - chatheadView.getHeight();
        }
        Point startInPos = new Point(currentPosX, currentPosY);
        Point endInPos = new Point(endInPosX, endInPosY);
        ValueAnimator animationIn = new MoveAnimation(chatheadView, startInPos, endInPos, TIME_ANIMATION_IN).animator;

        //chatHead button move from the out left screen position to the left screen border
        int startOutPosX = -DISTANCE_OUT_SCREEN;
        int endOutPosX = 0;
        Point startOutPos = new Point(startOutPosX, endInPosY);
        Point endOutPos = new Point(endOutPosX, endInPosY);
        ValueAnimator animationOut = new MoveAnimation(chatheadView, startOutPos, endOutPos, TIME_ANIMATION_OUT).animator;

        AnimatorSet animation = new AnimatorSet();
        animation.play(animationIn).before(animationOut);
        animation.start();
    }

    private  void moveToRight(int currentPosX, int currentPosY){
        //chatHead button move from current position to the out right screen position
        int endInPosX = szWindow.x - chatheadView.getWidth() + DISTANCE_OUT_SCREEN;
        int endInPosY = currentPosY;
        if (endInPosY < 0) {
            endInPosY = 0;
        } else if (endInPosY > szWindow.y - chatheadView.getHeight()){
            endInPosY = szWindow.y - chatheadView.getHeight();
        }
        Point startInPos = new Point(currentPosX, currentPosY);
        Point endInPos = new Point(endInPosX, endInPosY);
        ValueAnimator animationIn = new MoveAnimation(chatheadView, startInPos, endInPos, TIME_ANIMATION_IN).animator;

        //chatHead button move from the out right screen position to the right screen border
        int startOutPosX = szWindow.x - chatheadView.getWidth() + DISTANCE_OUT_SCREEN;
        int endOutPosX = szWindow.x - chatheadView.getWidth();
        Point startOutPos = new Point(startOutPosX, endInPosY);
        Point endOutPos = new Point(endOutPosX, endInPosY);
        ValueAnimator animationOut = new MoveAnimation(chatheadView, startOutPos, endOutPos, TIME_ANIMATION_OUT).animator;

        AnimatorSet animation = new AnimatorSet();
        animation.play(animationIn).before(animationOut);
        animation.start();
    }

    private int getStatusBarHeight() {
        int statusBarHeight = (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
        return statusBarHeight;
    }

    private void chathead_click(final int x_cord_now, final int y_cord_now, boolean flag){
        //when chat head button is clicked,
        //if chat dialog open
        //then close chat dialog and chat head button move to the previous position of it when it is clicked to open chat dialog
        //else open chat dialog and cat head button move to the right-top screen corner
        if(flag){
            ChatDialog.chatDialog.finish();
            Point startPos = new Point(x_cord_now, y_cord_now);
            Point endPos = new Point(x_pos_click, y_pos_click);
            new MoveAnimation(chatheadView, startPos, endPos, TIME_ANIMATION_IN).animator.start();
        }else{
            Intent it = new Intent(this, ChatDialog.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(it);
            Point startPos = new Point(x_cord_now, y_cord_now);
            Point endPos = new Point(szWindow.x - chatheadView.getWidth(), 0);
            new MoveAnimation(chatheadView, startPos, endPos, TIME_ANIMATION_IN).animator.start();
        }
    }

    //show remove button when chat head button is long clicked
    private void chathead_longclick(){
        WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
        int x_cord_remove = (szWindow.x - removeView.getWidth()) / 2;
        int y_cord_remove = szWindow.y - (removeView.getHeight() + getStatusBarHeight() );

        param_remove.x = x_cord_remove;
        param_remove.y = y_cord_remove;

        windowManager.updateViewLayout(removeView, param_remove);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        handleStart();

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        unregisterReceiver(serviceReceiver);

        if(chatheadView != null){
            windowManager.removeView(chatheadView);
        }

        if(removeView != null){
            windowManager.removeView(removeView);
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }


}