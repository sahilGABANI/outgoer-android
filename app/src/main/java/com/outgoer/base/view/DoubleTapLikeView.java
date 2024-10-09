package com.outgoer.base.view;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

import androidx.appcompat.widget.AppCompatImageView;

public class DoubleTapLikeView {

    public void animateIcon(final AppCompatImageView view) {
        view.setVisibility(View.VISIBLE);
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.0f, 0.95f,
                0.0f, 0.95f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        AnimationSet firstSet = new AnimationSet(false);
        firstSet.addAnimation(scaleAnimation);
        firstSet.addAnimation(new AlphaAnimation(0.5f, 1.0f));
        firstSet.setDuration(180);
        firstSet.setFillAfter(true);
        view.startAnimation(firstSet);

        firstSet.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ScaleAnimation scaleAnimation = new ScaleAnimation(
                        0.95f, 0.8f,
                        0.95f, 0.8f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(220);
                view.startAnimation(scaleAnimation);

                scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        ScaleAnimation shivalAnimation = new ScaleAnimation(0.8f, 0.84f, 0.8f, 0.84f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                        shivalAnimation.setDuration(100);
                        shivalAnimation.setFillAfter(true);
                        view.startAnimation(shivalAnimation);

                        shivalAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                ScaleAnimation scaleAnimation = new ScaleAnimation(0.84f, 0.0f, 0.84f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);

                                AnimationSet secondAnim = new AnimationSet(true);
                                secondAnim.addAnimation(scaleAnimation);
                                secondAnim.addAnimation(alphaAnimation);
                                secondAnim.setDuration(150);
                                secondAnim.setFillAfter(true);
                                secondAnim.setStartOffset(180);
                                view.startAnimation(secondAnim);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
}
