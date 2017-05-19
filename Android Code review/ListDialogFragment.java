package com.tempphoto.ui.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.tempphoto.R;
import com.tempphoto.app.TempPhotoApplication;
import com.tempphoto.manager.PhotoManager;
import com.tempphoto.models.Photo;
import com.tempphoto.ui.activity.MainActivity;
import com.tempphoto.ui.adapter.ThumbnailListAdapter;
import com.tempphoto.ui.layoutmanager.AutoFitGridRecyclerView;
import com.tempphoto.utils.Email;
import com.tempphoto.utils.Utils;
import com.tempphoto.voice.Command;
import com.tempphoto.voice.VoiceConfirmation;

import org.androidannotations.annotations.Click;

import java.io.File;
import java.util.ArrayList;

public class ListDialogFragment extends DialogFragment implements ThumbnailListAdapter.ThumbnailActionListener {
    private AutoFitGridRecyclerView mRecyclerView;
    private ThumbnailListAdapter mThumbnailsAdapter;

    public DialogInterface.OnDismissListener onDismissListener = null;
    public ThumbnailListAdapter.ListMode listMode = ThumbnailListAdapter.ListMode.ListModeThumbnailsAction;


    public static ListDialogFragment newInstance(ThumbnailListAdapter.ListMode listMode, DialogInterface.OnDismissListener onDismissListener) {
        ListDialogFragment f = new ListDialogFragment();
        f.listMode = listMode;
        f.onDismissListener = onDismissListener;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        ImageButton closeButton = (ImageButton) view.findViewById(R.id.closeButton);
        ImageButton takePhotoButton = (ImageButton) view.findViewById(R.id.takePhotoButton);

        Button clearAllButton = (Button) view.findViewById(R.id.clearAllButton);
        Button sendAllButton = (Button) view.findViewById(R.id.sendAllButton);


        ViewGroup buttonsLayout = (ViewGroup) view.findViewById(R.id.buttonsLayout);

        if (listMode == ThumbnailListAdapter.ListMode.ListModeThumbnailsAction) {
            buttonsLayout.setVisibility(View.VISIBLE);
            closeButton.setVisibility(View.VISIBLE);
        } else {
            buttonsLayout.setVisibility(View.VISIBLE);
            closeButton.setVisibility(View.VISIBLE);
        }

        mRecyclerView = (AutoFitGridRecyclerView) view.findViewById(R.id.recycler_view);
        mThumbnailsAdapter = new ThumbnailListAdapter(getActivity(), listMode, this);
        mRecyclerView.setAdapter(mThumbnailsAdapter);

        ViewGroup containerLayout = (ViewGroup) view.findViewById(R.id.containerLayout);
        containerLayout.setBackgroundColor(0xddffffff);
        containerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListDialogFragment.this.dismiss();
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListDialogFragment.this.dismiss();
            }
        });

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListDialogFragment.this.dismiss();
                TempPhotoApplication.getMainActivity().forceTakePhoto();
            }
        });

        sendAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mailAllPhotos();
            }
        });

        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearAllConfirmationAlert();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return view;
    }

    public void notifyList() {
        mThumbnailsAdapter.notifyDataSetChanged();
    }

    @Override
    public void mailPhoto(Photo photo) {
        try {
            Email email = new Email(getReceipents(),
                    ((MainActivity) getActivity()).mPreference.subjectEmail().getOr(""), ((MainActivity) getActivity()).mPreference.personName().getOr(Utils.DEFAULT_EMAIL_MAIN),
                    Html.fromHtml(((MainActivity) getActivity()).mPreference.messageBody().getOr("")));
            ArrayList<Uri> URIs = new ArrayList<>();
            URIs.addAll(photo.getOutputImageAndTxtURIs());
            email.attachPictures(URIs);
            Intent emailIntent = email.lickStamps();
            startActivity(emailIntent);
        } catch (Exception e) {
            ((MainActivity) getActivity()).showLongToast("Something went wrong");
        }
    }

    @Override
    public void editPhoto(Photo photo) {
        ((MainActivity) getActivity()).editPhoto(photo);
        ListDialogFragment.this.dismiss();
    }

    @Override
    public void viewPhoto(Photo photo) {
        ViewImageDialogFragment.newInstance(photo).show(getFragmentManager(), "ViewPhoto");
    }

    @Override
    public void deletePhoto(Photo photo) {
        showDeleteConfirmationAlert(photo);
    }

    public void deletePhotoAction(Photo photo) {
        PhotoManager.getInstance().deletePhoto(photo);
        notifyList();
    }

    public String[] getReceipents() {
        String main = ((MainActivity) getActivity()).mPreference.mainEmail().getOr(Utils.DEFAULT_EMAIL_MAIN);
        String sec = ((MainActivity) getActivity()).mPreference.secondaryEmail().getOr("");
        String third = ((MainActivity) getActivity()).mPreference.thirdEmail().getOr("");
        return new String[]{main, sec, third};
    }

    public void mailAllPhotos() {
        if (PhotoManager.getInstance().photos.size() == 0) {
            return;
        }
        try {
            Email email = new Email(getReceipents(),
                    ((MainActivity) getActivity()).mPreference.subjectEmail().getOr(""), ((MainActivity) getActivity()).mPreference.personName().getOr(Utils.DEFAULT_EMAIL_MAIN),
                    Html.fromHtml(((MainActivity) getActivity()).mPreference.messageBody().getOr("")));
            ArrayList<Uri> URIs = new ArrayList<>();
            for (Photo photo : PhotoManager.getInstance().photos) {
                URIs.addAll(photo.getOutputImageAndTxtURIs());
            }
            email.attachPictures(URIs);
            Intent emailIntent = email.lickStamps();
            startActivity(emailIntent);
        } catch (Exception e) {
            ((MainActivity) getActivity()).showLongToast("Something went wrong");
        }
    }

    private void showClearAllConfirmationAlert() {
        if (PhotoManager.getInstance().photos.size() == 0) {
            return;
        }
        AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                .mTitle(getString(R.string.attention_title))
                .mMessage(getString(R.string.want_to_delete_all_files))
                .mPositiveButtonText(getString(R.string.alert_yes))
                .mNeutralButtonText(getString(R.string.alert_no));
        AlertDialog fragment = alert.build();
        fragment.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
            @Override
            public void onPositiveClick() {
                PhotoManager.getInstance().deleteAllPhotos();
                notifyList();
                ListDialogFragment.this.dismiss();
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

    private void showDeleteConfirmationAlert(final Photo photo) {
        AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                .mTitle(getString(R.string.attention_title))
                .mMessage(getString(R.string.want_to_delete_file, photo.name))
                .mPositiveButtonText(getString(R.string.alert_yes))
                .mNeutralButtonText(getString(R.string.alert_no));
        AlertDialog fragment = alert.build();
        fragment.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
            @Override
            public void onPositiveClick() {
                deletePhotoAction(photo);
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


    /*****************************
     * Commands
     **************************/

    public void commandModifyPhoto(Command c) {
        if (isAdded()) {
            Photo photo = PhotoManager.getInstance().getPhotoWithTag(c.value);
            if (photo != null) {
                ((MainActivity) getActivity()).editPhotoCommand(c, photo);
                ListDialogFragment.this.dismiss();
            }
        }
    }

    public void commandDeletePhoto(Command c) {
        if (isAdded()) {
            VoiceConfirmation.getInstance((MainActivity) getActivity()).playConfirmationForCommand(c, new VoiceConfirmation.VoiceConfirmationListener() {
                @Override
                public void onConfirmationYes(Command command) {
                    Photo photo = PhotoManager.getInstance().getPhotoWithTag(command.value);
                    if (photo != null) {
                        deletePhotoAction(photo);
                    }
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

    public void commandSendPhoto(Command c) {
        if (isAdded()) {
            Photo photo = PhotoManager.getInstance().getPhotoWithTag(c.value);
            if (photo != null) {
                mailPhoto(photo);
            }
        }
    }

    public void commandSendAll(Command c) {
        if (isAdded()) {
            mailAllPhotos();
        }
    }

    public void commandClearAll(Command c) {
        if (isAdded()) {
            if (PhotoManager.getInstance().photos.size() == 0) {
                return;
            }
            VoiceConfirmation.getInstance((MainActivity) getActivity()).playConfirmationForCommand(c, new VoiceConfirmation.VoiceConfirmationListener() {
                @Override
                public void onConfirmationYes(Command command) {
                    PhotoManager.getInstance().deleteAllPhotos();
                    notifyList();
                    ListDialogFragment.this.dismiss();
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
}
