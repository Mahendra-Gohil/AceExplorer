package com.siju.acexplorer.filesystem.helper;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.siju.acexplorer.BaseActivity;
import com.siju.acexplorer.R;
import com.siju.acexplorer.common.Logger;
import com.siju.acexplorer.filesystem.FileConstants;
import com.siju.acexplorer.filesystem.model.FileInfo;
import com.siju.acexplorer.filesystem.task.CreateZipTask;
import com.siju.acexplorer.filesystem.task.DeleteTask;
import com.siju.acexplorer.filesystem.task.ExtractService;
import com.siju.acexplorer.filesystem.utils.FileOperations;
import com.siju.acexplorer.filesystem.utils.FileUtils;
import com.siju.acexplorer.utils.DialogUtils;
import com.siju.acexplorer.utils.Utils;

import java.io.File;
import java.util.ArrayList;


public class FileOpsHelper {

    private final BaseActivity mActivity;
    private final String TAG = this.getClass().getSimpleName();

    public FileOpsHelper(BaseActivity baseActivity) {
        mActivity = baseActivity;
    }


    public void mkDir(final boolean rootMode, final String path, final String fileName) {
        FileOperations.mkdir(path,fileName, mActivity, rootMode, new FileOperations.FileOperationCallBack() {
            @Override
            public void exists() {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FileUtils.showMessage(mActivity, mActivity.getString(R.string.file_exists));
                        String newFilePath = path + File.separator + fileName;

//                        if (ma != null && ma.getActivity() != null)
                        new FileUtils().createDirDialog(mActivity, rootMode, newFilePath);

                    }
                });
            }

            @Override
            public void launchSAF(final File file) {
//                if (toast != null) toast.cancel();
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.mNewFilePath = path;
                        mActivity.mFileName = fileName;
                        mActivity.mOperation = FileConstants.FOLDER_CREATE;
                        showSAFDialog(mActivity.mNewFilePath);

                    }
                });

            }

            @Override
            public void launchSAF(File oldFile, File newFile) {

            }


            @Override
            public void opCompleted(File hFile, final boolean success) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (success) {
                            Intent intent = new Intent(FileConstants.REFRESH);
                            intent.putExtra(FileConstants.OPERATION, FileConstants.FOLDER_CREATE);
                            mActivity.sendBroadcast(intent);
                            String newFilePath = path + File.separator + fileName;

                            FileUtils.scanFile(mActivity.getApplicationContext(), newFilePath);

                        } else
                            Toast.makeText(mActivity, R.string.msg_operation_failed,
                                    Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }


    public void mkFile(final boolean rootMode, final File file) {
        /*final Toast toast=Toast.makeText(ma.getActivity(), R.string.creatingfolder, Toast.LENGTH_LONG);
        toast.show();*/
        FileOperations.mkfile(file, mActivity, rootMode, new FileOperations.FileOperationCallBack() {
            @Override
            public void exists() {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        if (toast != null) toast.cancel();
                        FileUtils.showMessage(mActivity, mActivity.getString(R.string.file_exists));

//                        if (ma != null && ma.getActivity() != null)
                        new FileUtils().createFileDialog(mActivity, rootMode, file.getAbsolutePath());

                    }
                });
            }

            @Override
            public void launchSAF(final File file) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.mNewFilePath = file.getAbsolutePath();
                        mActivity.mOperation = FileConstants.FILE_CREATE;

                        showSAFDialog(mActivity.mNewFilePath);
                    }
                });

            }

            @Override
            public void launchSAF(File oldFile, File newFile) {

            }


            @Override
            public void opCompleted(final File file, final boolean success) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (success) {
                            Intent intent = new Intent(FileConstants.REFRESH);
                            intent.putExtra(FileConstants.OPERATION, FileConstants.FILE_CREATE);
                            mActivity.sendBroadcast(intent);
                            FileUtils.scanFile(mActivity.getApplicationContext(), file.getAbsolutePath());

                        } else
                            Toast.makeText(mActivity, R.string.msg_operation_failed,
                                    Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }


    public void renameFile(boolean rootmode, final File oldFile, final File newFile, final int position, final boolean
            isDualPane) {
        Logger.log(TAG, "Rename--oldFile=" + oldFile + " new file=" + newFile);
        FileOperations.rename(oldFile, newFile, rootmode, mActivity, new FileOperations.FileOperationCallBack() {
            @Override
            public void exists() {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FileUtils.showMessage(mActivity, mActivity.getString(R.string.file_exists));
                    }
                });
            }

            @Override
            public void launchSAF(final File file) {

            }

            @Override
            public void launchSAF(final File oldFile, final File newFile) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.mRenamedPosition = position;
                        mActivity.mOldFilePath = oldFile.getAbsolutePath();
                        mActivity.mNewFilePath = newFile.getAbsolutePath();
                        mActivity.mOperation = FileConstants.RENAME;
                        mActivity.mFileOpsHelper.showSAFDialog(mActivity.mNewFilePath);

                    }
                });

            }


            @Override
            public void opCompleted(final File file, final boolean success) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            Intent intent = new Intent(FileConstants.REFRESH);
                            intent.putExtra(FileConstants.OPERATION, FileConstants.RENAME);
                            intent.putExtra("position", position);
                            intent.putExtra("old_file", oldFile.getAbsolutePath());
                            intent.putExtra("new_file", file.getAbsolutePath());
                            intent.putExtra(FileConstants.KEY_FOCUS_DUAL, isDualPane);
                            mActivity.sendBroadcast(intent);

                        } else
                            Toast.makeText(mActivity, R.string.msg_operation_failed,
                                    Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void deleteFiles(ArrayList<FileInfo> files,boolean isRooted) {
        if (files == null) return;

        int mode = checkWriteAccessMode(mActivity, new File(files.get(0).getFilePath()).getParentFile());
        if (mode == 2) {
            mActivity.mFiles = files;
            mActivity.mOperation = FileConstants.DELETE;
        } else if (mode == 1 || mode == 0)
            new DeleteTask(mActivity, isRooted, files).execute();
    }

    public void extractFile(File currentFile, File file) {
        int mode = checkWriteAccessMode(mActivity, file.getParentFile());
        if (mode == 2) {
            mActivity.mOldFilePath = currentFile.getAbsolutePath();
            mActivity.mNewFilePath = file.getAbsolutePath();
            mActivity.mOperation = FileConstants.EXTRACT;

        } else if (mode == 1) {
            Intent intent = new Intent(mActivity, ExtractService.class);
            intent.putExtra("zip", currentFile.getPath());
            intent.putExtra("new_path", file.getAbsolutePath());
            new FileUtils().showExtractProgressDialog(mActivity, intent);
        } else Toast.makeText(mActivity, R.string.msg_operation_failed, Toast.LENGTH_SHORT).show();
    }

    public void compressFile(File newFile, ArrayList<FileInfo> files) {
        int mode = checkWriteAccessMode(mActivity, newFile.getParentFile());
        if (mode == 2) {
            mActivity.mNewFilePath = newFile.getAbsolutePath();
            mActivity.mFiles = files;
            mActivity.mOperation = FileConstants.COMPRESS;
        } else if (mode == 1) {
            Intent zipIntent = new Intent(mActivity, CreateZipTask.class);
            zipIntent.putExtra("name", newFile.getAbsolutePath());
            zipIntent.putParcelableArrayListExtra("files", files);
            new FileUtils().showZipProgressDialog(mActivity, zipIntent);
        } else Toast.makeText(mActivity, R.string.msg_operation_failed, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("ConstantConditions")
    private void showSAFDialog(String path) {

        String title = mActivity.getString(R.string.needsaccess);
        String texts[] = new String[]{title, mActivity.getString(R.string.open), "", mActivity.getString(R.string
                .dialog_cancel)};
        final MaterialDialog materialDialog = new DialogUtils().showCustomDialog(mActivity, R.layout.dialog_saf, texts);
        View view = materialDialog.getCustomView();
        TextView textView = (TextView) view.findViewById(R.id.description);
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.sd_operate_step);
        textView.setText(mActivity.getString(R.string.needs_access_summary, path));

        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                triggerStorageAccessFramework();
                materialDialog.dismiss();
            }
        });

        materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mActivity, R.string.error, Toast.LENGTH_SHORT).show();
                materialDialog.dismiss();
            }
        });

        materialDialog.show();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void triggerStorageAccessFramework() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (mActivity.getPackageManager().resolveActivity(intent, 0) != null) {
            mActivity.startActivityForResult(intent, BaseActivity.SAF_REQUEST);
        } else {
            Toast.makeText(mActivity, mActivity.getString(R.string.msg_error_not_supported), Toast.LENGTH_LONG).show();
        }
    }


    public int checkWriteAccessMode(Context context, final File folder) {
        if (Utils.isAtleastLollipop() && FileUtils.isOnExtSdCard(folder, context)) {
            if (!folder.exists() || !folder.isDirectory()) {
                return FileConstants.WRITE_MODES.ROOT.getValue();
            }

            if (FileUtils.isFileNonWritable(folder, context)) {
                // On Android 5 and above, trigger storage access framework.
                showSAFDialog(folder.getAbsolutePath());
                return FileConstants.WRITE_MODES.EXTERNAL.getValue();
            }
            return FileConstants.WRITE_MODES.INTERNAL.getValue();
        } else if (Utils.isKitkat() && FileUtils.isOnExtSdCard(folder, mActivity)) {
            // Assume that Kitkat workaround works
            return FileConstants.WRITE_MODES.INTERNAL.getValue();
        } else if (FileUtils.isWritable(new File(folder, "DummyFile"))) {
            return FileConstants.WRITE_MODES.INTERNAL.getValue();
        } else {
            return FileConstants.WRITE_MODES.ROOT.getValue();
        }
    }


}
