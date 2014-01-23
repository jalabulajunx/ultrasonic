/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package com.thejoshwa.ultrasonic.androidapp.view;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.thejoshwa.ultrasonic.androidapp.R;
import com.thejoshwa.ultrasonic.androidapp.domain.MusicDirectory.Entry;
import com.thejoshwa.ultrasonic.androidapp.service.DownloadFile;
import com.thejoshwa.ultrasonic.androidapp.service.DownloadService;
import com.thejoshwa.ultrasonic.androidapp.service.DownloadServiceImpl;
import com.thejoshwa.ultrasonic.androidapp.service.MusicService;
import com.thejoshwa.ultrasonic.androidapp.service.MusicServiceFactory;
import com.thejoshwa.ultrasonic.androidapp.util.Util;
import com.thejoshwa.ultrasonic.androidapp.util.VideoPlayerType;

import java.io.File;

/**
 * Used to display songs in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class SongView extends UpdateView implements Checkable
{

	private static final String TAG = SongView.class.getSimpleName();
	private static Drawable starHollowDrawable;
	private static Drawable starDrawable;
	private static Drawable unpinImage;
	private static Drawable downloadedImage;
	private static Drawable downloadingImage;
	private static Drawable playingImage;
	private static String theme;
	private static LayoutInflater inflater;

	private Entry song;
	private Context context;
	private Drawable leftImage;
	private ImageType previousLeftImageType;
	private ImageType previousRightImageType;
	private ImageType leftImageType;
	private ImageType rightImageType;
	private Drawable rightImage;
	private DownloadService downloadService;
	private DownloadFile downloadFile;
	private boolean playing;
	private EntryAdapter.SongViewHolder viewHolder;

	public SongView(Context context)
	{
		super(context);
		this.context = context;

		String theme = Util.getTheme(context);
		boolean themesMatch = theme.equals(SongView.theme);
		inflater = LayoutInflater.from(this.context);

		if (!themesMatch)
		{
			SongView.theme = theme;
		}

		if (starHollowDrawable == null || !themesMatch)
		{
			starHollowDrawable = Util.getDrawableFromAttribute(context, R.attr.star_hollow);
		}

		if (starDrawable == null || !themesMatch)
		{
			starDrawable = Util.getDrawableFromAttribute(context, R.attr.star_full);
		}

		if (unpinImage == null || !themesMatch)
		{
			unpinImage = Util.getDrawableFromAttribute(context, R.attr.unpin);
		}

		if (downloadedImage == null || !themesMatch)
		{
			downloadedImage = Util.getDrawableFromAttribute(context, R.attr.downloaded);
		}

		if (downloadingImage == null || !themesMatch)
		{
			downloadingImage = Util.getDrawableFromAttribute(context, R.attr.downloading);
		}

		if (playingImage == null || !themesMatch)
		{
			playingImage = Util.getDrawableFromAttribute(context, R.attr.media_play_small);
		}
	}

	public void setLayout(final Entry song)
	{
		inflater.inflate(song.isVideo() ? R.layout.video_list_item : R.layout.song_list_item, this, true);
		viewHolder = new EntryAdapter.SongViewHolder();
		viewHolder.check = (CheckedTextView) findViewById(R.id.song_check);
		viewHolder.star = (ImageView) findViewById(R.id.song_star);
		viewHolder.drag = (ImageView) findViewById(R.id.song_drag);
		viewHolder.track = (TextView) findViewById(R.id.song_track);
		viewHolder.title = (TextView) findViewById(R.id.song_title);
		viewHolder.artist = (TextView) findViewById(R.id.song_artist);
		viewHolder.duration = (TextView) findViewById(R.id.song_duration);
		viewHolder.status = (TextView) findViewById(R.id.song_status);
		setTag(viewHolder);
	}

	public void setViewHolder(EntryAdapter.SongViewHolder viewHolder)
	{
		this.viewHolder = viewHolder;
		setTag(this.viewHolder);
	}

	public Entry getEntry()
	{
		return this.song;
	}

	protected void setSong(final Entry song, boolean checkable, boolean draggable)
	{
		updateBackground();

		this.song = song;

		if (downloadService != null)
		{
			this.downloadFile = downloadService.forSong(song);
		}

		StringBuilder artist = new StringBuilder(60);

		String bitRate = null;

		if (song.getBitRate() != null)
		{
			bitRate = String.format(this.context.getString(R.string.song_details_kbps), song.getBitRate());
		}

		String fileFormat;
		String suffix = song.getSuffix();
		String transcodedSuffix = song.getTranscodedSuffix();

		fileFormat = transcodedSuffix == null || transcodedSuffix.equals(suffix) || (song.isVideo() && Util.getVideoPlayerType(this.context) != VideoPlayerType.FLASH) ? suffix : String.format("%s > %s", suffix, transcodedSuffix);

		String artistName = song.getArtist();

		if (artistName != null)
		{
			if (Util.shouldDisplayBitrateWithArtist(this.context))
			{
				artist.append(artistName).append(" (").append(String.format(this.context.getString(R.string.song_details_all), bitRate == null ? "" : String.format("%s ", bitRate), fileFormat)).append(')');
			}
			else
			{
				artist.append(artistName);
			}
		}

		int trackNumber = song.getTrack();

		if (viewHolder.track != null)
		{
			if (Util.shouldShowTrackNumber(this.context) && trackNumber != 0)
			{
				viewHolder.track.setText(String.format("%02d.", trackNumber));
			}
			else
			{
				viewHolder.track.setVisibility(View.GONE);
			}
		}

		viewHolder.title.setText(song.getTitle());

		if (viewHolder.artist != null)
		{
			viewHolder.artist.setText(artist);
		}

		Integer duration = song.getDuration();

		if (duration != null)
		{
			viewHolder.duration.setText(Util.formatTotalDuration(duration));
		}

		if (viewHolder.check != null)
		{
			viewHolder.check.setVisibility(checkable && !song.isVideo() ? View.VISIBLE : View.GONE);
		}

		if (viewHolder.drag != null)
		{
			viewHolder.drag.setVisibility(draggable ? View.VISIBLE : View.GONE);
		}

		if (Util.isOffline(this.context))
		{
			viewHolder.star.setVisibility(View.GONE);
		}
		else
		{
			viewHolder.star.setImageDrawable(song.getStarred() ? starDrawable : starHollowDrawable);

			viewHolder.star.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					final boolean isStarred = song.getStarred();
					final String id = song.getId();

					if (!isStarred)
					{
						viewHolder.star.setImageDrawable(starDrawable);
						song.setStarred(true);
					}
					else
					{
						viewHolder.star.setImageDrawable(starHollowDrawable);
						song.setStarred(false);
					}

					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							MusicService musicService = MusicServiceFactory.getMusicService(SongView.this.context);

							try
							{
								if (!isStarred)
								{
									musicService.star(id, null, null, SongView.this.context, null);
								}
								else
								{
									musicService.unstar(id, null, null, SongView.this.context, null);
								}
							}
							catch (Exception e)
							{
								Log.e(TAG, e.getMessage(), e);
							}
						}
					}).start();
				}
			});
		}

		update();
	}

	@Override
	protected void updateBackground()
	{
		if (downloadService == null)
		{
			downloadService = DownloadServiceImpl.getInstance();
		}
	}

	@Override
	protected void update()
	{
		updateBackground();

		if (downloadService == null)
		{
			return;
		}

		downloadFile = downloadService.forSong(this.song);
		File partialFile = downloadFile.getPartialFile();

		if (downloadFile.isWorkDone())
		{
			ImageType newLeftImageType = downloadFile.isSaved() ? ImageType.unpin : ImageType.downloaded;

			if (this.leftImageType != newLeftImageType)
			{
				this.leftImage = downloadFile.isSaved() ? unpinImage : downloadedImage;
				this.leftImageType = newLeftImageType;
			}
		}
		else
		{
			this.leftImageType = ImageType.none;
			this.leftImage = null;
		}

		if (downloadFile.isDownloading() && !downloadFile.isDownloadCancelled() && partialFile.exists())
		{
			if (this.viewHolder.status != null)
			{
				this.viewHolder.status.setText(Util.formatLocalizedBytes(partialFile.length(), this.context));
			}

			this.rightImageType = ImageType.downloading;
			this.rightImage = downloadingImage;
		}
		else
		{
			this.rightImageType = ImageType.none;
			this.rightImage = null;

			if (this.viewHolder.status != null)
			{
				CharSequence statusText = this.viewHolder.status.getText();

				if (statusText != "" || statusText != null)
				{
					this.viewHolder.status.setText(null);
				}
			}
		}

		if (this.previousLeftImageType != leftImageType || this.previousRightImageType != rightImageType)
		{
			this.previousLeftImageType = leftImageType;
			this.previousRightImageType = rightImageType;

			if (this.viewHolder.status != null)
			{
				this.viewHolder.status.setCompoundDrawablesWithIntrinsicBounds(leftImage, null, rightImage, null);

				if (rightImage == downloadingImage)
				{
					AnimationDrawable frameAnimation = (AnimationDrawable) rightImage;
					frameAnimation.setVisible(true, true);
				}
			}
		}

		if (!song.getStarred())
		{
			if (viewHolder.star != null)
			{
				if (viewHolder.star.getDrawable() != starHollowDrawable)
				{
					viewHolder.star.setImageDrawable(starHollowDrawable);
				}
			}
		}
		else
		{
			if (viewHolder.star != null)
			{
				if (viewHolder.star.getDrawable() != starDrawable)
				{
					viewHolder.star.setImageDrawable(starDrawable);
				}
			}
		}

		boolean playing = downloadService.getCurrentPlaying() == downloadFile;

		if (playing)
		{
			if (!this.playing)
			{
				this.playing = true;
				viewHolder.title.setCompoundDrawablesWithIntrinsicBounds(playingImage, null, null, null);
			}
		}
		else
		{
			if (this.playing)
			{
				this.playing = false;
				viewHolder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}
	}

	@Override
	public void setChecked(boolean b)
	{
		viewHolder.check.setChecked(b);
	}

	@Override
	public boolean isChecked()
	{
		return viewHolder.check.isChecked();
	}

	@Override
	public void toggle()
	{
		viewHolder.check.toggle();
	}

	public enum ImageType
	{
		none,
		unpin,
		downloaded,
		downloading
	}
}
