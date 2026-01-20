package com.example.onmyway.Entity;

import android.graphics.Bitmap;

public class Avatar {
    private Bitmap avatarBitmap;
    private boolean isSelected;

    public Avatar(Bitmap avatarBitmap, boolean isSelected) {
        this.avatarBitmap = avatarBitmap;
        this.isSelected = isSelected;
    }

    public Bitmap getAvatarBitmap() {
        return avatarBitmap;
    }

    public void setAvatarBitmap(Bitmap avatarBitmap) {
        this.avatarBitmap = avatarBitmap;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
