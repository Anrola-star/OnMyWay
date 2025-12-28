package com.example.onmyway.Utils;

import static com.example.onmyway.Utils.Unit.dp2px;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.EditText;

public class EditTextController {

    /**
     * 设置输入框在错误/非错误时的边框
     *
     * @param context  上下文
     * @param editText 输入框
     * @param isError  是否错误
     */
    public void setEditTextErrorBorder(Context context, EditText editText, int width, int cornerRadius, boolean isError) {

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        //shape.setColor(Color.WHITE);

        int borderColor;    // 边框颜色
        int borderWidth;    // 边框宽度

        if (isError) {
            // 错误
            borderColor = Color.parseColor("#FF3333");  // 红色
            borderWidth = dp2px(context, width);
        } else {
            // 正常
            borderColor = Color.parseColor("#CCCCCC");
            borderWidth = 0;    // 无边框
        }

        shape.setStroke(borderWidth, borderColor);  // 设置边框
        shape.setCornerRadius(dp2px(context, cornerRadius));    // 设置圆角
        editText.setBackground(shape);
    }

    /**
     * 输入框内容改变监听器, 强制恢复正常边框
     *
     * @param context 上下文
     * @param et      输入框
     */
    public void setTextListener(Context context, EditText et) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 输入内容时，强制恢复正常边框
                setEditTextErrorBorder(context, et, 3, 8, false);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        et.setOnFocusChangeListener((v, hasFocus) -> {
            // 获取焦点时，强制恢复正常边框
            if (hasFocus) {
                setEditTextErrorBorder(context, et, 3, 8, false);
            }
        });
    }

    /**
     * 输入框边框宽度内容改变监听器, 边框闪烁动画
     *
     * @param context      上下文
     * @param et           输入框
     * @param colorString  颜色，例：#FF3333 纯红色
     * @param width1       宽度1
     * @param width2       宽度2
     * @param cornerRadius 圆角
     * @param duration     持续时间，单位ms
     * @param finalWidth   最终宽度
     */
    public void startBorderWidthBlinkAnimation(Context context,
                                               EditText et,
                                               String colorString,
                                               int width1, int width2,
                                               int cornerRadius,
                                               int duration,
                                               int finalWidth) {
        // 创建渐变Drawable
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dp2px(context, cornerRadius)); // 圆角

        // 创建宽度动画：宽度1 → 宽度2 → 宽度1 → 宽度2 → finalWidth
        ValueAnimator widthAnimator = ValueAnimator.ofInt(
                dp2px(context, width1),
                dp2px(context, width2),
                dp2px(context, width1),
                dp2px(context, width2),
                dp2px(context, finalWidth)
        );

        widthAnimator.setDuration(duration);

        widthAnimator.addUpdateListener(animation -> {
            int currentWidth = (int) animation.getAnimatedValue();
            shape.setStroke(currentWidth, Color.parseColor(colorString));
            et.setBackground(shape);
        });

        widthAnimator.start();
    }

    /**
     * 输入框边框颜色内容改变监听器, 边框闪烁动画
     *
     * @param context          上下文
     * @param et               输入框
     * @param colorString1     颜色1，例：#FF3333 纯红色
     * @param colorString2     颜色2，例：#FF3333 纯红色
     * @param width            宽度
     * @param cornerRadius     圆角
     * @param duration         持续时间，单位ms
     * @param finalColorString 最终颜色，例：#FFFFFF 白色
     **/
    public void startBorderColorBlinkAnimation(Context context,
                                               EditText et,
                                               String colorString1, String colorString2,
                                               int width,
                                               int cornerRadius,
                                               int duration,
                                               String finalColorString) {
        // 创建渐变Drawable
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dp2px(context, cornerRadius)); // 圆角

        // 定义颜色动画：颜色1 → 颜色2 → 颜色1 → 颜色2 → finalColorString
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(
                Color.parseColor(colorString1), // 半透明红
                Color.parseColor(colorString2),   // 纯红
                Color.parseColor(colorString1), // 半透明红
                Color.parseColor(colorString2),    // 纯红
                Color.parseColor(finalColorString)   // 白色
        );

        colorAnimator.setDuration(duration);

        colorAnimator.addUpdateListener(animation -> {
            // 动画过程中更新边框颜色
            int currentColor = (int) animation.getAnimatedValue();
            shape.setStroke(dp2px(context, width), currentColor);
            et.setBackground(shape);
        });

        colorAnimator.start();
    }
}
