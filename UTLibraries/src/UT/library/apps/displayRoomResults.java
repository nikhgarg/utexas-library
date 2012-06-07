package UT.library.apps;


import java.util.ArrayList;

import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.IntentAction;

public class displayRoomResults extends Activity {


	ArrayList<ArrayList<Room>> allRooms;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		setContentView(R.layout.room_results2);

		LayoutInflater mInflater = LayoutInflater.from(this);

		View view = mInflater.inflate(R.layout.room_results2,null);
		LinearLayout linLayout = (LinearLayout) view.findViewById(R.id.roomsLinearLayout);

		// code downloaded from
		// https://github.com/johannilsson/android-actionbar/blob/master/README.md
		ActionBar actionBar = (ActionBar) linLayout.findViewById(R.id.actionbar);
		actionBar.setTitle("Room Results");
		actionBar.setBackgroundColor(Color.parseColor("#ff4500"));
		actionBar.setHomeAction(new IntentAction(this, new Intent(this,
				WelcomeScreen.class), R.drawable.book_image_placeholder)); // go home

		actionBar.addAction(new IntentAction(this, new Intent(this,
				settings.class), R.drawable.book_image_placeholder)); // go to
		// settings
		// ----------------------

		//log into UT direct with new client
		DefaultHttpClient client = new DefaultHttpClient();
		shared.logIntoUTDirect(this,client);

		String uri = createURIfromData(bundle);

		//TODO: implement this in a different thread
		String html = shared.retrieveProtectedWebPage(this,client, uri);

		//parse rooms page
		allRooms = parseRoomResults.extractRooms(html);

		//TODO:	display prettily. either improve listview or implement using tables
		displayAllRooms(allRooms, linLayout, mInflater);
		//		RoomBaseAdapter roomAdapter = new RoomBaseAdapter(this,allRooms);
		//		ListView listview = (ListView) findViewById(R.id.roomResultsListView);
		//		listview.setAdapter(roomAdapter);




	}

	public void displayAllRooms (ArrayList<ArrayList<Room>> rooms, LinearLayout linLayout, LayoutInflater mInflater)
	{
		Context context = this;

		try {

			int outer = 0, inner = 0; //setting tags in table rows so that know which button was pressed


//			setContentView(view);
//			linLayout.removeAllViews();
			for (ArrayList<Room> byLocation : rooms)
			{
				Log.i("displayRoomResults","in by Location");
				TextView tv = new TextView(context);
				tv.setText(byLocation.get(0).location);
				linLayout.addView(tv);

//				TextView tvv = new TextView(context);
//				tvv.setText(byLocation.get(0).location);
//				linLayout.addView(tvv);

				TableLayout table = new TableLayout(context);
				TableRow header = (TableRow) mInflater.inflate(R.layout.room_row, null);

				((TextView) header.findViewById(R.id.roomname))
				.setText("Room");
				((TextView) header.findViewById(R.id.hasFeatures))
				.setText("Has Requested Features");
				((TextView) header.findViewById(R.id.roomsize))
				.setText("Capacity");
				((TextView) header.findViewById(R.id.available))
				.setText("Available?");
				header.removeView(header.findViewById(R.id.reserveRoomButton));
				table.addView(header);

//				linLayout.add

				for (Room room: byLocation){

					Log.i("displayRoomResults","in room");


					TableRow roomRow = (TableRow) mInflater.inflate(R.layout.room_row, null);

					((TextView) roomRow.findViewById(R.id.roomname))
					.setText(room.room);
					((TextView) roomRow.findViewById(R.id.hasFeatures))
					.setText(room.reqFeatures);
					((TextView) roomRow.findViewById(R.id.roomsize))
					.setText(room.seating);
					((TextView) roomRow.findViewById(R.id.available))
					.setText(room.available);

					((Button)roomRow.findViewById(R.id.reserveRoomButton)).setTag(new int[]{outer, inner});

					table.addView(roomRow);
					inner++;
				}
				linLayout.addView(table);
				outer++;
			}
		}

		catch (Exception e) {
			Log.e("displayRoomResults", "Exception in displayRooms",e);
//			TextView tv = new TextView(this);
//			tv.setText(e.toString());
//			setContentView(tv);
		}
		ScrollView sv = new ScrollView(context);
		sv.addView(linLayout);
		setContentView(sv);

//		setContentView(view);

	}

	public void roomSelected(View view)
	{
		int[]tag = (int[])view.getTag();
		Room selected = allRooms.get(tag[0]).get(tag[1]);
		String roomLink = selected.reserveLink;
		roomLink = roomLink.substring(roomLink.indexOf("roomID"));
		String id = roomLink.substring(roomLink.indexOf("=")+1, roomLink.indexOf("&"));

		//just sending bundle from last intent, with room id appended
		Bundle bundle = new Bundle();
		bundle.putString("roomID", id);
		bundle.putString("year", year);
		bundle.putString("month", month);
		bundle.putString("day", day);


		Intent intent = new Intent(this,finalizeRoomReservation.class);
		intent.putExtras(bundle);
		startActivity(intent);

	}
	//needed for next activity - finalizing reservation
	String year;
	String month;
	String day;

	public String createURIfromData(Bundle bundle)
	{
		int [] date = bundle.getIntArray("Date");
		int number = bundle.getInt("number");
		boolean [] building = bundle.getBooleanArray("building");
		boolean [] roomreq = bundle.getBooleanArray("room");

		try {

			Uri.Builder build = new Uri.Builder();
			build.scheme("http");
			build.path("//www.lib.utexas.edu/studyrooms/index.php");

			build.appendQueryParameter("year", ""+date[0]);
			this.year = ""+date[0];
			date[1]++; //add 1 (month was 0 indexed, need 1 indexed)
			String month =""+ ((date[1]>9)?date[1]:"0"+date[1]); //add 0 to month if less than 10
			build.appendQueryParameter("month", month);
			this.month = month;
			String day =""+ ((date[2]>9)?date[2]:"0"+date[2]); //add 0 to day if less than 10
			build.appendQueryParameter("day", day);
			this.day = day;
			int startHour = date[3];

			//round startMinute to nearest 15 minutes
			int startMinute = date[4];
			startMinute = (int) (Math.round(startMinute/15.0))*15;
			if (startMinute==60){
				startMinute=0;
				startHour = (startHour+1)%24;
			}
			String startPm = "am";
			if (startHour>=12)
			{
				startPm = "pm";
				if (startHour>12)
					startHour-=12;
			}
			else if (startHour==0)
				startHour = 12;
			build.appendQueryParameter("startHour", ""+startHour);

			build.appendQueryParameter("startMinute", ""+startMinute);
			build.appendQueryParameter("isStartPM", startPm);

			int endHour = date[5];

			//round endMinute to nearest 15 minutes
			int endMinute = date[6];
			endMinute = (int) (Math.round(endMinute/15.0))*15;
			if(endMinute==60)
			{
				endMinute = 0;
				endHour = (endHour +1)%24;
			}

			String endPm = "am";
			if (endHour>=12)
			{
				endPm = "pm";
				if (endHour>12)
					endHour-=12;
			}
			else if (endHour==0)
				endHour = 12;
			build.appendQueryParameter("endHour", ""+endHour);


			build.appendQueryParameter("endMinute", ""+endMinute);

			build.appendQueryParameter("isEndPM", endPm);

			if (building[0])
				build.appendQueryParameter("building", "any");
			else
				for(int i=1;i<building.length;i++)
					if (building[i])
					{
						build.appendQueryParameter("building" + (i-1), "on");
					}

			build.appendQueryParameter("numPeople", ""+(number==0?0:(number+1)));

			for(int i=0;i<roomreq.length;i++)
				if (roomreq[i])
				{
					build.appendQueryParameter("option" + (i), "on");
				}

			build.appendQueryParameter("mode", "search");


			Uri uri = build.build();
			return uri.toString();

		} catch (Exception e) {
			TextView tv = new TextView(this);
			tv.setText(e.toString());
			setContentView(tv);
			return null;
		}
	}
}