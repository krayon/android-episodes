/*
 * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redcoracle.episodes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import com.redcoracle.episodes.tvdb.Client;
import com.redcoracle.episodes.tvdb.Show;

import java.util.List;

public class AddShowSearchFragment
	extends ListFragment
	implements LoaderManager.LoaderCallbacks<List<Show>>
{
	public static AddShowSearchFragment newInstance(String query) {
		AddShowSearchFragment instance = new AddShowSearchFragment();

		Bundle args = new Bundle();
		args.putString("query", query);

		instance.setArguments(args);
		return instance;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.add_show_search_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		String query = getArguments().getString("query");
		Bundle loaderArgs = new Bundle();
		loaderArgs.putString("query", query);

		LoaderManager.getInstance(this).initLoader(0, loaderArgs, this);
	}

	@NonNull
	@Override
	public Loader<List<Show>> onCreateLoader(int id, Bundle args) {

		getActivity().findViewById(R.id.search_progress_bar).setVisibility(View.VISIBLE);
		getActivity().findViewById(R.id.search_no_results_found).setVisibility(View.GONE);
        return new SearchLoader(getActivity(), args.getString("query"));
	}

	@Override
	public void onLoadFinished(Loader<List<Show>> loader, List<Show> data) {
		AddShowSearchResults results = AddShowSearchResults.getInstance();
		results.setData(data);

		Activity activity = getActivity();
		SearchResultsAdapter adapter = null;

		if (data != null) {
			if (data.size() == 0) {
				activity.findViewById(R.id.search_no_results_found).setVisibility(View.VISIBLE);
			}
			adapter = new SearchResultsAdapter(activity, data);
		}
		activity.findViewById(R.id.search_progress_bar).setVisibility(View.GONE);
		setListAdapter(adapter);
	}

	@Override
	public void onLoaderReset(Loader<List<Show>> loader) {
		AddShowSearchResults results = AddShowSearchResults.getInstance();
		results.setData(null);
		setListAdapter(null);
	}

	private static class SearchLoader extends AsyncTaskLoader<List<Show>> {
		private final String query;
		private List<Show> cachedResult;
		private final SharedPreferences preferences = Preferences.getSharedPreferences();

		SearchLoader(Context context, String query) {
			super(context);

			this.query = query;
			cachedResult = null;
		}

		@Override
		public List<Show> loadInBackground() {
			Client tvdbClient = new Client();
			String language = preferences.getString("pref_language", "en");

			List<Show> results = tvdbClient.searchShows(query, language);

			// If there are no results, try searching all languages or substituting &
			if(results.size() == 0 && query.contains(" and ")) {
				results = tvdbClient.searchShows(query.replace(" and ", " & "), "all");
			}
			if(results.size() == 0) {
				results =  tvdbClient.searchShows(query, "all");
			}

			return results;
		}

		@Override
		public void deliverResult(List<Show> data) {
			cachedResult = data;

			if (isStarted()) {
				super.deliverResult(data);
			}
		}

		@Override
		public void onStartLoading() {
			if (cachedResult != null) {
				deliverResult(cachedResult);
			} else {
				forceLoad();
			}
		}

		@Override
		public void onStopLoading() {
			cancelLoad();
		}

		@Override
		public void onReset() {
			onStopLoading();
			cachedResult = null;
		}
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), AddShowPreviewActivity.class);
		intent.putExtra("searchResultIndex", position);
		startActivity(intent);
	}

	private static class SearchResultsAdapter extends ArrayAdapter<Show> {
		private final LayoutInflater inflater;

		SearchResultsAdapter(Context context, List<Show> objects) {
			super(context, 0, 0, objects);
			inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@NonNull
        @Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.add_show_search_results_list_item, parent, false);
			}

			TextView textView = convertView.findViewById(R.id.show_name_view);
			textView.setText(getItem(position).getName());

			return convertView;
		}
	}
}
