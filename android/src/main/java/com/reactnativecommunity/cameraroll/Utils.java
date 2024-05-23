package com.reactnativecommunity.cameraroll;

import android.webkit.MimeTypeMap;

public class Utils {

    public static String getMimeType(String url) {
        String type = null;
        String extension = null;
        int dotPos = url.lastIndexOf('.');
        if (0 <= dotPos) {
            extension = url.substring(dotPos + 1);
        }
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static String getExtension(String mimeType) {
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return extension;
    }

}
