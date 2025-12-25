package Utils;

import android.content.Context;

public class Unit {
    /**
     * dp转px
     * @param context 上下文对象，用于获取资源对象
     * @param dp dp值
     * @return 对应的像素值
     */
    public static int dp2px(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f); // +0.5f避免浮点精度丢失
    }
}
