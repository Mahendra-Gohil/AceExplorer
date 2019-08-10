package com.siju.acexplorer.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.siju.acexplorer.R;
import com.siju.acexplorer.common.types.FileInfo;
import com.siju.acexplorer.main.model.FileConstants;
import com.siju.acexplorer.main.model.groups.Category;

import java.io.File;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.siju.acexplorer.main.model.helper.AppUtils.getAppIconForFolder;

public class ThumbnailUtils {

    private static final Uri AUDIO_URI = Uri.parse("content://media/external/audio/albumart");

    public static void displayThumb(Context context, FileInfo fileInfo, Category category, ImageView
            imageIcon, ImageView imageThumbIcon) {

        String filePath = fileInfo.getFilePath();
        String fileName = fileInfo.getFileName();
        boolean isDirectory = fileInfo.isDirectory();

        switch (category) {

            case FILES:
            case DOWNLOADS:
            case COMPRESSED:
            case FAVORITES:
            case PDF:
            case APPS:
            case LARGE_FILES:
            case ZIP_VIEWER:
            case RECENT_APPS:
                if (isDirectory) {
                    imageIcon.setImageResource(R.drawable.ic_folder);
                    if (imageThumbIcon != null) {
                        Drawable apkIcon = getAppIconForFolder(context, fileName); // TODO: 10/01/18 It should be package name and not filename
                        if (apkIcon != null) {
                            imageThumbIcon.setVisibility(View.VISIBLE);
                            imageThumbIcon.setImageDrawable(apkIcon);
                        } else {
                            imageThumbIcon.setVisibility(View.GONE);
                            imageThumbIcon.setImageDrawable(null);
                        }
                    }

                } else {
                    if (imageThumbIcon != null) {
                        imageThumbIcon.setVisibility(View.GONE);
                        imageThumbIcon.setImageDrawable(null);
                    }

                    imageIcon.setImageDrawable(null);
                    String extension = fileInfo.getExtension();
                    if (extension != null) {
                        changeFileIcon(context, imageIcon, extension
                                .toLowerCase(), filePath);
                    } else {
                        imageIcon.setImageResource(R.drawable.ic_doc_white);
                    }
                }
                setThumbHiddenFilter(imageIcon, fileName);
                break;
            case AUDIO:
            case RECENT_AUDIO:
                displayAudioAlbumArt(context, fileInfo.getBucketId(), imageIcon,
                                     filePath);
                setThumbHiddenFilter(imageIcon, fileName);
                break;

            case VIDEO:
            case GENERIC_VIDEOS:
            case FOLDER_VIDEOS:
            case RECENT_VIDEOS:
                displayVideoThumb(context, imageIcon, filePath);
                setThumbHiddenFilter(imageIcon, fileName);
                break;

            case IMAGE: // For images group
            case GENERIC_IMAGES:
            case FOLDER_IMAGES:
            case RECENT_IMAGES:
                displayImageThumb(context, imageIcon, filePath);
                setThumbHiddenFilter(imageIcon, fileName);
                break;
            case DOCS: // For docs group
            case RECENT_DOCS:
                String extension = fileInfo.getExtension();
                extension = extension.toLowerCase();
                changeFileIcon(context, imageIcon, extension, null);
                setThumbHiddenFilter(imageIcon, fileName);
                break;
            case APP_MANAGER:
                loadAppIcon(context, imageIcon, fileInfo.getFilePath());
                setThumbHiddenFilter(imageIcon, fileName);
                break;
            default:
                imageIcon.setImageResource(R.drawable.ic_folder);
                break;

        }
    }

    private static void setThumbHiddenFilter(ImageView imageIcon, String fileName) {
        if (fileName.startsWith(".")) {
            imageIcon.setColorFilter(Color.argb(200, 255, 255, 255));
        } else {
            imageIcon.clearColorFilter();
        }
    }


    private static void displayVideoThumb(Context context, ImageView imageIcon, String path) {
        Uri videoUri = Uri.fromFile(new File(path));
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.ic_movie);

        Glide.with(context).load(videoUri)
                .apply(options)
                .into(imageIcon);
    }

    private static void displayImageThumb(Context context, ImageView imageIcon, String path) {
        Uri imageUri = Uri.fromFile(new File(path));
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.ic_image_default);
        Glide.with(context).load(imageUri).transition(withCrossFade(2))
                .apply(options)
                .into(imageIcon);
    }

    private static void displayAudioAlbumArt(Context context, long bucketId, final ImageView imageIcon,
                                             String path) {
        if (bucketId != -1) {
            Uri uri = ContentUris.withAppendedId(AUDIO_URI, bucketId);
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.ic_music_default);
            Glide.with(context).load(uri).apply(options)
                    .into(imageIcon);
        } else {
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = new String[]{MediaStore.Audio.Media.ALBUM_ID};
            String selection = MediaStore.Audio.Media.DATA + " = ?";
            String[] selectionArgs = new String[]{path};

            Cursor cursor = context.getContentResolver().query(uri, projection, selection,
                                                               selectionArgs,
                                                               null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    Glide.with(context).clear(imageIcon);
                    int albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media
                                                                            .ALBUM_ID);

                    long albumId = cursor.getLong(albumIdIndex);

                    Uri newUri = ContentUris.withAppendedId(AUDIO_URI, albumId);
                    RequestOptions options = new RequestOptions()
                            .centerCrop()
                            .placeholder(R.drawable.ic_music_default)
                            .error(R.drawable.ic_music_default)
                            .fallback(R.drawable.ic_music_default);
                    Glide.with(context).load(newUri)
                            .apply(options)
                            .into(new SimpleTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    imageIcon.setImageDrawable(resource);
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    imageIcon.setImageDrawable(errorDrawable);
                                }
                            });
                }
                cursor.close();
            } else {
                imageIcon.setImageResource(R.drawable.ic_music_default);
            }
        }
    }

    private static void loadAppIcon(Context context, ImageView imageIcon, String name) {
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.ic_apk_green)
                .diskCacheStrategy(DiskCacheStrategy.NONE); // cannot disk cache
        // ApplicationInfo, nor Drawables

        Glide.with(context)
                .as(Drawable.class)
                .apply(options.dontAnimate().dontTransform().priority(Priority.LOW))
                .load(name)
                .into(imageIcon);

    }

    private static void changeFileIcon(Context context, ImageView imageIcon, String extension, String
            path) {
        switch (extension) {
            case FileConstants.APK_EXTENSION:
                loadAppIcon(context, imageIcon, path);

                break;
            case FileConstants.EXT_DOC:
            case FileConstants.EXT_DOCX:
                imageIcon.setImageResource(R.drawable.ic_doc);
                break;
            case FileConstants.EXT_XLS:
            case FileConstants.EXT_XLXS:
            case FileConstants.EXT_CSV:
                imageIcon.setImageResource(R.drawable.ic_xls);
                break;
            case FileConstants.EXT_PPT:
            case FileConstants.EXT_PPTX:
                imageIcon.setImageResource(R.drawable.ic_ppt);
                break;
            case FileConstants.EXT_PDF:
                imageIcon.setImageResource(R.drawable.ic_pdf);
                break;
            case FileConstants.EXT_TEXT:
                imageIcon.setImageResource(R.drawable.ic_txt);
                break;
            case FileConstants.EXT_HTML:
                imageIcon.setImageResource(R.drawable.ic_html);
                break;
            case FileConstants.EXT_ZIP:
                imageIcon.setImageResource(R.drawable.ic_file_zip);
                break;
            default:
                imageIcon.setImageResource(R.drawable.ic_doc_white);
                break;
        }
    }
}
