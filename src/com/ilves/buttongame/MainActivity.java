package com.ilves.buttongame;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.common.images.ImageManager.OnImageLoadedListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;

public class MainActivity extends BaseGameActivity implements
		OnClickListener,
		RoomUpdateListener,
		RealTimeMessageReceivedListener,
		RoomStatusUpdateListener,
		OnInvitationReceivedListener,
		OnImageLoadedListener {

	public static final String		TAG						= "Game";

	boolean							mExplicitSignOut		= false;
	boolean							mInSignInFlow			= false;
	// set to true when you're in the middle of the sign in flow, to know you
	// should not attempt to connect on onStart()

	private String					mIncomingInvitationId;
	private GameThread				gameThread;
	private int						score;
	public int						opponentScore;
	private int						gameTime;

	private SignInButton			mSignInButton;
	private Button					mSignOutButton;
	private Button					mGameAchievementsButton;
	private Button					mGameLeaderboardButton;
	private Button					mGameMatchesButton;
	private TextView				mPlayerName;
	private ImageView				mPlayerImage;

	private ArrayList<Integer>		buttonList;

	// GamesClient mGamesClient; // initialized in onCreate

	// request code (can be any number, as long as it's unique)
	final static int				RC_SELECT_PLAYERS		= 10000;
	final static int				RC_INVITATION_INBOX		= 10001;
	static final int				RC_WAITING_ROOM			= 10002;
	private static final int		REQUEST_ACHIEVEMENTS	= 2001;
	private static final int		REQUEST_LEADERBOARD		= 2002;
	private static final boolean	DEBUG_BUILD				= true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// set requested clients (games and cloud save)
		setRequestedClients(BaseGameActivity.CLIENT_GAMES);
		// |BaseGameActivity.CLIENT_APPSTATE);

		// enable debug log, if applicable

		if (DEBUG_BUILD) {
			enableDebugLog(true);
		}
		// call BaseGameActivity's onCreate()
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// game-specific logic:
		mPlayerName = (TextView) findViewById(R.id.player_name);
		mPlayerImage = (ImageView) findViewById(R.id.player_image);

		mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
		mSignInButton.setOnClickListener(this);
		mSignOutButton = (Button) findViewById(R.id.sign_out_button);
		mSignOutButton.setOnClickListener(this);

		mGameAchievementsButton = (Button) findViewById(R.id.game_achievements);
		mGameAchievementsButton.setOnClickListener(this);
		mGameLeaderboardButton = (Button) findViewById(R.id.game_leaderboard);
		mGameLeaderboardButton.setOnClickListener(this);
		mGameMatchesButton = (Button) findViewById(R.id.game_matches);
		mGameMatchesButton.setOnClickListener(this);

		buttonList = new ArrayList<Integer>();
		buttonList.add(R.id.button_one);
		buttonList.add(R.id.button_two);
		buttonList.add(R.id.button_three);
		buttonList.add(R.id.button_four);
		buttonList.add(R.id.button_five);
		buttonList.add(R.id.button_six);
		buttonList.add(R.id.button_seven);
		buttonList.add(R.id.button_eight);
		buttonList.add(R.id.button_nine);
		for (int i = 0; i < buttonList.size(); i++) {
			findViewById(buttonList.get(i)).setOnClickListener(this);
		}
		enableDebugLog(true);
		gameThread = new GameThread(this, buttonList);
		score = 500;
		((TextView) findViewById(R.id.text_score)).setText("Score: " + score);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_settings:
			// Show dialog to prompt user if want to log in
			debugLog("Settings");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		gameThread.isPlaying = false;
		super.onPause();
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.sign_in_button) {
			// start the asynchronous sign in flow
			beginUserInitiatedSignIn();
		} else if (id == R.id.sign_out_button) {
			// sign out.
			signOut();

			// show sign-in button, hide the sign-out button
			mSignInButton.setVisibility(View.VISIBLE);
			mSignOutButton.setVisibility(View.GONE);
			mGameMatchesButton.setEnabled(false);
			mGameAchievementsButton.setEnabled(false);
			mGameLeaderboardButton.setEnabled(false);
			mPlayerName.setText(getString(R.string.please_sign_in));
			mPlayerImage.setImageResource(R.drawable.ic_user);
		} else if (id == R.id.game_achievements) {
			startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()),
					REQUEST_ACHIEVEMENTS);
		} else if (id == R.id.game_leaderboard) {
			// Games.Leaderboards.submitScore(getApiClient(), LEADERBOARD_ID,
			// 1337);
			(new DialogHighscore()).show(getSupportFragmentManager(), "dialog");
			// startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(),
			// getString(R.string.leaderboard_5_sec_highscores)),
			// REQUEST_LEADERBOARD);
		} else if (id == R.id.game_matches) {
			// launch the intent to show the invitation inbox screen
			Intent intent = Games.Invitations.getInvitationInboxIntent(getApiClient());
			startActivityForResult(intent, RC_INVITATION_INBOX);
		} else {
			debugLog("Button");
			gameThread.buttonPressed(id);
		}
	}

	public void hit(int newScore, Integer oldButton, Integer newButton) {
		score = newScore;
		runOnUiThread(new HitRunnable(this, newScore, oldButton, newButton));
	}

	public void startGame(View v) {
		findViewById(R.id.start_overlay).setVisibility(View.GONE);
		int selectedRadioButtonId = ((RadioGroup) findViewById(R.id.radioGroup)).getCheckedRadioButtonId();
		String value = (String) ((RadioButton) findViewById(selectedRadioButtonId)).getText();
		gameTime = Integer.parseInt(value.split(" ")[0]) * 1000;
		gameThread.gameTime = gameTime;
		gameThread.isPlaying = true;
		gameThread.start();
	}

	public void gameEnded(int endScore) {
		debugLog("End score: " + endScore + " time: " + gameTime + " seconds");
		runOnUiThread(new GameEndRunnable(this, endScore, gameTime / 1000));
	}

	public void startOver(View v) {
		findViewById(R.id.score_overlay).setVisibility(View.GONE);
		findViewById(R.id.start_overlay).setVisibility(View.VISIBLE);
		((Button) findViewById(gameThread.currentButton)).setBackgroundResource(R.drawable.gametheme_btn_default_holo_light);
		// "Disable" all buttons
		// create a new game thread
		gameThread = new GameThread(this, buttonList);
	}

	public void submitScores(int score, int gameTime) {
		// Leaderboard
		if (mHelper.isSignedIn()) {
			switch (gameTime) {
			case 5:
				debugLog("5 sec: " + score);
				Games.Leaderboards.submitScore(mHelper.getApiClient(),
						getString(R.string.leaderboard_5_sec_highscores),
						score);
				break;
			case 10:
				debugLog("10 sec: " + score);
				Games.Leaderboards.submitScore(mHelper.getApiClient(),
						getString(R.string.leaderboard_10_sec_highscores),
						score);
				break;
			case 20:
				debugLog("20 sec: " + score);
				Games.Leaderboards.submitScore(mHelper.getApiClient(),
						getString(R.string.leaderboard_20_sec_highscores),
						score);
				break;
			default:
				break;
			}
			// Achievements
			Games.Achievements.unlock(mHelper.getApiClient(),
					getString(R.string.achievement_getting_started));
			Games.Achievements.increment(mHelper.getApiClient(),
					getString(R.string.achievement_10_games),
					1);
			Games.Achievements.increment(mHelper.getApiClient(),
					getString(R.string.achievement_20_games),
					1);
			Games.Achievements.increment(mHelper.getApiClient(),
					getString(R.string.achievement_50_games),
					1);
			Games.Achievements.increment(mHelper.getApiClient(),
					getString(R.string.achievement_100_games),
					1);
		}
	}

	/**
	 * GOOGLE PLAY SERVICES FUNCTIONS
	 */

	@Override
	public void onSignInFailed() {
		// Sign in has failed. So show the user the sign-in button.
		mSignInButton.setVisibility(View.VISIBLE);
		mSignOutButton.setVisibility(View.GONE);
		mPlayerName.setText(getString(R.string.please_sign_in));
		mPlayerImage.setImageResource(R.drawable.ic_user);
		Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onSignInSucceeded() {
		// show sign-out button, hide the sign-in button
		mSignInButton.setVisibility(View.GONE);
		mSignOutButton.setVisibility(View.VISIBLE);
		mGameMatchesButton.setEnabled(true);
		mGameAchievementsButton.setEnabled(true);
		mGameLeaderboardButton.setEnabled(true);
		Player p = Games.Players.getCurrentPlayer(getApiClient());
		Toast.makeText(this, p.getDisplayName(), Toast.LENGTH_SHORT).show();
		mPlayerName.setText(p.getDisplayName());
		ImageManager iManager = ImageManager.create(this);
		iManager.loadImage(this, p.getIconImageUri());

		// (your code here: update UI, enable functionality that depends on sign
		// in, etc)
		Log.i("Game", "registerInvitationListener");
		// getGamesClient().registerInvitationListener(this);
		// if (getInvitationId() != null) {
		// RoomConfig.Builder roomConfigBuilder =
		// makeBasicRoomConfigBuilder();
		// roomConfigBuilder.setInvitationIdToAccept(getInvitationId());
		// getGamesClient().joinRoom(roomConfigBuilder.build());
		//
		// // prevent screen from sleeping during handshake
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//
		// // go to game screen
		// }
	}

	public void openInvitationInbox(View v) {

		// launch the intent to show the invitation inbox screen
		// Intent intent = getApiClient().getInvitationInboxIntent();
		// this.startActivityForResult(intent, RC_INVITATION_INBOX);
	}

	public void invitePlayers(View v) {

		// launch the intent to show the invitation inbox screen
		// Intent intent = getApiClient().getSelectPlayersIntent(1, 3);
		// this.startActivityForResult(intent, RC_SELECT_PLAYERS);
	}

	@Override
	public void onActivityResult(int request, int response, Intent data) {
		super.onActivityResult(request, response, data);
		if (request == RC_INVITATION_INBOX) {
			if (response != Activity.RESULT_OK) {
				// canceled
				return;
			}

			// get the selected invitation
			Bundle extras = data.getExtras();
			Invitation invitation = extras.getParcelable(Multiplayer.EXTRA_INVITATION);

			// accept it!
			RoomConfig roomConfig = makeBasicRoomConfigBuilder().setInvitationIdToAccept(invitation.getInvitationId())
					.build();
			Games.RealTimeMultiplayer.join(getApiClient(), roomConfig);

			// prevent screen from sleeping during handshake
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			// go to game screen
		} else if (request == RC_SELECT_PLAYERS) {
			if (response != Activity.RESULT_OK) {
				// user canceled
				return;
			}
			// get the invitee list
			Bundle extras = data.getExtras();
			final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
			for (String string : invitees) {
				Log.i("Game", string);
			}
			// get auto-match criteria
			Bundle autoMatchCriteria = null;
			int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS,
					0);
			int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS,
					0);

			if (minAutoMatchPlayers > 0) {
				autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers,
						maxAutoMatchPlayers,
						0);
			} else {
				autoMatchCriteria = null;
			}

			// create the room and specify a variant if appropriate
			RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
			roomConfigBuilder.addPlayersToInvite(invitees);
			if (autoMatchCriteria != null) {
				roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
			}
			RoomConfig roomConfig = roomConfigBuilder.build();
			Games.RealTimeMultiplayer.create(getApiClient(), roomConfig);

			// prevent screen from sleeping during handshake
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	private RoomConfig.Builder makeBasicRoomConfigBuilder() {
		return RoomConfig.builder(this)
				.setMessageReceivedListener(this)
				.setRoomStatusUpdateListener(this);
	}

	@Override
	public void onInvitationReceived(Invitation invitation) {
		// show in-game popup to let user know of pending invitation
		AlertDialog.Builder mBuilder = new Builder(this).setPositiveButton("Accept",
				new AlertDialog.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
						roomConfigBuilder.setInvitationIdToAccept(mIncomingInvitationId);
						Games.RealTimeMultiplayer.join(getApiClient(),
								roomConfigBuilder.build());

						// prevent
						// screen
						// from
						// sleeping
						// during
						// handshake
						getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

						// now,
						// go
						// to
						// game
						// screen
					}
				});
		// store invitation for use when player accepts this invitation
		mIncomingInvitationId = invitation.getInvitationId();

	}

	@Override
	public void onConnectedToRoom(Room arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisconnectedFromRoom(Room arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeerDeclined(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeerInvitedToRoom(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeerJoined(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeerLeft(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeersConnected(Room room, List<String> peers) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPeersDisconnected(Room arg0, List<String> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRoomAutoMatching(Room arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRoomConnecting(Room arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRealTimeMessageReceived(RealTimeMessage arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		if (statusCode != GamesStatusCodes.STATUS_OK) {
			// let screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// show error message, return to the main screen
			// return;
		}
		// get waiting room intent
		Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(),
				room,
				Integer.MAX_VALUE);
		startActivityForResult(i, RC_WAITING_ROOM);
	}

	@Override
	public void onLeftRoom(int arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {
		if (statusCode != GamesStatusCodes.STATUS_OK) {
			// let screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// show error message, return to the main screen
		}
	}

	@Override
	public void onRoomCreated(int statusCode, Room room) {
		Log.i("Game", "onRoomCreated");
		if (statusCode != GamesStatusCodes.STATUS_OK) {
			Log.i("Game", "onRoomCreated fail: " + statusCode);
			// let screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// show error message, return to the main screen
			return;
		}

		// get waiting room intent
		Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(),
				room,
				Integer.MAX_VALUE);
		startActivityForResult(i, RC_WAITING_ROOM);
	}

	@Override
	public void onP2PConnected(String participantId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onP2PDisconnected(String participantId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onInvitationRemoved(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onImageLoaded(Uri arg0, Drawable arg1, boolean arg2) {
		// TODO Auto-generated method stub
		mPlayerImage.setImageDrawable(arg1);
	}

	public void debugLog(String message) {
		if (mDebugLog) {
			Log.i(TAG, message);
		}
	}

	private class DialogHighscore extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.highscore_dialog)
					.setItems(R.array.highscore_array,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									// The 'which' argument contains the index
									// position
									// of the selected item
									switch (which) {
									case 0:
										startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(),
												getString(R.string.leaderboard_5_sec_highscores)),
												REQUEST_LEADERBOARD);
										break;
									case 1:
										startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(),
												getString(R.string.leaderboard_10_sec_highscores)),
												REQUEST_LEADERBOARD);
										break;
									case 2:
										startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(),
												getString(R.string.leaderboard_20_sec_highscores)),
												REQUEST_LEADERBOARD);
										break;

									default:
										break;
									}
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									// User cancelled the dialog
								}
							});
			return builder.create();
		}

	}
}
