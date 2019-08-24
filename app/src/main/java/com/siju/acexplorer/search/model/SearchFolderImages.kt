package com.siju.acexplorer.search.model

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.siju.acexplorer.common.types.FileInfo
import com.siju.acexplorer.main.model.data.DataFetcher
import com.siju.acexplorer.main.model.data.recent.RecentCursorData
import com.siju.acexplorer.main.model.data.recent.RecentUtils
import com.siju.acexplorer.main.model.groups.Category
import java.util.*

class SearchFolderImages : DataFetcher {

    override fun fetchData(context: Context, path: String?, category: Category): ArrayList<FileInfo> {
        val showHidden = canShowHiddenFiles(context)
        val cursor = fetchImagesInFolder(context, path, showHidden)
        return RecentCursorData.getDataFromCursor(cursor, category, showHidden)
    }

    override fun fetchCount(context: Context, path: String?): Int {
        val cursor = fetchImagesInFolder(context, path, canShowHiddenFiles(context))
        return getCursorCount(cursor)
    }

    private fun fetchImagesInFolder(context: Context, path: String?, showHidden: Boolean): Cursor? {
        if (path == null) {
            return null
        }
        val uri = MediaStore.Files.getContentUri("external")
        val selection = RecentUtils.getImagesMediaType() +
                " AND " + MediaStore.Files.FileColumns.DATA + " LIKE ? "
        val selectionArgs = arrayOf("$path%")
        val sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        return context.contentResolver.query(uri, null, selection, selectionArgs,
                sortOrder)
    }
}