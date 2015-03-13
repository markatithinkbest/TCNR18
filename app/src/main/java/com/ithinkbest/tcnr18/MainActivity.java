package com.ithinkbest.tcnr18;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public final String LOG_TAG = "FINAL";
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    class AsyncTaskFetchCloudData extends AsyncTask<Void, Void, Void> {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */


        @Override
        protected Void doInBackground(Void... params) {
            fetchCloudData();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

//            getContentResolver().delete(MembersProvider.CONTENT_URL,"",null);
            String[] projection={MembersProvider.COLUMN_NICKNAME};
            Cursor cursor=getContentResolver().query(MembersProvider.CONTENT_URL, projection, "", null, "NICKNAME");
            String members="";
            String nickname="";
            ArrayList<String> memberList=new ArrayList<>();
            if (cursor.moveToFirst()){
                 nickname=cursor.getString(0);
                memberList.add(nickname);
                while (cursor.moveToNext()){
                    nickname=cursor.getString(0);
                    memberList.add(nickname);
                }
            }
            for (String str:memberList){
                Log.d(LOG_TAG,"onPostExecute members = "+ str);

            }
        }
/** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
//        protected void onPostExecute(Bitmap result) {
//            mImageView.setImageBitmap(result);
//        }
    }
    private void fetchCloudData(){
        Log.d(LOG_TAG, "Starting sync");
      //  String locationQuery = Utility.getPreferredLocation(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;
        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String BASE_URL =
                    "http://opensource-forever.com/final/list.php";


            URL url = new URL(BASE_URL);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
            updateSQLite(buffer.toString());
        //    getWeatherDataFromJson(forecastJsonStr, locationQuery);
            Log.d(LOG_TAG, forecastJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
//        } catch (JSONException e) {
//            Log.e(LOG_TAG, e.getMessage(), e);
//            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    private void updateSQLite(String str){
        JSONArray jsonArray=null;
        try {
            jsonArray=new JSONArray(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jsonArray.length()<1)return;
        int cnt=getContentResolver().delete(MembersProvider.CONTENT_URL,"",null);
        Log.d(LOG_TAG,"delete cnt= "+cnt);


        for (int i=0;i<jsonArray.length();i++){
            try {
                JSONObject jsonObject=jsonArray.getJSONObject(i);

                // *** NEED TO PAY ATTENTION TO ID
                int  memberid=jsonObject.getInt("ID");
                String username=jsonObject.getString(MembersProvider.COLUMN_USERNAME);
                String nickname=jsonObject.getString(MembersProvider.COLUMN_NICKNAME);
                String email=jsonObject.getString(MembersProvider.COLUMN_EMAIL);
                String grp=jsonObject.getString(MembersProvider.COLUMN_GRP);



                Log.d(LOG_TAG,"memberid,username => "+memberid+","+username);
                ContentValues values = new ContentValues();

                values.put(MembersProvider.COLUMN_MEMBERID, memberid);
                values.put(MembersProvider.COLUMN_USERNAME, username);
                values.put(MembersProvider.COLUMN_NICKNAME, nickname);
                values.put(MembersProvider.COLUMN_EMAIL, email);
                values.put(MembersProvider.COLUMN_GRP, grp);


                // Provides access to other applications Content Providers
                Uri uri = getContentResolver().insert(MembersProvider.CONTENT_URL, values);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }



        // Stores a key value pair
//        ContentValues values = new ContentValues();
//        values.put(MembersProvider.COLUMN_USERNAME, "aaa");
//
//        // Provides access to other applications Content Providers
//        Uri uri = getContentResolver().insert(MembersProvider.CONTENT_URL, values);

//        Toast.makeText(getBaseContext(), "New Contact Added", Toast.LENGTH_LONG)
//                .show();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //
        Log.d(LOG_TAG,"position is "+position);
        if (position==2){
            new AsyncTaskFetchCloudData().execute();


        }




        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

      //
            Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner);
// Create an ArrayAdapter using the string array and a default spinner layout
//            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
//                    R.array.planets_array, android.R.layout.simple_spinner_item);

//            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
//                    R.array.planets_array, android.R.layout.simple_spinner_item);
            String[] memberList={"AAA","BBB","CCC"};
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),  android.R.layout.simple_spinner_item, memberList);

// Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
            spinner.setAdapter(adapter);




            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
