package com.example.onmyway.Entity;

public class Avatar {
    private int avatarResId;
    private boolean isSelected;

    public Avatar(int avatarResId, boolean isSelected) {
        this.avatarResId = avatarResId;
        this.isSelected = isSelected;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    public void setAvatarResId(int avatarResId) {
        this.avatarResId = avatarResId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
