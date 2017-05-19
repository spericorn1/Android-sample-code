package com.tempphoto.ui.fragment;

import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.tempphoto.R;
import com.tempphoto.manager.OverlayManager;
import com.tempphoto.manager.PhotoManager;
import com.tempphoto.manager.TPLocationManager;
import com.tempphoto.models.DotMode;
import com.tempphoto.models.Overlay;
import com.tempphoto.models.OverlayDot;
import com.tempphoto.models.Photo;
import com.tempphoto.ui.activity.MainActivity;
import com.tempphoto.ui.view.DotLayout;
import com.tempphoto.utils.Utils;
import com.tempphoto.voice.Command;
import com.tempphoto.voice.VoiceConfirmation;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;

@EFragment(R.layout.fragment_take_photo)
public class TakePhotoFragment extends BaseFragment {
    private static final String TAG = TakePhotoFragment.class.getSimpleName();

    @ViewById(R.id.infoTextView)
    TextView mInfoTextView;

    @ViewById(R.id.takePhotoButton)
    Button mTakePhotoButton;

    @ViewById(R.id.connectSensorButton)
    ImageButton mConnectSensorButton;

    @ViewById(R.id.savePhotoButton)
    ImageButton mSavePhotoButton;

    @ViewById(R.id.cameraButton)
    ImageButton mCameraButton;

    @ViewById(R.id.clearPhotoButton)
    ImageButton mClearPhotoButton;

    @ViewById(R.id.editModeButton)
    ImageButton mEditModeButton;

    @ViewById(R.id.recordModeButton)
    ImageButton mRecordModeButton;

    @ViewById(R.id.clearDotsButton)
    ImageButton mClearDotsButton;

    @ViewById(R.id.deleteDotsButton)
    ImageButton mDeleteDotsButton;

    @ViewById(R.id.duplicateDotsButton)
    ImageButton mDuplicateDotsButton;

    @ViewById(R.id.dotLayout)
    DotLayout mDotLayout;

    @ViewById(R.id.dotLayoutContainer)
    ViewGroup mDotLayoutContainer;

    @ViewById(R.id.buttonsLayout)
    ViewGroup mButtonsLayout;

    @ViewById(R.id.nameTextView)
    TextView nameTextView;

    @ViewById(R.id.idTextView)
    TextView idTextView;

    @ViewById(R.id.birthDateTextView)
    TextView birthDateTextView;

    @ViewById(R.id.dateTextView)
    TextView dateTextView;

    @ViewById(R.id.photoInfoContainer)
    ViewGroup mPhotoInfoContainer;

    @ViewById(R.id.overlayButtonsGroup)
    RadioGroup overlayButtonsGroup;


    @ViewById(R.id.addNotesLayout)
    ViewGroup addNotesLayout;

    @ViewById(R.id.addNotesButton)
    ImageButton addNotesButton;

    @ViewById(R.id.notesEditText)
    EditText notesEditText;

    @ViewById(R.id.toggleTextModeButton)
    ImageButton toggleTextModeButton;

    ArrayList<RadioButton> overlayButtons = new ArrayList<>();
    int[] overlayButtonIds = {R.id.overlayButton1, R.id.overlayButton2, R.id.overlayButton3, R.id.overlayButton4,
            R.id.overlayButton5, R.id.overlayButton6, R.id.overlayButton7, R.id.overlayButton8};

    int[] overlayIndicatorIds = {R.id.overlayIndicator1, R.id.overlayIndicator2 , R.id.overlayIndicator3, R.id.overlayIndicator4,
            R.id.overlayIndicator5, R.id.overlayIndicator6, R.id.overlayIndicator7, R.id.overlayIndicator8};


    String mCurrentValue = null;

    @AfterViews
    void afterViews() {
        setUpOverlayButtons(null);
        setUpTextViewContainer();

        // Get the size of the relative layout
        ViewTreeObserver observer = mDotLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                // Get the height and width
                mDotLayout.initSize();
                mDotLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    View.OnClickListener overlayButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = ((RadioButton) view).isChecked();

            int index = -1;
            int viewId = view.getId();
            for (int i = 0; i < overlayButtonIds.length; i++) {
                if (viewId == overlayButtonIds[i]) {
                    index = i;
                    break;
                }
            }

            if (index != -1 && checked) {
                String overlayTag  = OverlayManager.getInstance().getOverlayTag(index);
                PhotoManager.getInstance().setSelectedOverlayTag(overlayTag);

                View indicatorView = getView().findViewById(overlayIndicatorIds[index]);
                indicatorView.setBackgroundColor(getResources().getColor(R.color.colorAccent));

                if(index > 0) {
                    mDuplicateDotsButton.setVisibility(View.VISIBLE);
                } else {
                    mDuplicateDotsButton.setVisibility(View.INVISIBLE);
                }

                mDotLayout.invalidate();
            }

            overlayButtonsGroup.clearCheck();
            overlayButtonsGroup.check(viewId);
        }
    };

    protected void setEnabled(RadioButton radioButton, boolean enabled) {
        radioButton.setEnabled(enabled);
        if (enabled) {
            radioButton.setTextColor(getResources().getColor(R.color.colorButtonText));
        } else {
            radioButton.setTextColor(getResources().getColor(R.color.colorButtonBorder));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("ON_RESUME", "ON_RESUME");
        loadCurrentPhoto();
        sensorMessageReceived("");
    }

    @Override
    public void onPause() {
        super.onPause();
        TPLocationManager.getInstance((MainActivity)getActivity()).stop();
    }

    private void setUpTextViewContainer() {
        // Set up the text view container with info
        nameTextView.setText(mPreference.personName().getOr(Utils.NAME_DEFAULT));
        idTextView.setText(mPreference.ID().getOr(""));
        birthDateTextView.setText(mPreference.birthDate().getOr(""));

        if (PhotoManager.getInstance().getCurrentPhoto() != null) {
            dateTextView.setText(PhotoManager.getInstance().getCurrentPhoto().name);
        }
    }

    @Click(R.id.thumbnailsButton)
    protected void showThumbnails() {
        ((MainActivity) getActivity()).showListDialogFragment();
    }

    @Click(R.id.takePhotoButton)
    protected void takePhoto() {
        boolean locationEnabled = mPreference.locationEnabled().getOr(true);
        if(!locationEnabled || TPLocationManager.getInstance((MainActivity)getActivity()).start()) {
            PhotoManager.getInstance().capturePhoto(this);
        }
    }

    @Click(R.id.savePhotoButton)
    protected void savePhoto() {
        savePhotoButtonPressed();
    }



    private boolean saveCurrentPhoto() {
        // to show only the photo view.
        setUpTextViewContainer();

        if (PhotoManager.getInstance().saveCurrentPhoto(mPhotoInfoContainer, mPreference)) {
            showLongToast(getString(R.string.photo_saved));
            return true;
        } else {
            showLongToast(getString(R.string.could_not_save_image));
            return false;
        }
    }


    private boolean savePhotoButtonPressed() {
        // to show only the photo view.
        setUpTextViewContainer();

        if (PhotoManager.getInstance().saveCurrentPhoto(mPhotoInfoContainer, mPreference)) {
            showLongToast(getString(R.string.photo_saved));
            showThumbnails();
            clearPhoto();
            return true;
        } else {
            showLongToast(getString(R.string.could_not_save_image));
            return false;
        }
    }

    @Click(R.id.cameraButton)
    protected void cameraButtonPressed() {
        if (PhotoManager.getInstance().isPhotoPresent()) {
            showSaveConfirmation(null, true);
        } else {
            forceTakePhoto();
        }
    }

    @Click(R.id.clearPhotoButton)
    protected void clearPhotoButtonPressed() {
        if (PhotoManager.getInstance().isPhotoPresent()) {
            showSaveConfirmation(null, false);
        } else {
            clearPhoto();
        }
    }

    public void editPhoto(Photo photo) {
        if (PhotoManager.getInstance().isPhotoPresent()) {
            showSaveConfirmation(photo, false);
        } else {
            setPhotoToEdit(photo);
        }
    }

    public void editPhotoCommand(Command c, final Photo photo) {
        if (PhotoManager.getInstance().isPhotoPresent()) {
            VoiceConfirmation.getInstance((MainActivity) getActivity()).playConfirmationForCommand(c, new VoiceConfirmation.VoiceConfirmationListener() {
                @Override
                public void onConfirmationYes(Command command) {
                    if (saveCurrentPhoto()) {
                        setPhotoToEdit(photo);
                    }
                }

                @Override
                public void onConfirmationNo(Command command) {
                    setPhotoToEdit(photo);
                }

                @Override
                public void onConfirmationCancel(Command command) {
                }
            });
        } else {
            setPhotoToEdit(photo);
        }
    }

    public void setPhotoToEdit(Photo photo) {
        PhotoManager.getInstance().setCurrentPhoto(photo);
        loadCurrentPhoto();
    }

    public void clearPhoto() {
        PhotoManager.getInstance().clearCurrentPhoto();
        loadCurrentPhoto();
    }

    public void forceTakePhoto() {
        clearPhoto();
        takePhoto();
    }

    @Click(R.id.editModeButton)
    protected void toggleEditMode() {
        if (this.mDotLayout.getMode() != DotMode.Setup) {
            setMode(DotMode.Setup);
        } else {
            setMode(DotMode.None);
        }
    }

    @Click(R.id.recordModeButton)
    protected void toggleRecordMode() {
        if (this.mDotLayout.getMode() != DotMode.Record) {
            setMode(DotMode.Record);
        } else {
            setMode(DotMode.None);
        }
    }

    @Click(R.id.duplicateDotsButton)
    protected void duplicateDots() {
        if(PhotoManager.getInstance().getCurrentPhoto().duplicateOverlay())
            mDotLayout.invalidate();
    }

    @Click(R.id.clearDotsButton)
    protected void clearAllDots() {
        AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                .mTitle(getString(R.string.clear_dots))
                .mMessage(getString(R.string.are_you_sure))
                .mPositiveButtonText(getString(R.string.alert_yes))
                .mNeutralButtonText(getString(R.string.alert_no));
        AlertDialog fragment = alert.build();
        fragment.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
            @Override
            public void onPositiveClick() {
                TakePhotoFragment.this.mDotLayout.clearAllDots();
            }

            @Override
            public void onNeutralClick() {
            }

            @Override
            public void onNegativeClick() {
            }
        });
        fragment.show(getFragmentManager(), AlertDialog.TAG);
    }

    @Click(R.id.deleteDotsButton)
    protected void deleteAllDots() {
        AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                .mTitle(getString(R.string.remove_all_dots))
                .mMessage(getString(R.string.are_you_sure))
                .mPositiveButtonText(getString(R.string.alert_yes))
                .mNeutralButtonText(getString(R.string.alert_no));
        AlertDialog fragment = alert.build();
        fragment.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
            @Override
            public void onPositiveClick() {
                TakePhotoFragment.this.mDotLayout.removeAllDots();
            }

            @Override
            public void onNeutralClick() {
                //NOP
            }

            @Override
            public void onNegativeClick() {

            }
        });
        fragment.show(getFragmentManager(), AlertDialog.TAG);
    }

    @OnActivityResult(PhotoManager.REQUEST_CAMERA)
    protected void onCameraResult(int resultCode) {
        PhotoManager.getInstance().onCameraResult(resultCode);
        boolean locationEnabled = mPreference.locationEnabled().getOr(true);
        if(locationEnabled) {
            PhotoManager.getInstance().setLocationToCurrentPhoto(TPLocationManager.getInstance((MainActivity) getActivity()).getLastLocation());
        }
        loadCurrentPhoto();
    }

    protected void loadCurrentPhoto() {
        mDotLayout.loadCurrentPhoto();
        overlayButtons.get(0).performClick();

        if ( PhotoManager.getInstance().isPhotoPresent()) {
            setMode(DotMode.None);
            mTakePhotoButton.setVisibility(View.GONE);

            mSavePhotoButton.setEnabled(true);
            mClearPhotoButton.setEnabled(true);
            mCameraButton.setEnabled(true);

            mEditModeButton.setEnabled(true);
            mRecordModeButton.setEnabled(true);

            mClearDotsButton.setEnabled(true);
            mDeleteDotsButton.setEnabled(true);

            toggleTextModeButton.setEnabled(true);

        } else {
            setMode(DotMode.None);
            mTakePhotoButton.setVisibility(View.VISIBLE);

            mSavePhotoButton.setEnabled(false);
            mClearPhotoButton.setEnabled(false);
            mCameraButton.setEnabled(false);

            mEditModeButton.setEnabled(false);
            mRecordModeButton.setEnabled(false);

            mClearDotsButton.setEnabled(false);
            mDeleteDotsButton.setEnabled(false);

            toggleTextModeButton.setEnabled(false);

        }

        setUpOverlayButtons(PhotoManager.getInstance().getCurrentPhoto());
    }

    private void setUpOverlayButtons(Photo photo) {
        for (int i = 0; i < overlayButtonIds.length; i++) {
            int overlayButtonId = overlayButtonIds[i];
            RadioButton overlayButton = (RadioButton) getView().findViewById(overlayButtonId);
            overlayButton.setText(OverlayManager.getInstance().overlays[i]);
            overlayButton.setOnClickListener(overlayButtonListener);
            String tag = OverlayManager.getInstance().overlays[i];

            Overlay overlay = null;
            if(photo != null) {
                overlay = photo.overlayForTag(tag);
            }
            View view = getView().findViewById(overlayIndicatorIds[i]);
            if(overlay == null) {
                view.setBackgroundColor(getResources().getColor(R.color.colorButtonBackgroundCheckedDark));
            } else {
                view.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            }

            if(OverlayManager.getInstance().overlaysEnabled.contains(tag)){
                setEnabled(overlayButton, true);
            } else {
                setEnabled(overlayButton, false);
            }
            overlayButtons.add(overlayButton);
        }
    }


    public void showSaveConfirmation(final Photo photo, final boolean takePhoto) {
        AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                .mTitle(getString(R.string.photo_not_saved))
                .mMessage(getString(R.string.save_photo))
                .mPositiveButtonText(getString(R.string.save))
                .mNeutralButtonText(getString(R.string.cancel))
                .mNegativeButtonText(getString(R.string.discard));
        AlertDialog fragment = alert.build();
        fragment.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
            @Override
            public void onPositiveClick() {
                if (saveCurrentPhoto()) {
                    if (photo != null) {
                        setPhotoToEdit(photo);
                    } else {
                        clearPhoto();
                        if (takePhoto) {
                            takePhoto();
                        }
                    }
                }
            }

            @Override
            public void onNeutralClick() {
                //NOP
            }

            @Override
            public void onNegativeClick() {
                if (photo != null) {
                    setPhotoToEdit(photo);
                } else {
                    clearPhoto();
                    if (takePhoto) {
                        takePhoto();
                    }
                }
            }
        });
        fragment.show(getFragmentManager(), AlertDialog.TAG);
    }

    public void setMode(DotMode mode) {

        toggleTextModeButton.setActivated(false);
        mEditModeButton.setActivated(false);
        mRecordModeButton.setActivated(false);

        if (mode == DotMode.Setup) {
            mEditModeButton.setActivated(true);
            mDotLayoutContainer.setBackgroundResource(R.drawable.background_status_edit);
        } else  if (mode == DotMode.Record) {
            mRecordModeButton.setActivated(true);
            mDotLayoutContainer.setBackgroundResource(R.drawable.background_status_record);
        } else if(mode == DotMode.None){
            mDotLayoutContainer.setBackgroundResource(R.drawable.background_status);
        } else if(mode == DotMode.Text){
            toggleTextModeButton.setActivated(true);
            mDotLayoutContainer.setBackgroundResource(R.drawable.background_status);
        }
        this.mDotLayout.setMode(mode);
    }


    public void setValueToSelectedDot() {
        if(isAdded()) {
            // Set the temp to the selected OverlayDot
            if (mDotLayout.getMode() == DotMode.Record && mCurrentValue != null) {
                mDotLayout.setValueToSelectedDot(mCurrentValue);
            }
            mDotLayout.invalidate();
        }
    }

    public void showAddNotesLayout() {
        this.addNotesLayout.setVisibility(View.VISIBLE);
        this.notesEditText.setText(mDotLayout.getNoteFromSelectedDot());
    }

    public void dotSelected() {
        if (mDotLayout.getMode() == DotMode.Record) {
            setValueToSelectedDot();
        } else if(mDotLayout.getMode() == DotMode.Text) {
            showAddNotesLayout();
        }
    }

    public void allDotsDeselected() {
//        if(mDotLayout.getMode() == DotMode.Text) {
            dismissKeyboard();
//        }
    }

    public void dismissKeyboard() {
        this.notesEditText.setText("");
        this.addNotesLayout.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(notesEditText.getWindowToken(), 0);
    }

    @Click(R.id.addNotesButton)
    protected void addNotes() {
        String note = notesEditText.getText().toString();
        if (note != null || note.length() > 0) {
            this.mDotLayout.setNoteToSelectedDot(note);
            dismissKeyboard();
        }

        this.mDotLayout.invalidate();
    }

    @Click(R.id.toggleTextModeButton)
    protected void toggleTextMode() {
        if (this.mDotLayout.getMode() != DotMode.Text ) {
            setMode(DotMode.Text);
        } else {
            setMode(DotMode.None);
        }
    }

    /*****************************Commands**************************/

    public void commandCamera(Command c){
        if(isAdded()) {
            if (PhotoManager.getInstance().isPhotoPresent()) {
                VoiceConfirmation.getInstance((MainActivity) getActivity()).playConfirmationForCommand(c, new VoiceConfirmation.VoiceConfirmationListener() {
                    @Override
                    public void onConfirmationYes(Command command) {
                        if (saveCurrentPhoto()) {
                            clearPhoto();
                            takePhoto();
                        }
                    }

                    @Override
                    public void onConfirmationNo(Command command) {
                        clearPhoto();
                        takePhoto();
                    }

                    @Override
                    public void onConfirmationCancel(Command command) {
                    }
                });
            } else {
                clearPhoto();
                takePhoto();
            }
        }
    }

    public void commandSetup(Command c){
        setMode(DotMode.Setup);
    }

    public void commandRemove(Command c) {
        if(isAdded()) {
            VoiceConfirmation.getInstance((MainActivity) getActivity()).playConfirmationForCommand(c, new VoiceConfirmation.VoiceConfirmationListener() {
                @Override
                public void onConfirmationYes(Command command) {
                    int index = (int) Float.parseFloat(command.value) - 1;
                    OverlayDot dot = mDotLayout.getDot(index);
                    if (dot != null) {
                        mDotLayout.removeDot(dot);
                    }
                    mDotLayout.invalidate();
                }

                @Override
                public void onConfirmationNo(Command command) {

                }

                @Override
                public void onConfirmationCancel(Command command) {

                }
            });
        }
    }

    public void commandClearDots(Command c) {
        if(isAdded()) {
            VoiceConfirmation.getInstance((MainActivity) getActivity()).playConfirmationForCommand(c, new VoiceConfirmation.VoiceConfirmationListener() {
                @Override
                public void onConfirmationYes(Command command) {
                    mDotLayout.removeAllDots();
                }

                @Override
                public void onConfirmationNo(Command command) {

                }

                @Override
                public void onConfirmationCancel(Command command) {

                }
            });
        }
    }

    public void commandExit(Command c) {
        setMode(DotMode.None);
    }

    public void commandGo(Command c) {
        int index = (int) Float.parseFloat(c.value) - 1;
        OverlayDot dot = mDotLayout.getDot(index);
        if (dot != null) {
            mDotLayout.setSelectedDot(dot);
        }

        setValueToSelectedDot();

        mDotLayout.invalidate();
    }

    public void commandDone(Command c) {
        mDotLayout.deselectDots();
    }

    public void commandPlay(Command c) {
        int index = (int) Float.parseFloat(c.value) - 1;
        OverlayDot dot = mDotLayout.getDot(index);
        if (dot != null) {
            String text = dot.getValue();

            ((MainActivity)getActivity()).playText(text);
        }
    }

    public void commandClear(Command c) {
        int index = (int) Float.parseFloat(c.value) - 1;
        OverlayDot dot = mDotLayout.getDot(index);
        if (dot != null) {
            dot.setValue("0");
        }
        mDotLayout.invalidate();
    }

    public void commandClearAll(Command c) {
        if(isAdded()) {
            mDotLayout.clearAllDots();
        }
    }

    public void commandSave(Command c) {
        savePhoto();
    }

    public void commandListPhotos(Command c) {
        showThumbnails();
    }

    public void commandClearPhoto(Command c) {
        if(isAdded()) {
            VoiceConfirmation.getInstance((MainActivity) getActivity()).playConfirmationForCommand(c, new VoiceConfirmation.VoiceConfirmationListener() {
                @Override
                public void onConfirmationYes(Command command) {
                    if(saveCurrentPhoto()) {
                        clearPhoto();
                    }
                }

                @Override
                public void onConfirmationNo(Command command) {
                    clearPhoto();
                }

                @Override
                public void onConfirmationCancel(Command command) {

                }
            });
        }
    }


    /*********************************************Bluetooth***********************************************/
    public void sensorMessageReceived(String message) {
        if(isAdded()) {
            if (message != null) {
                mInfoTextView.setText(message);
                mInfoTextView.setSelected(true);
            } else {
                mInfoTextView.setText("");
            }
        }
    }

    public void sensorValueReceived(String value) {
        if(isAdded()) {
            if (value != null) {
                // Save the value
                mCurrentValue = value;

//                if (!PhotoManager.getInstance().isPhotoPresent()) {
//                    sensorMessageReceived(getString(R.string.awaiting_new_photo));
//                } else
                {
                    // Show the temp
                    Log.d(TAG, "Setting temp : " + mCurrentValue);
                    mInfoTextView.setText(mCurrentValue);
                    mInfoTextView.setSelected(true);

                    setValueToSelectedDot();
                }
            } else {
                mInfoTextView.setText("");
            }
        }
    }

    /****************************Connect sensor button**************************/

    public void deviceConnected() {
        if (isAdded()) {
            mConnectSensorButton.setImageResource(R.drawable.ic_connect_done);
            mConnectSensorButton.setEnabled(false);
        }
    }

    public void deviceConnecting() {
        if (isAdded()) {
            mConnectSensorButton.setImageResource(R.drawable.ic_connect_progress);
            mConnectSensorButton.setEnabled(false);
        }
    }

    public void deviceNotConnected() {
        if (isAdded()) {
            mConnectSensorButton.setImageResource(R.drawable.ic_connect);
            mConnectSensorButton.setEnabled(true);
        }
    }

    @Click
    public void connectSensorButtonClicked() {
        ((MainActivity) getActivity()).initializeBluetoothDevice();
    }


//    @Override
//    public void onSaveInstanceState(Bundle outState) {
////        super.onSaveInstanceState(outState);
//    }
}
