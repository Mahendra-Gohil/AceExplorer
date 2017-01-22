package com.siju.acexplorer.common;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.siju.acexplorer.filesystem.FileConstants;
import com.siju.acexplorer.filesystem.model.FavInfo;
import com.siju.acexplorer.filesystem.model.LibrarySortModel;
import com.siju.acexplorer.filesystem.utils.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SharedPreferenceWrapper {

    private static final String PREFS_NAME = "PREFS";
    private static final String FAVORITES = "Product_Favorite";
    private static final String LIBRARIES = "Library";

    private static final String PREFS_VIEW_MODE = "view-mode";

    public SharedPreferenceWrapper() {
        super();
    }

    // This four methods are used for maintaining favorites.
    private void saveFavorites(Context context, List<FavInfo> favorites) {
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonFavorites = gson.toJson(favorites);

        editor.putString(FAVORITES, jsonFavorites);

        editor.apply();
    }

    public void addFavorite(Context context, FavInfo favInfo) {
        List<FavInfo> favorites = getFavorites(context);
        if (favorites == null)
            favorites = new ArrayList<>();
        if (!favorites.contains(favInfo)) {
            favorites.add(favInfo);
            saveFavorites(context, favorites);
        }
    }

    public void removeFavorite(Context context, FavInfo favInfo) {
        ArrayList<FavInfo> favorites = getFavorites(context);
        if (favorites != null) {
            favorites.remove(favInfo);
            saveFavorites(context, favorites);
        }
    }

    /**
     * Reset favourites to initial state
     *
     * @param context
     */
    public void resetFavourites(Context context) {
        ArrayList<FavInfo> favorites = getFavorites(context);
        if (favorites != null) {
            for (int i = favorites.size() - 1; i >= 0; i--) {
                if (!favorites.get(i).getFilePath().equalsIgnoreCase(FileUtils
                        .getDownloadsDirectory().getAbsolutePath())) {
                    Logger.log("TAG", "Fav path=" + favorites.get(i).getFilePath());
                    favorites.remove(i);
                }
            }

           /* for (FavInfo info : favorites) {
                if (!info.getFilePath().equalsIgnoreCase(FileUtils.getDownloadsDirectory()
                .getAbsolutePath())) {
                    favorites.remove(info);
                }
            }*/
            saveFavorites(context, favorites);
        }
    }

    public ArrayList<FavInfo> getFavorites(Context context) {
        SharedPreferences settings;
        ArrayList<FavInfo> favorites = new ArrayList<>();

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        if (settings.contains(FAVORITES)) {
            String jsonFavorites = settings.getString(FAVORITES, null);
            Gson gson = new Gson();
            FavInfo[] favoriteItems = gson.fromJson(jsonFavorites,
                    FavInfo[].class);
            favorites.addAll(Arrays.asList(favoriteItems));
        }

        return favorites;
    }

    public void addLibrary(Context context, LibrarySortModel librarySortModel) {
        List<LibrarySortModel> libraries = getLibraries(context);
        if (libraries == null)
            libraries = new ArrayList<>();
        if (!libraries.contains(librarySortModel)) {
            libraries.add(librarySortModel);
            saveLibrary(context, libraries);
        }
        Logger.log("SIJU", "addLibrary=" + libraries.size());

    }


    public ArrayList<LibrarySortModel> getLibraries(Context context) {
        SharedPreferences settings;
        ArrayList<LibrarySortModel> libraries = new ArrayList<>();

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        if (settings.contains(LIBRARIES)) {
            String jsonFavorites = settings.getString(LIBRARIES, null);
            Gson gson = new Gson();
            LibrarySortModel[] libItems = gson.fromJson(jsonFavorites,
                    LibrarySortModel[].class);
            libraries.addAll(Arrays.asList(libItems));

        }

        return libraries;
    }

    public void savePrefs(Context context, int viewMode) {
        SharedPreferences sharedPreferences;
        SharedPreferences.Editor editor;

        sharedPreferences = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putInt(PREFS_VIEW_MODE, viewMode);
        editor.apply();
    }

    public void saveLibrary(Context context, List<LibrarySortModel> librarySortModel) {
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonFavorites = gson.toJson(librarySortModel);
        Logger.log("SIJU", "Save library=" + jsonFavorites);
        editor.putString(LIBRARIES, jsonFavorites);

        editor.apply();
    }


// --Commented out by Inspection START (06-11-2016 11:08 PM):
//    public void removeLibrary(Context context, LibrarySortModel librarySortModel) {
//        ArrayList<LibrarySortModel> libraries = getLibraries(context);
//        if (libraries != null) {
//            libraries.remove(librarySortModel);
//            saveLibrary(context, libraries);
//        }
//    }
// --Commented out by Inspection STOP (06-11-2016 11:08 PM)

    public int getViewMode(Context context) {
        SharedPreferences sharedPreferences;
        int mode;

        sharedPreferences = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        if (sharedPreferences.contains(PREFS_VIEW_MODE)) {
            mode = sharedPreferences.getInt(PREFS_VIEW_MODE, FileConstants.KEY_LISTVIEW);
        } else {
            return FileConstants.KEY_LISTVIEW;
        }
        return mode;
    }


}