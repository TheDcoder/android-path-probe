package com.example.pathprobe;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Collects every public framework API that returns a file path. */
final class PathCollector {
	private static final String[] EXTERNAL_TYPES = {
			Environment.DIRECTORY_MUSIC,
			Environment.DIRECTORY_PODCASTS,
			Environment.DIRECTORY_RINGTONES,
			Environment.DIRECTORY_ALARMS,
			Environment.DIRECTORY_NOTIFICATIONS,
			Environment.DIRECTORY_PICTURES,
			Environment.DIRECTORY_MOVIES,
			Environment.DIRECTORY_DOWNLOADS,
			Environment.DIRECTORY_DCIM,
			Environment.DIRECTORY_DOCUMENTS,
			Environment.DIRECTORY_SCREENSHOTS,
			Environment.DIRECTORY_AUDIOBOOKS,
			Environment.DIRECTORY_RECORDINGS
	};

	private final Context context;

	PathCollector(Context context) {
		this.context = context;
	}

	@SuppressWarnings("deprecation")
	Report collect() {
		List<Section> sections = new ArrayList<>();

		Section internal = new Section("App internal storage");
		internal.add("getFilesDir()", context.getFilesDir());
		internal.add("getCacheDir()", context.getCacheDir());
		internal.add("getNoBackupFilesDir()", context.getNoBackupFilesDir());
		internal.add("getCodeCacheDir()", context.getCodeCacheDir());
		internal.add("getDatabasePath(\"probe.db\")", context.getDatabasePath("probe.db"));
		internal.add("getFileStreamPath(\"probe.txt\")", context.getFileStreamPath("probe.txt"));
		internal.add("getObbDir()", context.getObbDir());
		internal.addAll("getObbDirs()", context.getObbDirs());
		sections.add(internal);

		Section external = new Section("App external storage");
		external.add("getExternalFilesDir(null)", context.getExternalFilesDir(null));
		for (String type : EXTERNAL_TYPES) {
			external.add("getExternalFilesDir(\"" + type + "\")",
					context.getExternalFilesDir(type));
		}
		external.add("getExternalCacheDir()", context.getExternalCacheDir());
		external.addAll("getExternalFilesDirs(null)", context.getExternalFilesDirs(null));
		external.addAll("getExternalCacheDirs()", context.getExternalCacheDirs());
		external.addAll("getExternalMediaDirs()", context.getExternalMediaDirs());
		sections.add(external);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			Context deviceProtected = context.createDeviceProtectedStorageContext();
			Section device = new Section("Device-protected storage (API 24+)");
			device.add("getFilesDir()", deviceProtected.getFilesDir());
			device.add("getCacheDir()", deviceProtected.getCacheDir());
			device.add("getNoBackupFilesDir()", deviceProtected.getNoBackupFilesDir());
			device.add("getCodeCacheDir()", deviceProtected.getCodeCacheDir());
			device.add("getDatabasePath(\"probe.db\")",
					deviceProtected.getDatabasePath("probe.db"));
			device.add("getObbDir()", deviceProtected.getObbDir());
			device.addAll("getObbDirs()", deviceProtected.getObbDirs());
			sections.add(device);
		}

		ApplicationInfo app = context.getApplicationInfo();
		Section metadata = new Section("Application metadata");
		metadata.add("getPackageCodePath()", new File(context.getPackageCodePath()));
		metadata.add("getPackageResourcePath()", new File(context.getPackageResourcePath()));
		metadata.add("ApplicationInfo.sourceDir", new File(app.sourceDir));
		metadata.add("ApplicationInfo.publicSourceDir", new File(app.publicSourceDir));
		metadata.add("ApplicationInfo.nativeLibraryDir", fileOrNull(app.nativeLibraryDir));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			metadata.add("ApplicationInfo.dataDir", fileOrNull(app.dataDir));
			metadata.add("ApplicationInfo.deviceProtectedDataDir",
					fileOrNull(app.deviceProtectedDataDir));
		}
		metadata.addAllPaths("ApplicationInfo.splitSourceDirs", app.splitSourceDirs);
		metadata.addAllPaths("ApplicationInfo.splitPublicSourceDirs", app.splitPublicSourceDirs);
		sections.add(metadata);

		Section environment = new Section("Environment");
		environment.add("getDataDirectory()", Environment.getDataDirectory());
		environment.add("getDownloadCacheDirectory()", Environment.getDownloadCacheDirectory());
		environment.add("getRootDirectory()", Environment.getRootDirectory());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			environment.add("getStorageDirectory()", Environment.getStorageDirectory());
		}
		environment.add("getExternalStorageDirectory() (deprecated)",
				Environment.getExternalStorageDirectory());
		sections.add(environment);

		Section publicDirs = new Section("Public external directories (deprecated)");
		for (String type : EXTERNAL_TYPES) {
			publicDirs.add("getExternalStoragePublicDirectory(\"" + type + "\")",
					Environment.getExternalStoragePublicDirectory(type));
		}
		sections.add(publicDirs);

		Section volumes = new Section("Storage volumes");
		StorageManager storageManager =
				(StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
		if (storageManager == null) {
			volumes.add("StorageManager", null);
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				int index = 0;
				for (StorageVolume volume : storageManager.getStorageVolumes()) {
					String label = "Volume " + index + " | " + volume.getDescription(context)
							+ " | " + volume.getState()
							+ (volume.isPrimary() ? " | primary" : "")
							+ (volume.isRemovable() ? " | removable" : " | internal");
					File dir = null;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
						dir = volume.getDirectory();
					}
					volumes.add(label, dir);
					index++;
				}
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				volumes.add("getPrimaryStorageVolume().getDirectory()",
						storageManager.getPrimaryStorageVolume().getDirectory());
			}
		}
		sections.add(volumes);

		int total = 0;
		int existing = 0;
		for (Section section : sections) {
			for (PathEntry entry : section.entries) {
				total++;
				if (entry.file != null && entry.file.exists()) {
					existing++;
				}
			}
		}

		StringBuilder summary = new StringBuilder();
		summary.append(total).append(" path APIs | ")
				.append(existing).append(" exist | external ")
				.append(Environment.getExternalStorageState())
				.append(" | emulated=").append(Environment.isExternalStorageEmulated())
				.append(" | removable=").append(Environment.isExternalStorageRemovable());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			summary.append(" | all-files=").append(Environment.isExternalStorageManager());
		}

		String deviceLine = Build.MANUFACTURER + " " + Build.MODEL
				+ " | Android " + Build.VERSION.RELEASE
				+ " (API " + Build.VERSION.SDK_INT + ")";
		String appLine = context.getPackageName() + " | UID " + Process.myUid()
				+ " | no storage permissions requested";

		return new Report(deviceLine, appLine, summary.toString(), sections);
	}

	private static File fileOrNull(String path) {
		return path == null ? null : new File(path);
	}

	static final class Report {
		final String deviceLine;
		final String appLine;
		final String summaryLine;
		final List<Section> sections;
		final String collectedAt;

		Report(String deviceLine, String appLine, String summaryLine, List<Section> sections) {
			this.deviceLine = deviceLine;
			this.appLine = appLine;
			this.summaryLine = summaryLine;
			this.sections = sections;
			this.collectedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
					.format(new Date());
		}

		String toPlainText(Context context) {
			StringBuilder out = new StringBuilder();
			out.append("Path Probe report\n");
			out.append(deviceLine).append('\n');
			out.append(appLine).append('\n');
			out.append(summaryLine).append('\n');
			out.append("collected: ").append(collectedAt).append('\n');

			for (Section section : sections) {
				out.append('\n').append("== ").append(section.title)
						.append(" (").append(section.entries.size()).append(") ==\n");
				for (PathEntry entry : section.entries) {
					out.append(entry.label).append('\n');
					out.append("  ").append(entry.path()).append('\n');
					out.append("  ")
							.append(PathStatus.describe(context, entry.file)
									.replace("\n", "\n  "))
							.append('\n');
				}
			}
			return out.toString();
		}
	}
}
