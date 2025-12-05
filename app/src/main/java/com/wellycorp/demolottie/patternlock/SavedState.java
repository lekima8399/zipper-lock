package com.wellycorp.demolottie.patternlock;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

/**
 * Created by ThuanND on 12/5/2025
 */
public class SavedState extends View.BaseSavedState {

    private final String mSerializedPattern;
    private final int mDisplayMode;
    private final boolean mInputEnabled;
    private final boolean mInStealthMode;
    private final boolean mTactileFeedbackEnabled;

    /**
     * Constructor called from {@link PatternLockView#onSaveInstanceState()}
     */
    SavedState(Parcelable superState, String serializedPattern,
                       int displayMode, boolean inputEnabled, boolean inStealthMode,
                       boolean tactileFeedbackEnabled) {
        super(superState);

        mSerializedPattern = serializedPattern;
        mDisplayMode = displayMode;
        mInputEnabled = inputEnabled;
        mInStealthMode = inStealthMode;
        mTactileFeedbackEnabled = tactileFeedbackEnabled;
    }

    /**
     * Constructor called from {@link #CREATOR}
     */
    private SavedState(Parcel in) {
        super(in);

        mSerializedPattern = in.readString();
        mDisplayMode = in.readInt();
        mInputEnabled = (Boolean) in.readValue(null);
        mInStealthMode = (Boolean) in.readValue(null);
        mTactileFeedbackEnabled = (Boolean) in.readValue(null);
    }

    public String getSerializedPattern() {
        return mSerializedPattern;
    }

    public int getDisplayMode() {
        return mDisplayMode;
    }

    public boolean isInputEnabled() {
        return mInputEnabled;
    }

    public boolean isInStealthMode() {
        return mInStealthMode;
    }

    public boolean isTactileFeedbackEnabled() {
        return mTactileFeedbackEnabled;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mSerializedPattern);
        dest.writeInt(mDisplayMode);
        dest.writeValue(mInputEnabled);
        dest.writeValue(mInStealthMode);
        dest.writeValue(mTactileFeedbackEnabled);
    }

    @SuppressWarnings("unused")
    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

        public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
        }

        public SavedState[] newArray(int size) {
            return new SavedState[size];
        }
    };
}
