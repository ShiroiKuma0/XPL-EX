package eu.faircode.xlua.utilities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import eu.faircode.xlua.AdapterConfig;
import eu.faircode.xlua.api.objects.xmock.ConfigSetting;
import eu.faircode.xlua.api.objects.xmock.phone.MockConfigConversions;
import eu.faircode.xlua.api.objects.xmock.phone.MockPhoneConfig;

public class FileDialogUtil {
    private static final String TAG = "XLua.FileDialogUtil";

    public static MockPhoneConfig readPhoneConfig(Context context, Uri selectedFileUri) {
        String contents = readAllFile(context, selectedFileUri);
        try {
            MockPhoneConfig config = new MockPhoneConfig();
            config.fromJSONObject(new JSONObject(contents));
            return config;
        }catch (JSONException ex) {
            Log.e(TAG, "Failed to read phone config: " + ex);
            return null;
        }
    }

    public static String readAllFile(Context context, Uri selectedFileUri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(selectedFileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append('\n');

        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + selectedFileUri.getPath() + " e=" + e);
        }

        return sb.toString();
    }

    public static boolean saveConfigSettings(Context context, Uri selectedFileUri, AdapterConfig config) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, selectedFileUri);
        if (pickedDir != null) {
            String fName = config.getConfigName();
            if(fName == null || fName.isEmpty())
                return false;

            DocumentFile newFile = pickedDir.createFile("application/json", fName + ".json");
            if (newFile != null) {
                try (OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri())) {
                    List<ConfigSetting> settings = config.getEnabledSettings();
                    if(settings == null || settings.isEmpty())
                        throw new IOException("Settings is Empty");

                    MockPhoneConfig mockConfig = new MockPhoneConfig();
                    mockConfig.setName(fName);
                    mockConfig.setSettings(MockConfigConversions.listToHashMapSettings(settings, false));

                    byte[] bys = mockConfig.toJSON().getBytes();;
                    assert out != null;
                    out.write(bys);
                    Log.i(TAG, "Config File written successfully: " + fName);
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Error writing to config file: " + fName + "\n" + e);
                }catch (JSONException e) {
                    Log.e(TAG, "Failed to Read Data from Config=" + fName);
                }
            }
        }

        return false;
    }
}
