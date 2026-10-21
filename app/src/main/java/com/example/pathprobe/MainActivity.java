package com.example.pathprobe;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
	private LinearLayout entriesView;
	private TextView deviceView;
	private TextView summaryView;
	private Button refreshButton;
	private PathCollector collector;
	private PathCollector.Report report;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		applyInsets();

		collector = new PathCollector(this);
		entriesView = findViewById(R.id.entries);
		deviceView = findViewById(R.id.device_info);
		summaryView = findViewById(R.id.summary);
		refreshButton = findViewById(R.id.refresh);

		refreshButton.setOnClickListener(view -> refresh());
		findViewById(R.id.copy_all).setOnClickListener(view -> copyReport());

		refresh();
	}

	private void refresh() {
		refreshButton.setEnabled(false);
		summaryView.setText(R.string.collecting);

		final PathCollector activeCollector = collector;
		new Thread(() -> {
			try {
				PathCollector.Report result = activeCollector.collect();
				runOnUiThread(() -> {
					if (isFinishing() || isDestroyed()) {
						return;
					}
					report = result;
					render(result);
					refreshButton.setEnabled(true);
				});
			} catch (RuntimeException error) {
				runOnUiThread(() -> {
					if (isFinishing() || isDestroyed()) {
						return;
					}
					summaryView.setText(getString(
							R.string.collect_failed, error.toString()));
					refreshButton.setEnabled(true);
				});
			}
		}, "path-collector").start();
	}

	private void render(PathCollector.Report result) {
		deviceView.setText(result.deviceLine + "\n" + result.appLine);
		summaryView.setText(result.summaryLine + "\ncollected " + result.collectedAt);

		entriesView.removeAllViews();
		for (Section section : result.sections) {
			addSectionHeader(section);
			for (PathEntry entry : section.entries) {
				addEntryRow(entry);
			}
		}
	}

	private void addSectionHeader(Section section) {
		TextView header = new TextView(this);
		header.setText(section.title.toUpperCase(Locale.US) + "  " + section.entries.size());
		header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
		header.setTypeface(Typeface.DEFAULT_BOLD);
		header.setLetterSpacing(0.08f);
		header.setTextColor(getColor(R.color.accent));
		header.setPadding(dp(4), dp(20), dp(4), dp(8));
		entriesView.addView(header);
	}

	private void addEntryRow(PathEntry entry) {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.VERTICAL);
		row.setPadding(dp(14), dp(12), dp(14), dp(12));
		row.setBackgroundResource(R.drawable.row_background);
		row.setClickable(true);
		row.setFocusable(true);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		params.bottomMargin = dp(8);
		row.setLayoutParams(params);

		TextView label = new TextView(this);
		label.setText(entry.label);
		label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		label.setTypeface(Typeface.DEFAULT_BOLD);
		label.setTextColor(getColor(R.color.text_primary));
		row.addView(label);

		TextView path = new TextView(this);
		path.setText(entry.path());
		path.setTypeface(Typeface.MONOSPACE);
		path.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		path.setTextColor(getColor(R.color.text_path));
		path.setPadding(0, dp(3), 0, dp(3));
		row.addView(path);

		TextView status = new TextView(this);
		status.setText(PathStatus.describe(this, entry.file));
		status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		status.setTextColor(PathStatus.colorFor(this, entry.file));
		row.addView(status);

		row.setOnClickListener(view -> copyPath(entry));
		entriesView.addView(row);
	}

	private void copyPath(PathEntry entry) {
		if (entry.file == null) {
			Toast.makeText(this, R.string.no_path, Toast.LENGTH_SHORT).show();
			return;
		}
		copyToClipboard(entry.file.getAbsolutePath(), getString(R.string.path_copied));
	}

	private void copyReport() {
		if (report == null) {
			Toast.makeText(this, R.string.not_ready, Toast.LENGTH_SHORT).show();
			return;
		}
		copyToClipboard(report.toPlainText(this), getString(R.string.report_copied));
	}

	private void copyToClipboard(String text, String message) {
		ClipboardManager clipboard =
				(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboard != null) {
			clipboard.setPrimaryClip(ClipData.newPlainText("Path Probe", text));
		}
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@SuppressWarnings("deprecation")
	private void applyInsets() {
		View root = findViewById(R.id.root);
		root.setOnApplyWindowInsetsListener((view, insets) -> {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				applyModernInsets(view, insets);
			} else {
				view.setPadding(
						insets.getSystemWindowInsetLeft(),
						insets.getSystemWindowInsetTop(),
						insets.getSystemWindowInsetRight(),
						insets.getSystemWindowInsetBottom());
			}
			return insets;
		});
		root.requestApplyInsets();
	}

	@TargetApi(Build.VERSION_CODES.R)
	private static void applyModernInsets(View view, WindowInsets insets) {
		android.graphics.Insets bars = insets.getInsets(
				WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
		view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
	}

	private int dp(int value) {
		return Math.round(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				value,
				getResources().getDisplayMetrics()));
	}
}
