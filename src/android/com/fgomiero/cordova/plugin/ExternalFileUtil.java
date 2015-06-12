/**
 *
 */
package com.fgomiero.cordova.plugin;

import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;

import android.content.Context;

import org.json.JSONObject;

import android.content.ActivityNotFoundException;

/**
 * @author Fabio Gomiero
 *
 */
public class ExternalFileUtil extends CordovaPlugin {

	/* (non-Javadoc)
	 * @see org.apache.cordova.CordovaPlugin#execute(java.lang.String, org.json.JSONArray, org.apache.cordova.CallbackContext)
	 */
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		try {
            if (action.equals("openWith")) {
				JSONObject options = args.getJSONObject(2);
				String title = null;
				if(options != null && options.has("name")) {
					title = options.getString("name");
				}
                openFile(args.getString(0), args.getString(1), title, callbackContext);
            }
        } catch (JSONException e) {
        	callbackContext.error(e.getLocalizedMessage());
        } catch (IOException e) {
            callbackContext.error(e.getLocalizedMessage());
        }
		return false;
	}

	private Uri copyFile(String url, String fileName, Context context) throws IOException {

		// Create URI
		//https://github.com/Smile-SA/cordova-plugin-fileopener/blob/master/src/android/FileOpener.java

		File externalFolder = context.getExternalCacheDir();
		if(externalFolder == null) {
			throw new FileNotFoundException();
		}

		File tempFile = null;
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		Boolean success = false;

		try {
			String path[] = url.split("/");
			if(fileName == null) {
				fileName = path[path.length-1];
			}

			String folderName = path[path.length-2];
			File tempFolder = new File(externalFolder + "/" + folderName); //the user folder
			if(tempFolder.exists() || tempFolder.mkdir()) {
				tempFile = new File(tempFolder, fileName); //the file we will create

				//open streams
				inStream = new FileInputStream(new File(url));
				outStream = new FileOutputStream(tempFile);
				//create channel
				inChannel = inStream.getChannel();
				outChannel = outStream.getChannel();
				//copy file
				inChannel.transferTo(0, inChannel.size(), outChannel);
				success = true;
			} else {
				throw new FileNotFoundException();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			if (inStream != null) inStream.close();
			if (outStream != null) outStream.close();
			if (inChannel != null) inChannel.close();
			if (outChannel != null) outChannel.close();
		}

		if(success) {
			return Uri.fromFile(tempFile);
		} else {
			throw new IOException("File Copy Failed");
		}
	}

	/**
	 *
	 * @param url
	 * @throws IOException
	 */
	private void openFile(String url, String type, String fileName, CallbackContext callbackContext) throws IOException, JSONException {

		Context context = cordova.getActivity().getApplicationContext();
		JSONObject obj = new JSONObject();

		url = url.replace("file:///", "/");
		Uri tempUri = null;

		try {
			tempUri = copyFile(url, fileName, context);
		} catch (IOException e) {
			obj.put("message",  "Error writing " + url + " to external storage");
			callbackContext.error(obj);
			return;
		}

		if(tempUri == null) {
			obj.put("message", "Failed to view the file, could not move to shared storage");
			callbackContext.error(obj);
			return;
		}

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//String type = "*/*";

		//intent = new Intent(Intent.ACTION_VIEW);
		// Check what kind of file you are trying to open, by comparing the url with extensions.
		// When the if condition is matched, plugin sets the correct intent (mime) type,
		// so Android knew what application to use to open the file

		// if (url.contains(".doc") || url.contains(".docx")) { // Word document
		// 	type = "application/msword";
		// } else if (url.contains(".pdf")) { // PDF file
		// 	type = "application/pdf";
		// } else if (url.contains(".ppt") || url.contains(".pptx")) { // Powerpoint file
		// 	type = "application/vnd.ms-powerpoint";
		// } else if (url.contains(".xls") || url.contains(".xlsx")) { // Excel file
		// 	type = "application/vnd.ms-excel";
		// } else if (url.contains(".rtf")) { // RTF file
		// 	type = "application/rtf";
		// } else if (url.contains(".wav")) { // WAV audio file
		// 	type = "audio/x-wav";
		// } else if (url.contains(".gif")) { // GIF file
		// 	type = "image/gif";
		// } else if (url.contains(".jpg") || url.contains(".jpeg")) { // JPG file
		// 	type = "image/jpeg";
		// } else if (url.contains(".png")) { // JPG file
		// 	type = "image/png";
		// } else if (url.contains(".txt")) { // Text file
		// 	type = "text/plain";
		// } else if (url.contains(".mpg") || url.contains(".mpeg")
		// 		|| url.contains(".mpe") || url.contains(".mp4")
		// 		|| url.contains(".avi")) { // Video files
		// 	type = "video/*";
		// }

		// if you want you can also define the intent type for any other file
		// additionally use else clause below, to manage other unknown extensions
		// in this case, Android will show all applications installed on the device
		// so you can choose which application to use
		//
		// else {
		// }

		intent.setDataAndType(tempUri, type);

		try {
			context.startActivity(intent);
			//this.cordova.getActivity().startActivity(intent);
            obj.put("message", "File successfully opened");
            callbackContext.success(obj);
        } catch (ActivityNotFoundException e) {
            obj.put("message", "Failed to view the file, no viewer found");
			obj.put("ActivityNotFoundException", e.getMessage());
			callbackContext.error(obj);
        }
	}
}
